// MODULE: m1-common
// FILE: common.kt

open class Base() {
    open <!INCOMPATIBLE_MATCHING{JVM}!>fun overrideReturnType(): Any = ""<!>
    open <!INCOMPATIBLE_MATCHING{JVM}!>fun overrideModality1(): Any = ""<!>
    open <!INCOMPATIBLE_MATCHING{JVM}!>fun overrideModality2(): Any = ""<!>
    protected open <!INCOMPATIBLE_MATCHING{JVM}!>fun overrideVisibility(): Any = ""<!>
}

expect open <!INCOMPATIBLE_MATCHING{JVM}!>class Foo : Base {
    fun existingMethod()
    val existingParam: Int
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual open class Foo : Base() {
    actual fun existingMethod() {}
    actual val existingParam: Int = 904

    fun injectedMethod() {}
    val injectedProperty: Int = 42
    override fun overrideReturnType(): String = ""
    final override fun overrideModality1(): Any = ""
    final override fun overrideModality2(): Any = ""
    public override fun overrideVisibility(): Any = ""
}
