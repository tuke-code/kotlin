// MODULE: m1-common
expect <!INCOMPATIBLE_MATCHING{JVM}, INCOMPATIBLE_MATCHING{JVM}!>interface Base<!>

// MODULE: m1-jvm()()(m1-common)
actual interface Base {
    override fun equals(other: Any?): Boolean
}
