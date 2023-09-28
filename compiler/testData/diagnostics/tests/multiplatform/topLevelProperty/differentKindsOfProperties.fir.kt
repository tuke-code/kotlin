// !LANGUAGE: +LateinitTopLevelProperties
// MODULE: m1-common
// FILE: common.kt

expect <!NO_ACTUAL_FOR_EXPECT!>val justVal: String<!>
expect <!NO_ACTUAL_FOR_EXPECT!>var justVar: String<!>

expect <!NO_ACTUAL_FOR_EXPECT!>val String.extensionVal: Unit<!>
expect <!NO_ACTUAL_FOR_EXPECT!>var <T> T.genericExtensionVar: T<!>

expect <!NO_ACTUAL_FOR_EXPECT!>val valWithGet: String
    get<!>
expect <!NO_ACTUAL_FOR_EXPECT!>var varWithGetSet: String
    get set<!>

expect <!NO_ACTUAL_FOR_EXPECT!>var varWithPlatformGetSet: String
    <!WRONG_MODIFIER_TARGET!>expect<!> get
    <!WRONG_MODIFIER_TARGET!>expect<!> set<!>

expect <!NO_ACTUAL_FOR_EXPECT!>val backingFieldVal: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!><!>
expect <!NO_ACTUAL_FOR_EXPECT!>var backingFieldVar: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!><!>

expect <!NO_ACTUAL_FOR_EXPECT!>val customAccessorVal: String
    get() = "no"<!>
expect <!NO_ACTUAL_FOR_EXPECT!>var customAccessorVar: String
    get() = "no"
    set(value) {}<!>

expect <!CONST_VAL_WITHOUT_INITIALIZER!>const<!> <!NO_ACTUAL_FOR_EXPECT!>val constVal: Int<!>

expect <!EXPECTED_LATEINIT_PROPERTY!>lateinit<!> <!NO_ACTUAL_FOR_EXPECT!>var lateinitVar: String<!>

expect <!NO_ACTUAL_FOR_EXPECT!>val delegated: String by <!EXPECTED_DELEGATED_PROPERTY!>Delegate<!><!>
object Delegate { operator fun getValue(x: Any?, y: Any?): String = "" }

fun test(): String {
    <!WRONG_MODIFIER_TARGET!>expect<!> val localVariable: String
    localVariable = "no"
    return localVariable
}
