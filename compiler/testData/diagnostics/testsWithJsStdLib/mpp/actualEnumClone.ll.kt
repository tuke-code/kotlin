// LL_FIR_DIVERGENCE
// KT-69726, KT-69727: The Analysis API's stub-based deserializer synthesizes the JVM-only `clone()` member on `kotlin.Enum` for every
// platform (StubBasedClassDeserialization.addCloneForEnumIfNeeded), even on non-JVM platforms where `Enum.clone()` does not exist. So an
// enum declaring its own `clone()` gets false-positive VIRTUAL_MEMBER_HIDDEN / EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE (KT-69726), and a
// `clone()` call resolves to the synthetic protected member as INVISIBLE_REFERENCE instead of being unresolved (KT-69727). The compiler
// reports none of these.
// LL_FIR_DIVERGENCE

// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-69726, KT-69727
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

expect enum class Enums1 {
    A123, B456;
}

expect enum class Enums2 {
    C123, D456;
}

fun useCloneCommon() {
    Enums2.C123.<!INVISIBLE_REFERENCE!>clone<!>()
}

// MODULE: m2-js()()(m1-common)
// FILE: js.kt

actual enum class Enums1 {
    A123, B456;

    fun <!EXPECT_ACTUAL_INCOMPATIBLE_RETURN_TYPE, VIRTUAL_MEMBER_HIDDEN!>clone<!>() {}
}

actual enum class Enums2 {
    C123, D456;
}

fun useCloneJs() {
    Enums2.C123.<!INVISIBLE_REFERENCE!>clone<!>()
}
