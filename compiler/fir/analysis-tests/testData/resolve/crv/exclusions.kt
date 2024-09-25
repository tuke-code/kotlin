// WITH_STDLIB

fun stringF(): String = ""
fun nsf(): String? = "null"

fun coll(m: MutableCollection<String>) {
    m.add("")
}

fun exlusionPropagation(cond: Boolean, m: MutableList<String>) {
    if (cond) m.add("x") else throw IllegalStateException()
    if (cond) <!RETURN_VALUE_NOT_USED!>stringF()<!> else throw IllegalStateException()
}
