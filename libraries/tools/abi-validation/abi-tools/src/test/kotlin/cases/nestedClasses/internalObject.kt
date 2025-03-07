/*
 * Copyright 2016-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package cases.nestedClasses

internal object InternalObject {

    public object ObjPublic
    internal object ObjInternal
    private object ObjPrivate

    public class NestedPublic
    internal class NestedInternal
    private class NestedPrivate

    public interface NestedPublicInterface
    internal interface NestedInternalInterface
    private interface NestedPrivateInterface
}

