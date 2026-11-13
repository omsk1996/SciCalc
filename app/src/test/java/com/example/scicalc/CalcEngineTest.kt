package com.example.scicalc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CalcEngineTest {
    private fun eval(vararg t: String, mode: AngleMode = AngleMode.DEG) =
        CalcEngine.format(CalcEngine.evaluate(t.toList(), mode, 0.0))

    @Test fun precedence() = assertEquals("14", eval("2", "+", "3", "×", "4"))
    @Test fun unaryMinusPower() = assertEquals("−4", eval("−", "2", "^", "2"))
    @Test fun rightAssocPower() = assertEquals("512", eval("2", "^", "3", "^", "2"))
    @Test fun implicitMultiplication() = assertEquals("6.28318530718", eval("2", "π"))
    @Test fun autoCloseParens() = assertEquals("20", eval("(", "2", "+", "3", ")", "4"))
    @Test fun sinDegrees() = assertEquals("0.5", eval("sin(", "30", ")"))
    @Test fun sin180IsZero() = assertEquals("0", eval("sin(", "180", ")"))
    @Test fun tan45() = assertEquals("1", eval("tan(", "45", ")"))
    @Test fun cosRadians() = assertEquals("−1", eval("cos(", "π", ")", mode = AngleMode.RAD))
    @Test fun asinDegrees() = assertEquals("30", eval("sin⁻¹(", "0.5", ")"))
    @Test fun factorial() = assertEquals("120", eval("5", "!"))
    @Test fun gammaHalf() = assertEquals("0.886226925453", eval("0.5", "!"))
    @Test fun percent() = assertEquals("25", eval("50", "%", "×", "50"))
    @Test fun squareAndInverse() = assertEquals("0.0625", eval("4", "²", "⁻¹"))
    @Test fun exp() = assertEquals("1,500", eval("1.5", "E", "3"))
    @Test fun logLn() = assertEquals("3", eval("log(", "1000", ")"))
    @Test fun cubeRootNegative() = assertEquals("−3", eval("∛(", "−", "27", ")"))
    @Test fun grouping() = assertEquals("1,234,567.5", eval("1234567.5"))
    @Test fun bigScientific() = assertEquals("1E15", eval("10", "^", "15"))

    @Test fun divideByZero() {
        assertThrows(CalcException::class.java) { eval("1", "÷", "0") }
    }

    @Test fun domain() {
        assertThrows(CalcException::class.java) { eval("√(", "−", "4") }
    }

    @Test fun resultTokensRoundTrip() {
        assertEquals(listOf("−", "1.5", "E", "−", "12"), CalcEngine.resultToTokens(-1.5e-12))
    }
}
