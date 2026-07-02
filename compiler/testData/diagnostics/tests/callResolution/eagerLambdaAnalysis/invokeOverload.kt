// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound

object Type

operator fun Type.invoke(block: () -> Type): Int = 1

operator fun Type.invoke(block: () -> Unit): String = "(2)"

fun testLambdaReturnSelectsTypeInvoke() {
    val result = Type { Type }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, objectDeclaration, operator, propertyDeclaration, stringLiteral */
