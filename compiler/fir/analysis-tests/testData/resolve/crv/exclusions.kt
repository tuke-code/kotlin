// WITH_STDLIB

@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@Suppress("ClassName")
public annotation class _Discardable

fun stringF(): String = ""
fun nsf(): String? = "null"

fun coll(m: MutableCollection<String>) {
    m.add("")
}

fun exlusionPropagation(cond: Boolean, m: MutableList<String>) {
    if (cond) m.add("x") else throw IllegalStateException()
    if (cond) <!RETURN_VALUE_NOT_USED!>stringF()<!> else throw IllegalStateException()
}

@_Discardable
fun discardable(): String = ""

fun unused(cond: Boolean) {
    <!RETURN_VALUE_NOT_USED!>stringF()<!>
    discardable()
    if (cond) discardable() else <!RETURN_VALUE_NOT_USED!>stringF()<!>
    if (cond) discardable() else Unit
}

fun underscore() {
    val _ = stringF()
}
