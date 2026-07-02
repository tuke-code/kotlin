// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-87117

val (() -> Any).property: Int
    get() = 1

val (() -> Unit).property: String
    get() = "2"

fun test() {
    val result = { "" }.property
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, getter, integerLiteral, lambdaLiteral, localProperty,
propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
