class A {
    companion {
        val valChange = "propertyChange.v1"
        val removedVal = 42
        var varChange = valChange
        var removedVar = 42

        fun bodyChange() = "bodyChange.v1"
        fun removedFun() {}
    }
}
