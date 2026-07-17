package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isTopLevel
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsTopLevelTests(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - top level fun`() {
        val ktFile = inlineSourceCodeAnalysis.createKtFile(
            """
            fun topFun() {}
            class TopClass {
                fun classFun() {}
            }
        """.trimIndent()
        )

        analyze(ktFile) {
            val session = contextOf<KaSession>()
            assertTrue(session.isTopLevel(ktFile.getFunctionOrFail("topFun", session)))
            assertFalse(session.isTopLevel(ktFile.getClassOrFail("TopClass", session).getFunctionOrFail("classFun", session)))
        }
    }
}
