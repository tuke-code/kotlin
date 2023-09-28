// MODULE: m1-common
// FILE: common.kt

interface A

class B : A
expect <!NO_ACTUAL_FOR_EXPECT!>class Foo(b: B) : A by b<!>

expect <!NO_ACTUAL_FOR_EXPECT!>class Bar : A by B()<!>
