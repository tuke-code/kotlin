// ISSUE: KT-71416
// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

// FILE: A.kt
class A {
    private class Nested {
        fun foo() = "OK"
    }

    private inline fun privateFun() = <!IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_WARNING!>Nested()<!>.foo()
    internal inline fun internalInlineFun() = privateFun()
}

// FILE: main.kt
fun box(): String {
    return A().internalInlineFun()
}