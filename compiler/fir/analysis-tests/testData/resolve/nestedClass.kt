// RUN_PIPELINE_TILL: BACKEND
abstract class Base(val s: String)

class Outer {
    class Derived(s: String) : Base(s)

    object Obj : Base("")
}
