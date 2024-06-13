open class A
abstract class B

fun <T : A> find(): T = null as T
fun test(): B = find()
