package com.example.math

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

sealed interface ExprNode {
    fun evalBig(vars: Map<String, BigDecimal>, angleUnit: AngleUnit): BigDecimal
    fun evalDouble(x: Double, angleUnit: AngleUnit): Double
}

data class NumberNode(val value: BigDecimal) : ExprNode {
    private val dVal = value.toDouble()
    override fun evalBig(vars: Map<String, BigDecimal>, angleUnit: AngleUnit): BigDecimal = value
    override fun evalDouble(x: Double, angleUnit: AngleUnit): Double = dVal
}

data class VarNode(val name: String) : ExprNode {
    override fun evalBig(vars: Map<String, BigDecimal>, angleUnit: AngleUnit): BigDecimal {
        return vars[name] ?: throw MathDomainException("Undefined variable '$name'")
    }
    override fun evalDouble(x: Double, angleUnit: AngleUnit): Double {
        return when (name) {
            "x" -> x
            "pi", "π" -> Math.PI
            "e" -> Math.E
            "phi", "φ" -> 1.618033988749895
            else -> Double.NaN
        }
    }
}

data class UnaryNode(val op: Char, val child: ExprNode) : ExprNode {
    override fun evalBig(vars: Map<String, BigDecimal>, angleUnit: AngleUnit): BigDecimal {
        val v = child.evalBig(vars, angleUnit)
        return when (op) {
            '-' -> v.negate()
            '+' -> v
            '!' -> BigMath.factorial(v)
            '%' -> v.divide(BigDecimal.valueOf(100), BigMath.WORK_MC)
            else -> throw MathDomainException("Unknown unary operator $op")
        }
    }

    override fun evalDouble(x: Double, angleUnit: AngleUnit): Double {
        val v = child.evalDouble(x, angleUnit)
        return when (op) {
            '-' -> -v
            '+' -> v
            '%' -> v / 100.0
            '!' -> BigMath.factorial(BigDecimal.valueOf(v)).toDouble()
            else -> Double.NaN
        }
    }
}

data class BinaryNode(val op: Char, val left: ExprNode, val right: ExprNode) : ExprNode {
    override fun evalBig(vars: Map<String, BigDecimal>, angleUnit: AngleUnit): BigDecimal {
        val l = left.evalBig(vars, angleUnit)
        val r = right.evalBig(vars, angleUnit)
        return when (op) {
            '+' -> l.add(r, BigMath.WORK_MC)
            '-' -> l.subtract(r, BigMath.WORK_MC)
            '*' -> l.multiply(r, BigMath.WORK_MC)
            '/' -> {
                if (r.compareTo(BigDecimal.ZERO) == 0) throw MathDomainException("Division by zero")
                l.divide(r, BigMath.WORK_MC)
            }
            'm' -> {
                if (r.compareTo(BigDecimal.ZERO) == 0) throw MathDomainException("Modulo by zero")
                l.remainder(r, BigMath.WORK_MC)
            }
            '^' -> BigMath.pow(l, r)
            else -> throw MathDomainException("Unknown operator $op")
        }
    }

    override fun evalDouble(x: Double, angleUnit: AngleUnit): Double {
        val l = left.evalDouble(x, angleUnit)
        val r = right.evalDouble(x, angleUnit)
        return when (op) {
            '+' -> l + r
            '-' -> l - r
            '*' -> l * r
            '/' -> if (r == 0.0) Double.NaN else l / r
            'm' -> if (r == 0.0) Double.NaN else l % r
            '^' -> l.pow(r)
            else -> Double.NaN
        }
    }
}

data class FuncNode(val name: String, val args: List<ExprNode>) : ExprNode {
    override fun evalBig(vars: Map<String, BigDecimal>, angleUnit: AngleUnit): BigDecimal {
        val a = args.map { it.evalBig(vars, angleUnit) }
        val x = a.firstOrNull() ?: throw MathDomainException("Missing argument for $name")
        return when (name) {
            "sin" -> BigMath.sin(x, angleUnit)
            "cos" -> BigMath.cos(x, angleUnit)
            "tan" -> BigMath.tan(x, angleUnit)
            "csc" -> BigMath.csc(x, angleUnit)
            "sec" -> BigMath.sec(x, angleUnit)
            "cot" -> BigMath.cot(x, angleUnit)
            "asin", "arcsin", "sin⁻¹" -> BigMath.asin(x, angleUnit)
            "acos", "arccos", "cos⁻¹" -> BigMath.acos(x, angleUnit)
            "atan", "arctan", "tan⁻¹" -> BigMath.atan(x, angleUnit)
            "sinh" -> BigMath.sinh(x)
            "cosh" -> BigMath.cosh(x)
            "tanh" -> BigMath.tanh(x)
            "csch" -> BigMath.csch(x)
            "sech" -> BigMath.sech(x)
            "coth" -> BigMath.coth(x)
            "asinh", "arcsinh", "sinh⁻¹" -> BigMath.asinh(x)
            "acosh", "arccosh", "cosh⁻¹" -> BigMath.acosh(x)
            "atanh", "arctanh", "tanh⁻¹" -> BigMath.atanh(x)
            "ln" -> BigMath.ln(x)
            "log", "log10" -> BigMath.log10(x)
            "log2" -> BigMath.log2(x)
            "logb" -> {
                if (a.size < 2) throw MathDomainException("logb(base, x) requires 2 arguments")
                BigMath.logBase(a[0], a[1])
            }
            "exp" -> BigMath.exp(x)
            "sqrt", "√" -> BigMath.sqrt(x)
            "cbrt", "∛" -> BigMath.cbrt(x)
            "nroot" -> {
                if (a.size < 2) throw MathDomainException("nroot(n, x) requires 2 arguments")
                BigMath.nthRoot(a[0], a[1])
            }
            "abs" -> x.abs()
            "floor" -> x.setScale(0, RoundingMode.FLOOR)
            "ceil" -> x.setScale(0, RoundingMode.CEILING)
            "round" -> x.setScale(0, RoundingMode.HALF_UP)
            "sign", "sgn" -> BigDecimal.valueOf(x.signum().toLong())
            "npr" -> {
                if (a.size < 2) throw MathDomainException("nPr(n, r) requires 2 arguments")
                BigMath.nPr(a[0], a[1])
            }
            "ncr" -> {
                if (a.size < 2) throw MathDomainException("nCr(n, r) requires 2 arguments")
                BigMath.nCr(a[0], a[1])
            }
            "max" -> a.maxOrNull() ?: x
            "min" -> a.minOrNull() ?: x
            else -> throw MathDomainException("Unknown function '$name'")
        }
    }

    override fun evalDouble(x: Double, angleUnit: AngleUnit): Double {
        val v = args[0].evalDouble(x, angleUnit)
        if (v.isNaN()) return Double.NaN
        val toRad = when (angleUnit) {
            AngleUnit.RAD -> 1.0
            AngleUnit.DEG -> Math.PI / 180.0
            AngleUnit.GRAD -> Math.PI / 200.0
        }
        return when (name) {
            "sin" -> sin(v * toRad)
            "cos" -> cos(v * toRad)
            "tan" -> {
                val c = cos(v * toRad)
                if (abs(c) < 1e-12) Double.NaN else tan(v * toRad)
            }
            "csc" -> {
                val s = sin(v * toRad)
                if (abs(s) < 1e-12) Double.NaN else 1.0 / s
            }
            "sec" -> {
                val c = cos(v * toRad)
                if (abs(c) < 1e-12) Double.NaN else 1.0 / c
            }
            "cot" -> {
                val s = sin(v * toRad)
                if (abs(s) < 1e-12) Double.NaN else cos(v * toRad) / s
            }
            "asin", "arcsin", "sin⁻¹" -> if (abs(v) > 1.0) Double.NaN else asin(v) / toRad
            "acos", "arccos", "cos⁻¹" -> if (abs(v) > 1.0) Double.NaN else acos(v) / toRad
            "atan", "arctan", "tan⁻¹" -> atan(v) / toRad
            "sinh" -> sinh(v)
            "cosh" -> cosh(v)
            "tanh" -> tanh(v)
            "csch" -> if (v == 0.0) Double.NaN else 1.0 / sinh(v)
            "sech" -> 1.0 / cosh(v)
            "coth" -> if (v == 0.0) Double.NaN else cosh(v) / sinh(v)
            "asinh", "arcsinh", "sinh⁻¹" -> asinh(v)
            "acosh", "arccosh", "cosh⁻¹" -> if (v < 1.0) Double.NaN else acosh(v)
            "atanh", "arctanh", "tanh⁻¹" -> if (abs(v) >= 1.0) Double.NaN else atanh(v)
            "ln" -> if (v <= 0.0) Double.NaN else ln(v)
            "log", "log10" -> if (v <= 0.0) Double.NaN else log10(v)
            "log2" -> if (v <= 0.0) Double.NaN else log2(v)
            "logb" -> {
                val arg2 = args.getOrNull(1)?.evalDouble(x, angleUnit) ?: return Double.NaN
                if (v <= 0.0 || v == 1.0 || arg2 <= 0.0) Double.NaN else ln(arg2) / ln(v)
            }
            "exp" -> exp(v)
            "sqrt", "√" -> if (v < 0.0) Double.NaN else sqrt(v)
            "cbrt", "∛" -> cbrt(v)
            "nroot" -> {
                val arg2 = args.getOrNull(1)?.evalDouble(x, angleUnit) ?: return Double.NaN
                if (v == 0.0) Double.NaN else arg2.pow(1.0 / v)
            }
            "abs" -> abs(v)
            "floor" -> floor(v)
            "ceil" -> ceil(v)
            "round" -> round(v)
            "sign", "sgn" -> sign(v)
            else -> Double.NaN
        }
    }
}

object MathParser {
    private sealed interface Token {
        data class Num(val text: String) : Token
        data class Ident(val text: String) : Token
        data class Op(val ch: Char) : Token
        data object LParen : Token
        data object RParen : Token
        data object Comma : Token
    }

    private val defaultConstants: Map<String, BigDecimal> = mapOf(
        "π" to BigMath.PI,
        "pi" to BigMath.PI,
        "e" to BigMath.E,
        "φ" to BigMath.PHI,
        "phi" to BigMath.PHI,
        "c_light" to BigDecimal("299792458"),
        "h_planck" to BigDecimal("6.62607015e-34"),
        "g_grav" to BigDecimal("6.67430e-11"),
        "g0" to BigDecimal("9.80665"),
        "n_a" to BigDecimal("6.02214076e23"),
        "r_gas" to BigDecimal("8.31446261815324"),
        "k_b" to BigDecimal("1.380649e-23"),
        "eps0" to BigDecimal("8.8541878188e-12"),
        "mu0" to BigDecimal("1.25663706127e-6"),
        "q_e" to BigDecimal("1.602176634e-19")
    )

    fun parse(rawExpression: String): ExprNode {
        val cleaned = normalizeExpression(rawExpression)
        if (cleaned.isEmpty()) throw MathDomainException("Empty expression")
        val rawTokens = tokenize(cleaned)
        val tokens = insertImplicitMultiplication(rawTokens)
        val parser = RecursiveParser(tokens)
        val ast = parser.parseExpression()
        if (parser.hasMore()) {
            throw MathDomainException("Unexpected token near '${parser.peekDisplay()}'")
        }
        return ast
    }

    fun evaluate(
        rawExpression: String,
        angleUnit: AngleUnit = AngleUnit.RAD,
        variables: Map<String, BigDecimal> = emptyMap()
    ): BigDecimal {
        val ast = parse(rawExpression)
        val mergedVars = buildMap {
            putAll(defaultConstants)
            variables.forEach { (k, v) -> put(k.lowercase(), v) }
        }
        return BigMath.cleanZero(ast.evalBig(mergedVars, angleUnit))
    }

    private fun normalizeExpression(input: String): String {
        return input.trim()
            .replace("×", "*")
            .replace("·", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("–", "-")
            .replace("²", "^2")
            .replace("³", "^3")
            .replace("⁻¹", "_inv")
            .replace("√", "sqrt")
            .replace("∛", "cbrt")
            .replace("mod", " % ")
    }

    private fun tokenize(s: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    var hasDot = (ch == '.')
                    i++
                    while (i < s.length && (s[i].isDigit() || (s[i] == '.' && !hasDot))) {
                        if (s[i] == '.') hasDot = true
                        i++
                    }
                    // Handle scientific notation e.g. 1.5E-4 or 2e10 (only if followed by +/-digit or digit)
                    if (i < s.length && (s[i] == 'E' || s[i] == 'e')) {
                        val nextIdx = i + 1
                        val hasSign = nextIdx < s.length && (s[nextIdx] == '+' || s[nextIdx] == '-')
                        val digitIdx = if (hasSign) nextIdx + 1 else nextIdx
                        if (digitIdx < s.length && s[digitIdx].isDigit()) {
                            i = digitIdx + 1
                            while (i < s.length && s[i].isDigit()) i++
                        }
                    }
                    tokens.add(Token.Num(s.substring(start, i)))
                }
                ch == 'π' || ch == 'φ' -> {
                    tokens.add(Token.Ident(ch.toString()))
                    i++
                }
                ch.isLetter() || ch == '_' -> {
                    val start = i
                    i++
                    while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++
                    var ident = s.substring(start, i).lowercase()
                    ident = when (ident) {
                        "sin_inv" -> "asin"
                        "cos_inv" -> "acos"
                        "tan_inv" -> "atan"
                        "sinh_inv" -> "asinh"
                        "cosh_inv" -> "acosh"
                        "tanh_inv" -> "atanh"
                        else -> ident
                    }
                    tokens.add(Token.Ident(ident))
                }
                ch == '(' -> { tokens.add(Token.LParen); i++ }
                ch == ')' -> { tokens.add(Token.RParen); i++ }
                ch == ',' -> { tokens.add(Token.Comma); i++ }
                ch in charArrayOf('+', '-', '*', '/', '^', '!', '%') -> {
                    val mapped = if (ch == '%') {
                        // Distinguish binary modulo vs postfix percent later; keep '%'
                        '%'
                    } else ch
                    tokens.add(Token.Op(mapped))
                    i++
                }
                else -> throw MathDomainException("Unexpected character '$ch'")
            }
        }
        return tokens
    }

    private val functionNames = setOf(
        "sin", "cos", "tan", "csc", "sec", "cot",
        "asin", "arcsin", "acos", "arccos", "atan", "arctan",
        "sinh", "cosh", "tanh", "csch", "sech", "coth",
        "asinh", "arcsinh", "acosh", "arccosh", "atanh", "arctanh",
        "ln", "log", "log10", "log2", "logb", "exp",
        "sqrt", "cbrt", "nroot", "abs", "floor", "ceil", "round",
        "sign", "sgn", "npr", "ncr", "max", "min"
    )

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val out = mutableListOf<Token>()
        for (idx in tokens.indices) {
            val curr = tokens[idx]
            if (out.isNotEmpty()) {
                val prev = out.last()
                val prevIsValue = prev is Token.Num ||
                    prev is Token.RParen ||
                    (prev is Token.Ident && prev.text !in functionNames) ||
                    (prev is Token.Op && (prev.ch == '!' || prev.ch == '%'))

                val currStartsValueOrFunc = curr is Token.Num ||
                    curr is Token.LParen ||
                    curr is Token.Ident

                if (prevIsValue && currStartsValueOrFunc) {
                    // Don't insert * if prev is '%' followed by number/expr in mod context? Wait:
                    // Usually users type 50% * 2 or 10 mod 3. In normalizeExpression, "mod" was replaced with " % ",
                    // so if prev is Op('%') and curr is Num/LParen/Ident, wait: if it's binary mod (`10 % 3`), we must NOT insert '*'.
                    // If user typed `50%(2)`, we handle '%' as postfix only when followed by binary op or RParen or end,
                    // OR we treat '%' as postfix percent and 'mod' as 'm'. Let's check!
                    if (!(prev is Token.Op && prev.ch == '%')) {
                        out.add(Token.Op('*'))
                    }
                }
            }
            out.add(curr)
        }
        return out
    }

    private class RecursiveParser(private val tokens: List<Token>) {
        private var pos = 0

        fun hasMore(): Boolean = pos < tokens.size
        fun peek(): Token? = tokens.getOrNull(pos)
        fun peekDisplay(): String = peek()?.toString() ?: ""

        fun parseExpression(): ExprNode = parseAddSub()

        private fun parseAddSub(): ExprNode {
            var left = parseMulDiv()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                    pos++
                    val right = parseMulDiv()
                    left = BinaryNode(t.ch, left, right)
                } else break
            }
            return left
        }

        private fun parseMulDiv(): ExprNode {
            var left = parseUnary()
            while (true) {
                val t = peek()
                if (t is Token.Op && (t.ch == '*' || t.ch == '/')) {
                    pos++
                    val right = parseUnary()
                    left = BinaryNode(t.ch, left, right)
                } else if (t is Token.Op && t.ch == '%') {
                    // Check if binary modulo (next token can start a factor) or postfix percent
                    val next = tokens.getOrNull(pos + 1)
                    val startsFactor = next is Token.Num || next is Token.Ident || next is Token.LParen ||
                        (next is Token.Op && (next.ch == '+' || next.ch == '-'))
                    if (startsFactor) {
                        pos++
                        val right = parseUnary()
                        left = BinaryNode('m', left, right)
                    } else {
                        break
                    }
                } else break
            }
            return left
        }

        private fun parseUnary(): ExprNode {
            val t = peek()
            if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
                pos++
                return UnaryNode(t.ch, parseUnary())
            }
            return parsePower()
        }

        private fun parsePower(): ExprNode {
            val base = parsePostfix()
            val t = peek()
            if (t is Token.Op && t.ch == '^') {
                pos++
                val exp = parseUnary() // Right-associative: 2^3^2 = 2^(3^2) and 2^-1
                return BinaryNode('^', base, exp)
            }
            return base
        }

        private fun parsePostfix(): ExprNode {
            var node = parsePrimary()
            while (true) {
                val t = peek()
                if (t is Token.Op && t.ch == '!') {
                    pos++
                    node = UnaryNode('!', node)
                } else if (t is Token.Op && t.ch == '%') {
                    val next = tokens.getOrNull(pos + 1)
                    val startsFactor = next is Token.Num || next is Token.Ident || next is Token.LParen
                    if (!startsFactor) {
                        pos++
                        node = UnaryNode('%', node)
                    } else {
                        break
                    }
                } else break
            }
            return node
        }

        private fun parsePrimary(): ExprNode {
            val t = peek() ?: throw MathDomainException("Unexpected end of expression")
            return when (t) {
                is Token.Num -> {
                    pos++
                    val bd = try {
                        BigDecimal(t.text)
                    } catch (_: NumberFormatException) {
                        throw MathDomainException("Invalid number '${t.text}'")
                    }
                    NumberNode(bd)
                }
                is Token.LParen -> {
                    pos++
                    val inner = parseExpression()
                    val close = peek()
                    if (close !is Token.RParen) {
                        throw MathDomainException("Missing closing parenthesis ')'")
                    }
                    pos++
                    inner
                }
                is Token.Ident -> {
                    pos++
                    if (t.text in functionNames) {
                        val next = peek()
                        if (next is Token.LParen) {
                            pos++
                            val args = mutableListOf<ExprNode>()
                            if (peek() !is Token.RParen) {
                                args.add(parseExpression())
                                while (peek() is Token.Comma) {
                                    pos++
                                    args.add(parseExpression())
                                }
                            }
                            if (peek() !is Token.RParen) {
                                throw MathDomainException("Missing ')' after ${t.text}(...)")
                            }
                            pos++
                            FuncNode(t.text, args)
                        } else {
                            // Support shorthand e.g. "sin x" or "sqrt 9"
                            val arg = parseUnary()
                            FuncNode(t.text, listOf(arg))
                        }
                    } else {
                        // Variable or constant
                        VarNode(t.text)
                    }
                }
                else -> throw MathDomainException("Unexpected token near '$t'")
            }
        }
    }
}
