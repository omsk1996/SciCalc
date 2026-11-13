package com.example.scicalc

import android.app.Application
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

data class HistoryItem(val expression: String, val result: String, val value: Double)

class CalculatorViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("scicalc", Context.MODE_PRIVATE)

    var tokens by mutableStateOf<List<String>>(emptyList()); private set
    var angleMode by mutableStateOf(
        runCatching { AngleMode.valueOf(prefs.getString("angle", "DEG")!!) }.getOrDefault(AngleMode.DEG)
    ); private set
    var secondMode by mutableStateOf(false); private set
    var justEvaluated by mutableStateOf(false); private set
    var lastExpression by mutableStateOf(""); private set
    var error by mutableStateOf<String?>(null); private set
    var errorTick by mutableIntStateOf(0); private set
    val history = mutableStateListOf<HistoryItem>()
    private var ans = prefs.getString("ans", "0")!!.toDoubleOrNull() ?: 0.0

    /** Live result shown while typing; ignores a dangling operator at the end. */
    val preview: String? by derivedStateOf {
        val t = tokens
        if (justEvaluated || t.isEmpty()) return@derivedStateOf null
        var trimmed = t
        while (trimmed.isNotEmpty() &&
            (trimmed.last() in CalcEngine.BINARY || trimmed.last() == "(" || trimmed.last() in CalcEngine.FUNCTIONS)
        ) trimmed = trimmed.dropLast(1)
        if (trimmed.isEmpty() || (trimmed.size == 1 && CalcEngine.isNumber(trimmed[0]))) return@derivedStateOf null
        runCatching { CalcEngine.format(CalcEngine.evaluate(trimmed, angleMode, ans)) }.getOrNull()
    }

    init { loadHistory() }

    fun input(tok: String) {
        error = null
        var cur = tokens
        if (justEvaluated) {
            justEvaluated = false
            if (tok !in CalcEngine.BINARY && tok !in CalcEngine.POSTFIX) cur = emptyList()
        }
        if (cur.size >= 150) return
        val last = cur.lastOrNull()
        val afterOpener = last == "(" || last in CalcEngine.FUNCTIONS
        tokens = when {
            tok.length == 1 && (tok[0].isDigit() || tok == ".") -> appendDigit(cur, tok)
            tok == "−" -> if (last == "−" || last == "+") cur.dropLast(1) + tok else cur + tok
            tok in CalcEngine.BINARY -> when {
                cur.isEmpty() -> if (history.isNotEmpty()) listOf("Ans", tok) else cur
                afterOpener -> cur
                last in CalcEngine.BINARY -> {
                    val base = cur.dropLastWhile { it in CalcEngine.BINARY }
                    if (base.isEmpty() || base.last() == "(" || base.last() in CalcEngine.FUNCTIONS) cur else base + tok
                }
                else -> cur + tok
            }
            tok in CalcEngine.POSTFIX -> when {
                cur.isEmpty() -> if (history.isNotEmpty()) listOf("Ans", tok) else cur
                last in CalcEngine.BINARY || afterOpener -> cur
                else -> cur + tok
            }
            tok == ")" ->
                if (CalcEngine.openParens(cur) > 0 && last !in CalcEngine.BINARY && !afterOpener) cur + tok else cur
            else -> cur + tok
        }
    }

    /** Insert a ready-made token sequence such as e^( or 10^(. */
    fun insert(seq: List<String>) {
        error = null
        if (justEvaluated) { justEvaluated = false; tokens = emptyList() }
        tokens = tokens + seq
    }

    private fun appendDigit(cur: List<String>, d: String): List<String> {
        val last = cur.lastOrNull()
        if (CalcEngine.isNumber(last)) {
            last!!
            return when {
                d == "." && last.contains('.') -> cur
                last == "0" && d != "." -> cur.dropLast(1) + d
                last.count { it.isDigit() } >= 15 -> cur
                else -> cur.dropLast(1) + (last + d)
            }
        }
        return cur + (if (d == ".") "0." else d)
    }

    fun toggleSign() {
        error = null
        justEvaluated = false
        val cur = tokens.toMutableList()
        val i = cur.lastIndex
        if (i >= 0 && CalcEngine.isNumber(cur[i])) {
            val prev = cur.getOrNull(i - 1)
            val prev2 = cur.getOrNull(i - 2)
            when {
                prev == "−" && prev2 == "(" -> { cur.removeAt(i - 1); cur.removeAt(i - 2) }
                prev == "−" && (prev2 == null || prev2 in CalcEngine.BINARY || prev2 in CalcEngine.FUNCTIONS) ->
                    cur.removeAt(i - 1)
                else -> { cur.add(i, "−"); cur.add(i, "(") }
            }
        } else {
            cur += listOf("(", "−")
        }
        tokens = cur
    }

    fun backspace() {
        error = null
        if (justEvaluated) { clear(); return }
        val last = tokens.lastOrNull() ?: return
        tokens = if (CalcEngine.isNumber(last) && last.length > 1) {
            val shorter = last.dropLast(1)
            if (shorter == "0") tokens.dropLast(1) else tokens.dropLast(1) + shorter
        } else tokens.dropLast(1)
    }

    fun clear() {
        tokens = emptyList()
        error = null
        justEvaluated = false
        lastExpression = ""
    }

    fun equals() {
        if (tokens.isEmpty() || justEvaluated) return
        val full = tokens + List(CalcEngine.openParens(tokens).coerceAtLeast(0)) { ")" }
        try {
            val v = CalcEngine.evaluate(full, angleMode, ans)
            val expr = CalcEngine.plainText(full)
            history.add(0, HistoryItem(expr, CalcEngine.format(v), v))
            while (history.size > 100) history.removeAt(history.lastIndex)
            ans = v
            lastExpression = expr
            tokens = CalcEngine.resultToTokens(v)
            justEvaluated = true
            error = null
            save()
        } catch (e: CalcException) {
            error = e.message
            errorTick++
        }
    }

    fun toggleSecond() { secondMode = !secondMode }

    fun toggleAngle() {
        angleMode = if (angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
        prefs.edit().putString("angle", angleMode.name).apply()
    }

    fun useHistory(item: HistoryItem) {
        error = null
        val resTokens = CalcEngine.resultToTokens(item.value)
        val last = tokens.lastOrNull()
        tokens = if (justEvaluated || tokens.isEmpty()) resTokens
        else if (last in CalcEngine.BINARY || last == "(" || last in CalcEngine.FUNCTIONS) tokens + resTokens
        else tokens + "×" + resTokens
        justEvaluated = false
    }

    fun clearHistory() {
        history.clear()
        save()
    }

    private fun save() {
        val data = history.joinToString("\n") { "${it.expression}\u001F${it.result}\u001F${it.value}" }
        prefs.edit().putString("history", data).putString("ans", ans.toString()).apply()
    }

    private fun loadHistory() {
        val data = prefs.getString("history", null) ?: return
        data.split("\n").forEach { line ->
            val p = line.split("\u001F")
            if (p.size == 3) p[2].toDoubleOrNull()?.let { history += HistoryItem(p[0], p[1], it) }
        }
    }
}
