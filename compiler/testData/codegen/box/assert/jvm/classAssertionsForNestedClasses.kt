// TARGET_BACKEND: JVM
// ASSERTIONS_MODE: jvm
// WITH_STDLIB

package classAssertions

class ShouldBeEnabled {
    fun checkTrue() = A.B().hit

    class A {
        class B {
            var hit = false

            init {
                assert({ hit = true; true }())
            }
        }
    }
}

class ShouldBeDisabled {
    fun checkFalse() = A.B().hit

    class A {
        class B {
            var hit = false

            init {
                assert({ hit = true; true }())
            }
        }
    }
}

class Dummy

fun box(): String {
    val loader = Dummy::class.java.classLoader
    loader.setClassAssertionStatus("classAssertions.ShouldBeEnabled", true)
    loader.setClassAssertionStatus("classAssertions.ShouldBeDisabled", false)
    val c1 = loader.loadClass("classAssertions.ShouldBeEnabled").newInstance() as ShouldBeEnabled
    val c2 = loader.loadClass("classAssertions.ShouldBeDisabled").newInstance() as ShouldBeDisabled
    if (!c1.checkTrue()) return "FAIL 0"
    if (c2.checkFalse()) return "FAIL 1"
    return "OK"
}
