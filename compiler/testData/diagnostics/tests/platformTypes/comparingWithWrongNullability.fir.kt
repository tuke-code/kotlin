// FULL_JDK
// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN

import java.util.Comparator;

fun foo() {
    Comparator.comparing<String?, <!UPPER_BOUND_VIOLATED("kotlin.Comparable<in kotlin.Boolean?>!; kotlin.Boolean?")!>Boolean?<!>> <!ARGUMENT_TYPE_MISMATCH("kotlin.Function1<T!, U!>!; kotlin.Function1<kotlin.String?, kotlin.Boolean>")!>{
        it != ""
    }<!>
}
