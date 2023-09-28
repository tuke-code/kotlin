// MODULE: m1-common
// FILE: common.kt

open class Base {
    open <!INCOMPATIBLE_MATCHING{JVM}!>val foo: Int = 1<!>
}

expect open <!INCOMPATIBLE_MATCHING{JVM}!>class Foo : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override var foo: Int = 1
}
