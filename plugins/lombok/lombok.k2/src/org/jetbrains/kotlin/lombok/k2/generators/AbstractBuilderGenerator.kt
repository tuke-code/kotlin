/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.builder.buildConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.java.JavaScopeProvider
import org.jetbrains.kotlin.fir.java.declarations.*
import org.jetbrains.kotlin.fir.java.enhancement.FirJavaDeclarationList
import org.jetbrains.kotlin.fir.java.javaSymbolProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.load.java.structure.JavaType
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.AbstractBuilder
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Singular
import org.jetbrains.kotlin.lombok.k2.config.LombokService
import org.jetbrains.kotlin.lombok.k2.config.lombokService
import org.jetbrains.kotlin.lombok.k2.java.*
import org.jetbrains.kotlin.lombok.utils.LombokNames
import org.jetbrains.kotlin.lombok.utils.capitalize
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class AbstractBuilderGenerator<T : AbstractBuilder>(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private const val TO_BUILDER = "toBuilder"
    }

    protected val lombokService: LombokService
        get() = session.lombokService

    protected val builderClassCache: FirCache<FirClassSymbol<*>, FirJavaClass?, Nothing?> =
        session.firCachesFactory.createCache(::createAndInitializeBuilder)

    // Don't worry about multiple functions with the same name
    // Lombok checks by name,
    // and doesn't add a new function if a function with the same name already exists disregarding the number and types of parameters
    private val functionsCache: FirCache<FirClassSymbol<*>, Map<Name, FirJavaMethod>?, Nothing?> =
        session.firCachesFactory.createCache(::createFunctions)

    // It's used if already existing class clashes with the generated one
    // In this case, we need to add new methods to the existing builder class without creating a new one
    private val functionsForExistingClasses: MutableMap<FirClassSymbol<*>, MutableMap<Name, FirJavaMethod>> = mutableMapOf()

    protected abstract val builderModality: Modality

    protected abstract fun getBuilder(classSymbol: FirClassSymbol<*>): T?

    protected abstract fun constructBuilderType(builderClassId: ClassId): ConeClassLikeType

    protected abstract fun getBuilderType(builderClassSymbol: FirRegularClassSymbol): ConeKotlinType

    protected abstract fun MutableMap<Name, FirJavaMethod>.addBuilderMethodsIfNeeded(
        builder: T,
        classSymbol: FirClassSymbol<*>,
        builderClassSymbol: FirRegularClassSymbol,
        existingFunctionNames: Set<Name>,
    )

    protected abstract fun FirJavaClassBuilder.completeBuilder(classSymbol: FirClassSymbol<*>, builderClassSymbol: FirRegularClassSymbol)

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        return functionsCache.getValue(classSymbol)?.keys.orEmpty()
    }

    override fun getNestedClassifiersNames(classSymbol: FirClassSymbol<*>, context: NestedClassGenerationContext): Set<Name> {
        if (!classSymbol.isSuitableJavaClass()) return emptySet()
        val name = builderClassCache.getValue(classSymbol)?.name ?: return emptySet()
        return setOf(name)
    }

    override fun generateFunctions(callableId: CallableId, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        val classSymbol = context?.owner ?: return emptyList()
        return functionsCache.getValue(classSymbol)?.get(callableId.callableName)?.let { listOf(it.symbol) } ?: emptyList()
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (!owner.isSuitableJavaClass()) return null
        return builderClassCache.getValue(owner)?.symbol
    }

    private fun createFunctions(classSymbol: FirClassSymbol<*>): Map<Name, FirJavaMethod>? {
        // Containing classes are always created before their functions.
        // It means that if there are no existing functions, then the class is not created yet.
        val existingFunctions = functionsForExistingClasses[classSymbol]

        val builder = getBuilder(classSymbol) ?: return existingFunctions
        val functions = existingFunctions ?: mutableMapOf()
        val classId = classSymbol.classId
        val builderClassName = builder.builderClassName.replace("*", classId.shortClassName.asString())
        val builderClassId = classId.createNestedClassId(Name.identifier(builderClassName))

        val builderTypeRef = constructBuilderType(builderClassId).toFirResolvedTypeRef()
        val visibility = builder.visibility.toVisibility()
        val existingFunctionNames = classSymbol.declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().map { it.name }.toSet()

        functions.addIfNeeded(Name.identifier(builder.builderMethodName), existingFunctionNames) {
            classSymbol.createJavaMethod(
                it,
                valueParameters = emptyList(),
                returnTypeRef = builderTypeRef,
                visibility = visibility,
                modality = Modality.FINAL,
                dispatchReceiverType = null,
                isStatic = true
            )
        }

        if (builder.requiresToBuilder) {
            functions.addIfNeeded(Name.identifier(TO_BUILDER), existingFunctionNames) {
                classSymbol.createJavaMethod(
                    it,
                    valueParameters = emptyList(),
                    returnTypeRef = builderTypeRef,
                    visibility = visibility,
                    modality = Modality.FINAL,
                )
            }
        }

        return functions.takeIf { it.isNotEmpty() }
    }

    @OptIn(SymbolInternals::class)
    private fun createAndInitializeBuilder(classSymbol: FirClassSymbol<*>): FirJavaClass? {
        val javaClass = classSymbol.fir as? FirJavaClass ?: return null
        val builder = getBuilder(classSymbol) ?: return null
        val builderName = Name.identifier(builder.builderClassName.replace("*", classSymbol.name.asString()))
        val visibility = builder.visibility.toVisibility()

        fun MutableMap<Name, FirJavaMethod>.addNewMethods(symbol: FirRegularClassSymbol, existingFunctionNames: Set<Name>) {
            addBuilderMethodsIfNeeded(builder, classSymbol, symbol, existingFunctionNames)

            val fields = javaClass.declarations.filterIsInstance<FirJavaField>()
            for (field in fields) {
                when (val singular = lombokService.getSingular(field.symbol)) {
                    null -> {
                        addSetterMethodIfNeeded(builder, field, symbol, existingFunctionNames)
                    }
                    else -> {
                        addMethodsForSingularFieldsIfNeeded(builder, singular, field, symbol, existingFunctionNames)
                    }
                }
            }
        }

        val builderClassId = javaClass.classId.createNestedClassId(builderName)
        val existingBuilderClassSymbol = session.javaSymbolProvider?.getClassLikeSymbolByClassId(builderClassId)
        if (existingBuilderClassSymbol != null) {
            // Extend the existing class with the new methods
            val existingFunctionNames =
                existingBuilderClassSymbol.declarationSymbols.filterIsInstance<FirNamedFunctionSymbol>().mapTo(mutableSetOf()) { it.name }
            functionsForExistingClasses.getOrPut(existingBuilderClassSymbol) { mutableMapOf() }
                .addNewMethods(existingBuilderClassSymbol, existingFunctionNames)
            return null
        }

        val builderClass = classSymbol.createEmptyBuilderClass(
            session,
            builderName,
            visibility
        ) { symbol ->
            object : FirJavaDeclarationList {
                override val declarations: List<FirDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
                    val newMethods = mutableMapOf<Name, FirJavaMethod>().also { it.addNewMethods(symbol, emptySet()) }
                    listOf(symbol.createDefaultJavaConstructor(visibility)) + newMethods.values
                }
            }
        } ?: return null

        return builderClass
    }

    private fun MutableMap<Name, FirJavaMethod>.addSetterMethodIfNeeded(
        builder: AbstractBuilder,
        field: FirJavaField,
        builderClassSymbol: FirRegularClassSymbol,
        existingFunctionNames: Set<Name>,
    ) {
        val fieldName = field.name
        val setterName = fieldName.toMethodName(builder)
        addIfNeeded(setterName, existingFunctionNames) {
            builderClassSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(ConeLombokValueParameter(fieldName, field.returnTypeRef)),
                returnTypeRef = getBuilderType(builderClassSymbol).toFirResolvedTypeRef(),
                modality = Modality.FINAL,
                visibility = builder.visibility.toVisibility()
            )
        }
    }

    private fun MutableMap<Name, FirJavaMethod>.addMethodsForSingularFieldsIfNeeded(
        builder: AbstractBuilder,
        singular: Singular,
        field: FirJavaField,
        builderClassSymbol: FirRegularClassSymbol,
        existingFunctionNames: Set<Name>,
    ) {
        val fieldJavaTypeRef = field.returnTypeRef as? FirJavaTypeRef ?: return
        val javaClassifierType = fieldJavaTypeRef.type as? JavaClassifierType ?: return
        val typeName = (javaClassifierType.classifier as? JavaClass)?.fqName?.asString() ?: return

        val nameInSingularForm = (singular.singularName ?: field.name.identifier.singularForm)?.let(Name::identifier) ?: return

        val addMultipleParameterType: FirTypeRef
        val valueParameters: List<ConeLombokValueParameter>

        val fallbackParameterType = DummyJavaClassType.ObjectType.takeIf { javaClassifierType.isRaw }
        val source = builderClassSymbol.source?.fakeElement(KtFakeSourceElementKind.Enhancement)

        when (typeName) {
            in LombokNames.SUPPORTED_COLLECTIONS -> {
                val parameterType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(nameInSingularForm, parameterType.toRef(source))
                )

                val baseType = when (typeName) {
                    in LombokNames.SUPPORTED_GUAVA_COLLECTIONS -> JavaClasses.Iterable
                    else -> JavaClasses.Collection
                }

                addMultipleParameterType = DummyJavaClassType(baseType, typeArguments = listOf(parameterType))
                    .withProperNullability(singular.allowNull)
                    .toRef(source)
            }

            in LombokNames.SUPPORTED_MAPS -> {
                val keyType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                val valueType = javaClassifierType.parameterType(1) ?: fallbackParameterType ?: return
                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("key"), keyType.toRef(source)),
                    ConeLombokValueParameter(Name.identifier("value"), valueType.toRef(source)),
                )

                addMultipleParameterType = DummyJavaClassType(JavaClasses.Map, typeArguments = listOf(keyType, valueType))
                    .withProperNullability(singular.allowNull)
                    .toRef(source)
            }

            in LombokNames.SUPPORTED_TABLES -> {
                val rowKeyType = javaClassifierType.parameterType(0) ?: fallbackParameterType ?: return
                val columnKeyType = javaClassifierType.parameterType(1) ?: fallbackParameterType ?: return
                val valueType = javaClassifierType.parameterType(2) ?: fallbackParameterType ?: return

                valueParameters = listOf(
                    ConeLombokValueParameter(Name.identifier("rowKey"), rowKeyType.toRef(source)),
                    ConeLombokValueParameter(Name.identifier("columnKey"), columnKeyType.toRef(source)),
                    ConeLombokValueParameter(Name.identifier("value"), valueType.toRef(source)),
                )

                addMultipleParameterType = DummyJavaClassType(
                    JavaClasses.Table,
                    typeArguments = listOf(rowKeyType, columnKeyType, valueType)
                ).withProperNullability(singular.allowNull).toRef(source)
            }

            else -> return
        }

        val builderType = getBuilderType(builderClassSymbol).toFirResolvedTypeRef()
        val visibility = builder.visibility.toVisibility()

        addIfNeeded(nameInSingularForm.toMethodName(builder), existingFunctionNames) {
            builderClassSymbol.createJavaMethod(
                name = it,
                valueParameters,
                returnTypeRef = builderType,
                modality = Modality.FINAL,
                visibility = visibility
            )
        }

        addIfNeeded(field.name.toMethodName(builder), existingFunctionNames) {
            builderClassSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(ConeLombokValueParameter(field.name, addMultipleParameterType)),
                returnTypeRef = builderType,
                modality = Modality.FINAL,
                visibility = visibility
            )
        }

        addIfNeeded(Name.identifier("clear${field.name.identifier.capitalize()}"), existingFunctionNames) {
            builderClassSymbol.createJavaMethod(
                name = it,
                valueParameters = listOf(),
                returnTypeRef = builderType,
                modality = Modality.FINAL,
                visibility = visibility
            )
        }
    }

    /* Lombok doesn't add a generated method if a class already has a method with the same name.
       The number and types of parameters don't matter, see https://projectlombok.org/features/Builder#overview
       "Each listed generated element will be silently skipped if that element already exists (disregarding parameter counts and looking only at names)"
     */
    protected inline fun MutableMap<Name, FirJavaMethod>.addIfNeeded(
        functionName: Name,
        existingFunctionNames: Set<Name>,
        createJavaMethod: (name: Name) -> FirJavaMethod
    ) {
        if (functionName !in existingFunctionNames) {
            getOrPut(functionName) { createJavaMethod(functionName) }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirClassSymbol<*>.createEmptyBuilderClass(
        session: FirSession,
        name: Name,
        visibility: Visibility,
        declarationListProvider: (FirRegularClassSymbol) -> FirJavaDeclarationList,
    ): FirJavaClass? {
        val containingClass = this.fir as? FirJavaClass ?: return null
        val classId = containingClass.classId.createNestedClassId(name)
        val builderClassSymbol = FirRegularClassSymbol(classId)
        return buildJavaClass {
            containingClassSymbol = containingClass.symbol
            moduleData = containingClass.moduleData
            symbol = builderClassSymbol
            this.name = name
            isFromSource = true
            this.visibility = visibility
            this.modality = builderModality
            this.isStatic = true
            classKind = ClassKind.CLASS
            javaTypeParameterStack = containingClass.classJavaTypeParameterStack
            scopeProvider = JavaScopeProvider
            this.superTypeRefs += superTypeRefs
            val effectiveVisibility = containingClass.effectiveVisibility.lowerBound(
                visibility.toEffectiveVisibility(this@createEmptyBuilderClass, forClass = true),
                session.typeContext
            )
            isTopLevel = false
            status = FirResolvedDeclarationStatusImpl(
                visibility,
                builderModality,
                effectiveVisibility
            ).apply {
                this.isInner = !isTopLevel && !this@buildJavaClass.isStatic
                isCompanion = false
                isData = false
                isInline = false
                isFun = classKind == ClassKind.INTERFACE
            }

            declarationList = declarationListProvider(symbol)

            completeBuilder(this@createEmptyBuilderClass, builderClassSymbol)
        }
    }

    private fun Name.toMethodName(builder: AbstractBuilder): Name {
        val prefix = builder.setterPrefix
        return if (prefix.isNullOrBlank()) {
            this
        } else {
            Name.identifier("${prefix}${identifier.capitalize()}")
        }
    }

    private val String.singularForm: String?
        get() = StringUtil.unpluralize(this)

    private fun JavaClassifierType.parameterType(index: Int): JavaType? {
        return typeArguments.getOrNull(index)
    }

    private fun JavaType.withProperNullability(allowNull: Boolean): JavaType {
        return if (allowNull) makeNullable() else makeNotNullable()
    }
}

fun JavaType.makeNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.Nullable)
fun JavaType.makeNotNullable(): JavaType = withAnnotations(annotations + NullabilityJavaAnnotation.NotNull)

fun FirClassSymbol<*>.createJavaMethod(
    name: Name,
    valueParameters: List<ConeLombokValueParameter>,
    returnTypeRef: FirTypeRef,
    visibility: Visibility,
    modality: Modality,
    dispatchReceiverType: ConeSimpleKotlinType? = this.defaultType(),
    isStatic: Boolean = false,
): FirJavaMethod {
    return buildJavaMethod {
        containingClassSymbol = this@createJavaMethod
        moduleData = this@createJavaMethod.moduleData
        this.returnTypeRef = returnTypeRef
        this.dispatchReceiverType = dispatchReceiverType
        this.name = name
        symbol = FirNamedFunctionSymbol(CallableId(classId, name))
        status = FirResolvedDeclarationStatusImpl(visibility, modality, visibility.toEffectiveVisibility(this@createJavaMethod)).apply {
            this.isStatic = isStatic
        }
        isFromSource = true
        for (valueParameter in valueParameters) {
            this.valueParameters += buildJavaValueParameter {
                moduleData = this@createJavaMethod.moduleData
                this.returnTypeRef = valueParameter.typeRef
                containingFunctionSymbol = this@buildJavaMethod.symbol
                this.name = valueParameter.name
                isVararg = false
                isFromSource = true
            }
        }
    }.apply {
        if (isStatic) {
            containingClassForStaticMemberAttr = this@createJavaMethod.toLookupTag()
        }
    }
}

fun FirClassSymbol<*>.createDefaultJavaConstructor(
    visibility: Visibility,
): FirJavaConstructor {
    val outerClassSymbol = this
    return buildJavaConstructor {
        containingClassSymbol = outerClassSymbol
        moduleData = outerClassSymbol.moduleData
        isFromSource = true
        symbol = FirConstructorSymbol(classId)
        isInner = outerClassSymbol.rawStatus.isInner
        status = FirResolvedDeclarationStatusImpl(
            visibility,
            Modality.FINAL,
            visibility.toEffectiveVisibility(outerClassSymbol)
        ).apply {
            isExpect = false
            isActual = false
            isOverride = false
            isInner = this@buildJavaConstructor.isInner
        }
        isPrimary = false
        returnTypeRef = buildResolvedTypeRef {
            coneType = outerClassSymbol.defaultType()
        }
        dispatchReceiverType = if (isInner) outerClassSymbol.defaultType() else null
        typeParameters += outerClassSymbol.typeParameterSymbols.map { buildConstructedClassTypeParameterRef { symbol = it } }
    }
}

class ConeLombokValueParameter(val name: Name, val typeRef: FirTypeRef)
