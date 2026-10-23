package com.example.voicecalc

object SpeechParser {
    private val BIN = setOf("+", "-", "*", "/")
    private val ops = mapOf(
        "زائد" to "+", "زايد" to "+", "جمع" to "+", "plus" to "+", "add" to "+", "+" to "+",
        "ناقص" to "-", "طرح" to "-", "minus" to "-", "subtract" to "-", "-" to "-",
        "ضرب" to "*", "في" to "*", "times" to "*", "multiplied" to "*", "x" to "*", "×" to "*", "*" to "*",
        "قسمه" to "/", "على" to "/", "divided" to "/", "over" to "/", "÷" to "/", "/" to "/",
        "بالمئه" to "%", "percent" to "%", "%" to "%", "(" to "(", ")" to ")"
    )
    private fun m(v: Int, vararg w: String) = w.map { it to v }
    private val words: Map<String, Int> = listOf(
        m(0, "صفر", "zero"), m(1, "واحد", "احد", "واحده", "one"), m(2, "اثنان", "اثنين", "اثنا", "two"),
        m(3, "ثلاثه", "ثلاث", "three"), m(4, "اربعه", "اربع", "four"), m(5, "خمسه", "خمس", "five"),
        m(6, "سته", "ست", "six"), m(7, "سبعه", "سبع", "seven"), m(8, "ثمانيه", "ثماني", "ثمان", "eight"),
        m(9, "تسعه", "تسع", "nine"), m(10, "عشره", "عشر", "ten"), m(11, "eleven"), m(12, "twelve"),
        m(13, "thirteen"), m(14, "fourteen"), m(15, "fifteen"), m(16, "sixteen"), m(17, "seventeen"),
        m(18, "eighteen"), m(19, "nineteen"), m(20, "عشرون", "عشرين", "twenty"), m(30, "ثلاثون", "ثلاثين", "thirty"),
        m(40, "اربعون", "اربعين", "forty"), m(50, "خمسون", "خمسين", "fifty"), m(60, "ستون", "ستين", "sixty"),
        m(70, "سبعون", "سبعين", "seventy"), m(80, "ثمانون", "ثمانين", "eighty"), m(90, "تسعون", "تسعين", "ninety"),
        m(100, "مئه", "مائه", "ميه", "hundred"), m(200, "مئتان", "مئتين", "مائتان", "مائتين", "ميتين"),
        m(1000, "الف", "الاف", "thousand"), m(2000, "الفان", "الفين"), m(1_000_000, "مليون", "million")
    ).flatten().toMap()
    private val hundred = setOf("مئه", "مائه", "ميه", "hundred")
    private val scale = setOf("الف", "الاف", "thousand", "مليون", "million")
    private val point = setOf("فاصله", "فاصل", "point", "comma")
    private val num = Regex("""\d+(\.\d+)?""")

    fun normalize(s: String): String {
        val sb = StringBuilder()
        for (c in s.lowercase()) when {
            c in '\u0660'..'\u0669' -> sb.append(c - '\u0660')
            c in '\u06F0'..'\u06F9' -> sb.append(c - '\u06F0')
            c == '\u066B' -> sb.append('.')
            c in '\u064B'..'\u065F' || c == '\u0640' -> {}
            c == 'أ' || c == 'إ' || c == 'آ' -> sb.append('ا')
            c == 'ة' -> sb.append('ه')
            c == 'ى' -> sb.append('ي')
            else -> sb.append(c)
        }
        return sb.toString().replace(Regex("([+×÷*/()%-])"), " $1 ")
    }

    private fun lookup(t: String): Int? {
        words[t]?.let { return it }
        for (suf in listOf("مئه", "مائه"))
            if (t.endsWith(suf) && t.length > suf.length) words[t.removeSuffix(suf)]?.let { return it * 100 }
        return null
    }

    fun toExpression(raw: String): String {
        val tokens = mutableListOf<String>()
        var total = 0L; var cur = 0L; var inNum = false; var frac: StringBuilder? = null
        fun flush() {
            if (inNum) tokens.add((total + cur).toString() + (frac?.takeIf { it.isNotEmpty() }?.let { ".$it" } ?: ""))
            total = 0; cur = 0; inNum = false; frac = null
        }
        fun op(o: String) {
            flush()
            if (o == "%" || o == "(" || o == ")") { tokens.add(o); return }
            val last = tokens.lastOrNull()
            if (last != null && last in BIN) return
            if (last == null && o != "-") return
            tokens.add(o)
        }
        for (t in normalize(raw).split(Regex("\\s+"))) {
            if (t.isEmpty()) continue
                        val o = ops[t]
            if (o != null) { op(o); continue }
            if (t in point) { inNum = true; frac = StringBuilder(); continue }
            if (num.matches(t)) {
                if (frac != null && !t.contains('.')) { frac?.append(t); continue }
                flush(); tokens.add(t); continue
            }
            val key = if (t !in words && t.startsWith("و")) t.substring(1) else t
            val v = lookup(key) ?: continue
            if (frac != null) { if (v in 0..9) frac?.append(v); continue }
            inNum = true
            when (key) {
                in hundred -> cur = (if (cur == 0L) 1 else cur) * 100
                in scale -> { total += (if (cur == 0L) 1 else cur) * v; cur = 0 }
                else -> cur += v
            }
        }
        flush()
        return tokens.joinToString(" ")
    }
}
