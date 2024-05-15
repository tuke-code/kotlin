// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN
// ISSUE: KT-67999
// FIR_DUMP

// FILE: J.java
public interface J<X> {
    void foo(X x);
}

// FILE: main.kt

fun main() {
    J<String?> { x ->
        x.length
    }
}
