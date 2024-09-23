// ISSUE: KT-70917
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS

class B<out T : Any>() {
    var value: InnerB = InnerB()
    val constant: InnerB = InnerB()

    inner class InnerB

    fun producer(): InnerB {
        TODO()
    }

    fun acceptor(inbound: InnerB) {
        TODO()
    }

    fun differentVariance(inbound: List<InnerB>) {
        TODO()
    }
}