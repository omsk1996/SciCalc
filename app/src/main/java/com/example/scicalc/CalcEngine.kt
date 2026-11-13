package com.example.scicalc

import java.math.BigDecimal
import java.math.MathContext
import java.util.Locale
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

enum class AngleMode { DEG, RAD }

enum class TokenKind { NUMBER, OPERATOR, FUNCTION, CONSTANT, PAREN }

class CalcException(message: String) : Exception(message)

/**
 * Expression model: a list of display tokens such as
 * ["2", "×", "sin(", "30", ")", "+", "π"].
 * Functions carry their own opening parenthesis; missing closing
 * parentheses are tolerated (auto-closed) when evaluating.
 */
object CalcEngine {

    val FUNCTIONS = setOf(
        "sin(", "cos(", "tan(", "sin⁻¹(", "cos⁻¹(", "tan⁻¹(",
        "sinh(", "cosh(", "tanh(", "sinh⁻¹(", "cosh⁻¹(", "tanh⁻¹(",
        "ln(", "log(", "√(", "∛(", "abs(",
    )
    val BINARY = setOf("+", "−", "×", "÷", "^", "E")
    val POSTFIX = setOf("!", "%", "²", "³", "⁻¹")
    val CONSTANTS = setOf("π", "e", "Ans")

    fun isNumber(s: String?) = !s.isNullOrEmpty() && s.all { it.isDigit() || it == '.' }

    fun openParens(tokens: List<String>): Int =
        tokens.count { it == "(" || it in FUNCTIONS } - tokens.count { it == ")" }

    fun evaluate(tokens: List<String>, mode: AngleMode, ans: Double): Double {
        if (tokens.isEmpty()) throw CalcException("Syntax error")
        val parser = Parser(tokens, mode, ans)
        val value = try {
            parser.parseExpression()
        } catch (e: CalcException) {
            throw e
        } catch (e: Exception) {
            throw CalcException("Syntax error")
        }
        if (parser.pos < tokens.size) throw CalcException("Syntax error")
        if (value.isNaN()) throw CalcException("Domain error")
        if (value.isInfinite()) throw CalcException("Overflow")
        return value
    }

    /** Plain formatting without digit grouping, using ASCII '-' (e.g. "-1.5E-12"). */
    fun formatRaw(v: Double): String {
        if (v == 0.0) return "0"
        val a = abs(v)
        return if (a >= 1e12 || a < 1e-9) {
            val str = String.format(Locale.US, "%.10e", v)
            val (mant, exp) = str.split("e")
            val m = if (mant.contains('.')) mant.trimEnd('0').trimEnd('.') else mant
            "${m}E${exp.toInt()}"
        } else {
            BigDecimal(v).round(MathContext(12)).stripTrailingZeros().toPlainString()
        }
    }

    /** Pretty result for display: grouped digits and a real minus sign. */
    fun format(v: Double): String {
        val raw = formatRaw(v)
        if (raw.contains('E')) return raw.replace("-", "−")
        val neg = raw.startsWith("-")
        val body = if (neg) raw.substring(1) else raw
        return (if (neg) "−" else "") + groupNumber(body)
    }

    /** Turn a numeric result back into editable tokens. */
    fun resultToTokens(v: Double): List<String> {
        val raw = formatRaw(v)
        val out = mutableListOf<String>()
        var rest = raw
        if (rest.startsWith("-")) { out += "−"; rest = rest.substring(1) }
        val parts = rest.split("E")
        out += parts[0]
        if (parts.size == 2) {
            out += "E"
            var e = parts[1]
            if (e.startsWith("-")) { out += "−"; e = e.substring(1) }
            out += e
        }
        return out
    }

    fun groupNumber(s: String): String {
        val dot = s.indexOf('.')
        val intPart = if (dot >= 0) s.substring(0, dot) else s
        val rest = if (dot >= 0) s.substring(dot) else ""
        val grouped = intPart.reversed().chunked(3).joinToString(",").reversed()
        return grouped + rest
    }

    /** Render tokens into styled display pieces. */
    fun render(tokens: List<String>): List<Pair<String, TokenKind>> {
        val out = ArrayList<Pair<String, TokenKind>>(tokens.size)
        var prev: String? = null
        for (t in tokens) {
            val unaryCtx = prev == null || prev in BINARY || prev == "(" || prev in FUNCTIONS
            out += when {
                isNumber(t) -> groupNumber(t) to TokenKind.NUMBER
                t == "−" && unaryCtx -> "−" to TokenKind.OPERATOR
                t == "+" || t == "−" || t == "×" || t == "÷" -> " $t " to TokenKind.OPERATOR
                t == "^" || t == "E" || t in POSTFIX -> t to TokenKind.OPERATOR
                t in FUNCTIONS -> t to TokenKind.FUNCTION
                t == "(" || t == ")" -> t to TokenKind.PAREN
                else -> t to TokenKind.CONSTANT
            }
            prev = t
        }
        return out
    }

    fun plainText(tokens: List<String>) = render(tokens).joinToString("") { it.first }

    private class Parser(val t: List<String>, val mode: AngleMode, val ans: Double) {
        var pos = 0
        fun peek(): String? = t.getOrNull(pos)

        fun parseExpression(): Double {
            var v = parseTerm()
            while (true) {
                when (peek()) {
                    "+" -> { pos++; v += parseTerm() }
                    "−" -> { pos++; v -= parseTerm() }
                    else -> return v
                }
            }
        }

        private fun startsOperand(s: String?) = s != null &&
            (isNumber(s) || s == "(" || s in CONSTANTS || s in FUNCTIONS)

        private fun parseTerm(): Double {
            var v = parseUnary()
            while (true) {
                val s = peek()
                when {
                    s == "×" -> { pos++; v *= parseUnary() }
                    s == "÷" -> {
                        pos++
                        val d = parseUnary()
                        if (d == 0.0) throw CalcException("Can't divide by 0")
                        v /= d
                    }
                    startsOperand(s) -> v *= parsePower() // implicit multiplication: 2π, 3(4), 2sin(30)
                    else -> return v
                }
            }
        }

        private fun parseUnary(): Double = when (peek()) {
            "−" -> { pos++; -parseUnary() }
            "+" -> { pos++; parseUnary() }
            else -> parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            return when (peek()) {
                "^" -> { pos++; power(base, parseUnary()) }
                "E" -> { pos++; base * 10.0.pow(parseUnary()) }
                else -> base
            }
        }

        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (true) {
                v = when (peek()) {
                    "!" -> factorial(v)
                    "%" -> v / 100.0
                    "²" -> v * v
                    "³" -> v * v * v
                    "⁻¹" -> if (v == 0.0) throw CalcException("Can't divide by 0") else 1.0 / v
                    else -> return v
                }
                pos++
            }
        }

        private fun parsePrimary(): Double {
            val s = peek() ?: throw CalcException("Incomplete")
            pos++
            return when {
                isNumber(s) -> s.toDoubleOrNull() ?: throw CalcException("Syntax error")
                s == "π" -> PI
                s == "e" -> E
                s == "Ans" -> ans
                s == "(" -> parseExpression().also { if (peek() == ")") pos++ }
                s in FUNCTIONS -> {
                    val a = parseExpression()
                    if (peek() == ")") pos++
                    applyFunction(s, a)
                }
                else -> throw CalcException("Syntax error")
            }
        }

        private fun toRad(x: Double) = if (mode == AngleMode.DEG) Math.toRadians(x) else x
        private fun fromRad(x: Double) = if (mode == AngleMode.DEG) Math.toDegrees(x) else x
        private fun snap(x: Double) = if (abs(x) < 1e-14) 0.0 else x
        private fun domain(ok: Boolean) { if (!ok) throw CalcException("Domain error") }

        private fun applyFunction(f: String, a: Double): Double = when (f) {
            "sin(" -> snap(sin(toRad(a)))
            "cos(" -> snap(cos(toRad(a)))
            "tan(" -> {
                if (mode == AngleMode.DEG && (a - 90.0) % 180.0 == 0.0) throw CalcException("Undefined")
                snap(tan(toRad(a)))
            }
            "sin⁻¹(" -> { domain(a in -1.0..1.0); fromRad(asin(a)) }
            "cos⁻¹(" -> { domain(a in -1.0..1.0); fromRad(acos(a)) }
            "tan⁻¹(" -> fromRad(atan(a))
            "sinh(" -> sinh(a)
            "cosh(" -> cosh(a)
            "tanh(" -> tanh(a)
            "sinh⁻¹(" -> ln(a + sqrt(a * a + 1))
            "cosh⁻¹(" -> { domain(a >= 1); ln(a + sqrt(a * a - 1)) }
            "tanh⁻¹(" -> { domain(abs(a) < 1); 0.5 * ln((1 + a) / (1 - a)) }
            "ln(" -> { domain(a > 0); ln(a) }
            "log(" -> { domain(a > 0); log10(a) }
            "√(" -> { domain(a >= 0); sqrt(a) }
            "∛(" -> cbrt(a)
            "abs(" -> abs(a)
            else -> throw CalcException("Syntax error")
        }

        private fun power(b: Double, e: Double): Double {
            if (b < 0 && e != floor(e)) {
                // Allow odd roots of negatives, e.g. (−8)^(1/3)
                val inv = 1.0 / e
                if (abs(inv - round(inv)) < 1e-9 && round(inv).toLong() % 2L != 0L) {
                    return -((-b).pow(e))
                }
                throw CalcException("Domain error")
            }
            return b.pow(e)
        }

        private fun factorial(x: Double): Double {
            if (x < 0 && x == floor(x)) throw CalcException("Domain error")
            if (x > 170) throw CalcException("Overflow")
            if (x == floor(x)) {
                var r = 1.0
                var i = 2
                while (i <= x) { r *= i; i++ }
                return r
            }
            return gamma(x + 1)
        }

        private fun gamma(x: Double): Double {
            if (x < 0.5) return PI / (sin(PI * x) * gamma(1 - x))
            val c = doubleArrayOf(
                0.99999999999980993, 676.5203681218851, -1259.1392167224028,
                771.32342877765313, -176.61502916214059, 12.507343278686905,
                -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7,
            )
            val xx = x - 1
            var a = c[0]
            val t = xx + 7.5
            for (i in 1 until 9) a += c[i] / (xx + i)
            return sqrt(2 * PI) * t.pow(xx + 0.5) * exp(-t) * a
        }
    }
}
