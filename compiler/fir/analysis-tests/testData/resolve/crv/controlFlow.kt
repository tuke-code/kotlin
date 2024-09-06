// WITH_STDLIB

fun stringF(): String = ""
fun unitF(): Unit = Unit
fun nsf(): String? = "null"

val a: Int get() = 10

fun ifTest() {
    val x = if (a > 0) stringF() else "" // used
}
