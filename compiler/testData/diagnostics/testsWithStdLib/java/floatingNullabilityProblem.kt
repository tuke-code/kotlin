// FIR_IDENTICAL
// ISSUE: KT-59138

// FILE: J.java

import kotlin.Pair;

public class J<E> {
    public static <F> J<F> create() { return null; }

    public E get() { return null; }

    public <G> Pair<E, G> foo() { return null;}
}

// FILE: test.kt

fun foo(j: J<String?>) {
    J<String?>().get()<!UNSAFE_CALL!>.<!>length
    J.create<String?>().get().length

    j.foo<String?>().first<!UNSAFE_CALL!>.<!>length
    j.foo<String?>().second.length
}
