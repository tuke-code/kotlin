// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common

// FILE: StringValue.kt
expect <!NO_ACTUAL_FOR_EXPECT{JS}!>class StringValue<!>

expect <!NO_ACTUAL_FOR_EXPECT{JS}!>fun StringValue.plus(other: String): StringValue<!>

// MODULE: commonJS()()(common)
// TARGET_PLATFORM: JS

// FILE: StringValue.kt
actual class StringValue(<!NO_ACTUAL_FOR_EXPECT{JS}!>val value: String<!>)

actual<!NO_ACTUAL_FOR_EXPECT{JS}!> fun StringValue.plus(other: String) = StringVal<!>ue(this.value + other)

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

// FILE: StringDemoInterface.kt
expect interface StringDemoInterface

interface KotlinXStringDemoInterface {
    val value: String
}

expect <!INCOMPATIBLE_MATCHING{JS}!>fun StringDemoInterface.plusK(): String<!>

// MODULE: js()()(common, intermediate)
// TARGET_PLATFORM: JS

// FILE: StringDemoInterface.kt
actual typealias StringDemoInterface = KotlinXStringDemoInterface

actual fun StringDemoInterface<!INCOMPATIBLE_MATCHING!>.<!ACTUAL_WITHOUT_EXPECT("actual fun StringDemoInterface.plusK(): <ERROR TYPE REF: Unresolved name: value>; The following declaration is incompatible:    expect fun StringDemoInterface.plusK(): String")!>plusK<!>() = <!EXPECT_CLASS_AS_FUNCTION!>StringValue<!>(value).plus("K")<!>.<!UNRESOLVED_REFERENCE!>value<!>

// FILE: main.kt
class StringDemo(override val value: String) : StringDemoInterface

fun box() = StringDemo("O").plusK()
