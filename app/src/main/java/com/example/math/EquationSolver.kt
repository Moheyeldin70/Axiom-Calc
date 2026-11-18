package com.example.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.sqrt

data class ComplexNumber(
    val real: BigDecimal,
    val imag: BigDecimal = BigDecimal.ZERO
) {
    fun format(precision: Int = 12): String {
        val rClean = BigMath.cleanZero(real)
        val iClean = BigMath.cleanZero(imag)
        val rStr = BigMath.formatResult(rClean, precision)
        if (iClean.abs() < BigDecimal("1e-18")) return rStr
        val iAbsStr = BigMath.formatResult(iClean.abs(), precision)
        val sign = if (iClean < BigDecimal.ZERO) " - " else " + "
        val imagPart = if (iAbsStr == "1") "i" else "${iAbsStr}i"
        return if (rClean.abs() < BigDecimal("1e-18")) {
            if (iClean < BigDecimal.ZERO) "-$imagPart" else imagPart
        } else {
            "$rStr$sign$imagPart"
        }
    }
}

data class SolvedRoot(
    val value: BigDecimal,
    val residual: BigDecimal,
    val note: String = ""
)

data class PolynomialSolution(
    val degree: Int,
    val roots: List<ComplexNumber>,
    val discriminant: BigDecimal?,
    val summary: String
)

sealed interface LinearSystemResult {
    data class Unique(val solution: List<BigDecimal>, val determinant: BigDecimal) : LinearSystemResult
    data class Singular(val message: String, val determinant: BigDecimal) : LinearSystemResult
}

object EquationSolver {
    private val SOLVE_MC = MathContext(34, RoundingMode.HALF_EVEN)
    private val ROOT_TOL = BigDecimal("1e-22")

    fun solveGeneralEquation(
        equationText: String,
        xMin: Double = -50.0,
        xMax: Double = 50.0,
        angleUnit: AngleUnit = AngleUnit.RAD
    ): List<SolvedRoot> {
        val parts = equationText.split("=")
        val exprStr = when (parts.size) {
            1 -> parts[0].trim()
            2 -> "(${parts[0].trim()}) - (${parts[1].trim()})"
            else -> throw MathDomainException("Equation may contain at most one '=' sign")
        }
        if (exprStr.isEmpty()) throw MathDomainException("Enter an equation to solve")
        if (xMin >= xMax) throw MathDomainException("Search range requires min < max")

        val ast = MathParser.parse(exprStr)
        val constants = mapOf(
            "π" to BigMath.PI, "pi" to BigMath.PI, "e" to BigMath.E, "φ" to BigMath.PHI, "phi" to BigMath.PHI
        )

        fun evalBig(x: BigDecimal): BigDecimal? {
            return try {
                val vars = constants + ("x" to x)
                ast.evalBig(vars, angleUnit)
            } catch (_: Exception) {
                null
            }
        }

        val steps = 400
        val dx = (xMax - xMin) / steps
        val xs = DoubleArray(steps + 1) { i -> xMin + i * dx }
        val ys = DoubleArray(steps + 1) { i -> ast.evalDouble(xs[i], angleUnit) }

        val candidateRoots = mutableListOf<BigDecimal>()

        for (i in 0 until steps) {
            val y0 = ys[i]
            val y1 = ys[i + 1]
            if (y0.isFinite() && abs(y0) < 1e-12) {
                refineNewtonOrSecant(BigDecimal.valueOf(xs[i]), ::evalBig)?.let { candidateRoots.add(it) }
            }
            if (y0.isFinite() && y1.isFinite()) {
                if ((y0 > 0 && y1 < 0) || (y0 < 0 && y1 > 0)) {
                    bisectAndRefine(BigDecimal.valueOf(xs[i]), BigDecimal.valueOf(xs[i + 1]), ::evalBig)?.let {
                        candidateRoots.add(it)
                    }
                } else if (i > 0 && ys[i - 1].isFinite()) {
                    // Check for local minimum of |y| (tangent roots like (x-2)^2 = 0)
                    val prevAbs = abs(ys[i - 1])
                    val currAbs = abs(y0)
                    val nextAbs = abs(y1)
                    if (currAbs <= prevAbs && currAbs <= nextAbs && currAbs < 0.5) {
                        refineNewtonOrSecant(BigDecimal.valueOf(xs[i]), ::evalBig)?.let {
                            candidateRoots.add(it)
                        }
                    }
                }
            }
        }

        if (ys[steps].isFinite() && abs(ys[steps]) < 1e-12) {
            refineNewtonOrSecant(BigDecimal.valueOf(xs[steps]), ::evalBig)?.let { candidateRoots.add(it) }
        }

        // Deduplicate & verify residuals
        val uniqueRoots = mutableListOf<SolvedRoot>()
        val dedupTol = BigDecimal("1e-9")
        for (cand in candidateRoots.sorted()) {
            val res = evalBig(cand)?.abs() ?: continue
            if (res > BigDecimal("1e-7")) continue
            if (uniqueRoots.none { it.value.subtract(cand).abs() < dedupTol }) {
                val snapped = if (cand.abs() < BigDecimal("1e-20")) BigDecimal.ZERO else cand
                val snappedRes = evalBig(snapped)?.abs() ?: res
                uniqueRoots.add(SolvedRoot(value = snapped, residual = snappedRes))
            }
        }
        return uniqueRoots
    }

    private fun bisectAndRefine(
        aInit: BigDecimal,
        bInit: BigDecimal,
        eval: (BigDecimal) -> BigDecimal?
    ): BigDecimal? {
        var a = aInit
        var b = bInit
        var fa = eval(a) ?: return null
        var fb = eval(b) ?: return null
        if (fa.abs() < ROOT_TOL) return a
        if (fb.abs() < ROOT_TOL) return b
        if (fa.signum() == fb.signum()) return null

        var mid = a
        for (iter in 0 until 90) {
            mid = a.add(b, SOLVE_MC).divide(BigMath.TWO, SOLVE_MC)
            val fm = eval(mid) ?: return null
            if (fm.abs() < ROOT_TOL || b.subtract(a).abs() < ROOT_TOL) break
            if (fa.signum() != fm.signum()) {
                b = mid
                fb = fm
            } else {
                a = mid
                fa = fm
            }
        }
        // Final polish with Newton if smooth
        return refineNewtonOrSecant(mid, eval) ?: mid
    }

    private fun refineNewtonOrSecant(
        x0: BigDecimal,
        eval: (BigDecimal) -> BigDecimal?
    ): BigDecimal? {
        var x = x0
        val h = BigDecimal("1e-11")
        for (iter in 0 until 25) {
            val fx = eval(x) ?: return null
            if (fx.abs() < ROOT_TOL) return x
            val fxPlus = eval(x.add(h, SOLVE_MC)) ?: return null
            val fxMinus = eval(x.subtract(h, SOLVE_MC)) ?: return null
            val df = fxPlus.subtract(fxMinus, SOLVE_MC).divide(h.multiply(BigMath.TWO, SOLVE_MC), SOLVE_MC)
            if (df.abs() < BigDecimal("1e-24")) break
            val step = fx.divide(df, SOLVE_MC)
            // Guard against wild jumps
            if (step.abs() > BigDecimal.TEN) break
            x = x.subtract(step, SOLVE_MC)
            if (step.abs() < ROOT_TOL) break
        }
        val finalFx = eval(x) ?: return null
        return if (finalFx.abs() < BigDecimal("1e-8")) x else null
    }

    fun solveQuadratic(a: BigDecimal, b: BigDecimal, c: BigDecimal): PolynomialSolution {
        if (a.compareTo(BigDecimal.ZERO) == 0) {
            if (b.compareTo(BigDecimal.ZERO) == 0) {
                throw MathDomainException("Degenerate equation: a = 0 and b = 0")
            }
            val root = c.negate().divide(b, SOLVE_MC)
            return PolynomialSolution(
                degree = 1,
                roots = listOf(ComplexNumber(root)),
                discriminant = null,
                summary = "Linear equation (a = 0)"
            )
        }

        val four = BigDecimal.valueOf(4)
        val twoA = a.multiply(BigMath.TWO, SOLVE_MC)
        val disc = b.multiply(b, SOLVE_MC).subtract(four.multiply(a, SOLVE_MC).multiply(c, SOLVE_MC), SOLVE_MC)
        val negB = b.negate()

        return when {
            disc > BigDecimal.ZERO -> {
                val sqrtD = BigMath.sqrt(disc)
                val x1 = negB.add(sqrtD, SOLVE_MC).divide(twoA, SOLVE_MC)
                val x2 = negB.subtract(sqrtD, SOLVE_MC).divide(twoA, SOLVE_MC)
                PolynomialSolution(
                    degree = 2,
                    roots = listOf(ComplexNumber(x1), ComplexNumber(x2)),
                    discriminant = disc,
                    summary = "Two distinct real roots (Δ > 0)"
                )
            }
            disc.compareTo(BigDecimal.ZERO) == 0 -> {
                val x = negB.divide(twoA, SOLVE_MC)
                PolynomialSolution(
                    degree = 2,
                    roots = listOf(ComplexNumber(x), ComplexNumber(x)),
                    discriminant = BigDecimal.ZERO,
                    summary = "One repeated real root (Δ = 0)"
                )
            }
            else -> {
                val realPart = negB.divide(twoA, SOLVE_MC)
                val imagPart = BigMath.sqrt(disc.negate()).divide(twoA.abs(), SOLVE_MC)
                PolynomialSolution(
                    degree = 2,
                    roots = listOf(
                        ComplexNumber(realPart, imagPart),
                        ComplexNumber(realPart, imagPart.negate())
                    ),
                    discriminant = disc,
                    summary = "Two complex conjugate roots (Δ < 0)"
                )
            }
        }
    }

    fun solveCubic(a: BigDecimal, b: BigDecimal, c: BigDecimal, d: BigDecimal): PolynomialSolution {
        if (a.compareTo(BigDecimal.ZERO) == 0) return solveQuadratic(b, c, d)

        val ad = a.toDouble()
        val bd = b.toDouble()
        val cd = c.toDouble()
        val dd = d.toDouble()

        val bNorm = bd / ad
        val cNorm = cd / ad
        val dNorm = dd / ad

        val p = cNorm - bNorm * bNorm / 3.0
        val q = (2.0 * bNorm * bNorm * bNorm) / 27.0 - (bNorm * cNorm) / 3.0 + dNorm
        val disc = (q * q) / 4.0 + (p * p * p) / 27.0
        val shift = -bNorm / 3.0

        fun polishRealRoot(xInit: Double): BigDecimal {
            var x = BigDecimal.valueOf(xInit)
            val three = BigDecimal.valueOf(3)
            val two = BigMath.TWO
            repeat(8) {
                val fx = a.multiply(x.pow(3), SOLVE_MC)
                    .add(b.multiply(x.pow(2), SOLVE_MC), SOLVE_MC)
                    .add(c.multiply(x, SOLVE_MC), SOLVE_MC)
                    .add(d, SOLVE_MC)
                val dfx = three.multiply(a, SOLVE_MC).multiply(x.pow(2), SOLVE_MC)
                    .add(two.multiply(b, SOLVE_MC).multiply(x, SOLVE_MC), SOLVE_MC)
                    .add(c, SOLVE_MC)
                if (dfx.abs() < BigDecimal("1e-28")) return x
                x = x.subtract(fx.divide(dfx, SOLVE_MC), SOLVE_MC)
            }
            return x
        }

        val roots = if (disc > 1e-13) {
            // 1 real root + 2 complex conjugates
            val sqrtDisc = sqrt(disc)
            val u = cbrt(-q / 2.0 + sqrtDisc)
            val v = cbrt(-q / 2.0 - sqrtDisc)
            val r1 = polishRealRoot(u + v + shift)
            // Factor out (x - r1) via synthetic division to get exact remaining quadratic!
            val qA = a
            val qB = b.add(a.multiply(r1, SOLVE_MC), SOLVE_MC)
            val qC = c.add(qB.multiply(r1, SOLVE_MC), SOLVE_MC)
            val quad = solveQuadratic(qA, qB, qC)
            listOf(ComplexNumber(r1)) + quad.roots
        } else if (abs(disc) <= 1e-13) {
            // Multiple real roots
            val r1 = polishRealRoot(if (q == 0.0) shift else 2.0 * cbrt(-q / 2.0) + shift)
            val qA = a
            val qB = b.add(a.multiply(r1, SOLVE_MC), SOLVE_MC)
            val qC = c.add(qB.multiply(r1, SOLVE_MC), SOLVE_MC)
            val quad = solveQuadratic(qA, qB, qC)
            listOf(ComplexNumber(r1)) + quad.roots
        } else {
            // 3 distinct real roots (Vieta's trigonometric formula)
            val r = sqrt(-(p * p * p) / 27.0)
            val phi = acos((-q / (2.0 * r)).coerceIn(-1.0, 1.0))
            val mag = 2.0 * cbrt(r)
            val x1 = polishRealRoot(mag * cos(phi / 3.0) + shift)
            val x2 = polishRealRoot(mag * cos((phi + 2.0 * Math.PI) / 3.0) + shift)
            val x3 = polishRealRoot(mag * cos((phi + 4.0 * Math.PI) / 3.0) + shift)
            listOf(ComplexNumber(x1), ComplexNumber(x2), ComplexNumber(x3))
        }

        val summary = if (roots.all { it.imag.abs() < BigDecimal("1e-14") }) {
            "Three real roots"
        } else {
            "One real root & two complex conjugates"
        }
        return PolynomialSolution(
            degree = 3,
            roots = roots,
            discriminant = null,
            summary = summary
        )
    }

    fun solveLinearSystem(
        matrixInput: Array<Array<BigDecimal>>,
        rhsInput: Array<BigDecimal>
    ): LinearSystemResult {
        val n = rhsInput.size
        val a = Array(n) { r -> Array(n) { c -> matrixInput[r][c] } }
        val b = Array(n) { r -> rhsInput[r] }

        var det = BigDecimal.ONE
        var swaps = 0

        for (col in 0 until n) {
            // Partial pivoting
            var pivotRow = col
            var maxVal = a[col][col].abs()
            for (r in col + 1 until n) {
                val v = a[r][col].abs()
                if (v > maxVal) {
                    maxVal = v
                    pivotRow = r
                }
            }

            if (pivotRow != col) {
                val tmpRow = a[col]; a[col] = a[pivotRow]; a[pivotRow] = tmpRow
                val tmpB = b[col]; b[col] = b[pivotRow]; b[pivotRow] = tmpB
                swaps++
            }

            val pivot = a[col][col]
            det = det.multiply(pivot, SOLVE_MC)
            if (pivot.abs() < BigDecimal("1e-24")) {
                return LinearSystemResult.Singular(
                    message = "Matrix is singular (det = 0): system has no unique solution.",
                    determinant = BigDecimal.ZERO
                )
            }

            for (r in col + 1 until n) {
                val factor = a[r][col].divide(pivot, SOLVE_MC)
                for (c in col until n) {
                    a[r][c] = a[r][c].subtract(factor.multiply(a[col][c], SOLVE_MC), SOLVE_MC)
                }
                b[r] = b[r].subtract(factor.multiply(b[col], SOLVE_MC), SOLVE_MC)
            }
        }

        if (swaps % 2 != 0) det = det.negate()

        val x = Array(n) { BigDecimal.ZERO }
        for (i in n - 1 downTo 0) {
            var sum = b[i]
            for (j in i + 1 until n) {
                sum = sum.subtract(a[i][j].multiply(x[j], SOLVE_MC), SOLVE_MC)
            }
            x[i] = BigMath.cleanZero(sum.divide(a[i][i], SOLVE_MC))
        }
        return LinearSystemResult.Unique(x.toList(), BigMath.cleanZero(det))
    }

    fun numericalDerivative(
        expression: String,
        xValue: BigDecimal,
        angleUnit: AngleUnit = AngleUnit.RAD
    ): BigDecimal {
        val ast = MathParser.parse(expression)
        val constants = mapOf(
            "π" to BigMath.PI, "pi" to BigMath.PI, "e" to BigMath.E, "φ" to BigMath.PHI, "phi" to BigMath.PHI
        )
        fun f(x: BigDecimal): BigDecimal = ast.evalBig(constants + ("x" to x), angleUnit)

        // 5-point stencil: (-f(x+2h) + 8f(x+h) - 8f(x-h) + f(x-2h)) / (12h)
        val h = BigDecimal("1e-7")
        val twoH = h.multiply(BigMath.TWO, BigMath.WORK_MC)
        val fPlus2 = f(xValue.add(twoH, BigMath.WORK_MC))
        val fPlus1 = f(xValue.add(h, BigMath.WORK_MC))
        val fMinus1 = f(xValue.subtract(h, BigMath.WORK_MC))
        val fMinus2 = f(xValue.subtract(twoH, BigMath.WORK_MC))

        val eight = BigDecimal.valueOf(8)
        val twelve = BigDecimal.valueOf(12)
        val num = fPlus2.negate()
            .add(eight.multiply(fPlus1, BigMath.WORK_MC), BigMath.WORK_MC)
            .subtract(eight.multiply(fMinus1, BigMath.WORK_MC), BigMath.WORK_MC)
            .add(fMinus2, BigMath.WORK_MC)
        val denom = twelve.multiply(h, BigMath.WORK_MC)
        return BigMath.cleanZero(num.divide(denom, SOLVE_MC))
    }

    fun definiteIntegral(
        expression: String,
        lower: BigDecimal,
        upper: BigDecimal,
        angleUnit: AngleUnit = AngleUnit.RAD
    ): BigDecimal {
        if (lower.compareTo(upper) == 0) return BigDecimal.ZERO
        val ast = MathParser.parse(expression)
        val a = lower.toDouble()
        val b = upper.toDouble()
        if (!a.isFinite() || !b.isFinite()) throw MathDomainException("Integration bounds must be finite")

        val n = 400 // even number of subintervals for composite Simpson's rule
        val h = (b - a) / n
        var sum = 0.0
        for (i in 0..n) {
            val x = a + i * h
            val y = ast.evalDouble(x, angleUnit)
            if (!y.isFinite()) {
                throw MathDomainException("Integrand undefined or singular near x = ${BigMath.formatResult(BigDecimal.valueOf(x), 6)}")
            }
            val weight = when {
                i == 0 || i == n -> 1.0
                i % 2 == 1 -> 4.0
                else -> 2.0
            }
            sum += weight * y
        }
        val res = sum * h / 3.0
        return BigMath.cleanZero(BigDecimal.valueOf(res))
    }
}
