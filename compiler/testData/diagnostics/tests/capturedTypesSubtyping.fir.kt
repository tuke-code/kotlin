// ISSUE: KT-62959

fun bar(x: Inv<out CharSequence>) {
    x.foo { it } // Error both in K1 and K2
    x.bar { it.e() } // Error both in K1 and K2
}

interface Inv<E> {
    fun foo(x: (E) -> E) {}
    fun bar(x: (Inv<E>) -> E) {}

    fun e(): E = null!!
}
