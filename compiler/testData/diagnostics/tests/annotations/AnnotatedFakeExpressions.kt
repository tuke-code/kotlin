// FIR_IDENTICAL
// FIR_DUMP
// ISSUE: KT-52175

annotation class Ann
annotation class Ann2

fun test(x: String?) {
    if (x != null)
        @Ann() @Ann2() { Unit } // It should be Block with annotations

    @Ann() @Ann2() when { else -> {} }
    val y = <!WRONG_ANNOTATION_TARGET!>@Ann()<!> <!WRONG_ANNOTATION_TARGET!>@Ann2()<!> when { else -> 42 }

    if (x != null)
        <!WRONG_ANNOTATION_TARGET!>@Ann()<!> <!WRONG_ANNOTATION_TARGET!>@Ann2()<!> Unit // Annotations on expression

    <!WRONG_ANNOTATION_TARGET!>@Ann()<!> <!WRONG_ANNOTATION_TARGET!>@Ann2()<!> x
}
