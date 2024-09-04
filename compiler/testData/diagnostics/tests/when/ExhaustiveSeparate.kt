enum class MyEnum {
    A, B, C
}

fun foo(x: MyEnum): Int {
    if (x == MyEnum.A) return 1
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

fun bar(x: MyEnum): Int {
    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        MyEnum.B -> 2
        MyEnum.C -> 3
    }
}

sealed interface Animal {
    data class Dog(val name: String): Animal
    data class Cat(val eatsMice: Boolean): Animal
    data object Unknown: Animal
}

fun baz(a: Animal): String {
    if (a == Animal.Unknown) return "unknown"
    return <!NO_ELSE_IN_WHEN!>when<!> (a) {
        is Animal.Dog -> "${a.name} the Dog"
        is Animal.Cat -> "Mr. Cat"
    }
}
