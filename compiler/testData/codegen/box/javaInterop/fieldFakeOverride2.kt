// TARGET_BACKEND: JVM_IR
// ISSUE: KT-55851

// FILE: BaseJava.java
public class BaseJava {
    public String a = "O";

    String b = "K";
}

// FILE: Derived.kt
class Derived : BaseJava()

fun box(): String {
    val d = Derived()
    return Derived::a.invoke(d) + Derived::b.invoke(d)
}
