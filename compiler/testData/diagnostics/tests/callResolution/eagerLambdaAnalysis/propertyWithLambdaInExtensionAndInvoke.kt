// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound
// ISSUE: KT-87115

val (() -> Unit).propertyWithInvoke: (String) -> Int
    get() = { 1 }

val (() -> Int).propertyWithInvoke: (Int) -> String
    get() = { "(2)" }

fun testCallSyntax() {
    val result = { 8 }.propertyWithInvoke("")
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
}

fun testExplicitInvoke() {
    { 8 }.propertyWithInvoke.invoke(<!ARGUMENT_TYPE_MISMATCH!>""<!>)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetPropertyGetter, functionDeclaration, functionalType, getter,
integerLiteral, lambdaLiteral, localProperty, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
