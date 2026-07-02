// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// ISSUE: KT-87007
// LANGUAGE: +EagerLambdaAnalysis

fun <T> id(x: T): T = x

fun foo(block: SAM, block2: () -> Int) = "1"
fun foo(block: () -> Int, block2: () -> Unit) = "2"

fun interface SAM {
    fun run(): String
}

fun test() {
     foo(id { <!RETURN_TYPE_MISMATCH!>1<!> }) { 1 }
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, nullableType, samConversion, stringLiteral, typeParameter */
