// MODULE: m1-common
// FILE: common.kt

expect class E01
expect <!INCOMPATIBLE_MATCHING{JVM}!>class E02()<!>
expect open <!INCOMPATIBLE_MATCHING{JVM}!>class E03<!>

expect <!INCOMPATIBLE_MATCHING{JVM}!>class E04 {
    constructor()
}<!>

expect <!INCOMPATIBLE_MATCHING{JVM}!>class E05(e: E01)<!>
expect <!INCOMPATIBLE_MATCHING{JVM}!>class E06 {
    constructor(e: E02)
}<!>

expect <!INCOMPATIBLE_MATCHING{JVM}!>interface I01<!>

expect class M01 {
    fun foo()
}

expect enum <!INCOMPATIBLE_MATCHING{JVM}!>class ENUM01<!>

expect annotation <!INCOMPATIBLE_MATCHING{JVM}!>class ANNO01<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

object ActualObject {
    fun foo() {}
}

actual typealias E01 = ActualObject
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E02<!> = ActualObject
actual typealias <!ACTUAL_WITHOUT_EXPECT!>E03<!> = ActualObject

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E04<!> = ActualObject

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E05<!> = ActualObject
actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E06<!> = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>I01<!> = ActualObject

actual typealias M01 = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>ENUM01<!> = ActualObject

actual typealias <!ACTUAL_WITHOUT_EXPECT!>ANNO01<!> = ActualObject
