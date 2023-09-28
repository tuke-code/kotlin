// RENDER_DIAGNOSTICS_FULL_TEXT
// MODULE: m1-common
// FILE: common.kt
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE_PARAMETER,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPE,
)
annotation class Ann

@Ann
expect class OnClass

expect class OnMember {
    @Ann
    fun onMember()
}

@Ann
expect class ViaTypealias

expect class MemberScopeViaTypealias {
    @Ann
    fun foo()
}

annotation class WithArg(val s: String)

@WithArg("str")
expect fun withDifferentArg()

expect fun inValueParam(@Ann arg: String)

expect fun <@Ann T> inTypeParam()

@get:Ann
expect val onGetter: String

expect fun onType(param: @Ann Any)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>class <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>OnClass<!><!>

actual class OnMember {
    actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onMember<!>() {}<!>
}

class ViaTypealiasImpl

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>ViaTypealias<!> = ViaTypealiasImpl<!>

class MemberScopeViaTypealiasImpl {
    fun foo() {}
}
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>MemberScopeViaTypealias<!> = MemberScopeViaTypealiasImpl<!>

@WithArg("other str")
actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>withDifferentArg<!>() {}<!>

actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inValueParam<!>(arg: String) {}<!>

actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <T> <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>inTypeParam<!>() {}<!>

actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>val <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onGetter<!>: String = ""<!>

actual <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>onType<!>(param: Any) {}<!>
