/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJava8Test
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJava8WithPsiClassReadingTest
import org.jetbrains.kotlin.jvm.compiler.javac.AbstractLoadJava8UsingJavacTest
import org.jetbrains.kotlin.resolve.calls.AbstractEnhancedSignaturesResolvedCallsTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite(args) {
        testGroup("compiler/tests-java8/tests-gen", "compiler/testData") {
            testClass<AbstractLoadJava8Test> {
                model("loadJava8/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
                model("loadJava8/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
                model("loadJava8/sourceJava", extension = "java", testMethod = "doTestSourceJava")
            }

            testClass<AbstractLoadJava8UsingJavacTest> {
                model(
                    "loadJava8/compiledJava",
                    extension = "java",
                    testMethod = "doTestCompiledJava",
                    excludeDirs = listOf("typeUseAnnotations", "typeParameterAnnotations")
                )
                model(
                    "loadJava8/sourceJava",
                    extension = "java",
                    testMethod = "doTestSourceJava",
                    excludeDirs = listOf("typeUseAnnotations", "typeParameterAnnotations")
                )
            }

            testClass<AbstractLoadJava8WithPsiClassReadingTest> {
                model(
                    "loadJava8/compiledJava",
                    extension = "java",
                    testMethod = "doTestCompiledJava",
                    excludeDirs = listOf("typeUseAnnotations", "typeParameterAnnotations")
                )
            }

            testClass<AbstractEnhancedSignaturesResolvedCallsTest> {
                model("resolvedCalls/enhancedSignatures")
            }
        }
    }
}
