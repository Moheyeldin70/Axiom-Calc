package com.example.ui.solver

import androidx.lifecycle.ViewModel
import com.example.math.AngleUnit
import com.example.math.BigMath
import com.example.math.EquationSolver
import com.example.math.LinearSystemResult
import com.example.math.PolynomialSolution
import com.example.math.SolvedRoot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal

enum class SolverMode(val label: String) {
    ROOT_FINDER("Root Finder"),
    POLYNOMIAL("Polynomial"),
    LINEAR_SYSTEM("Linear Matrix"),
    CALCULUS("Calculus")
}

data class SolverUiState(
    val mode: SolverMode = SolverMode.ROOT_FINDER,
    // Root finder
    val equationText: String = "x^3 - 2*x - 5 = 0",
    val rangeMin: String = "-50",
    val rangeMax: String = "50",
    val foundRoots: List<SolvedRoot> = emptyList(),
    val rootError: String? = null,
    val hasSolvedRoot: Boolean = false,
    // Polynomial
    val polyDegree: Int = 2,
    val coefA: String = "1",
    val coefB: String = "-3",
    val coefC: String = "2",
    val coefD: String = "-6",
    val polySolution: PolynomialSolution? = null,
    val polyError: String? = null,
    // Linear system
    val systemSize: Int = 2,
    val matrixValues: List<List<String>> = listOf(
        listOf("2", "1", "0"),
        listOf("1", "-3", "0"),
        listOf("0", "0", "1")
    ),
    val rhsValues: List<String> = listOf("5", "-1", "0"),
    val linearResult: LinearSystemResult? = null,
    val linearError: String? = null,
    // Calculus
    val isIntegral: Boolean = false,
    val calcExpression: String = "x^2 * sin(x)",
    val calcPointA: String = "0",
    val calcPointB: String = "3.14159265358979",
    val calcResult: String? = null,
    val calcError: String? = null
)

class SolverViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SolverUiState())
    val uiState: StateFlow<SolverUiState> = _uiState.asStateFlow()

    fun setMode(mode: SolverMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    // --- General Root Finder ---
    fun updateEquationText(text: String) {
        _uiState.update { it.copy(equationText = text, rootError = null) }
    }

    fun appendToEquation(token: String) {
        _uiState.update { it.copy(equationText = it.equationText + token, rootError = null) }
    }

    fun updateRange(min: String, max: String) {
        _uiState.update { it.copy(rangeMin = min, rangeMax = max, rootError = null) }
    }

    fun solveGeneralEquation(angleUnit: AngleUnit) {
        val state = _uiState.value
        val xMin = state.rangeMin.toDoubleOrNull()
        val xMax = state.rangeMax.toDoubleOrNull()
        if (xMin == null || xMax == null) {
            _uiState.update { it.copy(rootError = "Invalid search interval numbers") }
            return
        }
        try {
            val roots = EquationSolver.solveGeneralEquation(
                equationText = state.equationText,
                xMin = xMin,
                xMax = xMax,
                angleUnit = angleUnit
            )
            _uiState.update {
                it.copy(foundRoots = roots, rootError = null, hasSolvedRoot = true)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(rootError = e.message ?: "Unable to solve equation", foundRoots = emptyList(), hasSolvedRoot = true)
            }
        }
    }

    fun buildPlottableExpression(): String {
        val parts = _uiState.value.equationText.split("=")
        return when (parts.size) {
            2 -> {
                val rhs = parts[1].trim()
                if (rhs == "0") parts[0].trim() else "(${parts[0].trim()}) - ($rhs)"
            }
            else -> _uiState.value.equationText.trim()
        }
    }

    // --- Polynomial ---
    fun setPolyDegree(deg: Int) {
        _uiState.update { it.copy(polyDegree = deg, polySolution = null, polyError = null) }
    }

    fun updatePolyCoefs(a: String? = null, b: String? = null, c: String? = null, d: String? = null) {
        _uiState.update {
            it.copy(
                coefA = a ?: it.coefA,
                coefB = b ?: it.coefB,
                coefC = c ?: it.coefC,
                coefD = d ?: it.coefD,
                polyError = null
            )
        }
    }

    fun solvePolynomial() {
        val s = _uiState.value
        try {
            val a = BigDecimal(s.coefA.trim().ifEmpty { "0" })
            val b = BigDecimal(s.coefB.trim().ifEmpty { "0" })
            val c = BigDecimal(s.coefC.trim().ifEmpty { "0" })
            val sol = if (s.polyDegree == 2) {
                EquationSolver.solveQuadratic(a, b, c)
            } else {
                val d = BigDecimal(s.coefD.trim().ifEmpty { "0" })
                EquationSolver.solveCubic(a, b, c, d)
            }
            _uiState.update { it.copy(polySolution = sol, polyError = null) }
        } catch (e: Exception) {
            _uiState.update { it.copy(polyError = e.message ?: "Invalid polynomial coefficients", polySolution = null) }
        }
    }

    // --- Linear System ---
    fun setSystemSize(size: Int) {
        _uiState.update { it.copy(systemSize = size, linearResult = null, linearError = null) }
    }

    fun updateMatrixCell(row: Int, col: Int, value: String) {
        _uiState.update { s ->
            val updated = s.matrixValues.mapIndexed { r, rowList ->
                if (r == row) rowList.mapIndexed { c, old -> if (c == col) value else old } else rowList
            }
            s.copy(matrixValues = updated, linearError = null)
        }
    }

    fun updateRhsCell(row: Int, value: String) {
        _uiState.update { s ->
            val updated = s.rhsValues.mapIndexed { r, old -> if (r == row) value else old }
            s.copy(rhsValues = updated, linearError = null)
        }
    }

    fun solveLinearSystem() {
        val s = _uiState.value
        val n = s.systemSize
        try {
            val mat = Array(n) { r ->
                Array(n) { c ->
                    BigDecimal(s.matrixValues[r][c].trim().ifEmpty { "0" })
                }
            }
            val rhs = Array(n) { r ->
                BigDecimal(s.rhsValues[r].trim().ifEmpty { "0" })
            }
            val res = EquationSolver.solveLinearSystem(mat, rhs)
            _uiState.update { it.copy(linearResult = res, linearError = null) }
        } catch (e: Exception) {
            _uiState.update { it.copy(linearError = e.message ?: "Invalid matrix entries", linearResult = null) }
        }
    }

    // --- Calculus ---
    fun setCalculusMode(isIntegral: Boolean) {
        _uiState.update {
            it.copy(
                isIntegral = isIntegral,
                calcResult = null,
                calcError = null,
                calcPointA = if (isIntegral) "0" else "1",
                calcPointB = if (isIntegral) "3.14159265358979" else it.calcPointB
            )
        }
    }

    fun updateCalculusFields(expr: String? = null, a: String? = null, b: String? = null) {
        _uiState.update {
            it.copy(
                calcExpression = expr ?: it.calcExpression,
                calcPointA = a ?: it.calcPointA,
                calcPointB = b ?: it.calcPointB,
                calcError = null
            )
        }
    }

    fun computeCalculus(angleUnit: AngleUnit) {
        val s = _uiState.value
        try {
            val a = BigDecimal(s.calcPointA.trim())
            val res = if (s.isIntegral) {
                val b = BigDecimal(s.calcPointB.trim())
                EquationSolver.definiteIntegral(s.calcExpression, a, b, angleUnit)
            } else {
                EquationSolver.numericalDerivative(s.calcExpression, a, angleUnit)
            }
            _uiState.update {
                it.copy(calcResult = BigMath.formatResult(res, 14), calcError = null)
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(calcError = e.message ?: "Unable to evaluate calculus expression", calcResult = null)
            }
        }
    }
}
