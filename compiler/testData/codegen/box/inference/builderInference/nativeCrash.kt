// DUMP_IR

fun <T : Any> build(f: Builder<T>.() -> Unit): T {
    val b = Builder<T>()
    b.f()
    return b.build()
}

class Builder<T : Any> {
    var t: T? = null

    fun emit(t: T) {
        this.t = t
    }

    fun build(): T = t!!
}

fun box(): String {
    val some = build {
        emit(1)
        build().toString()
    }
    return "OK"
}
