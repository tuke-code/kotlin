// MODULE: m1-common
// FILE: common.kt
annotation class Ann

expect <!INCOMPATIBLE_MATCHING{JVM}!>class WeakIncompatibility {
    @Ann
    <!INCOMPATIBLE_MATCHING{JVM}!>fun foo(p: String)<!>
}<!>

expect <!INCOMPATIBLE_MATCHING{JVM}!>class StrongIncompatibility {
    @Ann
    <!INCOMPATIBLE_MATCHING{JVM}!>fun foo(p: Int)<!>
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class WeakIncompatibilityImpl {
    fun foo(differentName: String) {}
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>WeakIncompatibility<!> = WeakIncompatibilityImpl

class StrongIncompatibilityImpl {
    fun foo(p: String) {} // Different param type
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>StrongIncompatibility<!> = StrongIncompatibilityImpl
