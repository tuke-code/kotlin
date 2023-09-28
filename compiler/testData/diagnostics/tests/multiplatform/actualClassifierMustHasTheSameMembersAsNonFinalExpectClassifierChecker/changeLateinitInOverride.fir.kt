// MODULE: m1-common
// FILE: common.kt

open class Base {
    open <!INCOMPATIBLE_MATCHING{JVM}!>var red1: String = ""<!>
    open lateinit <!INCOMPATIBLE_MATCHING{JVM}!>var red2: String<!>
    open lateinit var green: String
}

expect open <!INCOMPATIBLE_MATCHING{JVM}!>class Foo : Base {
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    override lateinit var red1: String
    override var red2: String = ""
    override lateinit var green: String
}
