// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +EagerLambdaAnalysis, +CallCompletionRefinementsFor25, +UnitConversionsOnArbitraryExpressions, +InferThrowableTypeParameterToUpperBound

fun nestedFunctionalType(block: () -> Unit): Int = 1

fun nestedFunctionalType(block: () -> (() -> String)): String = "(2)"

fun returnString(): String = ""
fun returnInt(): Int = 1
fun returnUnit(): Unit = Unit
fun returnNothing(): Nothing = TODO()

fun testNestedFunctionalType() {
    val stringResult = nestedFunctionalType { ::returnString }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>stringResult<!>

    nestedFunctionalType { ::<!INAPPLICABLE_CANDIDATE!>returnInt<!> }

    val unitResult = nestedFunctionalType { ::<!INAPPLICABLE_CANDIDATE!>returnUnit<!> }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>unitResult<!>

    val nothingResult = nestedFunctionalType { ::returnNothing }
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>nothingResult<!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
localProperty, propertyDeclaration, stringLiteral */
