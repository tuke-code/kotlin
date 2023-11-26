// ISSUE: KT-62959

fun bar(x: Inv<out CharSequence>) {
    x.foo { <!ARGUMENT_TYPE_MISMATCH("CapturedType(out kotlin.CharSequence); kotlin.CharSequence")!>it<!> } // Error both in K1 and K2
    x.bar { <!ARGUMENT_TYPE_MISMATCH("CapturedType(out kotlin.CharSequence); kotlin.CharSequence"), TYPE_MISMATCH("CapturedType(out kotlin.CharSequence); CapturedType(out kotlin.CharSequence)")!>it.e()<!> } // Error both in K1 and K2
}

interface Inv<E> {
    fun foo(x: (E) -> E) {}
    fun bar(x: (Inv<E>) -> E) {}

    fun e(): E = null!!
}
