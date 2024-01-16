// WITH_STDLIB
// FILE: BaseOwnerJava.java

import java.util.Collection;

public interface BaseOwnerJava {
    void setSomething(Collection<? extends BaseOwnerJava> arg);
}

// FILE: test.kt

abstract class Impl : BaseOwnerJava {
    override fun setSomething(arg: Collection<BaseOwnerJava>) = throw IllegalStateException()
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Final<!> : Impl(), BaseOwnerJava
