/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin.generators

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.plugin.fqn
import org.jetbrains.kotlin.fir.types.constructStarProjectedType
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallablePath
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/*
 * Generates `dummyClassName(value: ClassName): String` function for each class annotated with @DummyFunction
 */
class TopLevelDeclarationsGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PREDICATE = LookupPredicate.create { annotated("DummyFunction".fqn()) }
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }

    override fun generateFunctions(callablePath: CallablePath, context: MemberGenerationContext?): List<FirNamedFunctionSymbol> {
        if (context != null) return emptyList()
        val matchedClassSymbol = findMatchedClassForFunction(callablePath) ?: return emptyList()
        val function = createTopLevelFunction(Key, callablePath, session.builtinTypes.stringType.type) {
            valueParameter(Name.identifier("value"), matchedClassSymbol.constructStarProjectedType())
        }
        return listOf(function.symbol)
    }

    private fun findMatchedClassForFunction(callablePath: CallablePath): FirRegularClassSymbol? {
        // We generate only top-level functions
        if (callablePath.classId != null) return null
        return matchedClasses
            .filter { it.classId.packageFqName == callablePath.packageName }
            .firstOrNull { callablePath.callableName.identifier == it.classId.toDummyCallableName() }
    }

    private fun ClassId.toDummyCallableName(): String {
        return "dummy${shortClassName.identifier}"
    }

    override fun getTopLevelCallableIds(): Set<CallablePath> {
        return matchedClasses.mapTo(mutableSetOf()) {
            val classId = it.classId
            CallablePath(classId.packageFqName, Name.identifier(classId.toDummyCallableName()))
        }
    }

    object Key : GeneratedDeclarationKey()

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}
