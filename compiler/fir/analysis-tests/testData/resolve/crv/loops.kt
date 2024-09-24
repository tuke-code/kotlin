// WITH_STDLIB

fun getAny(): Any = true

fun whileLoop() {
    var cur = 10
    while (cur >= 0) {
        cur--
    }

    while (getAny() as Boolean) {
        cur--
    }
}

fun inOperator(c: Char, vararg cs: Char) {
    var cur = 10
    <!RETURN_VALUE_NOT_USED!>c in cs<!> // unused
    val z = c in cs // used
    do {
        cur--
    } while (cur >= 0 && c in cs)
}

fun forLoop() {
    val cs = listOf('a', 'b', 'c')
    for (c in cs) {
        c // unused, but OK because it is local
    }
    for (i in 1..10) {
        <!RETURN_VALUE_NOT_USED!>i + 1<!>
    }
}
