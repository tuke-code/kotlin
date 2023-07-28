annotation class Ann

fun test(x: String?) {
    if (x != null)
        @Ann <!REPEATED_ANNOTATION!>@Ann<!> { Unit }

    @Ann <!REPEATED_ANNOTATION!>@Ann<!>
    when { else -> {} }

    @Ann <!REPEATED_ANNOTATION!>@Ann<!>
    while (2 < 1) {}

    @Ann <!REPEATED_ANNOTATION!>@Ann<!>
    for (i in 1..2) {}
}
