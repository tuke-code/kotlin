// MODULE: m1-common
// FILE: common.kt

expect <!NO_ACTUAL_FOR_EXPECT{JVM}!>class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>() {
    fun foo()
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
}
