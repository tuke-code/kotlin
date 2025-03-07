// LANGUAGE: +ValueClasses
// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// CHECK_BYTECODE_LISTING

import kotlin.reflect.KProperty

interface Abstract {
    val x: Int
}

@JvmInline
value class A(override val x: Int, val y: Int): Abstract {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return x + y
    }
}

@JvmInline
value class D(override val x: Int, val y: Int): Abstract {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): A {
        return A(x * 100, y * 200)
    }
}

class B(var x: A, var y: A?) {
    val a by lazy { A(-100, -200) }
    val b by A(-100, -200)
    val c by ::a
    val d by D(-3, -7)
}

class C(a: A): Abstract by a

fun box(): String {
    val a = A(1, 2)
    val b = B(a, a)
    
    require(b.x == b.y)
    require(b.x.x == b.y?.x)
    require(b.x.y == b.y?.y)
    require(b.x.hashCode() == b.y.hashCode())
    
    require(b.a == A(-100, -200))
    require(b.a.x == -100)
    require(b.a.y == -200)
    require(b.b == -300)
    require(b.c == A(-100, -200))
    require(b.c.x == -100)
    require(b.c.y == -200)
    require(b.d == A(-300, -1400))
    require(b.d.x == -300)
    require(b.d.y == -1400)

    require(C(a).x == 1)
    
    return "OK"
}
