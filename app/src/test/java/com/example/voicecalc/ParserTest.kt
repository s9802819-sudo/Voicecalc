package com.example.voicecalc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ParserTest {
    private fun r(s: String) = Evaluator.eval(SpeechParser.toExpression(s))

    @Test fun arabic() {
        assertEquals("11", r("خمسة زائد ثلاثة ضرب اثنين"))
        assertEquals("125", r("مئة وخمسة وعشرون"))
        assertEquals("8", r("٥ + ٣"))
        assertEquals("3.5", r("ثلاثة فاصلة خمسة"))
        assertEquals("2", r("عشرة قسمة خمسة"))
    }
    @Test fun english() {
        assertEquals("11", r("five plus three times two"))
        assertEquals("235", r("two hundred and thirty five"))
        assertEquals("5", r("ten divided by two"))
    }
    @Test fun divByZero() { assertThrows(ArithmeticException::class.java) { r("one divided by zero") } }
}
