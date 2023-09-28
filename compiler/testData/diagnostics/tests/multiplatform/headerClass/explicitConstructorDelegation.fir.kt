// MODULE: m1-common
// FILE: common.kt
expect open <!NO_ACTUAL_FOR_EXPECT!>class A {
    constructor(s: String)

    constructor(n: Number) : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("A")
}<!>

expect <!NO_ACTUAL_FOR_EXPECT!>class B : A {
    constructor(i: Int)

    constructor() : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL!>super<!>("B")
}<!>
