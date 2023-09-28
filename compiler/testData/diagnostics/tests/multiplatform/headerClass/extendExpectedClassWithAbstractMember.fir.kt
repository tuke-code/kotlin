// MODULE: m1-common
// FILE: common.kt

expect abstract <!NO_ACTUAL_FOR_EXPECT!>class BaseA() {
    abstract fun foo()
}<!>
expect open <!NO_ACTUAL_FOR_EXPECT!><!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class BaseAImpl<!>() : BaseA<!>

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class DerivedA1<!> : BaseAImpl()
class DerivedA2 : BaseAImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect <!NO_ACTUAL_FOR_EXPECT!>interface BaseB {
    fun foo()
}<!>
expect open <!NO_ACTUAL_FOR_EXPECT!><!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class BaseBImpl<!>() : BaseB<!>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class DerivedB1<!> : BaseBImpl()
class DerivedB2 : BaseBImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect <!NO_ACTUAL_FOR_EXPECT!>interface BaseC {
    fun foo()
}<!>
expect abstract <!NO_ACTUAL_FOR_EXPECT!>class BaseCImpl() : BaseC<!>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class DerivedC1<!> : BaseCImpl()
class DerivedC2 : BaseCImpl() {
    override fun foo() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect <!NO_ACTUAL_FOR_EXPECT!>interface BaseD {
    fun foo()
}<!>
abstract class BaseDImpl() : BaseD {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect <!NO_ACTUAL_FOR_EXPECT!>interface BaseE {
    fun foo()
}<!>
sealed class BaseEImpl() : BaseE {
    fun bar() = super.<!ABSTRACT_SUPER_CALL!>foo<!>()
}



expect <!NO_ACTUAL_FOR_EXPECT!>interface BaseF {
    fun foo()
}<!>
expect <!NO_ACTUAL_FOR_EXPECT!><!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class BaseFImpl<!>() : BaseF<!>



expect abstract <!NO_ACTUAL_FOR_EXPECT!>class BaseG() {
    abstract fun foo()
}<!>
expect open <!NO_ACTUAL_FOR_EXPECT!>class BaseGImpl() : BaseG {
    override fun foo()
}<!>
class DerivedG1 : BaseGImpl()
