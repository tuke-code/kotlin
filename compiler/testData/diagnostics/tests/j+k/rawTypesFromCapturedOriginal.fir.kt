// WITH_STDLIB
// ISSUE: KT-56616

// FILE: StubElement.java

import org.jetbrains.annotations.NotNull;

public interface StubElement<T extends PsiElement> {
    <E extends PsiElement> E @NotNull [] getChildrenByType(@NotNull String filter, final E[] array);
}

// FILE: PsiElement.java

public interface PsiElement

// FILE: StubBasedPsiElement.java

public interface StubBasedPsiElement<Stub extends StubElement> extends PsiElement {
    Stub getStub();
}

// FILE: test.kt

private val STRING_TEMPLATE_EMPTY_ARRAY = emptyArray<KtStringTemplateExpression>()

open class KtStringTemplateExpression : PsiElement

fun StubBasedPsiElement<*>.foo(): KtStringTemplateExpression? {
    stub?.let {
        // K1: Array<KtStringTemplateExpression>, was K2: Array<PsiElement>
        val expressions = it.getChildrenByType("", STRING_TEMPLATE_EMPTY_ARRAY)
        // Ok in K1, Should not be error in K2
        return <!RETURN_TYPE_MISMATCH("KtStringTemplateExpression?; PsiElement?")!>expressions.firstOrNull()<!>
    }
    return null
}
