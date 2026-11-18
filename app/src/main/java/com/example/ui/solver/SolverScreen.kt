package com.example.ui.solver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.math.AngleUnit
import com.example.math.BigMath
import com.example.math.LinearSystemResult
import com.example.ui.components.PrecisionCard
import com.example.ui.components.QuickTokenRow
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun SolverScreen(
    viewModel: SolverViewModel,
    angleUnit: AngleUnit,
    onPlotExpression: (String) -> Unit,
    onSendToCalculator: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    fun copyText(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        onShowMessage("Copied $text")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Angle Mode badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Equation Solver",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Roots, polynomials, linear matrices & calculus",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cs.secondaryContainer
            ) {
                Text(
                    text = angleUnit.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.secondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Solver Mode Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SolverMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { viewModel.setMode(mode) },
                    label = { Text(mode.label) },
                    modifier = Modifier.testTag("solver_mode_${mode.name.lowercase()}")
                )
            }
        }

        when (state.mode) {
            SolverMode.ROOT_FINDER -> RootFinderSection(
                state = state,
                viewModel = viewModel,
                angleUnit = angleUnit,
                onPlot = { onPlotExpression(viewModel.buildPlottableExpression()) },
                onCopy = { copyText("Root", it) },
                onSendToCalc = onSendToCalculator
            )
            SolverMode.POLYNOMIAL -> PolynomialSection(
                state = state,
                viewModel = viewModel,
                onCopy = { copyText("Root", it) }
            )
            SolverMode.LINEAR_SYSTEM -> LinearSystemSection(
                state = state,
                viewModel = viewModel,
                onCopy = { copyText("Solution", it) }
            )
            SolverMode.CALCULUS -> CalculusSection(
                state = state,
                viewModel = viewModel,
                angleUnit = angleUnit,
                onCopy = { copyText("Calculus Result", it) },
                onPlot = { onPlotExpression(state.calcExpression) },
                onSendToCalc = onSendToCalculator
            )
        }
    }
}

@Composable
private fun RootFinderSection(
    state: SolverUiState,
    viewModel: SolverViewModel,
    angleUnit: AngleUnit,
    onPlot: () -> Unit,
    onCopy: (String) -> Unit,
    onSendToCalc: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    PrecisionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("General Equation (f(x) = g(x))", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.equationText,
                onValueChange = viewModel::updateEquationText,
                label = { Text("Equation in x") },
                placeholder = { Text("e.g. cos(x) = x or x^3 - 2*x - 5 = 0") },
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 16.sp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("root_equation_input")
            )

            // Quick math symbol strip
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                QuickTokenRow(
                    tokens = listOf(
                        "x" to "x",
                        "=" to " = ",
                        "^" to "^",
                        "sin(" to "sin(",
                        "cos(" to "cos(",
                        "sinh(" to "sinh(",
                        "ln(" to "ln(",
                        "sqrt(" to "sqrt(",
                        "π" to "π",
                        "e" to "e"
                    ),
                    onInsert = viewModel::appendToEquation
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.rangeMin,
                    onValueChange = { viewModel.updateRange(it, state.rangeMax) },
                    label = { Text("Search Min") },
                    textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("root_range_min")
                )
                OutlinedTextField(
                    value = state.rangeMax,
                    onValueChange = { viewModel.updateRange(state.rangeMin, it) },
                    label = { Text("Search Max") },
                    textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("root_range_max")
                )
            }

            // Quick presets
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "x^3 - 2*x - 5 = 0",
                    "cos(x) = x",
                    "e^x - 4*x = 0",
                    "sinh(x) = 2"
                ).forEach { preset ->
                    FilterChip(
                        selected = state.equationText == preset,
                        onClick = { viewModel.updateEquationText(preset) },
                        label = {
                            Text(preset, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.solveGeneralEquation(angleUnit) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("solve_equation_button")
                ) {
                    Icon(Icons.Outlined.Calculate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Solve for x")
                }
                OutlinedButton(
                    onClick = onPlot,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("plot_equation_button")
                ) {
                    Icon(Icons.Outlined.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Plot")
                }
            }
        }
    }

    if (state.rootError != null) {
        PrecisionCard(borderColor = cs.error) {
            Text(
                text = state.rootError,
                color = cs.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("root_error_text")
            )
        }
    } else if (state.hasSolvedRoot) {
        PrecisionCard(borderColor = cs.secondary.copy(alpha = 0.5f)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (state.foundRoots.isEmpty()) {
                        "No real roots found in [${state.rangeMin}, ${state.rangeMax}]"
                    } else {
                        "Found ${state.foundRoots.size} real root${if (state.foundRoots.size > 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.secondary,
                    modifier = Modifier.testTag("root_results_header")
                )
                state.foundRoots.forEachIndexed { idx, root ->
                    val valStr = BigMath.formatResult(root.value, 14)
                    val resStr = BigMath.formatResult(root.residual, 4)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = cs.surfaceContainer,
                        border = BorderStroke(1.dp, cs.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "x${idx + 1} = $valStr",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = cs.onSurface,
                                    modifier = Modifier.testTag("root_result_$idx")
                                )
                                Text(
                                    text = "Residual |f(x)| ≈ $resStr",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = cs.onSurfaceVariant
                                )
                            }
                            Row {
                                IconButton(onClick = { onCopy(valStr) }) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy root", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onSendToCalc(valStr) }) {
                                    Icon(Icons.Outlined.Calculate, contentDescription = "Use in Compute", modifier = Modifier.size(18.dp), tint = cs.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PolynomialSection(
    state: SolverUiState,
    viewModel: SolverViewModel,
    onCopy: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    PrecisionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.polyDegree == 2,
                    onClick = { viewModel.setPolyDegree(2) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("poly_deg_2")
                ) { Text("Quadratic (ax² + bx + c)") }
                SegmentedButton(
                    selected = state.polyDegree == 3,
                    onClick = { viewModel.setPolyDegree(3) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("poly_deg_3")
                ) { Text("Cubic (ax³ + bx² + cx + d)") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoefField("a", state.coefA, { viewModel.updatePolyCoefs(a = it) }, Modifier.weight(1f), "poly_coef_a")
                CoefField("b", state.coefB, { viewModel.updatePolyCoefs(b = it) }, Modifier.weight(1f), "poly_coef_b")
                CoefField("c", state.coefC, { viewModel.updatePolyCoefs(c = it) }, Modifier.weight(1f), "poly_coef_c")
                if (state.polyDegree == 3) {
                    CoefField("d", state.coefD, { viewModel.updatePolyCoefs(d = it) }, Modifier.weight(1f), "poly_coef_d")
                }
            }

            Button(
                onClick = viewModel::solvePolynomial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("solve_poly_button")
            ) {
                Text("Solve Polynomial (Real & Complex)")
            }
        }
    }

    if (state.polyError != null) {
        PrecisionCard(borderColor = cs.error) {
            Text(state.polyError, color = cs.error)
        }
    } else if (state.polySolution != null) {
        val sol = state.polySolution
        PrecisionCard(borderColor = cs.secondary.copy(alpha = 0.5f)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sol.summary, style = MaterialTheme.typography.titleMedium, color = cs.secondary)
                sol.discriminant?.let { disc ->
                    Text(
                        text = "Discriminant Δ = ${BigMath.formatResult(disc, 12)}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = cs.outlineVariant)
                sol.roots.forEachIndexed { i, cRoot ->
                    val formatted = cRoot.format(12)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "x${i + 1} = $formatted",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("poly_root_$i")
                        )
                        IconButton(onClick = { onCopy(formatted) }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy root", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoefField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 15.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.testTag(testTag)
    )
}

@Composable
private fun LinearSystemSection(
    state: SolverUiState,
    viewModel: SolverViewModel,
    onCopy: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val vars = listOf("x", "y", "z")
    PrecisionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.systemSize == 2,
                    onClick = { viewModel.setSystemSize(2) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("linear_size_2")
                ) { Text("2 × 2 System") }
                SegmentedButton(
                    selected = state.systemSize == 3,
                    onClick = { viewModel.setSystemSize(3) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("linear_size_3")
                ) { Text("3 × 3 System") }
            }

            Text(
                text = "Enter coefficients for [A | B] where Ax = B:",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )

            for (r in 0 until state.systemSize) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in 0 until state.systemSize) {
                        OutlinedTextField(
                            value = state.matrixValues[r][c],
                            onValueChange = { viewModel.updateMatrixCell(r, c, it) },
                            label = { Text("${vars[c]}${r + 1}", fontSize = 11.sp) },
                            textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mat_${r}_${c}")
                        )
                    }
                    Text("=", fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = state.rhsValues[r],
                        onValueChange = { viewModel.updateRhsCell(r, it) },
                        label = { Text("b${r + 1}", fontSize = 11.sp) },
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rhs_$r")
                    )
                }
            }

            Button(
                onClick = viewModel::solveLinearSystem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("solve_linear_button")
            ) {
                Text("Solve Linear System")
            }
        }
    }

    if (state.linearError != null) {
        PrecisionCard(borderColor = cs.error) {
            Text(state.linearError, color = cs.error)
        }
    } else if (state.linearResult != null) {
        when (val res = state.linearResult) {
            is LinearSystemResult.Singular -> {
                PrecisionCard(borderColor = cs.error) {
                    Text(res.message, color = cs.error)
                }
            }
            is LinearSystemResult.Unique -> {
                PrecisionCard(borderColor = cs.secondary.copy(alpha = 0.5f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Unique Solution (det(A) = ${BigMath.formatResult(res.determinant, 10)})",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.secondary
                        )
                        HorizontalDivider(color = cs.outlineVariant)
                        res.solution.forEachIndexed { idx, v ->
                            val formatted = BigMath.formatResult(v, 14)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${vars[idx]} = $formatted",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("linear_sol_$idx")
                                )
                                IconButton(onClick = { onCopy(formatted) }) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy value", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculusSection(
    state: SolverUiState,
    viewModel: SolverViewModel,
    angleUnit: AngleUnit,
    onCopy: (String) -> Unit,
    onPlot: () -> Unit,
    onSendToCalc: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    PrecisionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !state.isIntegral,
                    onClick = { viewModel.setCalculusMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("calc_mode_deriv")
                ) { Text("Derivative d/dx") }
                SegmentedButton(
                    selected = state.isIntegral,
                    onClick = { viewModel.setCalculusMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("calc_mode_integral")
                ) { Text("Integral ∫ₐᵇ") }
            }

            OutlinedTextField(
                value = state.calcExpression,
                onValueChange = { viewModel.updateCalculusFields(expr = it) },
                label = { Text("Function f(x)") },
                textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 15.sp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calc_expr_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.calcPointA,
                    onValueChange = { viewModel.updateCalculusFields(a = it) },
                    label = { Text(if (state.isIntegral) "Lower Limit a" else "Point x = a") },
                    textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calc_point_a")
                )
                if (state.isIntegral) {
                    OutlinedTextField(
                        value = state.calcPointB,
                        onValueChange = { viewModel.updateCalculusFields(b = it) },
                        label = { Text("Upper Limit b") },
                        textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_point_b")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.computeCalculus(angleUnit) },
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("compute_calculus_button")
                ) {
                    Text(if (state.isIntegral) "Compute Integral" else "Compute f'(a)")
                }
                OutlinedButton(
                    onClick = onPlot,
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Outlined.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Plot")
                }
            }
        }
    }

    if (state.calcError != null) {
        PrecisionCard(borderColor = cs.error) {
            Text(state.calcError, color = cs.error)
        }
    } else if (state.calcResult != null) {
        PrecisionCard(borderColor = cs.secondary.copy(alpha = 0.5f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.isIntegral) "Definite Integral" else "Derivative f'(${state.calcPointA})",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.secondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.calcResult,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("calculus_result_text")
                    )
                }
                Row {
                    IconButton(onClick = { onCopy(state.calcResult) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy result")
                    }
                    IconButton(onClick = { onSendToCalc(state.calcResult) }) {
                        Icon(Icons.Outlined.Calculate, contentDescription = "Send to Compute", tint = cs.primary)
                    }
                }
            }
        }
    }
}
