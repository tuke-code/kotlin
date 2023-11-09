// ISSUE: KT-56002
// FIR_DUMP
// FIR_IDENTICAL

annotation class Anno1
annotation class Anno2
annotation class Anno3
annotation class Anno4

@property:Anno1
@Anno2
var i: Int
@Anno1 @get:Anno2 <!INAPPLICABLE_TARGET_ON_PROPERTY, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@set:Anno3<!>
get() = 4
@Anno1 @set:Anno2
set(@setparam:Anno4 @Anno1 value) = Unit

class Klass(@param:Anno1 val int: Int, @Anno1 b: Long)
