// WITH_STDLIB
// ISSUE: KT-66229

fun foo() {
    buildMap {
        for (<!BUILDER_INFERENCE_STUB_PARAMETER_TYPE!>v<!> in this) {
            put(1, 1)
        }
    }
}

fun bar() {
    buildMap {
        mapValues { (key: Int, value: String) -> "1" }
    }
}
