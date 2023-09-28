// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Ann

expect fun inMethod(@Ann arg: String)

expect class InConstructor(@Ann arg: String)

expect <!INCOMPATIBLE_MATCHING{JVM}!>fun withIncopatibility(@Ann p1: String, @Ann p2: String)<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethod<!>(arg: String) {}<!>

actual class InConstructor <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>constructor(arg: String)<!><!> {}

actual fun <!ACTUAL_WITHOUT_EXPECT!>withIncopatibility<!>(p1: String) {}
