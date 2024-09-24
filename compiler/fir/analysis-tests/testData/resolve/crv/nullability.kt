fun stringF(): String = ""
fun nsf(): String? = "null"

fun Any.consume(): Unit = Unit

fun elvis(e: String?): String {
    val c = nsf() ?: stringF() // used
    <!RETURN_VALUE_NOT_USED!>nsf() ?: stringF()<!> // unused

    return e ?: nsf() ?: stringF()
}

fun safeCalls() {
    stringF().consume() // used
    <!RETURN_VALUE_NOT_USED!>stringF().toString()<!> // unused
    nsf()?.consume() // used
    <!RETURN_VALUE_NOT_USED!>nsf()?.toString()<!> // unused
}

fun notNullCall(s: String?) {
    s!! // locals are discardable, we propagate
    <!RETURN_VALUE_NOT_USED!>nsf()<!>!!
}

fun notNullCall2(s: String?) {
    val x = s!!
    val y = nsf()!!
}
