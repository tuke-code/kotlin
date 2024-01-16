// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1
// FILE: VeryBaseOwnerJava.java

import java.util.Collection;

public interface VeryBaseOwnerJava {
    Collection<? extends VeryBaseOwnerJava> getSomething();
}

// FILE: BaseOwnerJava.java

import java.util.Collection;

public interface BaseOwnerJava extends VeryBaseOwnerJava {
    Collection<? extends BaseOwnerJava> getSomething();
    void setSomething(Collection<? extends BaseOwnerJava> arg);
}

// FILE: SomethingOwnerJava.java

import java.util.Collection;

public interface SomethingOwnerJava extends BaseOwnerJava {
    @Override
    Collection<? extends SomethingOwnerJava> getSomething();
}

// FILE: ImplJava.java

public class ImplJava {}

// MODULE: m2(m1)
// FILE: test.kt

interface SomethingOwner : SomethingOwnerJava

abstract class Impl : ImplJava(), SomethingOwner {
    override fun getSomething(): Collection<SomethingOwnerJava> = emptyList()
    override fun setSomething(arg: Collection<BaseOwnerJava>) {}
}

class Final : Impl(), SomethingOwner
