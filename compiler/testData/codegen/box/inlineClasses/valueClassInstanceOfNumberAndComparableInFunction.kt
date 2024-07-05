// ISSUE: KT-67518

inline class X(val x: String)

fun box(): String = if (check(X("")) || checkInline(X(""))) "Not OK" else "OK"

fun check(a: Any): Boolean = a is Comparable<*>

inline fun checkInline(a: Any): Boolean = a is Comparable<*>
