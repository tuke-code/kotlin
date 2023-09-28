// MODULE: m1-common
// FILE: common.kt

open class Base {
    protected open <!INCOMPATIBLE_MATCHING{JVM}!>fun foo() {}<!>
}

expect open <!INCOMPATIBLE_MATCHING{JVM}!>class Foo : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    public override fun foo() {}
}
