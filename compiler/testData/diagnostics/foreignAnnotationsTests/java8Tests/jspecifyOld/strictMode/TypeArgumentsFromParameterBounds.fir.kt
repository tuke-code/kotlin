// JSPECIFY_STATE: strict
// MUTE_FOR_PSI_CLASS_FILES_READING
// LANGUAGE: +TypeEnhancementImprovementsInStrictMode

// FILE: TypeArgumentsFromParameterBounds.java
import org.jspecify.nullness.*;

@NullMarked
public class TypeArgumentsFromParameterBounds<T extends Object, E extends @Nullable Object, F extends @NullnessUnspecified Object> {}

// FILE: A.java
import org.jspecify.nullness.*;

@NullMarked
public class A {
    public void bar(TypeArgumentsFromParameterBounds<Test, Test, Test> a) {}
}

// FILE: B.java
import org.jspecify.nullness.*;

public class B {
    public void bar(TypeArgumentsFromParameterBounds<Test, Test, Test> a) {}
}

// FILE: Test.java
public class Test {}

// FILE: main.kt
fun main(
    aNotNullNotNullNotNull: TypeArgumentsFromParameterBounds<Test, Test, Test>,
    aNotNullNotNullNull: TypeArgumentsFromParameterBounds<Test, Test, Test?>,
    aNotNullNullNotNull: TypeArgumentsFromParameterBounds<Test, Test?, Test>,
    aNotNullNullNull: TypeArgumentsFromParameterBounds<Test, Test?, Test?>,
    a: A, b: B
): Unit {
    a.bar(aNotNullNotNullNotNull)
    a.bar(<!ARGUMENT_TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    a.bar(<!ARGUMENT_TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    a.bar(<!ARGUMENT_TYPE_MISMATCH!>aNotNullNullNull<!>)

    b.bar(aNotNullNotNullNotNull)
    b.bar(aNotNullNotNullNull)
    b.bar(aNotNullNullNotNull)
    b.bar(aNotNullNullNull)
}
