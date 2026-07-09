fun valChangeGet() = A.valChange
fun removedValGet() = A.removedVal
fun varChangeGet() = A.varChange
fun removedVarGet() = A.removedVar
fun removedVarSet() {
    A.removedVar = 0
}

val valChangeRef = A::valChange
val removedValRef = A::removedVal
val varChangeRef = A::varChange
val removedVarRef = A::removedVar

fun bodyChangeCall() = A.bodyChange()
fun removedFunCall() = A.removedFun()

val bodyChangeRef = A::bodyChange
val removedFunRef = A::removedFun
