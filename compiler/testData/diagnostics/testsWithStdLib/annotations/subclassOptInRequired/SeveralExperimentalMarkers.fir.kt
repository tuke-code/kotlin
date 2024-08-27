// FIR_IDENTICAL
@file:OptIn(ExperimentalSubclassOptIn::class)

@RequiresOptIn
annotation class ApiMarkerA

@RequiresOptIn
annotation class ApiMarkerB

@SubclassOptInRequired(ApiMarkerA::class, ApiMarkerB::class)
open class OpenKlass

class MyKlass() : <!OPT_IN_USAGE_ERROR, OPT_IN_USAGE_ERROR!>OpenKlass<!>()
