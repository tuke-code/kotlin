// ISSUE: KT-64474

// MODULE: m1
// FILE: m1.kt

class Some(val x: Int)

// MODULE: m2(m1)
// FILE: m2.kt

fun foo(f: (Some, String) -> Unit) {}
fun bar(f: (Some) -> Unit) {}

fun baz() = Some(42)

// MODULE: m3(m2)
// FILE: m3.kt

fun test() {
    // Green in K1, yellow in K2
    foo { _, _ -> }

    // Green in K1, yellow in K2
    foo { some, str -> }

    // Red in K1 & K2 (K1 does not report MISSING_DEPENDENCY_CLASS_IN_LAMBDA_PARAMETER warning)
    foo { some, _ -> some.toString() }

    // Red (same) in K1 & K2
    foo { some: <!UNRESOLVED_REFERENCE!>Some<!>, _ -> }

    // Red (same) in K1 & K2
    bar(fun(s: <!UNRESOLVED_REFERENCE!>Some<!>) {})
}

// Red (same) in K1 & K2
fun test2() = baz()
