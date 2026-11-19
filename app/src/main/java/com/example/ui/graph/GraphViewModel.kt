package com.example.ui.graph

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.example.math.AngleUnit
import com.example.math.ExprNode
import com.example.math.MathParser
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EngineeringOrange
import com.example.ui.theme.PrecisionTeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

data class GraphFunction(
    val id: Int,
    val label: String,
    val expression: String,
    val color: Color,
    val isVisible: Boolean = true,
    val compiledAst: ExprNode? = null,
    val error: String? = null
)

data class GraphFeaturePoint(
    val kind: String, // "Root", "Min", "Max", "Intersect"
    val x: Double,
    val y: Double,
    val color: Color
)

data class GraphTableRow(
    val x: Double,
    val values: List<Double?>
)

data class GraphUiState(
    val functions: List<GraphFunction> = emptyList(),
    val activeFunctionIndex: Int = 0,
    val centerX: Double = 0.0,
    val centerY: Double = 0.0,
    val spanX: Double = 14.0,
    val spanY: Double = 10.0,
    val isTraceMode: Boolean = false,
    val traceX: Double? = null,
    val traceValues: List<Pair<String, Double?>> = emptyList(),
    val showKeyPoints: Boolean = true,
    val keyPoints: List<GraphFeaturePoint> = emptyList(),
    val angleUnit: AngleUnit = AngleUnit.RAD
)

class GraphViewModel : ViewModel() {
    private val palette = listOf(
        PrecisionTeal,
        EngineeringOrange,
        ElectricCyan
    )

    private val _uiState = MutableStateFlow(
        GraphUiState(
            functions = listOf(
                compileFunction(0, "f₁(x)", "sin(x) + 0.5*cos(2*x)", palette[0], true),
                compileFunction(1, "f₂(x)", "0.2*x^2 - 1.5", palette[1], true),
                compileFunction(2, "f₃(x)", "", palette[2], false)
            )
        ).let { computeKeyPoints(it) }
    )
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    fun setAngleUnit(unit: AngleUnit) {
        _uiState.update { s ->
            val updated = s.copy(angleUnit = unit)
            computeKeyPoints(updateTraceInternal(updated))
        }
    }

    fun selectFunction(index: Int) {
        _uiState.update { it.copy(activeFunctionIndex = index.coerceIn(0, it.functions.lastIndex)) }
    }

    fun updateFunctionExpression(index: Int, expr: String) {
        _uiState.update { s ->
            val updatedList = s.functions.mapIndexed { idx, fn ->
                if (idx == index) {
                    compileFunction(fn.id, fn.label, expr, fn.color, if (expr.isNotBlank()) true else fn.isVisible)
                } else fn
            }
            computeKeyPoints(updateTraceInternal(s.copy(functions = updatedList)))
        }
    }

    fun appendToActiveFunction(token: String) {
        val idx = _uiState.value.activeFunctionIndex
        val cur = _uiState.value.functions.getOrNull(idx)?.expression ?: ""
        updateFunctionExpression(idx, cur + token)
    }

    fun toggleFunctionVisibility(index: Int) {
        _uiState.update { s ->
            val updatedList = s.functions.mapIndexed { idx, fn ->
                if (idx == index) fn.copy(isVisible = !fn.isVisible) else fn
            }
            computeKeyPoints(updateTraceInternal(s.copy(functions = updatedList)))
        }
    }

    fun loadExternalExpression(expr: String) {
        _uiState.update { s ->
            val updatedList = s.functions.mapIndexed { idx, fn ->
                when (idx) {
                    0 -> compileFunction(fn.id, fn.label, expr, fn.color, true)
                    else -> fn.copy(isVisible = false)
                }
            }
            computeKeyPoints(
                s.copy(
                    functions = updatedList,
                    activeFunctionIndex = 0,
                    centerX = 0.0,
                    centerY = 0.0,
                    spanX = 16.0,
                    spanY = 12.0,
                    isTraceMode = false,
                    traceX = null
                )
            )
        }
    }

    fun pan(deltaXUnits: Double, deltaYUnits: Double) {
        _uiState.update { s ->
            computeKeyPoints(
                s.copy(
                    centerX = s.centerX + deltaXUnits,
                    centerY = s.centerY + deltaYUnits
                )
            )
        }
    }

    fun transform(
        panPxX: Float,
        panPxY: Float,
        centroidPxX: Float,
        centroidPxY: Float,
        zoom: Float,
        widthPx: Int,
        heightPx: Int
    ) {
        if (widthPx <= 0 || heightPx <= 0 || !zoom.isFinite() || zoom <= 0f) return
        _uiState.update { s ->
            val fracX = (centroidPxX.toDouble() / widthPx.toDouble()).coerceIn(0.0, 1.0)
            val fracY = (centroidPxY.toDouble() / heightPx.toDouble()).coerceIn(0.0, 1.0)

            val worldXAtCentroid = s.centerX + (fracX - 0.5) * s.spanX
            val worldYAtCentroid = s.centerY + (0.5 - fracY) * s.spanY

            val safeZoom = zoom.toDouble().coerceIn(0.5, 2.0)
            val newSpanX = (s.spanX / safeZoom).coerceIn(0.05, 5000.0)
            val newSpanY = (s.spanY / safeZoom).coerceIn(0.05, 5000.0)

            val newCenterX = worldXAtCentroid - (fracX - 0.5) * newSpanX -
                (panPxX.toDouble() / widthPx.toDouble()) * newSpanX
            val newCenterY = worldYAtCentroid - (0.5 - fracY) * newSpanY +
                (panPxY.toDouble() / heightPx.toDouble()) * newSpanY

            computeKeyPoints(
                s.copy(
                    centerX = newCenterX,
                    centerY = newCenterY,
                    spanX = newSpanX,
                    spanY = newSpanY
                )
            )
        }
    }

    fun zoom(factor: Double) {
        _uiState.update { s ->
            val newSpanX = (s.spanX * factor).coerceIn(0.05, 5000.0)
            val newSpanY = (s.spanY * factor).coerceIn(0.05, 5000.0)
            computeKeyPoints(s.copy(spanX = newSpanX, spanY = newSpanY))
        }
    }

    fun resetView() {
        _uiState.update { s ->
            computeKeyPoints(s.copy(centerX = 0.0, centerY = 0.0, spanX = 14.0, spanY = 10.0))
        }
    }

    fun autoFitY() {
        val s = _uiState.value
        val xMin = s.centerX - s.spanX / 2.0
        val xMax = s.centerX + s.spanX / 2.0
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        val visibleAsts = s.functions.filter { it.isVisible && it.compiledAst != null }.map { it.compiledAst!! }
        if (visibleAsts.isEmpty()) return

        for (i in 0..160) {
            val x = xMin + (xMax - xMin) * i / 160.0
            for (ast in visibleAsts) {
                val y = ast.evalDouble(x, s.angleUnit)
                if (y.isFinite() && abs(y) < 1e6) {
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (minY.isFinite() && maxY.isFinite()) {
            val rawSpan = (maxY - minY).coerceAtLeast(1.0)
            val padSpan = rawSpan * 1.25
            _uiState.update {
                it.copy(
                    centerY = (minY + maxY) / 2.0,
                    spanY = padSpan.coerceIn(0.2, 5000.0)
                )
            }
        }
    }

    fun toggleTraceMode() {
        _uiState.update { s ->
            val next = !s.isTraceMode
            val initX = if (next) (s.traceX ?: s.centerX) else null
            updateTraceInternal(s.copy(isTraceMode = next, traceX = initX))
        }
    }

    fun setTraceX(x: Double) {
        _uiState.update { s ->
            updateTraceInternal(s.copy(traceX = x))
        }
    }

    fun toggleKeyPoints() {
        _uiState.update { it.copy(showKeyPoints = !it.showKeyPoints) }
    }

    fun generateTable(startX: Double, step: Double, count: Int = 21): List<GraphTableRow> {
        val s = _uiState.value
        val safeStep = if (abs(step) < 1e-9) 1.0 else step
        return (0 until count).map { i ->
            val x = startX + i * safeStep
            val vals = s.functions.map { fn ->
                if (fn.isVisible && fn.compiledAst != null) {
                    val y = fn.compiledAst.evalDouble(x, s.angleUnit)
                    if (y.isFinite()) y else null
                } else null
            }
            GraphTableRow(x = x, values = vals)
        }
    }

    private fun updateTraceInternal(s: GraphUiState): GraphUiState {
        val tx = s.traceX ?: return s.copy(traceValues = emptyList())
        val vals = s.functions.filter { it.isVisible && it.compiledAst != null }.map { fn ->
            val y = fn.compiledAst!!.evalDouble(tx, s.angleUnit)
            fn.label to if (y.isFinite()) y else null
        }
        return s.copy(traceValues = vals)
    }

    private fun computeKeyPoints(s: GraphUiState): GraphUiState {
        val primary = s.functions.firstOrNull { it.isVisible && it.compiledAst != null }
            ?: return s.copy(keyPoints = emptyList())
        val ast = primary.compiledAst ?: return s.copy(keyPoints = emptyList())

        val xMin = s.centerX - s.spanX / 2.0
        val xMax = s.centerX + s.spanX / 2.0
        val yMin = s.centerY - s.spanY / 2.0
        val yMax = s.centerY + s.spanY / 2.0

        val n = 220
        val dx = (xMax - xMin) / n
        val xs = DoubleArray(n + 1) { i -> xMin + i * dx }
        val ys = DoubleArray(n + 1) { i -> ast.evalDouble(xs[i], s.angleUnit) }

        val pts = mutableListOf<GraphFeaturePoint>()

        // Roots & local extrema of primary visible function
        for (i in 1 until n) {
            val yPrev = ys[i - 1]
            val yCurr = ys[i]
            val yNext = ys[i + 1]
            if (!yPrev.isFinite() || !yCurr.isFinite() || !yNext.isFinite()) continue

            // Root crossing
            if ((yPrev > 0 && yCurr < 0) || (yPrev < 0 && yCurr > 0)) {
                val rootX = refineBisection(xs[i - 1], xs[i]) { x -> ast.evalDouble(x, s.angleUnit) }
                if (rootX != null && 0.0 in yMin..yMax) {
                    pts.add(GraphFeaturePoint("Root", rootX, 0.0, primary.color))
                }
            } else if (abs(yCurr) < 1e-6 && abs(yCurr) <= abs(yPrev) && abs(yCurr) <= abs(yNext)) {
                pts.add(GraphFeaturePoint("Root", xs[i], 0.0, primary.color))
            }

            // Local maximum / minimum
            if (yCurr > yPrev && yCurr > yNext && abs(yCurr - yPrev) < s.spanY * 0.5) {
                if (yCurr in yMin..yMax) {
                    pts.add(GraphFeaturePoint("Max", xs[i], yCurr, Color(0xFFFBBF24)))
                }
            } else if (yCurr < yPrev && yCurr < yNext && abs(yPrev - yCurr) < s.spanY * 0.5) {
                if (yCurr in yMin..yMax) {
                    pts.add(GraphFeaturePoint("Min", xs[i], yCurr, Color(0xFFFBBF24)))
                }
            }
        }

        // Intersections between first two visible functions
        val secondary = s.functions.filter { it.isVisible && it.compiledAst != null }.getOrNull(1)
        if (secondary?.compiledAst != null) {
            val ast2 = secondary.compiledAst
            val diffs = DoubleArray(n + 1) { i ->
                val y2 = ast2.evalDouble(xs[i], s.angleUnit)
                if (ys[i].isFinite() && y2.isFinite()) ys[i] - y2 else Double.NaN
            }
            for (i in 1..n) {
                val d0 = diffs[i - 1]
                val d1 = diffs[i]
                if (d0.isFinite() && d1.isFinite() && ((d0 > 0 && d1 < 0) || (d0 < 0 && d1 > 0))) {
                    val ix = refineBisection(xs[i - 1], xs[i]) { x ->
                        ast.evalDouble(x, s.angleUnit) - ast2.evalDouble(x, s.angleUnit)
                    }
                    if (ix != null) {
                        val iy = ast.evalDouble(ix, s.angleUnit)
                        if (iy.isFinite() && iy in yMin..yMax) {
                            pts.add(GraphFeaturePoint("Intersect", ix, iy, Color(0xFFA78BFA)))
                        }
                    }
                }
            }
        }

        return s.copy(keyPoints = pts.take(18))
    }

    private fun refineBisection(aInit: Double, bInit: Double, f: (Double) -> Double): Double? {
        var a = aInit
        var b = bInit
        var fa = f(a)
        var fb = f(b)
        if (!fa.isFinite() || !fb.isFinite() || fa * fb > 0) return null
        repeat(24) {
            val mid = (a + b) * 0.5
            val fm = f(mid)
            if (!fm.isFinite()) return null
            if (abs(fm) < 1e-11) return mid
            if (fa * fm <= 0) {
                b = mid; fb = fm
            } else {
                a = mid; fa = fm
            }
        }
        val mid = (a + b) * 0.5
        return if (abs(f(mid)) < 1.0) mid else null
    }

    private fun compileFunction(
        id: Int,
        label: String,
        expr: String,
        color: Color,
        isVisible: Boolean
    ): GraphFunction {
        if (expr.isBlank()) {
            return GraphFunction(id, label, expr, color, isVisible, compiledAst = null, error = null)
        }
        return try {
            val ast = MathParser.parse(expr)
            GraphFunction(id, label, expr, color, isVisible, compiledAst = ast, error = null)
        } catch (e: Exception) {
            GraphFunction(id, label, expr, color, isVisible, compiledAst = null, error = e.message ?: "Syntax error")
        }
    }
}
