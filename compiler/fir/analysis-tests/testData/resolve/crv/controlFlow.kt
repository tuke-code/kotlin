// WITH_STDLIB

fun stringF(): String = ""
fun intF(): Int = 10
fun unitF(): Unit = Unit
fun nsf(): String? = "null"


fun ifCondition() {
    <!RETURN_VALUE_NOT_USED!>intF() > 0<!> // not used
    val y = intF() > 0 // used
    if (intF() > 0) Unit else Unit // used
    println(intF() > 0) // used
}

fun whenCondition() {
    when (intF()) {
        0 -> Unit
    }

    when (val x = intF()) {
        0 -> x
    }

    when (intF()) {
        intF() -> Unit
    }

    // TODO: this should be reported
    when (intF()) {
        intF() -> intF() // only part after -> should be reported unused
    }
}

fun ifBranches() {
    val x = if (intF() > 0) intF() else 0 // used
    <!RETURN_VALUE_NOT_USED!>if (intF() > 0) intF() else 0<!> // unused
}

