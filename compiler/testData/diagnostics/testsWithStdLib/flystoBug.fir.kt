// For PCLA branch
// FIR_DUMP

class Data<D : Comparable<D>>(val converter: DoubleConverter<D>)

interface Units<UValue : Any> : DoubleConverter<UValue> {
    companion object {
        val Percent = SimpleDoubleUnits()
    }
}

class SimpleDoubleUnits : BaseUnits<Double>()

abstract class BaseUnits<BValue : Any> : Units<BValue>

interface DoubleConverter<DValue : Any>

interface Renderer<TX : Any, Left : Any, Right : Any> {
    companion object {
        fun <BX : Any, BLeft : Any, BRight : Any> build(
            xConverter: DoubleConverter<BX>,
            builderCode: RendererBuilder<BX, BLeft, BRight>.() -> Unit
        ): Renderer<BX, BLeft, BRight> = null!!
    }
}

class RendererBuilder<RBX : Any, RBLeft : Any, RBRight : Any> {
    var leftScaleCurves: CurveSet<RBLeft>? = null


    fun addDecorations(render: RenderDecorations<RBX, RBLeft, RBRight>) {}
}

typealias RenderDecorations<GDX, GDLeft, GDRight> = suspend RenderContext<GDX, GDLeft, GDRight>.() -> Unit

interface RenderContext<RCX : Any, RCLeft : Any, RCRight : Any> {
    val leftScaleValueToY: ((RCLeft) -> Double)?
}

class State<S : Comparable<S>>(val data: Data<S>) {
    suspend fun render() {
        val renderer = <!NEW_INFERENCE_ERROR!>Renderer.build(xConverter = data.converter) {
            leftScaleCurves = CurveSet(units = Units.Percent)
            <!NEW_INFERENCE_ERROR!>addDecorations {
                val top = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>leftScaleValueToY!!<!>(0.67)
                <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>renderCurrentValue<!>(top)
            }<!>
        }<!>
    }

    private fun RenderContext<S, Double, Unit>.renderCurrentValue(d: Double) {}
}

class CurveSet<CY : Any>(units: Units<CY>)
