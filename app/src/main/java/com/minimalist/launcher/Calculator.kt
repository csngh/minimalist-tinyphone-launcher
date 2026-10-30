package com.minimalist.launcher

import java.math.BigDecimal
import java.math.MathContext

/**
 * A plain four-function calculator that works left to right, like a desk calculator: 2 + 3 × 4 = 20.
 * Decimal arithmetic, so 0.1 + 0.2 is 0.3. Pure Kotlin so it can be unit tested.
 */
class Calculator {
    var display = "0"
        private set
    /** The pending operation shown above the display, such as "12 ×". */
    var pending = ""
        private set
    val isError: Boolean get() = display == ERROR
    val activeOp: Char? get() = op

    private var acc: BigDecimal? = null
    private var op: Char? = null
    private var fresh = true

    fun digit(d: Char) {
        if (isError) clear()
        if (fresh) { display = d.toString(); fresh = false; return }
        if (display == "0") { display = d.toString(); return }
        if (display.count { it.isDigit() } < MAX_DIGITS) display += d
    }

    fun dot() {
        if (isError) clear()
        if (fresh) { display = "0."; fresh = false; return }
        if ('.' !in display) display += "."
    }

    fun operator(o: Char) {
        if (isError) return
        val cur = value()
        acc = if (acc != null && op != null && !fresh) apply(acc!!, op!!, cur) ?: return fail() else cur
        op = o
        fresh = true
        display = format(acc!!)
        pending = "$display $o"
    }

    fun equals() {
        if (isError) return
        val a = acc
        val o = op
        if (a == null || o == null) return
        val r = apply(a, o, value()) ?: return fail()
        display = format(r)
        pending = ""
        acc = null; op = null; fresh = true
    }

    fun negate() {
        if (isError || display == "0") return
        display = if (display.startsWith("-")) display.drop(1) else "-$display"
    }

    fun percent() {
        if (isError) return
        display = format(value().divide(HUNDRED))
        fresh = true
    }

    fun backspace() {
        if (isError) { clear(); return }
        if (fresh) return
        display = display.dropLast(1).let { if (it.isEmpty() || it == "-") "0" else it }
    }

    fun clear() {
        display = "0"; pending = ""; acc = null; op = null; fresh = true
    }

    private fun value(): BigDecimal = display.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun fail() { display = ERROR; pending = ""; acc = null; op = null; fresh = true }

    private fun apply(a: BigDecimal, o: Char, b: BigDecimal): BigDecimal? = when (o) {
        '+' -> a + b
        '−' -> a - b
        '×' -> a * b
        '÷' -> if (b.signum() == 0) null else a.divide(b, MathContext(12))
        else -> b
    }

    private fun format(v: BigDecimal): String {
        val r = v.round(MathContext(12)).stripTrailingZeros()
        // Stripping zeros turns 20 into 2E+1; show it as 20 unless the number really is huge.
        val plain = if (r.scale() < 0) r.setScale(0) else r
        val s = if (plain.precision() - plain.scale() > 15) r.toString() else plain.toPlainString()
        return if (s == "-0") "0" else s
    }

    companion object {
        const val ERROR = "Error"
        private const val MAX_DIGITS = 12
        private val HUNDRED = BigDecimal(100)
    }
}
