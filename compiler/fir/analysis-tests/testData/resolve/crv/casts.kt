// WITH_STDLIB
// SKIP_TXT

fun getAny(): Any = ""
fun Any.consume(): Unit = Unit

fun locals(a: Any) {
    a
    a as String
    val b: Any = ""
    b
    b as String
}

val nonLocal: Any get() = ""

fun consume(s: String) {}

fun nonLocals() {
    <!RETURN_VALUE_NOT_USED!>nonLocal<!>
    <!RETURN_VALUE_NOT_USED!>nonLocal<!> as String
    val used = nonLocal as String
    consume(nonLocal as String)
    (nonLocal as String).consume()
}


//fun bar(a: Any) {
//    a is String
//}
//
//fun foo2() {
//    getAny() as String
//}
//
//fun bar2() {
//    getAny() is String
//}
