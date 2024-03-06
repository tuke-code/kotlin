// TARGET_BACKEND: JVM_IR
// ISSUE: KT-55851
// IGNORE_BACKEND_K2: JVM_IR

// FILE: test/Base.java
package test;

class Base {
    public String s = "s";
}

// FILE: test/Jaba.java
package test;

public class Jaba extends Base {
}

// FILE: test.kt
import test.Jaba

fun box(): String {
    if (Jaba()::s.get() != "s") return "FAIL 1"
    if (Jaba::s.invoke(Jaba()) != "s") return "FAIL 2"
    return "OK"
}
