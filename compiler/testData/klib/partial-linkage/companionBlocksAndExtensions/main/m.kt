import abitestutils.abiTest

fun box() = abiTest {
    expectSuccess("propertyChange.v2") { valChangeGet() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/A.removedVal.<get-removedVal>'")) { removedValGet() }
    expectSuccess("propertyChange.v2") { varChangeGet() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/A.removedVar.<get-removedVar>'")) { removedVarGet() }
    expectFailure(linkage("Property accessor 'removedVar.<set-removedVar>' can not be called: No property accessor found for symbol '/A.removedVar.<set-removedVar>'")) { removedVarSet() }

    expectSuccess("propertyChange.v2") { valChangeRef.invoke() }
    expectFailure(linkage("Property accessor 'removedVal.<get-removedVal>' can not be called: No property accessor found for symbol '/A.removedVal.<get-removedVal>'")) { removedValRef.invoke() }
    expectSuccess("propertyChange.v2") { varChangeRef.invoke() }
    expectFailure(linkage("Property accessor 'removedVar.<get-removedVar>' can not be called: No property accessor found for symbol '/A.removedVar.<get-removedVar>'")) { removedVarRef.invoke() }

    expectSuccess("bodyChange.v2") { bodyChangeCall() }
    expectFailure(linkage("Function 'removedFun' can not be called: No function found for symbol '/A.removedFun'")) { removedFunCall() }

    expectSuccess("bodyChange.v2") { bodyChangeRef.invoke() }
    expectFailure(linkage("Function 'removedFun' can not be called: No function found for symbol '/A.removedFun'")) { removedFunRef.invoke() }

    expectFailure(linkage("Function 'blockToObject' can not be called: No function found for symbol '/A.blockToObject'")) { blockToObjectCall() }
    expectFailure(linkage("Function 'objectToBlock' can not be called: No function found for symbol '/A.Companion.objectToBlock'")) { objectToBlockCall() }

    expectFailure(linkage("Function 'sameFun' can not be called: No function found for symbol '/RemovedBlock.sameFun'")) { noBlockSameFunCall() }
    expectSuccess("object") { newBlockSameFunCall() }
}
