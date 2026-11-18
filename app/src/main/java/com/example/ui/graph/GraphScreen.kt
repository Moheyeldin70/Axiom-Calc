package com.example.ui.graph

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.math.AngleUnit
import com.example.ui.components.QuickTokenRow
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    angleUnit: AngleUnit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    var showTableSheet by remember { mutableStateOf(false) }
    var showFunctionsPanel by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Header + Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Function Grapher",
                    style = MaterialTheme.typography.titleLarge
                )
                StatusPill(
                    text = angleUnit.label,
                    testTag = "graph_angle_badge",
                    onClick = {},
                    active = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(
                    text = if (showFunctionsPanel) "Hide f(x)" else "Edit f(x)",
                    testTag = "toggle_functions_panel",
                    onClick = { showFunctionsPanel = !showFunctionsPanel },
                    active = showFunctionsPanel,
                    accentColor = cs.tertiary
                )
                StatusPill(
                    text = "Table",
                    testTag = "graph_table_button",
                    onClick = { showTableSheet = true },
                    active = false
                )
            }
        }

        // Collapsible Function Input Panel
        if (showFunctionsPanel) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = cs.surface,
                border = BorderStroke(1.dp, cs.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.functions.forEachIndexed { idx, fn ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(fn.color)
                            )
                            OutlinedTextField(
                                value = fn.expression,
                                onValueChange = {
                                    viewModel.selectFunction(idx)
                                    viewModel.updateFunctionExpression(idx, it)
                                },
                                label = { Text(fn.label, fontSize = 11.sp) },
                                placeholder = { Text("Enter expression in x...") },
                                isError = fn.error != null,
                                supportingText = fn.error?.let { err -> { Text(err, fontSize = 10.sp) } },
                                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("graph_fn_input_$idx")
                            )
                            IconButton(
                                onClick = { viewModel.toggleFunctionVisibility(idx) },
                                modifier = Modifier.testTag("graph_fn_vis_$idx")
                            ) {
                                Icon(
                                    imageVector = if (fn.isVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle ${fn.label} visibility",
                                    tint = if (fn.isVisible) fn.color else cs.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        QuickTokenRow(
                            tokens = listOf(
                                "x" to "x",
                                "^" to "^",
                                "sin(" to "sin(",
                                "cos(" to "cos(",
                                "sinh(" to "sinh(",
                                "ln(" to "ln(",
                                "sqrt(" to "sqrt(",
                                "abs(" to "abs(",
                                "π" to "π",
                                "e" to "e"
                            ),
                            onInsert = viewModel::appendToActiveFunction
                        )
                    }
                }
            }
        }

        // Interactive Graph Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            color = cs.surfaceContainerLowest,
            border = BorderStroke(1.dp, cs.outline)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                InteractiveGraphCanvas(
                    state = state,
                    onPan = viewModel::pan,
                    onZoom = viewModel::zoom,
                    onTraceX = viewModel::setTraceX,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("graph_canvas")
                )

                // Floating Trace Readout Overlay
                if (state.isTraceMode && state.traceX != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = cs.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, cs.secondary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = "x = ${formatGraphNum(state.traceX!!)}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = cs.secondary,
                                modifier = Modifier.testTag("trace_readout_x")
                            )
                            state.traceValues.forEach { (label, yVal) ->
                                Text(
                                    text = "$label = ${yVal?.let { formatGraphNum(it) } ?: "undef"}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = cs.onSurface
                                )
                            }
                        }
                    }
                }

                // Floating Graph Action Toolbar (bottom-right)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.surface.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, cs.outlineVariant),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = viewModel::toggleTraceMode,
                            modifier = Modifier.testTag("graph_trace_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = "Toggle trace mode",
                                tint = if (state.isTraceMode) cs.secondary else cs.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.zoom(0.75) },
                            modifier = Modifier.testTag("graph_zoom_in")
                        ) {
                            Icon(Icons.Outlined.ZoomIn, contentDescription = "Zoom in", tint = cs.onSurface)
                        }
                        IconButton(
                            onClick = { viewModel.zoom(1.3333) },
                            modifier = Modifier.testTag("graph_zoom_out")
                        ) {
                            Icon(Icons.Outlined.ZoomOut, contentDescription = "Zoom out", tint = cs.onSurface)
                        }
                        IconButton(
                            onClick = viewModel::autoFitY,
                            modifier = Modifier.testTag("graph_autofit_y")
                        ) {
                            Icon(Icons.Outlined.FitScreen, contentDescription = "Auto-fit Y axis", tint = cs.onSurface)
                        }
                        IconButton(
                            onClick = viewModel::resetView,
                            modifier = Modifier.testTag("graph_reset_view")
                        ) {
                            Icon(Icons.Outlined.CenterFocusStrong, contentDescription = "Reset viewport", tint = cs.onSurface)
                        }
                    }
                }
            }
        }

        // Key Points Summary Strip (Roots, Min, Max, Intersections)
        if (state.keyPoints.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.showKeyPoints,
                    onClick = viewModel::toggleKeyPoints,
                    label = { Text("Key Points", fontSize = 11.sp) }
                )
                if (state.showKeyPoints) {
                    state.keyPoints.forEach { pt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = cs.surfaceContainer,
                            border = BorderStroke(1.dp, pt.color.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                if (!state.isTraceMode) viewModel.toggleTraceMode()
                                viewModel.setTraceX(pt.x)
                            }
                        ) {
                            Text(
                                text = "${pt.kind}: (${formatGraphNum(pt.x)}, ${formatGraphNum(pt.y)})",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = cs.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTableSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTableSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            GraphTableSheetContent(
                state = state,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun InteractiveGraphCanvas(
    state: GraphUiState,
    onPan: (Double, Double) -> Unit,
    onZoom: (Double) -> Unit,
    onTraceX: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val gridColor = cs.outlineVariant.copy(alpha = 0.6f)
    val subGridColor = cs.outlineVariant.copy(alpha = 0.25f)
    val axisColor = cs.onSurface.copy(alpha = 0.65f)
    val labelColor = cs.onSurfaceVariant
    val crosshairColor = cs.secondary

    val gestureModifier = if (state.isTraceMode) {
        modifier
            .pointerInput(state.centerX, state.spanX) {
                detectTapGestures { offset ->
                    val x = state.centerX + (offset.x / size.width - 0.5) * state.spanX
                    onTraceX(x)
                }
            }
            .pointerInput(state.centerX, state.spanX) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val x = state.centerX + (change.position.x / size.width - 0.5) * state.spanX
                    onTraceX(x)
                }
            }
    } else {
        modifier.pointerInput(state.spanX, state.spanY) {
            detectTransformGestures { _, pan, zoom, _ ->
                val dxUnits = -pan.x.toDouble() / size.width * state.spanX
                val dyUnits = pan.y.toDouble() / size.height * state.spanY
                if (abs(dxUnits) > 1e-12 || abs(dyUnits) > 1e-12) {
                    onPan(dxUnits, dyUnits)
                }
                if (abs(zoom - 1f) > 0.005f) {
                    onZoom(1.0 / zoom.toDouble())
                }
            }
        }
    }

    Canvas(
        modifier = gestureModifier.clipToBounds()
    ) {
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas

        val xMin = state.centerX - state.spanX / 2.0
        val xMax = state.centerX + state.spanX / 2.0
        val yMin = state.centerY - state.spanY / 2.0
        val yMax = state.centerY + state.spanY / 2.0

        fun toScreenX(x: Double): Float = ((x - xMin) / state.spanX * w).toFloat()
        fun toScreenY(y: Double): Float = (h - (y - yMin) / state.spanY * h).toFloat()

        // Grid step calculation
        val stepX = niceGridStep(state.spanX / 6.0)
        val stepY = niceGridStep(state.spanY / 5.0)

        // Subgrid lines
        val subStepX = stepX / 2.0
        var gxSub = floor(xMin / subStepX) * subStepX
        while (gxSub <= xMax) {
            val sx = toScreenX(gxSub)
            drawLine(subGridColor, Offset(sx, 0f), Offset(sx, h), strokeWidth = 1f)
            gxSub += subStepX
        }
        val subStepY = stepY / 2.0
        var gySub = floor(yMin / subStepY) * subStepY
        while (gySub <= yMax) {
            val sy = toScreenY(gySub)
            drawLine(subGridColor, Offset(0f, sy), Offset(w, sy), strokeWidth = 1f)
            gySub += subStepY
        }

        // Major grid lines + labels
        val labelStyle = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = labelColor
        )

        val axisXScreen = toScreenX(0.0).coerceIn(4f, w - 42f)
        val axisYScreen = toScreenY(0.0).coerceIn(4f, h - 22f)

        var gx = ceil(xMin / stepX) * stepX
        while (gx <= xMax) {
            val sx = toScreenX(gx)
            drawLine(gridColor, Offset(sx, 0f), Offset(sx, h), strokeWidth = 1.2f)
            if (abs(gx) > stepX * 0.05) {
                val txt = formatTickLabel(gx)
                val layout = textMeasurer.measure(txt, labelStyle)
                val tx = (sx - layout.size.width / 2f).coerceIn(2f, w - layout.size.width - 2f)
                drawText(layout, topLeft = Offset(tx, axisYScreen + 4f))
            }
            gx += stepX
        }

        var gy = ceil(yMin / stepY) * stepY
        while (gy <= yMax) {
            val sy = toScreenY(gy)
            drawLine(gridColor, Offset(0f, sy), Offset(w, sy), strokeWidth = 1.2f)
            if (abs(gy) > stepY * 0.05) {
                val txt = formatTickLabel(gy)
                val layout = textMeasurer.measure(txt, labelStyle)
                val ty = (sy - layout.size.height / 2f).coerceIn(2f, h - layout.size.height - 2f)
                drawText(layout, topLeft = Offset(axisXScreen + 4f, ty))
            }
            gy += stepY
        }

        // Primary Axes
        if (0.0 in xMin..xMax) {
            val sx0 = toScreenX(0.0)
            drawLine(axisColor, Offset(sx0, 0f), Offset(sx0, h), strokeWidth = 2.5f)
        }
        if (0.0 in yMin..yMax) {
            val sy0 = toScreenY(0.0)
            drawLine(axisColor, Offset(0f, sy0), Offset(w, sy0), strokeWidth = 2.5f)
        }

        // Plot each visible function
        val samples = (w / 2.2f).toInt().coerceIn(240, 720)
        state.functions.filter { it.isVisible && it.compiledAst != null }.forEach { fn ->
            val ast = fn.compiledAst!!
            val path = Path()
            var prevValid = false
            var prevY = 0.0

            for (i in 0..samples) {
                val frac = i.toDouble() / samples
                val x = xMin + frac * state.spanX
                val y = ast.evalDouble(x, state.angleUnit)
                if (!y.isFinite() || abs(y) > 1e7) {
                    prevValid = false
                    continue
                }
                val sx = (frac * w).toFloat()
                val sy = toScreenY(y)

                val jumpDiscontinuity = prevValid && abs(y - prevY) > state.spanY * 1.4 &&
                    ((y > yMax && prevY < yMin) || (y < yMin && prevY > yMax) || abs(y - prevY) > state.spanY * 2.0)

                if (!prevValid || jumpDiscontinuity) {
                    path.moveTo(sx, sy)
                } else {
                    path.lineTo(sx, sy)
                }
                prevValid = true
                prevY = y
            }

            drawPath(
                path = path,
                color = fn.color,
                style = Stroke(
                    width = 5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Key Point Markers
        if (state.showKeyPoints) {
            state.keyPoints.forEach { pt ->
                val sx = toScreenX(pt.x)
                val sy = toScreenY(pt.y)
                if (sx in -10f..(w + 10f) && sy in -10f..(h + 10f)) {
                    drawCircle(color = pt.color, radius = 9f, center = Offset(sx, sy))
                    drawCircle(color = Color.White, radius = 4f, center = Offset(sx, sy))
                }
            }
        }

        // Trace Crosshair
        if (state.isTraceMode && state.traceX != null) {
            val tx = state.traceX
            val sx = toScreenX(tx)
            drawLine(
                color = crosshairColor.copy(alpha = 0.7f),
                start = Offset(sx, 0f),
                end = Offset(sx, h),
                strokeWidth = 2f
            )
            state.functions.filter { it.isVisible && it.compiledAst != null }.forEach { fn ->
                val ty = fn.compiledAst!!.evalDouble(tx, state.angleUnit)
                if (ty.isFinite()) {
                    val sy = toScreenY(ty)
                    if (sy in 0f..h) {
                        drawCircle(color = fn.color, radius = 11f, center = Offset(sx, sy))
                        drawCircle(color = Color.White, radius = 5f, center = Offset(sx, sy))
                    }
                }
            }
        }
    }
}

@Composable
private fun GraphTableSheetContent(
    state: GraphUiState,
    viewModel: GraphViewModel
) {
    val cs = MaterialTheme.colorScheme
    var startText by remember { mutableStateOf("-5") }
    var stepText by remember { mutableStateOf("0.5") }
    val startVal = startText.toDoubleOrNull() ?: -5.0
    val stepVal = stepText.toDoubleOrNull() ?: 0.5
    val rows = remember(startVal, stepVal, state.functions, state.angleUnit) {
        viewModel.generateTable(startVal, stepVal, count = 25)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.graph_table_values),
            style = MaterialTheme.typography.titleLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it },
                label = { Text("Start x") },
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = stepText,
                onValueChange = { stepText = it },
                label = { Text("Step Δx") },
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cs.surfaceContainer, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "x",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            state.functions.filter { it.isVisible && it.compiledAst != null }.forEach { fn ->
                Text(
                    text = fn.label,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = fn.color,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
        ) {
            items(rows) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = formatGraphNum(row.x),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    state.functions.forEachIndexed { idx, fn ->
                        if (fn.isVisible && fn.compiledAst != null) {
                            val v = row.values.getOrNull(idx)
                            Text(
                                text = v?.let { formatGraphNum(it) } ?: "undef",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = cs.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

private fun niceGridStep(rawStep: Double): Double {
    if (rawStep <= 0.0 || !rawStep.isFinite()) return 1.0
    val mag = 10.0.pow(floor(log10(rawStep)))
    val norm = rawStep / mag
    return when {
        norm < 1.5 -> 1.0 * mag
        norm < 3.5 -> 2.0 * mag
        norm < 7.5 -> 5.0 * mag
        else -> 10.0 * mag
    }
}

private fun formatTickLabel(v: Double): String {
    if (abs(v) < 1e-10) return "0"
    return if (abs(v) >= 10000 || abs(v) < 0.01) {
        String.format("%.1e", v)
    } else {
        val rounded = (v * 1000.0).let { kotlin.math.round(it) / 1000.0 }
        if (rounded == floor(rounded)) rounded.toLong().toString() else rounded.toString()
    }
}

private fun formatGraphNum(v: Double): String {
    if (abs(v) < 1e-9) return "0"
    return if (abs(v) >= 1e5 || abs(v) < 1e-3) {
        String.format("%.4e", v)
    } else {
        String.format("%.4f", v).trimEnd('0').trimEnd('.')
    }
}
