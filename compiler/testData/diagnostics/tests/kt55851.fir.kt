// ISSUE: KT-55851

// FILE: test/Base.java
package test;

class Base {
    public String s;
}

// FILE: test/Jaba.java
package test;

public class Jaba extends Base {
}

// FILE: test.kt
import test.Jaba

fun box(): String {
    // Any of the following two lines is enough to crash the program in runtime (trying to access Base...)
    <!REFERENCE_TO_PACKAGE_PRIVATE_CLASS_FIELD!>Jaba()::s<!>.get()
    <!REFERENCE_TO_PACKAGE_PRIVATE_CLASS_FIELD!>Jaba::s<!>.invoke(Jaba())
    return "OK"
}
