/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.builtins.StandardNames.BACKING_FIELD
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.util.PersistentMultimap
import org.jetbrains.kotlin.name.Name

class FirLocalScope private constructor(
    private val properties: PersistentMap<Name, FirVariableSymbol<*>>,
    private val functions: PersistentMultimap<Name, FirNamedFunctionSymbol>,
    private val classLikes: PersistentMap<Name, FirClassLikeSymbol<*>>,
    private val useSiteSession: FirSession
) : FirContainingNamesAwareScope() {
    constructor(session: FirSession) : this(persistentMapOf(), PersistentMultimap(), persistentMapOf(), session)

    fun storeClass(klass: FirRegularClass, session: FirSession): FirLocalScope {
        return FirLocalScope(
            properties, functions, classLikes.put(klass.name, klass.symbol), session
        )
    }

    fun storeTypeAlias(typeAlias: FirTypeAlias, session: FirSession): FirLocalScope {
        return FirLocalScope(
            properties, functions, classLikes.put(typeAlias.name, typeAlias.symbol), session
        )
    }

    fun storeFunction(function: FirSimpleFunction, session: FirSession): FirLocalScope {
        return FirLocalScope(
            properties, functions.put(function.name, function.symbol), classLikes, session
        )
    }

    fun storeVariable(variable: FirVariable, session: FirSession): FirLocalScope {
        return FirLocalScope(
            properties.put(variable.name, variable.symbol), functions, classLikes, session
        )
    }

    fun storeBackingField(property: FirProperty, session: FirSession): FirLocalScope {
        val enhancedProperties = property.backingField?.symbol?.let {
            properties.put(BACKING_FIELD, it)
        }

        return FirLocalScope(
            enhancedProperties ?: properties,
            functions,
            classLikes,
            session
        )
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        for (function in functions[name]) {
            processor(function)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val property = properties[name]
        if (property != null) {
            processor(property)
        }
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        val classLike = classLikes[name]
        if (classLike != null) {
            val substitution = classLike.typeParameterSymbols.associateWith { it.toConeType() }
            processor(classLike, ConeSubstitutorByMap(substitution, useSiteSession))
        }
    }

    override fun mayContainName(name: Name) = properties.containsKey(name) || functions[name].isNotEmpty() || classLikes.containsKey(name)

    override fun getCallableNames(): Set<Name> = properties.keys + functions.keys
    override fun getClassifierNames(): Set<Name> = classLikes.keys
}
