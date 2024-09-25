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

    when (intF()) {
        intF() -> <!RETURN_VALUE_NOT_USED!>intF()<!> // only part after -> should be reported unused
    }
}

fun ifBranches() {
    val x = if (intF() > 0) intF() else 0 // used
    if (intF() > 0) <!RETURN_VALUE_NOT_USED!>intF()<!> else <!RETURN_VALUE_NOT_USED!>0<!> // unused
}

fun ifBranches2(cond: Boolean): String? {
    return if (cond) {
        val x = intF() // unrelated
        stringF()
    } else {
        <!RETURN_VALUE_NOT_USED!>intF()<!> // unused
        nsf()
    }

    if (cond) {
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
    } else {
        <!RETURN_VALUE_NOT_USED!>nsf()<!>
    }
}

fun tryCatch() {
    val x = try {
        nsf()
    } catch (e: Exception) {
        "x"
    } finally {
        stringF()
    }

    try {
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
    } catch (e: Exception) {
        <!RETURN_VALUE_NOT_USED!>nsf()<!>
    }

    try {
        val used = stringF()
    } catch (e: Exception) {
        <!RETURN_VALUE_NOT_USED!>nsf()<!>
    } finally {
        unitF() // Unit, OK to discard
    }
}

fun typicalError(cond: Boolean): String {
    if (cond) {
        <!RETURN_VALUE_NOT_USED!>nsf()<!> // value unused
    } else {
        return stringF()
    }
    return "default"
}
