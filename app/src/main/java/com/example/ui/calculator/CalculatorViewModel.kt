package com.example.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalculationRecord
import com.example.data.CalculatorRepository
import com.example.data.CalculatorSettings
import com.example.math.AngleUnit
import com.example.math.BigMath
import com.example.math.MathDomainException
import com.example.math.MathParser
import com.example.math.NotationMode
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CalculatorUiState(
    val expression: String = "",
    val resultDisplay: String = "0",
    val livePreview: String = "",
    val fractionHint: String? = null,
    val errorMessage: String? = null,
    val isEvaluated: Boolean = false,
    val isSecondMode: Boolean = false,
    val isHyperbolicMode: Boolean = false,
    val settings: CalculatorSettings = CalculatorSettings(),
    val history: List<CalculationRecord> = emptyList()
)

class CalculatorViewModel(
    private val repository: CalculatorRepository
) : ViewModel() {

    private data class DraftState(
        val expression: String = "",
        val resultDisplay: String = "0",
        val livePreview: String = "",
        val fractionHint: String? = null,
        val errorMessage: String? = null,
        val isEvaluated: Boolean = false,
        val isSecondMode: Boolean = false,
        val isHyperbolicMode: Boolean = false
    )

    private val draft = MutableStateFlow(DraftState())

    val uiState: StateFlow<CalculatorUiState> = combine(
        draft,
        repository.settingsFlow,
        repository.historyFlow
    ) { d, settings, history ->
        CalculatorUiState(
            expression = d.expression,
            resultDisplay = d.resultDisplay,
            livePreview = d.livePreview,
            fractionHint = d.fractionHint,
            errorMessage = d.errorMessage,
            isEvaluated = d.isEvaluated,
            isSecondMode = d.isSecondMode,
            isHyperbolicMode = d.isHyperbolicMode,
            settings = settings,
            history = history
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalculatorUiState()
    )

    fun appendToken(token: String, isOperator: Boolean = false, isDigitOrDot: Boolean = false) {
        draft.update { cur ->
            val nextExpr = when {
                cur.isEvaluated && isDigitOrDot -> token
                cur.isEvaluated && isOperator -> {
                    val base = if (cur.errorMessage != null) "" else "ans"
                    base + token
                }
                cur.isEvaluated -> token
                else -> cur.expression + token
            }
            recomputePreview(
                cur.copy(
                    expression = nextExpr,
                    isEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun appendFunction(funcPrefix: String) {
        draft.update { cur ->
            val nextExpr = if (cur.isEvaluated) {
                if (cur.errorMessage == null && cur.expression.isNotEmpty()) {
                    "${funcPrefix}ans)"
                } else {
                    funcPrefix
                }
            } else {
                cur.expression + funcPrefix
            }
            recomputePreview(
                cur.copy(
                    expression = nextExpr,
                    isEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun setExternalValue(value: String) {
        draft.update { cur ->
            recomputePreview(
                cur.copy(
                    expression = value,
                    resultDisplay = value,
                    isEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun toggleNegate() {
        draft.update { cur ->
            val expr = cur.expression
            val nextExpr = when {
                expr.isEmpty() -> "-"
                cur.isEvaluated -> "-ans"
                expr.startsWith("-") && expr.length > 1 && expr.none { it in "+*/^" } -> expr.drop(1)
                else -> "-($expr)"
            }
            recomputePreview(cur.copy(expression = nextExpr, isEvaluated = false, errorMessage = null))
        }
    }

    fun backspace() {
        draft.update { cur ->
            if (cur.expression.isEmpty()) return@update cur
            val nextExpr = removeSmartToken(cur.expression)
            recomputePreview(cur.copy(expression = nextExpr, isEvaluated = false, errorMessage = null))
        }
    }

    private fun removeSmartToken(expr: String): String {
        val suffixes = listOf(
            "asinh(", "acosh(", "atanh(", "sinh(", "cosh(", "tanh(",
            "csch(", "sech(", "coth(", "asin(", "acos(", "atan(",
            "sin(", "cos(", "tan(", "csc(", "sec(", "cot(",
            "nroot(", "cbrt(", "sqrt(", "log2(", "logb(", "log(",
            "ln(", "abs(", "npr(", "ncr(", "exp(", "ans",
            "c_light", "h_planck", "g_grav", "n_a", "r_gas", "k_b", "eps0", "mu0", "q_e", "g0"
        )
        for (s in suffixes) {
            if (expr.endsWith(s)) {
                return expr.dropLast(s.length)
            }
        }
        return expr.dropLast(1)
    }

    fun clearAll() {
        draft.update {
            it.copy(
                expression = "",
                resultDisplay = "0",
                livePreview = "",
                fractionHint = null,
                errorMessage = null,
                isEvaluated = false
            )
        }
    }

    fun evaluate() {
        val current = uiState.value
        val raw = current.expression.trim()
        if (raw.isEmpty()) return

        val balanced = autoCloseParentheses(raw)
        val settings = current.settings
        val ansBd = runCatching { BigDecimal(settings.lastAns) }.getOrDefault(BigDecimal.ZERO)

        try {
            val resultBd = MathParser.evaluate(
                rawExpression = balanced,
                angleUnit = settings.angleUnit,
                variables = mapOf("ans" to ansBd)
            )
            val formatted = BigMath.formatResult(
                value = resultBd,
                precision = settings.precisionDigits,
                notationMode = settings.notationMode
            )
            val fracHint = BigMath.toFractionHint(resultBd)

            draft.update {
                it.copy(
                    expression = balanced,
                    resultDisplay = formatted,
                    livePreview = "",
                    fractionHint = fracHint,
                    errorMessage = null,
                    isEvaluated = true
                )
            }
            viewModelScope.launch {
                repository.saveCalculation(
                    expression = balanced,
                    result = formatted,
                    angleUnit = settings.angleUnit.label
                )
            }
        } catch (e: MathDomainException) {
            draft.update {
                it.copy(
                    errorMessage = e.message ?: "Math error",
                    fractionHint = null,
                    isEvaluated = true
                )
            }
        } catch (e: Exception) {
            draft.update {
                it.copy(
                    errorMessage = "Syntax error",
                    fractionHint = null,
                    isEvaluated = true
                )
            }
        }
    }

    private fun recomputePreview(state: DraftState): DraftState {
        val raw = state.expression.trim()
        if (raw.isEmpty()) {
            return state.copy(livePreview = "", resultDisplay = "0", fractionHint = null)
        }
        val settings = uiState.value.settings
        val ansBd = runCatching { BigDecimal(settings.lastAns) }.getOrDefault(BigDecimal.ZERO)
        val balanced = autoCloseParentheses(raw)
        return try {
            val res = MathParser.evaluate(
                rawExpression = balanced,
                angleUnit = settings.angleUnit,
                variables = mapOf("ans" to ansBd)
            )
            val formatted = BigMath.formatResult(
                value = res,
                precision = settings.precisionDigits,
                notationMode = settings.notationMode
            )
            val frac = BigMath.toFractionHint(res)
            state.copy(livePreview = formatted, resultDisplay = formatted, fractionHint = frac)
        } catch (_: Exception) {
            state.copy(livePreview = "", fractionHint = null)
        }
    }

    private fun autoCloseParentheses(expr: String): String {
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        val diff = openCount - closeCount
        return if (diff > 0) expr + ")".repeat(diff) else expr
    }

    fun toggleSecondMode() {
        draft.update { it.copy(isSecondMode = !it.isSecondMode) }
    }

    fun toggleHyperbolicMode() {
        draft.update { it.copy(isHyperbolicMode = !it.isHyperbolicMode) }
    }

    fun cycleAngleUnit() {
        val next = uiState.value.settings.angleUnit.next()
        viewModelScope.launch {
            repository.updateAngleUnit(next)
            draft.update { recomputePreview(it) }
        }
    }

    fun cycleNotationMode() {
        val next = uiState.value.settings.notationMode.next()
        viewModelScope.launch {
            repository.updateNotationMode(next)
            draft.update { recomputePreview(it) }
        }
    }

    fun setPrecision(digits: Int) {
        viewModelScope.launch {
            repository.updatePrecisionDigits(digits)
            draft.update { recomputePreview(it) }
        }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            repository.updateThemePreference(theme)
        }
    }

    fun memoryClear() {
        viewModelScope.launch { repository.updateMemoryValue("0") }
    }

    fun memoryRecall() {
        val mem = uiState.value.settings.memoryValue
        appendToken(mem, isDigitOrDot = true)
    }

    fun memoryAdd(subtract: Boolean = false) {
        val current = uiState.value
        val curVal = runCatching {
            if (current.expression.isNotEmpty() && !current.isEvaluated) {
                val ansBd = runCatching { BigDecimal(current.settings.lastAns) }.getOrDefault(BigDecimal.ZERO)
                MathParser.evaluate(autoCloseParentheses(current.expression), current.settings.angleUnit, mapOf("ans" to ansBd))
            } else {
                BigDecimal(current.resultDisplay)
            }
        }.getOrNull() ?: return

        val oldMem = runCatching { BigDecimal(current.settings.memoryValue) }.getOrDefault(BigDecimal.ZERO)
        val newMem = if (subtract) oldMem.subtract(curVal) else oldMem.add(curVal)
        val formatted = BigMath.formatResult(newMem, current.settings.precisionDigits, NotationMode.NORMAL)
        viewModelScope.launch { repository.updateMemoryValue(formatted) }
    }

    fun loadHistoryRecord(record: CalculationRecord, useResultOnly: Boolean = false) {
        draft.update { cur ->
            val next = if (useResultOnly) {
                if (cur.isEvaluated) record.result else cur.expression + record.result
            } else {
                record.expression
            }
            recomputePreview(
                cur.copy(
                    expression = next,
                    isEvaluated = false,
                    errorMessage = null
                )
            )
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch { repository.deleteHistoryItem(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    class Factory(private val repository: CalculatorRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalculatorViewModel(repository) as T
        }
    }
}
