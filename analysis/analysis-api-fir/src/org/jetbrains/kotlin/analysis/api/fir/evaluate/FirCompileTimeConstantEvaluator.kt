/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.evaluate

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirEvaluatorResult
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionEvaluator
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.PrivateConstantEvaluatorAPI
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * An evaluator that transform numeric operation, such as div, into compile-time constant iff involved operands, such as explicit receiver
 * and the argument, are compile-time constant as well.
 */
internal object FirCompileTimeConstantEvaluator {
    @OptIn(PrivateConstantEvaluatorAPI::class)
    fun evaluate(
        fir: FirElement?,
        analysisSession: KaFirSession,
    ): FirLiteralExpression? =
        when (fir) {
            is FirExpression -> {
                val evaluatorResult = FirExpressionEvaluator.evaluateExpression(fir, analysisSession.useSiteSession)
                if (evaluatorResult is FirEvaluatorResult.DivisionByZero) {
                    throw ArithmeticException("/ by zero") // TODO: return error instead
                }
                val literalExpression = (evaluatorResult as? FirEvaluatorResult.Evaluated)?.result as? FirLiteralExpression
                literalExpression?.adaptToConstKind()
            }
            is FirNamedReference -> {
                fir.toResolvedPropertySymbol()?.toLiteralExpression(analysisSession)
            }
            else -> null
        }

    private fun FirPropertySymbol.toLiteralExpression(analysisSession: KaFirSession): FirLiteralExpression? {
        return if (isConst && isVal) {
            evaluate(resolvedInitializer, analysisSession)
        } else null
    }

    fun evaluateAsKtConstantValue(
        fir: FirElement,
        analysisSession: KaFirSession,
    ): KaConstantValue? {
        val evaluated = evaluate(fir, analysisSession) ?: return null

        val value = evaluated.value
        val psi = evaluated.psi as? KtElement
        return when (evaluated.kind) {
            ConstantValueKind.Byte -> KaConstantValue.KaByteConstantValue(value as Byte, psi)
            ConstantValueKind.Int -> KaConstantValue.KaIntConstantValue(value as Int, psi)
            ConstantValueKind.Long -> KaConstantValue.KaLongConstantValue(value as Long, psi)
            ConstantValueKind.Short -> KaConstantValue.KaShortConstantValue(value as Short, psi)

            ConstantValueKind.UnsignedByte -> KaConstantValue.KaUnsignedByteConstantValue(value as UByte, psi)
            ConstantValueKind.UnsignedInt -> KaConstantValue.KaUnsignedIntConstantValue(value as UInt, psi)
            ConstantValueKind.UnsignedLong -> KaConstantValue.KaUnsignedLongConstantValue(value as ULong, psi)
            ConstantValueKind.UnsignedShort -> KaConstantValue.KaUnsignedShortConstantValue(value as UShort, psi)

            ConstantValueKind.Double -> KaConstantValue.KaDoubleConstantValue(value as Double, psi)
            ConstantValueKind.Float -> KaConstantValue.KaFloatConstantValue(value as Float, psi)

            ConstantValueKind.Boolean -> KaConstantValue.KaBooleanConstantValue(value as Boolean, psi)
            ConstantValueKind.Char -> KaConstantValue.KaCharConstantValue(value as Char, psi)
            ConstantValueKind.String -> KaConstantValue.KaStringConstantValue(value as String, psi)
            ConstantValueKind.Null -> KaConstantValue.KaNullConstantValue(psi)


            ConstantValueKind.IntegerLiteral -> {
                val long = value as Long
                if (Int.MIN_VALUE < long && long < Int.MAX_VALUE) KaConstantValue.KaIntConstantValue(long.toInt(), psi)
                else KaConstantValue.KaLongConstantValue(long, psi)
            }

            ConstantValueKind.UnsignedIntegerLiteral -> {
                val long = value as ULong
                if (UInt.MIN_VALUE < long && long < UInt.MAX_VALUE) KaConstantValue.KaUnsignedIntConstantValue(long.toUInt(), psi)
                else KaConstantValue.KaUnsignedLongConstantValue(long, psi)
            }

            ConstantValueKind.Error -> errorWithFirSpecificEntries("Should not be possible to get from FIR tree", fir = fir)
        }
    }

    private fun FirLiteralExpression.adaptToConstKind(): FirLiteralExpression {
        return kind.toLiteralExpression(
            source,
            kind.convertToNumber(value) ?: value
        )
    }

    private fun ConstantValueKind.convertToNumber(value: Any?): Any? {
        if (value == null) {
            return null
        }
        return when (this) {
            ConstantValueKind.Boolean -> value as Boolean
            ConstantValueKind.Char -> value as Char
            ConstantValueKind.String -> value as String
            ConstantValueKind.Byte -> (value as Number).toByte()
            ConstantValueKind.Double -> (value as Number).toDouble()
            ConstantValueKind.Float -> (value as Number).toFloat()
            ConstantValueKind.Int -> (value as Number).toInt()
            ConstantValueKind.Long -> (value as Number).toLong()
            ConstantValueKind.Short -> (value as Number).toShort()
            ConstantValueKind.UnsignedByte -> (value as Number).toLong().toUByte()
            ConstantValueKind.UnsignedShort -> (value as Number).toLong().toUShort()
            ConstantValueKind.UnsignedInt -> (value as Number).toLong().toUInt()
            ConstantValueKind.UnsignedLong -> (value as Number).toLong().toULong()
            ConstantValueKind.UnsignedIntegerLiteral -> (value as Number).toLong().toULong()
            else -> null
        }
    }

    private fun ConstantValueKind.toLiteralExpression(source: KtSourceElement?, value: Any?): FirLiteralExpression =
        buildLiteralExpression(source, this, value, setType = false)
}
