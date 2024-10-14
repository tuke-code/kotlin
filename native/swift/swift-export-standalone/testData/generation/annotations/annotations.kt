// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

class normalT {
    fun normalF() = Unit
    val normalV: Unit get() = Unit
    class normalT

    @Deprecated("Deprecated")
    fun deprecatedF() = Unit
    @Deprecated("Deprecated")
    val deprecatedV: Unit get() = Unit
    @Deprecated("Deprecated")
    class deprecatedT

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    fun obsoletedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    val obsoletedV: Unit get() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    class obsoletedT

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    fun removedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val removedV: Unit get() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    class removedT
}

@Deprecated("Deprecated")
fun deprecatedImplicitlyF() = Unit
@Deprecated("Deprecated")
val deprecationInheritedImplicitlyV: Unit get() = Unit
@Deprecated("Deprecated")
typealias deprecatedImplicitlyA = Unit

@Deprecated("Deprecated", level = DeprecationLevel.WARNING)
fun deprecatedF() = Unit
@Deprecated("Deprecated", level = DeprecationLevel.WARNING)
val deprecationInheritedV: Unit get() = Unit
@Deprecated("Deprecated", level = DeprecationLevel.WARNING)
typealias deprecatedA = Unit

@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
fun renamedF() = Unit
@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
val renamedV: Unit get() = Unit
@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
typealias renamedA = Unit
@Deprecated("Deprecated", replaceWith = ReplaceWith("renamed"))
class renamedT

@Deprecated("Foo deprecated")
class deprecatedT {
    fun deprecationInheritedF() = Unit
    val deprecationInheritedV: Unit get() = Unit
    class deprecationInheritedT

    @Deprecated("Deprecated")
    fun deprecationRestatedF() = Unit
    @Deprecated("Deprecated")
    val deprecationRestatedV: Unit get() = Unit
    @Deprecated("Deprecated")
    class deprecationRestatedT

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    fun deprecationReinforcedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    val deprecationReinforcedV: Unit get() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    class deprecationReinforcedT

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    fun deprecationFurtherReinforcedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val deprecationFurtherReinforcedV: Unit get() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    class deprecationFurtherReinforcedT
}

@Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
fun obsoletedF() = Unit
@Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
val obsoletedV: Unit get() = Unit
@Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
typealias obsoletedA = Unit

@Deprecated("Bar obsoleted", level = DeprecationLevel.ERROR)
class obsoletedT {
    fun deprecationInheritedF() = Unit
    val deprecationInheritedV: Unit get() = Unit
    class deprecationInheritedT

    @Deprecated("Deprecated")
    fun deprecationRelaxedF() = Unit
    @Deprecated("Deprecated")
    val deprecationRelaxedV: Unit get() = Unit
    @Deprecated("Deprecated")
    class deprecationRelaxedT

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    fun deprecationRestatedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    val deprecationRestatedV: Unit get() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    class deprecationRestatedT

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    fun deprecationReinforcedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val deprecationReinforcedV: Unit get() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    class deprecationReinforcedT
}

@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
fun hiddenF() = Unit
@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
val hiddenV: Unit get() = Unit
@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
typealias hiddenA = Unit

@Deprecated("Bar obsoleted", level = DeprecationLevel.HIDDEN)
class hiddenT {
    fun deprecationInheritedF() = Unit
    val deprecationInheritedV: Unit get() = Unit
    class deprecationInheritedT

    @Deprecated("Deprecated")
    fun deprecationFurtherRelaxedF() = Unit
    @Deprecated("Deprecated")
    val deprecationFurtherRelaxedV: Unit get() = Unit
    @Deprecated("Deprecated")
    class deprecationFurtherRelaxedT

    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    fun deprecationRelaxedF() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    val deprecationRelaxedV: Unit get() = Unit
    @Deprecated("Obsoleted", level = DeprecationLevel.ERROR)
    class deprecationRelaxedT

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    fun deprecationRestatedF() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val deprecationRestatedV: Unit get() = Unit
    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    class deprecationRestatedT
}
