// MODULE: m1-common
// FILE: common.kt
expect <!INCOMPATIBLE_MATCHING{JVM}!>class DefaultArgsInNestedClass {
    annotation <!INCOMPATIBLE_MATCHING{JVM}!>class Nested<!INCOMPATIBLE_MATCHING{JVM}!>(val p: String = "")<!><!>
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class DefaultArgsInNestedClassImpl {
    annotation class Nested(val p: String = "")
}

// Incompatible because of bug KT-31636
actual typealias DefaultArgsInNestedClass = DefaultArgsInNestedClassImpl
