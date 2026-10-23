package com.example.voicecalc

import java.math.BigDecimal
import java.math.MathContext

object Evaluator {
    private val tok = Regex("""\d+(\.\d+)?|[-+*/()%]""")

    fun eval(src: String): String {
        val t = tok.findAll(src.replace('×', '*').replace('÷', '/').replace('−', '-')).map { it.value }.toList()
        val p = Parser(t)
        val v = p.expr()
        require(t.isNotEmpty() && p.pos == t.size) { "syntax" }
        return v.stripTrailingZeros().toPlainString()
    }

    private class Parser(val t: List<String>) {
        var pos = 0
        private fun peek() = t.getOrNull(pos)
        fun expr(): BigDecimal {
            var v = term()
            while (peek() == "+" || peek() == "-") { val o = t[pos++]; val r = term(); v = if (o == "+") v + r else v - r }
            return v
        }
        private fun term(): BigDecimal {
            var v = unary()
            while (peek() == "*" || peek() == "/") {
                val o = t[pos++]; val r = unary()
                v = if (o == "*") v * r else {
                    if (r.signum() == 0) throw ArithmeticException("div0")
                    v.divide(r, MathContext.DECIMAL64)
                }
            }
            return v
        }
        private fun unary(): BigDecimal = if (peek() == "-") { pos++; unary().negate() } else postfix()
        private fun postfix(): BigDecimal {
            var v = primary()
            while (peek() == "%") { pos++; v = v.divide(BigDecimal(100)) }
            return v
        }
        private fun primary(): BigDecimal {
            val s = t.getOrNull(pos++) ?: throw IllegalArgumentException("eof")
            return if (s == "(") expr().also { require(t.getOrNull(pos++) == ")") }
            else s.toBigDecimalOrNull() ?: throw IllegalArgumentException("bad")
        }
    }
}
