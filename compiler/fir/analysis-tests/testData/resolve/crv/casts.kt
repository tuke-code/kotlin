// WITH_STDLIB

fun getAny(): Any = ""
fun Any.consume(): Unit = Unit

val nonLocal: Any get() = ""

fun consume(s: String) {}


fun locals(a: Any) {
    a
    a as String
    val b: Any = ""
    b
    b as String
}

fun nonLocals() {
    <!RETURN_VALUE_NOT_USED!>nonLocal<!>
    <!RETURN_VALUE_NOT_USED!>nonLocal<!> as String
    val used = nonLocal as String
    consume(nonLocal as String)
    (nonLocal as String).consume()
}

fun classRefs(instance: Any) {
    <!RETURN_VALUE_NOT_USED!>instance::class<!>
    <!RETURN_VALUE_NOT_USED!>String::class<!>
    <!RETURN_VALUE_NOT_USED!>nonLocal::class<!>
    val s = String::class
    val ss = instance::class
    val sss = nonLocal::class
}
