// MODULE: m1-common
// FILE: common.kt

interface Base {
    <!INCOMPATIBLE_MATCHING{JVM}!>fun foo()<!>
}

expect open <!INCOMPATIBLE_MATCHING{JVM}!><!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Foo<!> : Base<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base {
    final override fun foo() {}
}
