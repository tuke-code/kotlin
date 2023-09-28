// MODULE: m1-common
// FILE: common.kt

open class Base<T> {
    open <!INCOMPATIBLE_MATCHING{JVM}!>fun foo(t: T) {}<!>
}

expect open <!INCOMPATIBLE_MATCHING{JVM}!>class Foo : Base<String><!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base<String>() {
    final override fun foo(t: String) {}
}
