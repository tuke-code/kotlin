// DUMP_IR
// FIR_IDENTICAL

fun box(): String {
    val res = 42.toString()
    if (res != "42") return res
    return "OK"
}
