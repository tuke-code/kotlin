@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedT::class, "4main11deprecatedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedT.deprecationInheritedT::class, "4main11deprecatedTC21deprecationInheritedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedT.deprecationReinforcedT::class, "4main11deprecatedTC22deprecationReinforcedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(deprecatedT.deprecationRestatedT::class, "4main11deprecatedTC20deprecationRestatedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalT::class, "4main7normalTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalT.deprecatedT::class, "4main7normalTC11deprecatedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalT.normalT::class, "4main7normalTC7normalTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(normalT.obsoletedT::class, "4main7normalTC10obsoletedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(obsoletedT::class, "4main10obsoletedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(obsoletedT.deprecationInheritedT::class, "4main10obsoletedTC21deprecationInheritedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(obsoletedT.deprecationRelaxedT::class, "4main10obsoletedTC19deprecationRelaxedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(obsoletedT.deprecationRestatedT::class, "4main10obsoletedTC20deprecationRestatedTC")
@file:kotlin.native.internal.objc.BindClassToObjCName(renamedT::class, "4main8renamedTC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___deprecatedF")
public fun __root___deprecatedF(): Unit {
    deprecatedF()
}

@ExportedBridge("__root___deprecatedImplicitlyF")
public fun __root___deprecatedImplicitlyF(): Unit {
    deprecatedImplicitlyF()
}

@ExportedBridge("__root___deprecatedT_init_allocate")
public fun __root___deprecatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___deprecatedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___deprecatedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, deprecatedT())
}

@ExportedBridge("__root___deprecationInheritedImplicitlyV_get")
public fun __root___deprecationInheritedImplicitlyV_get(): Unit {
    deprecationInheritedImplicitlyV
}

@ExportedBridge("__root___deprecationInheritedV_get")
public fun __root___deprecationInheritedV_get(): Unit {
    deprecationInheritedV
}

@ExportedBridge("__root___normalT_init_allocate")
public fun __root___normalT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___normalT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___normalT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, normalT())
}

@ExportedBridge("__root___obsoletedF")
public fun __root___obsoletedF(): Unit {
    obsoletedF()
}

@ExportedBridge("__root___obsoletedT_init_allocate")
public fun __root___obsoletedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<obsoletedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___obsoletedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___obsoletedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, obsoletedT())
}

@ExportedBridge("__root___obsoletedV_get")
public fun __root___obsoletedV_get(): Unit {
    obsoletedV
}

@ExportedBridge("__root___renamedF")
public fun __root___renamedF(): Unit {
    renamedF()
}

@ExportedBridge("__root___renamedT_init_allocate")
public fun __root___renamedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<renamedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___renamedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun __root___renamedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, renamedT())
}

@ExportedBridge("__root___renamedV_get")
public fun __root___renamedV_get(): Unit {
    renamedV
}

@ExportedBridge("deprecatedT_deprecationInheritedF")
public fun deprecatedT_deprecationInheritedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationInheritedF()
}

@ExportedBridge("deprecatedT_deprecationInheritedT_init_allocate")
public fun deprecatedT_deprecationInheritedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedT.deprecationInheritedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun deprecatedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, deprecatedT.deprecationInheritedT())
}

@ExportedBridge("deprecatedT_deprecationInheritedV_get")
public fun deprecatedT_deprecationInheritedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationInheritedV
}

@ExportedBridge("deprecatedT_deprecationReinforcedF")
public fun deprecatedT_deprecationReinforcedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationReinforcedF()
}

@ExportedBridge("deprecatedT_deprecationReinforcedT_init_allocate")
public fun deprecatedT_deprecationReinforcedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedT.deprecationReinforcedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedT_deprecationReinforcedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun deprecatedT_deprecationReinforcedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, deprecatedT.deprecationReinforcedT())
}

@ExportedBridge("deprecatedT_deprecationReinforcedV_get")
public fun deprecatedT_deprecationReinforcedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationReinforcedV
}

@ExportedBridge("deprecatedT_deprecationRestatedF")
public fun deprecatedT_deprecationRestatedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationRestatedF()
}

@ExportedBridge("deprecatedT_deprecationRestatedT_init_allocate")
public fun deprecatedT_deprecationRestatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<deprecatedT.deprecationRestatedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun deprecatedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, deprecatedT.deprecationRestatedT())
}

@ExportedBridge("deprecatedT_deprecationRestatedV_get")
public fun deprecatedT_deprecationRestatedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as deprecatedT
    __self.deprecationRestatedV
}

@ExportedBridge("normalT_deprecatedF")
public fun normalT_deprecatedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.deprecatedF()
}

@ExportedBridge("normalT_deprecatedT_init_allocate")
public fun normalT_deprecatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalT.deprecatedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun normalT_deprecatedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, normalT.deprecatedT())
}

@ExportedBridge("normalT_deprecatedV_get")
public fun normalT_deprecatedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.deprecatedV
}

@ExportedBridge("normalT_normalF")
public fun normalT_normalF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.normalF()
}

@ExportedBridge("normalT_normalT_init_allocate")
public fun normalT_normalT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalT.normalT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("normalT_normalT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun normalT_normalT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, normalT.normalT())
}

@ExportedBridge("normalT_normalV_get")
public fun normalT_normalV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.normalV
}

@ExportedBridge("normalT_obsoletedF")
public fun normalT_obsoletedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.obsoletedF()
}

@ExportedBridge("normalT_obsoletedT_init_allocate")
public fun normalT_obsoletedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<normalT.obsoletedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("normalT_obsoletedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun normalT_obsoletedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, normalT.obsoletedT())
}

@ExportedBridge("normalT_obsoletedV_get")
public fun normalT_obsoletedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as normalT
    __self.obsoletedV
}

@ExportedBridge("obsoletedT_deprecationInheritedF")
public fun obsoletedT_deprecationInheritedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as obsoletedT
    __self.deprecationInheritedF()
}

@ExportedBridge("obsoletedT_deprecationInheritedT_init_allocate")
public fun obsoletedT_deprecationInheritedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<obsoletedT.deprecationInheritedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("obsoletedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun obsoletedT_deprecationInheritedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, obsoletedT.deprecationInheritedT())
}

@ExportedBridge("obsoletedT_deprecationInheritedV_get")
public fun obsoletedT_deprecationInheritedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as obsoletedT
    __self.deprecationInheritedV
}

@ExportedBridge("obsoletedT_deprecationRelaxedF")
public fun obsoletedT_deprecationRelaxedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as obsoletedT
    __self.deprecationRelaxedF()
}

@ExportedBridge("obsoletedT_deprecationRelaxedT_init_allocate")
public fun obsoletedT_deprecationRelaxedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<obsoletedT.deprecationRelaxedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("obsoletedT_deprecationRelaxedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun obsoletedT_deprecationRelaxedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, obsoletedT.deprecationRelaxedT())
}

@ExportedBridge("obsoletedT_deprecationRelaxedV_get")
public fun obsoletedT_deprecationRelaxedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as obsoletedT
    __self.deprecationRelaxedV
}

@ExportedBridge("obsoletedT_deprecationRestatedF")
public fun obsoletedT_deprecationRestatedF(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as obsoletedT
    __self.deprecationRestatedF()
}

@ExportedBridge("obsoletedT_deprecationRestatedT_init_allocate")
public fun obsoletedT_deprecationRestatedT_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<obsoletedT.deprecationRestatedT>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("obsoletedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UInt__")
public fun obsoletedT_deprecationRestatedT_init_initialize__TypesOfArguments__Swift_UInt__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, obsoletedT.deprecationRestatedT())
}

@ExportedBridge("obsoletedT_deprecationRestatedV_get")
public fun obsoletedT_deprecationRestatedV_get(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as obsoletedT
    __self.deprecationRestatedV
}

