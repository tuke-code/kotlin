// RUN_PIPELINE_TILL: FRONTEND
// +JDK

typealias Exn = java.lang.Exception

fun test() {
    throw <!NO_COMPANION_OBJECT, TYPE_MISMATCH!>Exn<!>
}
