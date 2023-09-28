// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class Ann

expect fun <@Ann A> inMethod()

expect fun <A, @Ann B> inMethodTwoParams()

expect class InClass<@Ann A>

expect class ViaTypealias<@Ann A>

expect class TypealiasParamNotAccepted<@Ann A>

expect <!INCOMPATIBLE_MATCHING{JVM}!>fun <@Ann A, @Ann B> withIncompatibility()<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <A> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethod<!>() {}<!>

actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <@Ann A, B> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inMethodTwoParams<!>() {}<!>

actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>InClass<!><A><!>

class ViaTypealiasImpl<@Ann A>

actual typealias ViaTypealias<A> = ViaTypealiasImpl<A>

class TypealiasParamNotAcceptedImpl<A>

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>TypealiasParamNotAccepted<!><@Ann A> = TypealiasParamNotAcceptedImpl<A><!>

actual fun <A> <!ACTUAL_WITHOUT_EXPECT!>withIncompatibility<!>() {}
