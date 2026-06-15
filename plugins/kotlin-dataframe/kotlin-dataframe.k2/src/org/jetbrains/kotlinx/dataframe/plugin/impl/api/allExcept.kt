/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.impl.api

import org.jetbrains.kotlinx.dataframe.plugin.impl.AbstractInterpreter
import org.jetbrains.kotlinx.dataframe.plugin.impl.Arguments

// region ColumnSet except

/** `df.select { colsOf<Number>() except { "age" and height } }` */
internal class ColumnSetExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { cols(name, age) except ("age" and height) }` */
internal class ColumnSetExceptColumnsResolver : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { colsOf<Number>().except(age, userData.height) }` */
internal class ColumnSetExceptColumnsResolvers : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { colsOf<Number>() except "age" }` */
internal class ColumnSetExceptString : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { colsOf<Number>().except("age", "height") }` */
internal class ColumnSetExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { colsOf<Number>() except "userdata"["age"] }` */
internal class ColumnSetExceptColumnPath : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { colsOf<Number>().except(pathOf("age"), "userdata"["height"]) }` */
internal class ColumnSetExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion

// region ColumnsSelectionDsl allExcept

/** `df.select { allExcept { "age" and height } }` */
internal class CSDslAllExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { allExcept(age, height) }` */
internal class CSDslAllExceptColumnsResolvers : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { allExcept("age", "height") }` */
internal class CSDslAllExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { allExcept(pathOf("age"), "userdata"["height"]) }` */
internal class CSDslAllExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion

// region SingleColumn allColsExcept


/** `df.select { userData.allColsExcept { "age" and height } }` */
internal class ColumnGroupAllColsExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { userData.allColsExcept("age", "height") }` */
internal class ColumnGroupAllColsExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { userData.allColsExcept(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnGroupAllColsExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion

// region String allColsExcept

/** `df.select { "userData".allColsExcept { "age" and height } }` */
internal class StringAllColsExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { "userData".allColsExcept("age", "height") }` */
internal class StringAllColsExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { "userData".allColsExcept(pathOf("age"), "extraData"["item1"]) }` */
internal class StringAllColsExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion

// region ColumnPath allColsExcept

/** `df.select { pathOf("userData").allColsExcept { "age" and height } }` */
internal class ColumnPathAllColsExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { pathOf("userData").allColsExcept("age", "height") }` */
internal class ColumnPathAllColsExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { pathOf("userData").allColsExcept(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnPathAllColsExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion

// region SingleColumn except

/** `df.select { userData.except { "age" and height } }` */
internal class ColumnGroupExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { userData.except("age", "height") }` */
internal class ColumnGroupExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { userData.except(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnGroupExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion

// region String except

/** `df.select { "userData".except { "age" and height } }` */
internal class StringExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { "userData".except("age", "height") }` */
internal class StringExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { "userData".except(pathOf("age"), "extraData"["item1"]) }` */
internal class StringExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion

// region ColumnPath except

/** `df.select { pathOf("userData").except { "age" and height } }` */
internal class ColumnPathExceptSelector : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { pathOf("userData").except("age", "height") }` */
internal class ColumnPathExceptStrings : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

/** `df.select { pathOf("userData").except(pathOf("age"), "extraData"["item1"]) }` */
internal class ColumnPathExceptColumnPaths : AbstractInterpreter<ColumnsResolver>() {
    override fun Arguments.interpret(): ColumnsResolver {
        TODO("Not yet implemented")
    }
}

// endregion
