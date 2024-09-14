// ISSUE: KT-70917
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS

class B<out T : Any>() {
    // compiles on old compiler
    // gives compilation error on K2:
    // Type parameter 'T' is declared as 'out' but occurs in 'invariant' position in type 'B.InnerB<T>'.
    var value: InnerB = InnerB()
    // valid on both
    val constant: InnerB = InnerB()

    inner class InnerB

    // valid on both
    fun producer(): InnerB {
        TODO()
    }

    // valid on both
    fun acceptor(inbound: InnerB) {
        TODO()
    }

    // compiles on old compiler
    // gives compilation error on K2:
    // Type parameter 'T' is declared as 'out' but occurs in 'in' position in type 'kotlin.collections.List<B.InnerB<T>>'.
    fun differentVariance(inbound: List<InnerB>) {
        TODO()
    }
}