package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.objcexport.hasCompanionObject
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NeedsCompanionPropertyTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - no companion`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class NoCompanion {
            
            }
        """.trimIndent()
        )
        analyze(file) {
            val session = contextOf<KaSession>()
            assertFalse(session.hasCompanionObject(session.getClassOrFail(file, "NoCompanion")))
        }
    }

    @Test
    fun `test - empty companion`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """            
            class EmptyCompanion {
                companion object {}
            }
        """.trimIndent()
        )
        analyze(file) {
            val session = contextOf<KaSession>()
            assertTrue(session.hasCompanionObject(session.getClassOrFail(file, "EmptyCompanion")))
        }
    }

    @Test
    fun `test - simple companion`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            class SimpleCompanion {
                companion object {
                    const val a = 42
                }
            }
        """.trimIndent()
        )
        analyze(file) {
            val session = contextOf<KaSession>()
            assertTrue(session.hasCompanionObject(session.getClassOrFail(file, "SimpleCompanion")))
        }
    }
}
