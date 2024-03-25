// ISSUE: KT-64474

// MODULE: m1
// FILE: m1.kt

class Some<T>(val x: T)

// MODULE: m2(m1)
// FILE: m2.kt

fun foo(f: (Some<String>, String) -> Unit) {}

class Generic<T>

fun gen() = Generic<Some<String>>()

fun take(g: Generic<Some<*>>) {}

fun takeAny(g: Generic<Some<Any?>>) {}

fun takeString(g: Generic<Some<String>>) {}

// MODULE: m3(m2)
// FILE: m3.kt

fun test() {
    // Green in K1, red in K2
    foo { <!MISSING_DEPENDENCY_CLASS!>_<!>, _ -> }

    // Green in K1, yellow in K2
    val z = <!MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE!>gen<!>()

    // Red in K1 & K2
    take(<!ARGUMENT_TYPE_MISMATCH, MISSING_DEPENDENCY_CLASS!>z<!>)

    // Red in K1 & K2
    takeAny(<!ARGUMENT_TYPE_MISMATCH, MISSING_DEPENDENCY_CLASS!>z<!>)

    // Green in K1, red in K2
    takeString(<!ARGUMENT_TYPE_MISMATCH, MISSING_DEPENDENCY_CLASS!>z<!>)
}
