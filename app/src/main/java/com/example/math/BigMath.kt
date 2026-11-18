package com.example.math

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln as dLn
import kotlin.math.pow as dPow
import kotlin.math.sin as dSin

class MathDomainException(message: String) : IllegalArgumentException(message)

object BigMath {
    val WORK_MC = MathContext(56, RoundingMode.HALF_EVEN)
    private val EPSILON_ZERO = BigDecimal("1e-44")

    val ZERO: BigDecimal = BigDecimal.ZERO
    val ONE: BigDecimal = BigDecimal.ONE
    val TWO: BigDecimal = BigDecimal.valueOf(2)
    val HALF: BigDecimal = BigDecimal("0.5")

    // 64-digit high precision mathematical constants
    val PI = BigDecimal("3.1415926535897932384626433832795028841971693993751058209749445923")
    val TWO_PI = PI.multiply(TWO, MathContext(64, RoundingMode.HALF_EVEN))
    val HALF_PI = PI.divide(TWO, MathContext(64, RoundingMode.HALF_EVEN))
    val QUARTER_PI = PI.divide(BigDecimal.valueOf(4), MathContext(64, RoundingMode.HALF_EVEN))
    val E = BigDecimal("2.7182818284590452353602874713526624977572470936999595749669676277")
    val PHI = BigDecimal("1.6180339887498948482045868343656381177203091798057628621354486227")
    val LN2 = BigDecimal("0.6931471805599453094172321214581765680755001343602552541206800095")
    val LN10 = BigDecimal("2.3025850929940456840179914546843642076011014886287729760333279009")
    val SQRT2 = BigDecimal("1.4142135623730950488016887242096980785696718753769480731766797379")

    private val D180 = BigDecimal.valueOf(180)
    private val D360 = BigDecimal.valueOf(360)
    private val D200 = BigDecimal.valueOf(200)
    private val D400 = BigDecimal.valueOf(400)
    private val D90 = BigDecimal.valueOf(90)
    private val D270 = BigDecimal.valueOf(270)
    private val D100 = BigDecimal.valueOf(100)
    private val D300 = BigDecimal.valueOf(300)

    fun cleanZero(v: BigDecimal): BigDecimal =
        if (v.abs() < EPSILON_ZERO) ZERO else v

    fun toRadians(angle: BigDecimal, unit: AngleUnit): BigDecimal = when (unit) {
        AngleUnit.RAD -> angle
        AngleUnit.DEG -> angle.multiply(PI, WORK_MC).divide(D180, WORK_MC)
        AngleUnit.GRAD -> angle.multiply(PI, WORK_MC).divide(D200, WORK_MC)
    }

    fun fromRadians(rad: BigDecimal, unit: AngleUnit): BigDecimal = when (unit) {
        AngleUnit.RAD -> rad
        AngleUnit.DEG -> rad.multiply(D180, WORK_MC).divide(PI, WORK_MC)
        AngleUnit.GRAD -> rad.multiply(D200, WORK_MC).divide(PI, WORK_MC)
    }

    // --- Exact Quadrant-Aware Trigonometric Functions ---

    fun sin(angle: BigDecimal, unit: AngleUnit): BigDecimal {
        return when (unit) {
            AngleUnit.DEG -> {
                val rem = normalizeDegrees(angle)
                if (rem.compareTo(ZERO) == 0 || rem.compareTo(D180) == 0) return ZERO
                if (rem.compareTo(D90) == 0) return ONE
                if (rem.compareTo(D270) == 0) return ONE.negate()
                sinRad(toRadians(rem, AngleUnit.DEG))
            }
            AngleUnit.GRAD -> {
                val rem = normalizeGradians(angle)
                if (rem.compareTo(ZERO) == 0 || rem.compareTo(D200) == 0) return ZERO
                if (rem.compareTo(D100) == 0) return ONE
                if (rem.compareTo(D300) == 0) return ONE.negate()
                sinRad(toRadians(rem, AngleUnit.GRAD))
            }
            AngleUnit.RAD -> sinRad(angle)
        }
    }

    fun cos(angle: BigDecimal, unit: AngleUnit): BigDecimal {
        return when (unit) {
            AngleUnit.DEG -> {
                val rem = normalizeDegrees(angle)
                if (rem.compareTo(ZERO) == 0) return ONE
                if (rem.compareTo(D90) == 0 || rem.compareTo(D270) == 0) return ZERO
                if (rem.compareTo(D180) == 0) return ONE.negate()
                cosRad(toRadians(rem, AngleUnit.DEG))
            }
            AngleUnit.GRAD -> {
                val rem = normalizeGradians(angle)
                if (rem.compareTo(ZERO) == 0) return ONE
                if (rem.compareTo(D100) == 0 || rem.compareTo(D300) == 0) return ZERO
                if (rem.compareTo(D200) == 0) return ONE.negate()
                cosRad(toRadians(rem, AngleUnit.GRAD))
            }
            AngleUnit.RAD -> cosRad(angle)
        }
    }

    fun tan(angle: BigDecimal, unit: AngleUnit): BigDecimal {
        val c = cos(angle, unit)
        if (c.abs() < BigDecimal("1e-42")) {
            throw MathDomainException("Tangent undefined at asymptote")
        }
        val s = sin(angle, unit)
        return cleanZero(s.divide(c, WORK_MC))
    }

    fun csc(angle: BigDecimal, unit: AngleUnit): BigDecimal {
        val s = sin(angle, unit)
        if (s.abs() < BigDecimal("1e-42")) {
            throw MathDomainException("Cosecant undefined (sin = 0)")
        }
        return ONE.divide(s, WORK_MC)
    }

    fun sec(angle: BigDecimal, unit: AngleUnit): BigDecimal {
        val c = cos(angle, unit)
        if (c.abs() < BigDecimal("1e-42")) {
            throw MathDomainException("Secant undefined (cos = 0)")
        }
        return ONE.divide(c, WORK_MC)
    }

    fun cot(angle: BigDecimal, unit: AngleUnit): BigDecimal {
        val s = sin(angle, unit)
        if (s.abs() < BigDecimal("1e-42")) {
            throw MathDomainException("Cotangent undefined (sin = 0)")
        }
        val c = cos(angle, unit)
        return cleanZero(c.divide(s, WORK_MC))
    }

    private fun normalizeDegrees(deg: BigDecimal): BigDecimal {
        var r = deg.remainder(D360, WORK_MC)
        if (r < ZERO) r = r.add(D360)
        return r
    }

    private fun normalizeGradians(grad: BigDecimal): BigDecimal {
        var r = grad.remainder(D400, WORK_MC)
        if (r < ZERO) r = r.add(D400)
        return r
    }

    private fun reduceRadians(rad: BigDecimal): Pair<BigDecimal, Int> {
        val mcHigh = MathContext(64, RoundingMode.HALF_EVEN)
        // Determine quadrant k = round(rad / (pi/2))
        val kBd = rad.divide(HALF_PI, mcHigh).setScale(0, RoundingMode.HALF_EVEN)
        val k = kBd.remainder(BigDecimal.valueOf(4)).toInt().let { if (it < 0) it + 4 else it }
        val reduced = rad.subtract(kBd.multiply(HALF_PI, mcHigh), mcHigh)
        return Pair(reduced.round(WORK_MC), k)
    }

    private fun sinRad(rad: BigDecimal): BigDecimal {
        if (rad.compareTo(ZERO) == 0) return ZERO
        val (r, k) = reduceRadians(rad)
        val result = when (k) {
            0 -> sinTaylor(r)
            1 -> cosTaylor(r)
            2 -> sinTaylor(r).negate()
            else -> cosTaylor(r).negate()
        }
        return cleanZero(result)
    }

    private fun cosRad(rad: BigDecimal): BigDecimal {
        if (rad.compareTo(ZERO) == 0) return ONE
        val (r, k) = reduceRadians(rad)
        val result = when (k) {
            0 -> cosTaylor(r)
            1 -> sinTaylor(r).negate()
            2 -> cosTaylor(r).negate()
            else -> sinTaylor(r)
        }
        return cleanZero(result)
    }

    private fun sinTaylor(x: BigDecimal): BigDecimal {
        if (x.abs() < EPSILON_ZERO) return x
        val x2 = x.multiply(x, WORK_MC)
        var term = x
        var sum = x
        var n = 1L
        val threshold = BigDecimal("1e-58")
        while (term.abs() > threshold && n < 200) {
            val denom = BigDecimal.valueOf((2 * n) * (2 * n + 1))
            term = term.multiply(x2, WORK_MC).negate().divide(denom, WORK_MC)
            sum = sum.add(term, WORK_MC)
            n++
        }
        return sum
    }

    private fun cosTaylor(x: BigDecimal): BigDecimal {
        if (x.abs() < EPSILON_ZERO) return ONE
        val x2 = x.multiply(x, WORK_MC)
        var term = ONE
        var sum = ONE
        var n = 1L
        val threshold = BigDecimal("1e-58")
        while (term.abs() > threshold && n < 200) {
            val denom = BigDecimal.valueOf((2 * n - 1) * (2 * n))
            term = term.multiply(x2, WORK_MC).negate().divide(denom, WORK_MC)
            sum = sum.add(term, WORK_MC)
            n++
        }
        return sum
    }

    // --- Inverse Trigonometric Functions ---

    fun asin(x: BigDecimal, unit: AngleUnit): BigDecimal {
        if (x.abs() > ONE) throw MathDomainException("arcsin domain error: |x| ≤ 1 required")
        if (x.compareTo(ONE) == 0) return fromRadians(HALF_PI.round(WORK_MC), unit)
        if (x.compareTo(ONE.negate()) == 0) return fromRadians(HALF_PI.round(WORK_MC).negate(), unit)
        if (x.compareTo(ZERO) == 0) return ZERO
        val denom = sqrt(ONE.subtract(x.multiply(x, WORK_MC), WORK_MC))
        val rad = atanRad(x.divide(denom, WORK_MC))
        return cleanZero(fromRadians(rad, unit))
    }

    fun acos(x: BigDecimal, unit: AngleUnit): BigDecimal {
        if (x.abs() > ONE) throw MathDomainException("arccos domain error: |x| ≤ 1 required")
        val rad = HALF_PI.round(WORK_MC).subtract(toRadians(asin(x, AngleUnit.RAD), AngleUnit.RAD), WORK_MC)
        return cleanZero(fromRadians(rad, unit))
    }

    fun atan(x: BigDecimal, unit: AngleUnit): BigDecimal {
        return cleanZero(fromRadians(atanRad(x), unit))
    }

    private fun atanRad(x: BigDecimal): BigDecimal {
        if (x.compareTo(ZERO) == 0) return ZERO
        val sign = x.signum()
        var v = x.abs()
        val inverted = v > ONE
        if (inverted) {
            v = ONE.divide(v, WORK_MC)
        }
        // Halve angle 4 times using v = v / (1 + sqrt(1 + v^2)) so |v| < 0.07
        var scale = 1
        repeat(4) {
            val s = sqrt(ONE.add(v.multiply(v, WORK_MC), WORK_MC))
            v = v.divide(ONE.add(s, WORK_MC), WORK_MC)
            scale *= 2
        }
        val v2 = v.multiply(v, WORK_MC)
        var term = v
        var sum = v
        var k = 1L
        val threshold = BigDecimal("1e-58")
        while (term.abs() > threshold && k < 150) {
            term = term.multiply(v2, WORK_MC).negate()
            val contrib = term.divide(BigDecimal.valueOf(2 * k + 1), WORK_MC)
            sum = sum.add(contrib, WORK_MC)
            k++
        }
        var res = sum.multiply(BigDecimal.valueOf(scale.toLong()), WORK_MC)
        if (inverted) {
            res = HALF_PI.round(WORK_MC).subtract(res, WORK_MC)
        }
        return if (sign < 0) res.negate() else res
    }

    // --- Hyperbolic & Inverse Hyperbolic Functions ---

    fun sinh(x: BigDecimal): BigDecimal {
        if (x.compareTo(ZERO) == 0) return ZERO
        if (x.abs() < BigDecimal("0.01")) {
            // Direct Taylor series avoids cancellation near 0
            val x2 = x.multiply(x, WORK_MC)
            var term = x
            var sum = x
            var n = 1L
            val threshold = BigDecimal("1e-58")
            while (term.abs() > threshold && n < 100) {
                val denom = BigDecimal.valueOf((2 * n) * (2 * n + 1))
                term = term.multiply(x2, WORK_MC).divide(denom, WORK_MC)
                sum = sum.add(term, WORK_MC)
                n++
            }
            return cleanZero(sum)
        }
        val ex = exp(x)
        val eNegX = ONE.divide(ex, WORK_MC)
        return cleanZero(ex.subtract(eNegX, WORK_MC).divide(TWO, WORK_MC))
    }

    fun cosh(x: BigDecimal): BigDecimal {
        if (x.compareTo(ZERO) == 0) return ONE
        val ex = exp(x)
        val eNegX = ONE.divide(ex, WORK_MC)
        return ex.add(eNegX, WORK_MC).divide(TWO, WORK_MC)
    }

    fun tanh(x: BigDecimal): BigDecimal {
        if (x.compareTo(ZERO) == 0) return ZERO
        if (x > BigDecimal.valueOf(80)) return ONE
        if (x < BigDecimal.valueOf(-80)) return ONE.negate()
        return cleanZero(sinh(x).divide(cosh(x), WORK_MC))
    }

    fun csch(x: BigDecimal): BigDecimal {
        if (x.abs() < EPSILON_ZERO) throw MathDomainException("csch undefined at 0")
        return ONE.divide(sinh(x), WORK_MC)
    }

    fun sech(x: BigDecimal): BigDecimal = ONE.divide(cosh(x), WORK_MC)

    fun coth(x: BigDecimal): BigDecimal {
        if (x.abs() < EPSILON_ZERO) throw MathDomainException("coth undefined at 0")
        if (x > BigDecimal.valueOf(80)) return ONE
        if (x < BigDecimal.valueOf(-80)) return ONE.negate()
        return cosh(x).divide(sinh(x), WORK_MC)
    }

    fun asinh(x: BigDecimal): BigDecimal {
        if (x.compareTo(ZERO) == 0) return ZERO
        val sign = x.signum()
        val ax = x.abs()
        val inner = ax.add(sqrt(ax.multiply(ax, WORK_MC).add(ONE, WORK_MC)), WORK_MC)
        val res = ln(inner)
        return if (sign < 0) res.negate() else res
    }

    fun acosh(x: BigDecimal): BigDecimal {
        if (x < ONE) throw MathDomainException("arccosh domain error: x ≥ 1 required")
        if (x.compareTo(ONE) == 0) return ZERO
        val inner = x.add(sqrt(x.multiply(x, WORK_MC).subtract(ONE, WORK_MC)), WORK_MC)
        return ln(inner)
    }

    fun atanh(x: BigDecimal): BigDecimal {
        if (x.abs() >= ONE) throw MathDomainException("arctanh domain error: |x| < 1 required")
        if (x.compareTo(ZERO) == 0) return ZERO
        val num = ONE.add(x, WORK_MC)
        val den = ONE.subtract(x, WORK_MC)
        return HALF.multiply(ln(num.divide(den, WORK_MC)), WORK_MC)
    }

    // --- Exponential & Logarithmic Functions ---

    fun exp(x: BigDecimal): BigDecimal {
        if (x.compareTo(ZERO) == 0) return ONE
        if (x > BigDecimal.valueOf(5000)) throw MathDomainException("Result overflow")
        if (x < BigDecimal.valueOf(-5000)) return ZERO

        val k = x.divide(LN2, 0, RoundingMode.HALF_EVEN).toInt()
        val r = x.subtract(LN2.multiply(BigDecimal.valueOf(k.toLong()), WORK_MC), WORK_MC)

        var term = ONE
        var sum = ONE
        var n = 1L
        val threshold = BigDecimal("1e-58")
        while (term.abs() > threshold && n < 200) {
            term = term.multiply(r, WORK_MC).divide(BigDecimal.valueOf(n), WORK_MC)
            sum = sum.add(term, WORK_MC)
            n++
        }

        val factor = TWO.pow(abs(k), WORK_MC)
        return if (k >= 0) sum.multiply(factor, WORK_MC) else sum.divide(factor, WORK_MC)
    }

    fun ln(x: BigDecimal): BigDecimal {
        if (x <= ZERO) throw MathDomainException("Logarithm domain error: x > 0 required")
        if (x.compareTo(ONE) == 0) return ZERO

        // Scale x into [0.7, 1.45] using powers of 2
        val dApprox = x.toDouble()
        var k = if (dApprox.isFinite() && dApprox > 0.0) {
            floor(dLn(dApprox) / 0.6931471805599453).toInt()
        } else {
            0
        }
        var m = if (k >= 0) {
            x.divide(TWO.pow(k, WORK_MC), WORK_MC)
        } else {
            x.multiply(TWO.pow(-k, WORK_MC), WORK_MC)
        }
        val lowBound = BigDecimal("0.70710678")
        val highBound = BigDecimal("1.41421356")
        while (m > highBound) {
            m = m.divide(TWO, WORK_MC)
            k++
        }
        while (m < lowBound) {
            m = m.multiply(TWO, WORK_MC)
            k--
        }

        // ln(m) = 2 * (u + u^3/3 + u^5/5 + ...) where u = (m-1)/(m+1)
        val u = m.subtract(ONE, WORK_MC).divide(m.add(ONE, WORK_MC), WORK_MC)
        val u2 = u.multiply(u, WORK_MC)
        var term = u
        var sum = u
        var n = 1L
        val threshold = BigDecimal("1e-58")
        while (term.abs() > threshold && n < 200) {
            term = term.multiply(u2, WORK_MC)
            val contrib = term.divide(BigDecimal.valueOf(2 * n + 1), WORK_MC)
            sum = sum.add(contrib, WORK_MC)
            n++
        }
        val lnM = sum.multiply(TWO, WORK_MC)
        return cleanZero(lnM.add(LN2.multiply(BigDecimal.valueOf(k.toLong()), WORK_MC), WORK_MC))
    }

    fun log10(x: BigDecimal): BigDecimal = ln(x).divide(LN10, WORK_MC)

    fun log2(x: BigDecimal): BigDecimal = ln(x).divide(LN2, WORK_MC)

    fun logBase(base: BigDecimal, x: BigDecimal): BigDecimal {
        if (base <= ZERO || base.compareTo(ONE) == 0) {
            throw MathDomainException("Log base must be > 0 and ≠ 1")
        }
        return ln(x).divide(ln(base), WORK_MC)
    }

    // --- Roots & Powers ---

    fun sqrt(x: BigDecimal): BigDecimal {
        if (x < ZERO) throw MathDomainException("Square root of negative number")
        if (x.compareTo(ZERO) == 0) return ZERO
        val d = x.toDouble()
        var guess = if (d.isFinite() && d > 0.0) {
            BigDecimal.valueOf(kotlin.math.sqrt(d))
        } else {
            x.divide(TWO, WORK_MC)
        }
        if (guess.compareTo(ZERO) == 0) guess = ONE
        repeat(10) {
            guess = guess.add(x.divide(guess, WORK_MC), WORK_MC).divide(TWO, WORK_MC)
        }
        return guess
    }

    fun cbrt(x: BigDecimal): BigDecimal = nthRoot(BigDecimal.valueOf(3), x)

    fun nthRoot(n: BigDecimal, x: BigDecimal): BigDecimal {
        if (n.compareTo(ZERO) == 0) throw MathDomainException("0th root is undefined")
        val isInt = isExactInteger(n)
        if (isInt) {
            val nInt = n.toBigIntegerExact()
            val isOdd = nInt.mod(BigInteger.valueOf(2)) != BigInteger.ZERO
            if (x < ZERO) {
                if (!isOdd) throw MathDomainException("Even root of negative number")
                val absRoot = pow(x.abs(), ONE.divide(n, WORK_MC))
                return absRoot.negate()
            }
        } else if (x < ZERO) {
            throw MathDomainException("Fractional root of negative number")
        }
        if (x.compareTo(ZERO) == 0) {
            if (n < ZERO) throw MathDomainException("Division by zero")
            return ZERO
        }
        return pow(x, ONE.divide(n, WORK_MC))
    }

    fun pow(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        if (exponent.compareTo(ZERO) == 0) return ONE
        if (base.compareTo(ZERO) == 0) {
            if (exponent < ZERO) throw MathDomainException("Division by zero (0^negative)")
            return ZERO
        }
        if (base.compareTo(ONE) == 0) return ONE

        if (isExactInteger(exponent)) {
            val expInt = try {
                exponent.intValueExact()
            } catch (_: ArithmeticException) {
                null
            }
            if (expInt != null && abs(expInt) <= 99999) {
                return if (expInt >= 0) {
                    base.pow(expInt, WORK_MC)
                } else {
                    ONE.divide(base.pow(-expInt, WORK_MC), WORK_MC)
                }
            }
        }

        if (base < ZERO) {
            // Check if exponent is a rational p/q with small odd denominator (e.g., (-8)^(1/3) or (-8)^(2/3))
            val frac = approximateSimpleFraction(exponent, maxDenom = 99)
            if (frac != null && frac.second % 2L != 0L) {
                val p = frac.first
                val q = frac.second
                val root = exp(ln(base.abs()).divide(BigDecimal.valueOf(q), WORK_MC))
                val signedRoot = root.negate()
                return pow(signedRoot, BigDecimal.valueOf(p))
            }
            throw MathDomainException("Complex result: negative base with non-integer exponent")
        }

        return exp(exponent.multiply(ln(base), WORK_MC))
    }

    // --- Combinatorics & Special Functions ---

    fun factorial(x: BigDecimal): BigDecimal {
        if (isExactInteger(x)) {
            val n = try {
                x.intValueExact()
            } catch (_: ArithmeticException) {
                throw MathDomainException("Factorial argument too large")
            }
            if (n < 0) throw MathDomainException("Factorial undefined for negative integers")
            if (n > 500) throw MathDomainException("Factorial overflow (n ≤ 500)")
            var res = ONE
            for (i in 2..n) {
                res = res.multiply(BigDecimal.valueOf(i.toLong()), WORK_MC)
            }
            return res
        }
        // Non-integer factorial via Gamma(x + 1) using Lanczos approximation (double precision fallback into BigDecimal)
        val xd = x.toDouble()
        if (!xd.isFinite() || xd < -170.0 || xd > 170.0) {
            throw MathDomainException("Factorial out of range")
        }
        return BigDecimal.valueOf(gamma(xd + 1.0)).round(MathContext(15, RoundingMode.HALF_EVEN))
    }

    private fun gamma(z: Double): Double {
        val g = 7
        val p = doubleArrayOf(
            0.99999999999980993,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
        )
        return if (z < 0.5) {
            kotlin.math.PI / (dSin(kotlin.math.PI * z) * gamma(1.0 - z))
        } else {
            val z1 = z - 1.0
            var x = p[0]
            for (i in 1 until g + 2) {
                x += p[i] / (z1 + i)
            }
            val t = z1 + g + 0.5
            kotlin.math.sqrt(2 * kotlin.math.PI) * t.dPow(z1 + 0.5) * kotlin.math.exp(-t) * x
        }
    }

    fun nPr(n: BigDecimal, r: BigDecimal): BigDecimal {
        if (!isExactInteger(n) || !isExactInteger(r)) throw MathDomainException("nPr requires integers")
        val ni = n.toInt()
        val ri = r.toInt()
        if (ni < 0 || ri < 0 || ri > ni) throw MathDomainException("Requires 0 ≤ r ≤ n")
        if (ni > 500) throw MathDomainException("Overflow (n ≤ 500)")
        var res = ONE
        for (i in (ni - ri + 1)..ni) {
            res = res.multiply(BigDecimal.valueOf(i.toLong()), WORK_MC)
        }
        return res
    }

    fun nCr(n: BigDecimal, r: BigDecimal): BigDecimal {
        if (!isExactInteger(n) || !isExactInteger(r)) throw MathDomainException("nCr requires integers")
        val ni = n.toInt()
        val ri = r.toInt()
        if (ni < 0 || ri < 0 || ri > ni) throw MathDomainException("Requires 0 ≤ r ≤ n")
        if (ni > 500) throw MathDomainException("Overflow (n ≤ 500)")
        val k = minOf(ri, ni - ri)
        var res = ONE
        for (i in 1..k) {
            res = res.multiply(BigDecimal.valueOf((ni - k + i).toLong()), WORK_MC)
                .divide(BigDecimal.valueOf(i.toLong()), WORK_MC)
        }
        return res
    }

    fun isExactInteger(value: BigDecimal): Boolean {
        val stripped = value.stripTrailingZeros()
        return stripped.scale() <= 0
    }

    // --- Formatting & Rational Hints ---

    fun formatResult(
        value: BigDecimal,
        precision: Int = 14,
        notationMode: NotationMode = NotationMode.NORMAL
    ): String {
        val cleaned = cleanZero(value)
        if (cleaned.compareTo(ZERO) == 0) return "0"

        val mc = MathContext(precision.coerceIn(6, 34), RoundingMode.HALF_UP)
        val rounded = cleaned.round(mc).stripTrailingZeros()

        val absVal = rounded.abs()
        val useSci = when (notationMode) {
            NotationMode.SCIENTIFIC -> true
            NotationMode.ENGINEERING -> true
            NotationMode.NORMAL -> absVal >= BigDecimal("1e12") || (absVal > ZERO && absVal < BigDecimal("1e-7"))
        }

        if (!useSci) {
            val plain = rounded.toPlainString()
            return if (plain.contains('.')) plain.trimEnd('0').trimEnd('.') else plain
        }

        if (notationMode == NotationMode.ENGINEERING) {
            val sciStr = rounded.toString()
            val expVal = rounded.precision() - rounded.scale() - 1
            val engExp = floor(expVal / 3.0).toInt() * 3
            val shift = engExp
            val mantissa = if (shift >= 0) {
                rounded.movePointLeft(shift)
            } else {
                rounded.movePointRight(-shift)
            }.stripTrailingZeros()
            val mStr = mantissa.toPlainString().let {
                if (it.contains('.')) it.trimEnd('0').trimEnd('.') else it
            }
            return if (engExp == 0) mStr else "${mStr}E$engExp"
        }

        // Standard scientific notation
        val expVal = rounded.precision() - rounded.scale() - 1
        val mantissa = if (expVal >= 0) {
            rounded.movePointLeft(expVal)
        } else {
            rounded.movePointRight(-expVal)
        }.stripTrailingZeros()
        val mStr = mantissa.toPlainString().let {
            if (it.contains('.')) it.trimEnd('0').trimEnd('.') else it
        }
        return if (expVal == 0) mStr else "${mStr}E$expVal"
    }

    fun toFractionHint(value: BigDecimal): String? {
        if (value.abs() > BigDecimal.valueOf(100000) || value.abs() < BigDecimal("1e-9")) return null
        if (isExactInteger(value)) return null
        val frac = approximateSimpleFraction(value, maxDenom = 2000, tol = 1e-11) ?: return null
        if (frac.second <= 1) return null
        return "${frac.first}/${frac.second}"
    }

    private fun approximateSimpleFraction(
        value: BigDecimal,
        maxDenom: Int = 1000,
        tol: Double = 1e-12
    ): Pair<Long, Long>? {
        val x = value.toDouble()
        if (!x.isFinite()) return null
        val sign = if (x < 0) -1L else 1L
        var a = abs(x)
        var h1 = 1L; var h0 = 0L
        var k1 = 0L; var k0 = 1L
        var b = a
        for (i in 0 until 24) {
            val whole = floor(b).toLong()
            val h2 = whole * h1 + h0
            val k2 = whole * k1 + k0
            if (k2 > maxDenom) break
            h0 = h1; h1 = h2
            k0 = k1; k1 = k2
            val diff = abs(a - h1.toDouble() / k1.toDouble())
            if (diff < tol) {
                return Pair(sign * h1, k1)
            }
            val rem = b - whole
            if (rem < 1e-14) break
            b = 1.0 / rem
        }
        return null
    }
}
