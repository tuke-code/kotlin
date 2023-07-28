annotation class Ann

fun test(x: String?) {
    if (x != null)
        @Ann @Ann { Unit }

    @Ann @Ann
    when { else -> {} }

    @Ann @Ann
    while (2 < 1) {}

    @Ann @Ann
    for (i in 1..2) {}
}