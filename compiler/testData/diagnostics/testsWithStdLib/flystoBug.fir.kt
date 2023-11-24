// For PCLA branch
// FIR_DUMP

interface Units<UValue : Any>

class SimpleDoubleUnits : Units<Double>

fun <BLeft : Any> build(builderCode: RendererBuilder<BLeft>.() -> Unit) {}

class RendererBuilder<RBLeft : Any> {
    var leftScaleCurves: CurveSet<RBLeft>? = null

    fun addDecorations(render: suspend RenderContext<RBLeft>.() -> Unit) {}
}

interface RenderContext<RCLeft : Any> {
    val leftScaleValueToY: ((RCLeft) -> Double)?
}

class State {
    suspend fun render() {
        <!NEW_INFERENCE_ERROR!>build {
            leftScaleCurves = CurveSet(SimpleDoubleUnits())
            <!NEW_INFERENCE_ERROR!>addDecorations {
                <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>leftScaleValueToY!!<!>(0.67)
            }<!>
        }<!>
    }
}

class CurveSet<CY : Any>(units: Units<CY>)
