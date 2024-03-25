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
    foo { _, _ -> }

    // Green in K1, yellow in K2
    val z = gen()

    // Red in K1 & K2
    take(<!TYPE_MISMATCH!>z<!>)

    // Red in K1 & K2
    takeAny(<!TYPE_MISMATCH!>z<!>)

    // Green in K1, red in K2
    takeString(z)
}
