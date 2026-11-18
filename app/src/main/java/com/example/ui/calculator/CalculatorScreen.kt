package com.example.ui.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CalculationRecord
import com.example.math.ScientificConstant
import com.example.math.ScientificConstants
import com.example.ui.components.KeyRole
import com.example.ui.components.PrecisionKeyButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ThemePreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showHistorySheet by remember { mutableStateOf(false) }
    var showConstantsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Precision Instrument Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPill(
                    text = state.settings.angleUnit.label,
                    testTag = "angle_unit_pill",
                    onClick = viewModel::cycleAngleUnit,
                    active = true,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
                StatusPill(
                    text = state.settings.notationMode.label,
                    testTag = "notation_mode_pill",
                    onClick = viewModel::cycleNotationMode,
                    active = state.settings.notationMode.name != "NORMAL",
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
                StatusPill(
                    text = "${state.settings.precisionDigits}d",
                    testTag = "precision_pill",
                    onClick = { showSettingsSheet = true },
                    active = false
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showConstantsSheet = true },
                    modifier = Modifier.testTag("constants_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = stringResource(R.string.title_constants),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showHistorySheet = true },
                    modifier = Modifier.testTag("history_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = stringResource(R.string.title_history),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.title_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Adaptive Display + Keypad Area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            val isShortViewport = maxHeight < 520.dp
            val isWide = maxWidth >= 660.dp

            val contentColumnModifier = if (isShortViewport) {
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier.fillMaxSize()
            }

            Column(
                modifier = contentColumnModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Precision Readout Display
                CalculatorDisplayPanel(
                    state = state,
                    onCopyResult = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Result", state.resultDisplay))
                        onShowMessage("Copied ${state.resultDisplay}")
                    },
                    modifier = if (isShortViewport) {
                        Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .weight(0.28f, fill = true)
                    }
                )

                // Memory & Mode Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val stripModifier = Modifier.weight(1f)
                    StatusPill(
                        text = "2nd",
                        testTag = "key_second",
                        onClick = viewModel::toggleSecondMode,
                        active = state.isSecondMode,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = stripModifier
                    )
                    StatusPill(
                        text = "HYP",
                        testTag = "key_hyp",
                        onClick = viewModel::toggleHyperbolicMode,
                        active = state.isHyperbolicMode,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = stripModifier
                    )
                    StatusPill(
                        text = "MC",
                        testTag = "key_mc",
                        onClick = viewModel::memoryClear,
                        modifier = stripModifier
                    )
                    StatusPill(
                        text = "MR",
                        testTag = "key_mr",
                        onClick = viewModel::memoryRecall,
                        active = state.settings.memoryValue != "0",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = stripModifier
                    )
                    StatusPill(
                        text = "M+",
                        testTag = "key_m_plus",
                        onClick = { viewModel.memoryAdd(subtract = false) },
                        modifier = stripModifier
                    )
                    StatusPill(
                        text = "M-",
                        testTag = "key_m_minus",
                        onClick = { viewModel.memoryAdd(subtract = true) },
                        modifier = stripModifier
                    )
                }

                // Adaptive Keypad: Side-by-side on wide screens/landscape, stacked on phones
                val keypadModifier = if (isShortViewport) {
                    Modifier
                        .fillMaxWidth()
                        .height(410.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .weight(0.72f, fill = true)
                }

                Box(modifier = keypadModifier) {
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ScientificPad(
                                isSecond = state.isSecondMode,
                                isHyp = state.isHyperbolicMode,
                                onToken = { tok -> viewModel.appendToken(tok) },
                                onFunc = { fn -> viewModel.appendFunction(fn) },
                                modifier = Modifier
                                    .weight(0.48f)
                                    .fillMaxHeight()
                            )
                            StandardNumberPad(
                                onDigit = { d -> viewModel.appendToken(d, isDigitOrDot = true) },
                                onOperator = { op -> viewModel.appendToken(op, isOperator = true) },
                                onNegate = viewModel::toggleNegate,
                                onBackspace = viewModel::backspace,
                                onClear = viewModel::clearAll,
                                onEquals = viewModel::evaluate,
                                modifier = Modifier
                                    .weight(0.52f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ScientificPad(
                                isSecond = state.isSecondMode,
                                isHyp = state.isHyperbolicMode,
                                onToken = { tok -> viewModel.appendToken(tok) },
                                onFunc = { fn -> viewModel.appendFunction(fn) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.37f)
                            )
                            StandardNumberPad(
                                onDigit = { d -> viewModel.appendToken(d, isDigitOrDot = true) },
                                onOperator = { op -> viewModel.appendToken(op, isOperator = true) },
                                onNegate = viewModel::toggleNegate,
                                onBackspace = viewModel::backspace,
                                onClear = viewModel::clearAll,
                                onEquals = viewModel::evaluate,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.63f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            HistorySheetContent(
                history = state.history,
                onSelectExpression = {
                    viewModel.loadHistoryRecord(it, useResultOnly = false)
                    showHistorySheet = false
                },
                onSelectResult = {
                    viewModel.loadHistoryRecord(it, useResultOnly = true)
                    showHistorySheet = false
                },
                onDelete = viewModel::deleteHistoryItem,
                onClearAll = viewModel::clearHistory
            )
        }
    }

    if (showConstantsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConstantsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ConstantsSheetContent(
                constants = ScientificConstants.all,
                onSelect = { constant ->
                    viewModel.appendToken(constant.insertToken, isDigitOrDot = true)
                    showConstantsSheet = false
                }
            )
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            SettingsSheetContent(
                precision = state.settings.precisionDigits,
                themePreference = state.settings.themePreference,
                onPrecisionChange = viewModel::setPrecision,
                onThemeChange = viewModel::setThemePreference
            )
        }
    }
}

@Composable
private fun CalculatorDisplayPanel(
    state: CalculatorUiState,
    onCopyResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val exprScroll = rememberScrollState()

    LaunchedEffect(state.expression) {
        exprScroll.animateScrollTo(exprScroll.maxValue)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = cs.surfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(cs.outline, cs.outlineVariant)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status indicators row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.settings.memoryValue != "0") {
                        Text(
                            text = "M=${state.settings.memoryValue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.tertiary
                        )
                    }
                    state.fractionHint?.let { frac ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = cs.secondaryContainer
                        ) {
                            Text(
                                text = "≈ $frac",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.secondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onCopyResult,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_result_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy result",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expression input line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(exprScroll),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = state.expression.ifEmpty { "0" },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (state.expression.length > 22) 19.sp else 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (state.isEvaluated) cs.onSurfaceVariant else cs.onBackground,
                    maxLines = 1,
                    modifier = Modifier.testTag("calculator_expression_text")
                )
            }

            // Error or High-Precision Result readout
            AnimatedVisibility(visible = state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.error,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calculator_error_text")
                )
            }

            if (state.errorMessage == null) {
                val resultText = if (state.isEvaluated) {
                    state.resultDisplay
                } else if (state.livePreview.isNotEmpty()) {
                    "= ${state.livePreview}"
                } else {
                    state.resultDisplay
                }
                val resultFontSize = when {
                    resultText.length > 22 -> 20.sp
                    resultText.length > 15 -> 26.sp
                    else -> 36.sp
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = resultText,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = resultFontSize,
                        fontWeight = if (state.isEvaluated) FontWeight.Bold else FontWeight.Medium,
                        color = if (state.isEvaluated) cs.onBackground else cs.secondary,
                        maxLines = 1,
                        modifier = Modifier.testTag("calculator_result_text")
                    )
                }
            }
        }
    }
}

@Composable
private fun ScientificPad(
    isSecond: Boolean,
    isHyp: Boolean,
    onToken: (String) -> Unit,
    onFunc: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trigSinLabel = when {
        isHyp && isSecond -> "sinh⁻¹"
        isHyp -> "sinh"
        isSecond -> "sin⁻¹"
        else -> "sin"
    }
    val trigSinFunc = when {
        isHyp && isSecond -> "asinh("
        isHyp -> "sinh("
        isSecond -> "asin("
        else -> "sin("
    }

    val trigCosLabel = when {
        isHyp && isSecond -> "cosh⁻¹"
        isHyp -> "cosh"
        isSecond -> "cos⁻¹"
        else -> "cos"
    }
    val trigCosFunc = when {
        isHyp && isSecond -> "acosh("
        isHyp -> "cosh("
        isSecond -> "acos("
        else -> "cos("
    }

    val trigTanLabel = when {
        isHyp && isSecond -> "tanh⁻¹"
        isHyp -> "tanh"
        isSecond -> "tan⁻¹"
        else -> "tan"
    }
    val trigTanFunc = when {
        isHyp && isSecond -> "atanh("
        isHyp -> "tanh("
        isSecond -> "atan("
        else -> "tan("
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Row 1: Trig / Hyperbolic & Logs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PrecisionKeyButton(
                label = trigSinLabel,
                testTag = "key_sin",
                onClick = { onFunc(trigSinFunc) },
                role = KeyRole.SCIENTIFIC,
                fontSize = 12.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = trigCosLabel,
                testTag = "key_cos",
                onClick = { onFunc(trigCosFunc) },
                role = KeyRole.SCIENTIFIC,
                fontSize = 12.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = trigTanLabel,
                testTag = "key_tan",
                onClick = { onFunc(trigTanFunc) },
                role = KeyRole.SCIENTIFIC,
                fontSize = 12.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "eˣ" else "ln",
                subLabel = if (isSecond) "ln" else "eˣ",
                testTag = "key_ln",
                onClick = { if (isSecond) onFunc("exp(") else onFunc("ln(") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "10ˣ" else "log",
                subLabel = if (isSecond) "log" else "10ˣ",
                testTag = "key_log",
                onClick = { if (isSecond) onToken("10^") else onFunc("log(") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "log₂" else "logb",
                testTag = "key_log2",
                onClick = { if (isSecond) onFunc("log2(") else onFunc("logb(") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 12.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 2: Powers, Roots, Parentheses, Factorial
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PrecisionKeyButton(
                label = if (isSecond) "√x" else "x²",
                subLabel = if (isSecond) "x²" else "√x",
                testTag = "key_sq_sqrt",
                onClick = { if (isSecond) onFunc("sqrt(") else onToken("^2") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "∛x" else "x³",
                subLabel = if (isSecond) "x³" else "∛x",
                testTag = "key_cube_cbrt",
                onClick = { if (isSecond) onFunc("cbrt(") else onToken("^3") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "ⁿ√x" else "xʸ",
                subLabel = if (isSecond) "xʸ" else "ⁿ√x",
                testTag = "key_pow_nroot",
                onClick = { if (isSecond) onFunc("nroot(") else onToken("^") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = "(",
                testTag = "key_lparen",
                onClick = { onToken("(") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 15.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = ")",
                testTag = "key_rparen",
                onClick = { onToken(")") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 15.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "nCr" else "n!",
                subLabel = if (isSecond) "n!" else "nCr",
                testTag = "key_fact_ncr",
                onClick = { if (isSecond) onFunc("ncr(") else onToken("!") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Row 3: Constants, Comma, Abs, Reciprocal, ANS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PrecisionKeyButton(
                label = "π",
                testTag = "key_pi",
                onClick = { onToken("π") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 15.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = "e",
                testTag = "key_e",
                onClick = { onToken("e") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 15.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "nPr" else "|x|",
                testTag = "key_abs_npr",
                onClick = { if (isSecond) onFunc("npr(") else onFunc("abs(") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = "1/x",
                testTag = "key_inv",
                onClick = { onToken("^(-1)") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = if (isSecond) "," else "EE",
                subLabel = if (isSecond) "EE" else ",",
                testTag = "key_ee_comma",
                onClick = { if (isSecond) onToken(",") else onToken("E") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 13.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            PrecisionKeyButton(
                label = "ANS",
                testTag = "key_ans",
                onClick = { onToken("ans") },
                role = KeyRole.SCIENTIFIC,
                fontSize = 12.sp,
                minHeight = 48.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun StandardNumberPad(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onNegate: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onEquals: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrecisionKeyButton("AC", "key_ac", onClear, role = KeyRole.ACTION, fontSize = 16.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("key_del")
                    .clickable(onClick = onBackspace),
                shape = RoundedCornerShape(12.dp),
                color = cs.surfaceContainerHighest,
                border = BorderStroke(1.dp, cs.outlineVariant)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Backspace",
                        tint = cs.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            PrecisionKeyButton("%", "key_percent", { onOperator("%") }, role = KeyRole.OPERATOR, fontSize = 19.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("÷", "key_div", { onOperator("/") }, role = KeyRole.OPERATOR, fontSize = 22.sp, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        // Row 2
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrecisionKeyButton("7", "key_7", { onDigit("7") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("8", "key_8", { onDigit("8") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("9", "key_9", { onDigit("9") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("×", "key_mul", { onOperator("*") }, role = KeyRole.OPERATOR, fontSize = 22.sp, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        // Row 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrecisionKeyButton("4", "key_4", { onDigit("4") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("5", "key_5", { onDigit("5") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("6", "key_6", { onDigit("6") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("−", "key_sub", { onOperator("-") }, role = KeyRole.OPERATOR, fontSize = 22.sp, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        // Row 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrecisionKeyButton("1", "key_1", { onDigit("1") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("2", "key_2", { onDigit("2") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("3", "key_3", { onDigit("3") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("+", "key_add", { onOperator("+") }, role = KeyRole.OPERATOR, fontSize = 22.sp, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        // Row 5
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PrecisionKeyButton("±", "key_negate", onNegate, role = KeyRole.DIGIT, fontSize = 20.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("0", "key_0", { onDigit("0") }, fontSize = 21.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton(".", "key_dot", { onDigit(".") }, fontSize = 22.sp, modifier = Modifier.weight(1f).fillMaxHeight())
            PrecisionKeyButton("=", "key_equals", onEquals, role = KeyRole.EQUALS, fontSize = 24.sp, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun HistorySheetContent(
    history: List<CalculationRecord>,
    onSelectExpression: (CalculationRecord) -> Unit,
    onSelectResult: (CalculationRecord) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.title_history),
                style = MaterialTheme.typography.titleLarge
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Text(stringResource(R.string.clear_all), color = cs.error)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Functions,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(R.string.empty_history_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.empty_history_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cs.surfaceContainer,
                        border = BorderStroke(1.dp, cs.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectExpression(item) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = cs.secondaryContainer
                                    ) {
                                        Text(
                                            text = item.angleUnit,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cs.secondary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = item.expression,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 13.sp,
                                        color = cs.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "= ${item.result}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = cs.onSurface,
                                    modifier = Modifier.clickable { onSelectResult(item) }
                                )
                            }
                            IconButton(onClick = { onDelete(item.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete record",
                                    tint = cs.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConstantsSheetContent(
    constants: List<ScientificConstant>,
    onSelect: (ScientificConstant) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text(
            text = stringResource(R.string.title_constants),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Tap any physical or mathematical constant to insert into your expression.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(constants, key = { it.insertToken }) { c ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cs.surfaceContainer,
                    border = BorderStroke(1.dp, cs.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(c) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = cs.tertiaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = c.symbol,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = cs.onTertiaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = c.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${c.displayValue} ${c.unit}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                color = cs.secondary
                            )
                        }
                        Text(
                            text = c.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSheetContent(
    precision: Int,
    themePreference: ThemePreference,
    onPrecisionChange: (Int) -> Unit,
    onThemeChange: (ThemePreference) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.title_settings),
            style = MaterialTheme.typography.titleLarge
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Display Precision", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "$precision significant digits",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 13.sp,
                    color = cs.secondary
                )
            }
            Slider(
                value = precision.toFloat(),
                onValueChange = { onPrecisionChange(it.roundToInt()) },
                valueRange = 6f..28f,
                steps = 21,
                modifier = Modifier.testTag("precision_slider")
            )
            Text(
                text = "Internal calculations always execute at 56-digit precision regardless of display rounding.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
        }

        HorizontalDivider(color = cs.outlineVariant)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleSmall)
            val themes = listOf(ThemePreference.DARK, ThemePreference.LIGHT, ThemePreference.SYSTEM)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themes.forEachIndexed { idx, pref ->
                    SegmentedButton(
                        selected = themePreference == pref,
                        onClick = { onThemeChange(pref) },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = themes.size),
                        modifier = Modifier.testTag("theme_${pref.name.lowercase()}")
                    ) {
                        Text(
                            text = when (pref) {
                                ThemePreference.DARK -> "Obsidian"
                                ThemePreference.LIGHT -> "Drafting"
                                ThemePreference.SYSTEM -> "System"
                            }
                        )
                    }
                }
            }
        }
    }
}
