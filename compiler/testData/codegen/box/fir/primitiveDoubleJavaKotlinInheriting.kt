// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// FIR status: reports OVERLOAD_RESOLUTION_AMBIGUITY on foo call
// JVM_TARGET: 1.8
// ISSUE: KT-63242
// FILE: box.kt
private open class C : B()

private class E : D()

fun box(): String =
    E().foo(0.0)

// FILE: A.java
import org.jetbrains.annotations.NotNull;

interface A<T> {
    @NotNull
    default String foo(@NotNull T value) {
        return "Fail: A";
    }
}

abstract class B implements A<Double> {
    @NotNull
    public String foo(double value) {
        return "Fail: B";
    }
}

class D extends C {
    @NotNull
    @Override
    public String foo(@NotNull Double value) {
        return "OK";
    }
}
