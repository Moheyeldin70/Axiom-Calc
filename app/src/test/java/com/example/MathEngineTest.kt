package com.example

import com.example.math.AngleUnit
import com.example.math.BigMath
import com.example.math.EquationSolver
import com.example.math.LinearSystemResult
import com.example.math.MathDomainException
import com.example.math.MathParser
import com.example.math.UnitCategory
import com.example.math.UnitConverterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import kotlin.math.abs

class MathEngineTest {

    private fun assertClose(expected: String, actual: BigDecimal, tol: String = "1e-20") {
        val diff = actual.subtract(BigDecimal(expected)).abs()
        assertTrue(
            "Expected $expected, got ${actual.toPlainString()} (diff=$diff)",
            diff < BigDecimal(tol)
        )
    }

    @Test
    fun `high precision decimal arithmetic avoids float drift`() {
        val res = MathParser.evaluate("0.1 + 0.2")
        assertEquals("0.3", BigMath.formatResult(res, 20))
    }

    @Test
    fun `exact quadrant trigonometry in degrees and radians`() {
        assertEquals("0", BigMath.formatResult(MathParser.evaluate("sin(180)", AngleUnit.DEG), 20))
        assertEquals("1", BigMath.formatResult(MathParser.evaluate("sin(90)", AngleUnit.DEG), 20))
        assertEquals("0", BigMath.formatResult(MathParser.evaluate("cos(90)", AngleUnit.DEG), 20))
        assertEquals("-1", BigMath.formatResult(MathParser.evaluate("cos(180)", AngleUnit.DEG), 20))
        assertEquals("1", BigMath.formatResult(MathParser.evaluate("tan(45)", AngleUnit.DEG), 20))
        assertClose("0.5", MathParser.evaluate("sin(π/6)", AngleUnit.RAD), "1e-30")
        assertClose("0.5", MathParser.evaluate("cos(π/3)", AngleUnit.RAD), "1e-30")
    }

    @Test(expected = MathDomainException::class)
    fun `tan 90 deg throws domain error`() {
        MathParser.evaluate("tan(90)", AngleUnit.DEG)
    }

    @Test
    fun `logarithms powers and roots`() {
        assertClose("3", MathParser.evaluate("log(1000)"), "1e-30")
        assertClose("4", MathParser.evaluate("log2(16)"), "1e-30")
        assertClose("1", MathParser.evaluate("ln(e)"), "1e-30")
        assertClose("3", MathParser.evaluate("logb(3, 27)"), "1e-30")
        assertClose("1.4142135623730950488016887242097", MathParser.evaluate("sqrt(2)"), "1e-28")
        assertEquals("-3", BigMath.formatResult(MathParser.evaluate("cbrt(-27)"), 20))
        assertClose("2", MathParser.evaluate("nroot(5, 32)"), "1e-28")
        assertClose("512", MathParser.evaluate("2^3^2"), "1e-20")
    }

    @Test
    fun `hyperbolic and inverse hyperbolic identities`() {
        // cosh^2(x) - sinh^2(x) == 1
        val identity = MathParser.evaluate("cosh(2.5)^2 - sinh(2.5)^2")
        assertClose("1", identity, "1e-30")

        // asinh(sinh(x)) == x
        val invSinh = MathParser.evaluate("asinh(sinh(1.75))")
        assertClose("1.75", invSinh, "1e-30")

        // acosh(cosh(x)) == x for x >= 0
        val invCosh = MathParser.evaluate("acosh(cosh(3.2))")
        assertClose("3.2", invCosh, "1e-30")

        // atanh(tanh(x)) == x
        val invTanh = MathParser.evaluate("atanh(tanh(0.65))")
        assertClose("0.65", invTanh, "1e-30")
    }

    @Test
    fun `equation solver finds general transcendental and polynomial roots`() {
        val roots = EquationSolver.solveGeneralEquation("cos(x) = x", -5.0, 5.0, AngleUnit.RAD)
        assertEquals(1, roots.size)
        // Dottie number: 0.7390851332151606416553120876738734
        assertClose("0.73908513321516064165", roots.first().value, "1e-18")

        val quadComplex = EquationSolver.solveQuadratic(
            BigDecimal("1"), BigDecimal("2"), BigDecimal("5")
        )
        assertEquals(2, quadComplex.roots.size)
        assertEquals("-1 + 2i", quadComplex.roots[0].format(10))
        assertEquals("-1 - 2i", quadComplex.roots[1].format(10))

        val cubic = EquationSolver.solveCubic(
            BigDecimal("1"), BigDecimal("-6"), BigDecimal("11"), BigDecimal("-6")
        )
        val sortedRoots = cubic.roots.map { it.real.toDouble() }.sorted()
        assertEquals(3, sortedRoots.size)
        assertTrue(abs(sortedRoots[0] - 1.0) < 1e-12)
        assertTrue(abs(sortedRoots[1] - 2.0) < 1e-12)
        assertTrue(abs(sortedRoots[2] - 3.0) < 1e-12)
    }

    @Test
    fun `linear system and calculus accuracy`() {
        val lin = EquationSolver.solveLinearSystem(
            arrayOf(
                arrayOf(BigDecimal("2"), BigDecimal("1")),
                arrayOf(BigDecimal("1"), BigDecimal("-3"))
            ),
            arrayOf(BigDecimal("5"), BigDecimal("-1"))
        )
        assertTrue(lin is LinearSystemResult.Unique)
        val sol = (lin as LinearSystemResult.Unique).solution
        assertClose("2", sol[0], "1e-20")
        assertClose("1", sol[1], "1e-20")

        // d/dx(x^3) at x=2 is 12
        val deriv = EquationSolver.numericalDerivative("x^3", BigDecimal("2"), AngleUnit.RAD)
        assertClose("12", deriv, "1e-10")

        // integral_0^pi sin(x) dx is 2
        val integral = EquationSolver.definiteIntegral("sin(x)", BigDecimal.ZERO, BigMath.PI, AngleUnit.RAD)
        assertClose("2", integral, "1e-7")
    }

    @Test
    fun `unit converter temperature and length precision`() {
        val lenUnits = UnitConverterEngine.unitsByCategory.getValue(UnitCategory.LENGTH)
        val ft = lenUnits.first { it.id == "ft" }
        val cm = lenUnits.first { it.id == "cm" }
        assertClose("30.48", UnitConverterEngine.convert(BigDecimal("1"), ft, cm), "1e-20")

        val tempUnits = UnitConverterEngine.unitsByCategory.getValue(UnitCategory.TEMPERATURE)
        val c = tempUnits.first { it.id == "c" }
        val f = tempUnits.first { it.id == "f" }
        assertClose("212", UnitConverterEngine.convert(BigDecimal("100"), c, f), "1e-20")
        assertClose("-40", UnitConverterEngine.convert(BigDecimal("-40"), f, c), "1e-20")
    }
}
