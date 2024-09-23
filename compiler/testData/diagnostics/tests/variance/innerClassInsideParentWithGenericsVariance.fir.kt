// ISSUE: KT-70917
// DIAGNOSTICS: -DEBUG_INFO_LEAKING_THIS

class B<out T : Any>() {
    var value: <!TYPE_VARIANCE_CONFLICT_ERROR!>InnerB<!> = InnerB()
    val constant: InnerB = InnerB()

    inner class InnerB

    fun producer(): InnerB {
        TODO()
    }

    fun acceptor(inbound: <!TYPE_VARIANCE_CONFLICT_ERROR!>InnerB<!>) {
        TODO()
    }

    fun differentVariance(inbound: List<<!TYPE_VARIANCE_CONFLICT_ERROR!>InnerB<!>>) {
        TODO()
    }
}
