@_exported import ExportedKotlinPackages
import KotlinRuntime
@_implementationOnly import KotlinBridges_main

public final class Class_without_package : KotlinRuntime.KotlinBase {
    public final class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Class_without_package_INNER_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Class_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: main.Class_without_package.INNER_OBJECT {
            get {
                return main.Class_without_package.INNER_OBJECT(__externalRCRef: Class_without_package_INNER_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public override init() {
        let __kt = __root___Class_without_package_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Class_without_package_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Demo : KotlinRuntime.KotlinBase {
    public final class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Demo_INNER_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Demo_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: main.Demo.INNER_OBJECT {
            get {
                return main.Demo.INNER_OBJECT(__externalRCRef: Demo_INNER_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public var arg1: main.Class_without_package {
        get {
            return main.Class_without_package(__externalRCRef: Demo_arg1_get(self.__externalRCRef()))
        }
    }
    public var arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: Demo_arg2_get(self.__externalRCRef()))
        }
    }
    public var arg3: main.Object_without_package {
        get {
            return main.Object_without_package(__externalRCRef: Demo_arg3_get(self.__externalRCRef()))
        }
    }
    public var arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: Demo_arg4_get(self.__externalRCRef()))
        }
    }
    public var var1: main.Class_without_package {
        get {
            return main.Class_without_package(__externalRCRef: Demo_var1_get(self.__externalRCRef()))
        }
        set {
            return Demo_var1_set__TypesOfArguments__main_Class_without_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var2: ExportedKotlinPackages.namespace.deeper.Class_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: Demo_var2_get(self.__externalRCRef()))
        }
        set {
            return Demo_var2_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var3: main.Object_without_package {
        get {
            return main.Object_without_package(__externalRCRef: Demo_var3_get(self.__externalRCRef()))
        }
        set {
            return Demo_var3_set__TypesOfArguments__main_Object_without_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public var var4: ExportedKotlinPackages.namespace.deeper.Object_with_package {
        get {
            return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: Demo_var4_get(self.__externalRCRef()))
        }
        set {
            return Demo_var4_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(self.__externalRCRef(), newValue.__externalRCRef())
        }
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
    public init(
        arg1: main.Class_without_package,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
    ) {
        let __kt = __root___Demo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Demo_init_initialize__TypesOfArguments__Swift_UInt_main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(__kt, arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
    }
    public func combine(
        arg1: main.Class_without_package,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
        arg3: main.Object_without_package,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
    ) -> main.Demo {
        return main.Demo(__externalRCRef: Demo_combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_classses(
        arg1: main.Class_without_package.INNER_CLASS,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_CLASS,
        arg3: main.Object_without_package.INNER_CLASS,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_CLASS
    ) -> main.Demo.INNER_CLASS {
        return main.Demo.INNER_CLASS(__externalRCRef: Demo_combine_inner_classses__TypesOfArguments__main_Class_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_CLASS_main_Object_without_package_INNER_CLASS_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_CLASS__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
    public func combine_inner_objects(
        arg1: main.Class_without_package.INNER_OBJECT,
        arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT,
        arg3: main.Object_without_package.INNER_OBJECT,
        arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT
    ) -> main.Demo.INNER_OBJECT {
        return main.Demo.INNER_OBJECT(__externalRCRef: Demo_combine_inner_objects__TypesOfArguments__main_Class_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Class_with_package_INNER_OBJECT_main_Object_without_package_INNER_OBJECT_ExportedKotlinPackages_namespace_deeper_Object_with_package_INNER_OBJECT__(self.__externalRCRef(), arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef()))
    }
}
open class OPEN_CLASS : KotlinRuntime.KotlinBase {
    public override init() {
        let __kt = __root___OPEN_CLASS_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___OPEN_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public final class Object_without_package : KotlinRuntime.KotlinBase {
    public final class INNER_CLASS : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Object_without_package_INNER_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            Object_without_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
        public static var shared: main.Object_without_package.INNER_OBJECT {
            get {
                return main.Object_without_package.INNER_OBJECT(__externalRCRef: Object_without_package_INNER_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static var shared: main.Object_without_package {
        get {
            return main.Object_without_package(__externalRCRef: __root___Object_without_package_get())
        }
    }
    private override init() {
        fatalError()
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public var nullablePrim: Swift.Int32? {
    get {
        return __root___nullablePrim_get()?.int32Value
    }
    set {
        return __root___nullablePrim_set__TypesOfArguments__Swift_Int32_opt___(newValue.flatMap { it in NSNumber(value: it) })
    }
}
public var nullableRef: main.Class_without_package? {
    get {
        return switch __root___nullableRef_get() { case 0: .none; case let res: main.Class_without_package(__externalRCRef: res); }
    }
    set {
        return __root___nullableRef_set__TypesOfArguments__main_Class_without_package_opt___(newValue?.__externalRCRef() ?? 0)
    }
}
public var val_class: main.Class_without_package {
    get {
        return main.Class_without_package(__externalRCRef: __root___val_class_get())
    }
}
public var val_class_wp: ExportedKotlinPackages.namespace.deeper.Class_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: __root___val_class_wp_get())
    }
}
public var val_object: main.Object_without_package {
    get {
        return main.Object_without_package(__externalRCRef: __root___val_object_get())
    }
}
public var val_object_wp: ExportedKotlinPackages.namespace.deeper.Object_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: __root___val_object_wp_get())
    }
}
public var var_class: main.Class_without_package {
    get {
        return main.Class_without_package(__externalRCRef: __root___var_class_get())
    }
    set {
        return __root___var_class_set__TypesOfArguments__main_Class_without_package__(newValue.__externalRCRef())
    }
}
public var var_class_wp: ExportedKotlinPackages.namespace.deeper.Class_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: __root___var_class_wp_get())
    }
    set {
        return __root___var_class_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(newValue.__externalRCRef())
    }
}
public var var_object: main.Object_without_package {
    get {
        return main.Object_without_package(__externalRCRef: __root___var_object_get())
    }
    set {
        return __root___var_object_set__TypesOfArguments__main_Object_without_package__(newValue.__externalRCRef())
    }
}
public var var_object_wp: ExportedKotlinPackages.namespace.deeper.Object_with_package {
    get {
        return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: __root___var_object_wp_get())
    }
    set {
        return __root___var_object_wp_set__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(newValue.__externalRCRef())
    }
}
public func combine(
    arg1: main.Class_without_package,
    arg2: ExportedKotlinPackages.namespace.deeper.Class_with_package,
    arg3: main.Object_without_package,
    arg4: ExportedKotlinPackages.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___combine__TypesOfArguments__main_Class_without_package_ExportedKotlinPackages_namespace_deeper_Class_with_package_main_Object_without_package_ExportedKotlinPackages_namespace_deeper_Object_with_package__(arg1.__externalRCRef(), arg2.__externalRCRef(), arg3.__externalRCRef(), arg4.__externalRCRef())
}
public func nullable_input_prim(
    i: Swift.Int32?
) -> Swift.Void {
    return __root___nullable_input_prim__TypesOfArguments__Swift_Int32_opt___(i.flatMap { it in NSNumber(value: it) })
}
public func nullable_input_ref(
    i: main.Class_without_package?
) -> Swift.Void {
    return __root___nullable_input_ref__TypesOfArguments__main_Class_without_package_opt___(i?.__externalRCRef() ?? 0)
}
public func nullable_output_prim() -> Swift.Int32? {
    return __root___nullable_output_prim()?.int32Value
}
public func nullable_output_ref() -> main.Class_without_package? {
    return switch __root___nullable_output_ref() { case 0: .none; case let res: main.Class_without_package(__externalRCRef: res); }
}
public func produce_DATA_OBJECT() -> ExportedKotlinPackages.namespace.deeper.DATA_OBJECT {
    return ExportedKotlinPackages.namespace.deeper.DATA_OBJECT(__externalRCRef: __root___produce_DATA_OBJECT())
}
public func produce_OPEN_CLASS() -> main.OPEN_CLASS {
    return main.OPEN_CLASS(__externalRCRef: __root___produce_OPEN_CLASS())
}
public func produce_class() -> main.Class_without_package {
    return main.Class_without_package(__externalRCRef: __root___produce_class())
}
public func produce_class_wp() -> ExportedKotlinPackages.namespace.deeper.Class_with_package {
    return ExportedKotlinPackages.namespace.deeper.Class_with_package(__externalRCRef: __root___produce_class_wp())
}
public func produce_object() -> main.Object_without_package {
    return main.Object_without_package(__externalRCRef: __root___produce_object())
}
public func produce_object_wp() -> ExportedKotlinPackages.namespace.deeper.Object_with_package {
    return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: __root___produce_object_wp())
}
public func recieve_DATA_OBJECT(
    x: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT
) -> Swift.Void {
    return __root___recieve_DATA_OBJECT__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_DATA_OBJECT__(x.__externalRCRef())
}
public func recieve_OPEN_CLASS(
    x: main.OPEN_CLASS
) -> Swift.Void {
    return __root___recieve_OPEN_CLASS__TypesOfArguments__main_OPEN_CLASS__(x.__externalRCRef())
}
public func recieve_class(
    arg: main.Class_without_package
) -> Swift.Void {
    return __root___recieve_class__TypesOfArguments__main_Class_without_package__(arg.__externalRCRef())
}
public func recieve_class_wp(
    arg: ExportedKotlinPackages.namespace.deeper.Class_with_package
) -> Swift.Void {
    return __root___recieve_class_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Class_with_package__(arg.__externalRCRef())
}
public func recieve_object(
    arg: main.Object_without_package
) -> Swift.Void {
    return __root___recieve_object__TypesOfArguments__main_Object_without_package__(arg.__externalRCRef())
}
public func recieve_object_wp(
    arg: ExportedKotlinPackages.namespace.deeper.Object_with_package
) -> Swift.Void {
    return __root___recieve_object_wp__TypesOfArguments__ExportedKotlinPackages_namespace_deeper_Object_with_package__(arg.__externalRCRef())
}
public extension ExportedKotlinPackages.namespace.deeper {
    public final class Class_with_package : KotlinRuntime.KotlinBase {
        public final class INNER_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = namespace_deeper_Class_with_package_INNER_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_Class_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.Class_with_package.INNER_OBJECT(__externalRCRef: namespace_deeper_Class_with_package_INNER_OBJECT_get())
                }
            }
            private override init() {
                fatalError()
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public override init() {
            let __kt = namespace_deeper_Class_with_package_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_deeper_Class_with_package_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class DATA_OBJECT : KotlinRuntime.KotlinBase {
        public var a: Swift.Int32 {
            get {
                return namespace_deeper_DATA_OBJECT_a_get(self.__externalRCRef())
            }
        }
        public static var shared: ExportedKotlinPackages.namespace.deeper.DATA_OBJECT {
            get {
                return ExportedKotlinPackages.namespace.deeper.DATA_OBJECT(__externalRCRef: namespace_deeper_DATA_OBJECT_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public func hashCode() -> Swift.Int32 {
            return namespace_deeper_DATA_OBJECT_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return namespace_deeper_DATA_OBJECT_toString(self.__externalRCRef())
        }
    }
    public final class Object_with_package : KotlinRuntime.KotlinBase {
        public final class INNER_CLASS : KotlinRuntime.KotlinBase {
            public override init() {
                let __kt = namespace_deeper_Object_with_package_INNER_CLASS_init_allocate()
                super.init(__externalRCRef: __kt)
                namespace_deeper_Object_with_package_INNER_CLASS_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public final class INNER_OBJECT : KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT {
                get {
                    return ExportedKotlinPackages.namespace.deeper.Object_with_package.INNER_OBJECT(__externalRCRef: namespace_deeper_Object_with_package_INNER_OBJECT_get())
                }
            }
            private override init() {
                fatalError()
            }
            public override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
        }
        public static var shared: ExportedKotlinPackages.namespace.deeper.Object_with_package {
            get {
                return ExportedKotlinPackages.namespace.deeper.Object_with_package(__externalRCRef: namespace_deeper_Object_with_package_get())
            }
        }
        private override init() {
            fatalError()
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension ExportedKotlinPackages.ignored {
    public final class DATA_CLASS : KotlinRuntime.KotlinBase {
        public var a: Swift.Int32 {
            get {
                return ignored_DATA_CLASS_a_get(self.__externalRCRef())
            }
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            a: Swift.Int32
        ) {
            let __kt = ignored_DATA_CLASS_init_allocate()
            super.init(__externalRCRef: __kt)
            ignored_DATA_CLASS_init_initialize__TypesOfArguments__Swift_UInt_Swift_Int32__(__kt, a)
        }
        public func copy(
            a: Swift.Int32
        ) -> ExportedKotlinPackages.ignored.DATA_CLASS {
            return ExportedKotlinPackages.ignored.DATA_CLASS(__externalRCRef: ignored_DATA_CLASS_copy__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), a))
        }
        public func hashCode() -> Swift.Int32 {
            return ignored_DATA_CLASS_hashCode(self.__externalRCRef())
        }
        public func toString() -> Swift.String {
            return ignored_DATA_CLASS_toString(self.__externalRCRef())
        }
    }
    public static func produce_ABSTRACT_CLASS() -> Swift.Never {
        fatalError()
    }
    public static func produce_DATA_CLASS() -> ExportedKotlinPackages.ignored.DATA_CLASS {
        return ExportedKotlinPackages.ignored.DATA_CLASS(__externalRCRef: ignored_produce_DATA_CLASS())
    }
    public static func produce_ENUM() -> Swift.Never {
        fatalError()
    }
    public static func produce_INTERFACE() -> Swift.Never {
        fatalError()
    }
    public static func produce_VALUE_CLASS() -> Swift.Never {
        fatalError()
    }
    public static func receive_ABSTRACT_CLASS(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func receive_DATA_CLASS(
        x: ExportedKotlinPackages.ignored.DATA_CLASS
    ) -> Swift.Void {
        return ignored_receive_DATA_CLASS__TypesOfArguments__ExportedKotlinPackages_ignored_DATA_CLASS__(x.__externalRCRef())
    }
    public static func receive_ENUM(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func receive_INTERFACE(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
    public static func receive_VALUE_CLASS(
        x: Swift.Never
    ) -> Swift.Void {
        fatalError()
    }
}
