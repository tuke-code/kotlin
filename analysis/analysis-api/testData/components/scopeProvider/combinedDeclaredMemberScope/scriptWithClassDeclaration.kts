// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// script
abstract class Person {
    abstract val name: String
}

fun foo(action: () -> Int) {
    action()
}

foo {
    42 + 42
}
