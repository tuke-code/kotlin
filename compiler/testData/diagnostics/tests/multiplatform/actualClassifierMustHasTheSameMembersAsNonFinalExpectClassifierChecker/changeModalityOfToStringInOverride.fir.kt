// MODULE: m1-common
// FILE: common.kt

expect open <!INCOMPATIBLE_MATCHING{JVM}, INCOMPATIBLE_MATCHING{JVM}!>class Foo<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo {
    final override fun toString() = "Foo"
}
