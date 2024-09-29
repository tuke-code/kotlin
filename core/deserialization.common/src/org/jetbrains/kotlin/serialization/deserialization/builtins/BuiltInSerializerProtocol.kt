/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.builtins

import org.jetbrains.kotlin.metadata.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import java.io.InputStream
import java.net.URL

object BuiltInSerializerProtocol : SerializerExtensionProtocol(
    ExtensionRegistryLite.newInstance().apply(BuiltInsProtoBuf::registerAllExtensions),
    BuiltInsProtoBuf.packageFqName,
    BuiltInsProtoBuf.constructorAnnotation,
    BuiltInsProtoBuf.classAnnotation,
    BuiltInsProtoBuf.functionAnnotation,
    functionExtensionReceiverAnnotation = null,
    BuiltInsProtoBuf.propertyAnnotation,
    BuiltInsProtoBuf.propertyGetterAnnotation,
    BuiltInsProtoBuf.propertySetterAnnotation,
    propertyExtensionReceiverAnnotation = null,
    propertyBackingFieldAnnotation = null,
    propertyDelegatedFieldAnnotation = null,
    BuiltInsProtoBuf.enumEntryAnnotation,
    BuiltInsProtoBuf.compileTimeValue,
    BuiltInsProtoBuf.parameterAnnotation,
    BuiltInsProtoBuf.typeAnnotation,
    BuiltInsProtoBuf.typeParameterAnnotation
) {
    const val BUILTINS_FILE_EXTENSION = "kotlin_builtins"
    const val DOT_DEFAULT_EXTENSION = ".$BUILTINS_FILE_EXTENSION"

    fun getBuiltInsFilePath(fqName: FqName): String =
        fqName.asString().replace('.', '/') + "/" + getBuiltInsFileName(
            fqName
        )

    // Do not throw an exception in case concurrent.kotlin_builtins file is not found,
    // It is only present in kotlin-stdlib starting from 2.1.0.
    private fun checkConcurrentBuiltInPackage(builtInFileName: String) =
        if (builtInFileName == "kotlin/concurrent/concurrent.kotlin_builtins") {
            null
        } else {
            error("Resource for builtin $builtInFileName not found")
        }

    fun loadBuiltInResource(builtInPackageFqName: FqName, classLoader: ClassLoader): URL? {
        val resourcePath = getBuiltInsFilePath(builtInPackageFqName)
        return classLoader.getResource(resourcePath) ?: checkConcurrentBuiltInPackage(resourcePath)
    }

    // Throws an IllegalStateException if a builtin file from the given builtInPackage cannot be loaded
    fun getBuiltInFileInputStream(builtInPackageFqName: FqName, inputStreamProvider: (String) -> InputStream?): InputStream? {
        val resourcePath = getBuiltInsFilePath(builtInPackageFqName)
        return inputStreamProvider(resourcePath) ?: checkConcurrentBuiltInPackage(resourcePath)
    }

    fun getBuiltInsFileName(fqName: FqName): String =
        shortName(fqName) + DOT_DEFAULT_EXTENSION

    private fun shortName(fqName: FqName): String =
        if (fqName.isRoot) "default-package" else fqName.shortName().asString()
}
