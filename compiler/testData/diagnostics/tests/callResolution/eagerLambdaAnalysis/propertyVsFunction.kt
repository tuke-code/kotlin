// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound

val (() -> String).propertyOrFunction: () -> Int
    get() = { 1 }

fun (() -> Unit).propertyOrFunction(): String = "(2)"

fun test() {
    val result = with({ "" }) { propertyOrFunction() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>

    val result2 = { "" }.propertyOrFunction()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result2<!>

    val result3 = with({ TODO() }) { propertyOrFunction() }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result3<!>

    val result4 = { TODO() }.propertyOrFunction()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result4<!>
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, getter, integerLiteral,
lambdaLiteral, localProperty, propertyDeclaration, propertyWithExtensionReceiver, stringLiteral */
