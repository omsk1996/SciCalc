package com.example.scicalc.ui

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.scicalc.AngleMode
import com.example.scicalc.CalcEngine
import com.example.scicalc.CalculatorViewModel
import com.example.scicalc.HistoryItem
import com.example.scicalc.TokenKind
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun CalculatorScreen(vm: CalculatorViewModel) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var sciExpanded by rememberSaveable { mutableStateOf(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.BgTop, Palette.BgBottom)))
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val landscape = maxWidth > maxHeight
            if (landscape) {
                val gap = 8.dp
                val keyH = (maxHeight - gap * 4) / 5
                val sciKeyH = (maxHeight - gap * 3) / 4
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1.1f).fillMaxHeight()) {
                        TopBar(vm, onHistory = { showHistory = true })
                        Spacer(Modifier.height(8.dp))
                        DisplayPanel(vm, Modifier.weight(1f).fillMaxWidth())
                    }
                    ScientificPad(
                        vm, keyHeight = sciKeyH, gap = gap,
                        modifier = Modifier.weight(1.25f),
                    )
                    MainPad(vm, keyHeight = keyH, gap = gap, modifier = Modifier.weight(1f))
                }
            } else {
                val gap = 10.dp
                val sciGap = 8.dp
                val keyW = (maxWidth - gap * 3) / 4
                val keyH = min(keyW * 0.84f, maxHeight * 0.085f)
                val sciW = (maxWidth - sciGap * 4) / 5
                val sciH = min(sciW * 0.68f, maxHeight * 0.055f)
                Column(Modifier.fillMaxSize()) {
                    TopBar(vm, onHistory = { showHistory = true })
                    Spacer(Modifier.height(8.dp))
                    DisplayPanel(vm, Modifier.weight(1f).fillMaxWidth())
                    PanelHandle(expanded = sciExpanded, onToggle = { sciExpanded = it })
                    AnimatedVisibility(
                        visible = sciExpanded,
                        enter = expandVertically(spring(stiffness = 500f)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut(),
                    ) {
                        ScientificPad(
                            vm, keyHeight = sciH, gap = sciGap,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    MainPad(vm, keyHeight = keyH, gap = gap)
                }
            }
        }

        if (showHistory) HistorySheet(vm, onDismiss = { showHistory = false })
    }
}

/* ---------------------------------------------------------------- Top bar */

@Composable
private fun TopBar(vm: CalculatorViewModel, onHistory: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "SciCalc",
            style = TextStyle(
                brush = Brush.linearGradient(listOf(Palette.Accent, Palette.AccentDeep)),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
        )
        Spacer(Modifier.weight(1f))
        AngleToggle(vm.angleMode, onToggle = vm::toggleAngle)
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onHistory, modifier = Modifier.semantics { contentDescription = "History" }) {
            HistoryGlyph(Palette.TextSecondary, Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AngleToggle(mode: AngleMode, onToggle: () -> Unit) {
    val itemW = 50.dp
    val view = LocalView.current
    val offset by animateDpAsState(
        if (mode == AngleMode.DEG) 0.dp else itemW,
        spring(dampingRatio = 0.7f, stiffness = 500f), label = "angleOffset",
    )
    Box(
        Modifier
            .clip(CircleShape)
            .background(Palette.FnKey)
            .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onToggle()
            }
            .semantics { contentDescription = "Angle unit: ${if (mode == AngleMode.DEG) "degrees" else "radians"}. Tap to switch." }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(itemW, 30.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Palette.Teal, Color(0xFF3FB8D9))))
        )
        Row {
            AngleMode.entries.forEach { m ->
                val color by animateColorAsState(
                    if (m == mode) Color(0xFF06221E) else Palette.TextSecondary, label = "angleText",
                )
                Box(Modifier.size(itemW, 30.dp), contentAlignment = Alignment.Center) {
                    Text(m.name, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HistoryGlyph(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val s = 2.dp.toPx()
        drawArc(
            color, startAngle = 150f, sweepAngle = 300f, useCenter = false,
            topLeft = Offset(s, s), size = Size(size.width - 2 * s, size.height - 2 * s),
            style = Stroke(s, cap = StrokeCap.Round),
        )
        // little arrow head at arc start
        val r = (size.width - 2 * s) / 2
        val start = Offset(center.x - r * 0.866f, center.y + r * 0.5f)
        drawLine(color, start, start + Offset(-s * 1.6f, -s * 1.6f), s, StrokeCap.Round)
        drawLine(color, start, start + Offset(s * 2f, -s * 0.6f), s, StrokeCap.Round)
        drawLine(color, center, Offset(center.x, center.y - r * 0.55f), s, StrokeCap.Round)
        drawLine(color, center, Offset(center.x + r * 0.4f, center.y + r * 0.25f), s, StrokeCap.Round)
    }
}

/* ---------------------------------------------------------------- Display */

private data class DisplayState(val text: AnnotatedString, val evaluated: Boolean)

@Composable
private fun DisplayPanel(vm: CalculatorViewModel, modifier: Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val view = LocalView.current
    val density = LocalDensity.current
    val shake = remember { Animatable(0f) }

    LaunchedEffect(vm.errorTick) {
        if (vm.errorTick > 0) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            for (x in listOf(18f, -14f, 10f, -6f, 3f, 0f)) shake.animateTo(x, tween(45))
        }
    }

    val annotated = buildAnnotatedString {
        if (vm.tokens.isEmpty()) {
            withStyle(SpanStyle(color = Palette.TextDim)) { append("0") }
        } else {
            CalcEngine.render(vm.tokens).forEach { (text, kind) ->
                val color = when {
                    vm.justEvaluated -> Palette.TextPrimary
                    kind == TokenKind.OPERATOR -> Palette.Accent
                    kind == TokenKind.FUNCTION -> Palette.Teal
                    kind == TokenKind.CONSTANT -> Color(0xFFB9A6FF)
                    kind == TokenKind.PAREN -> Palette.TextSecondary
                    else -> Palette.TextPrimary
                }
                withStyle(SpanStyle(color = color)) { append(text) }
            }
            val ghost = CalcEngine.openParens(vm.tokens)
            if (ghost > 0 && !vm.justEvaluated) {
                withStyle(SpanStyle(color = Palette.TextDim)) { append(")".repeat(ghost)) }
            }
        }
    }
    val len = annotated.length
    val targetSize = when {
        len <= 8 -> 60f
        len <= 11 -> 50f
        len <= 15 -> 42f
        len <= 20 -> 34f
        else -> 30f
    }
    val fontSize by animateFloatAsState(targetSize, spring(stiffness = 400f), label = "exprSize")
    val scroll = rememberScrollState()
    LaunchedEffect(annotated) {
        withFrameNanos { }
        scroll.animateScrollTo(scroll.maxValue)
    }

    Column(
        modifier
            .graphicsLayer { translationX = shake.value * density.density }
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.015f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(28.dp))
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (abs(total) > 60.dp.toPx()) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            vm.backspace()
                        }
                    },
                ) { _, dx -> total += dx }
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = {
                    val text = if (vm.justEvaluated) vm.history.firstOrNull()?.result
                    else vm.preview ?: CalcEngine.plainText(vm.tokens)
                    if (!text.isNullOrBlank()) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        clipboard.setText(AnnotatedString(text.replace(",", "").replace("−", "-")))
                        Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Small "previous expression" line after pressing =
        AnimatedVisibility(
            visible = vm.justEvaluated,
            enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { it },
            exit = fadeOut(tween(150)),
        ) {
            Text(
                vm.lastExpression + " =",
                color = Palette.TextSecondary,
                fontSize = 20.sp,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(4.dp))

        AnimatedContent(
            targetState = DisplayState(annotated, vm.justEvaluated),
            contentKey = { it.evaluated },
            transitionSpec = {
                if (targetState.evaluated) {
                    (slideInVertically(spring(dampingRatio = 0.75f, stiffness = 380f)) { it } + fadeIn()) togetherWith
                        (slideOutVertically(tween(200)) { -it } + fadeOut(tween(150)))
                } else {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(90))
                }
            },
            contentAlignment = Alignment.BottomEnd,
            label = "mainDisplay",
        ) { state ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    state.text,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.horizontalScroll(scroll),
                )
            }
        }

        // Live preview or error message
        Box(Modifier.fillMaxWidth().height(38.dp), contentAlignment = Alignment.CenterEnd) {
            val line = vm.error ?: vm.preview?.let { "= $it" } ?: ""
            val isError = vm.error != null
            AnimatedContent(
                targetState = line,
                transitionSpec = { fadeIn(tween(140)) togetherWith fadeOut(tween(100)) },
                contentAlignment = Alignment.CenterEnd,
                label = "preview",
            ) { text ->
                Text(
                    text,
                    color = if (isError) Palette.Coral else Palette.TextSecondary,
                    fontSize = 24.sp,
                    fontWeight = if (isError) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/* ---------------------------------------------------------------- Panels */

@Composable
private fun PanelHandle(expanded: Boolean, onToggle: (Boolean) -> Unit) {
    val rotation by animateFloatAsState(if (expanded) 0f else 180f, label = "chevron")
    val view = LocalView.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onToggle(!expanded)
            }
            .pointerInput(expanded) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total > 24.dp.toPx() && expanded) onToggle(false)
                        if (total < -24.dp.toPx() && !expanded) onToggle(true)
                    },
                ) { _, dy -> total += dy }
            }
            .semantics { contentDescription = if (expanded) "Hide scientific keys" else "Show scientific keys" },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp, 4.dp).clip(CircleShape).background(Palette.TextDim))
        Spacer(Modifier.width(10.dp))
        Text(
            if (expanded) "Scientific" else "Show scientific",
            color = Palette.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
        )
        Icon(
            Icons.Filled.KeyboardArrowDown, contentDescription = null,
            tint = Palette.TextSecondary,
            modifier = Modifier.size(18.dp).rotate(rotation),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.size(28.dp, 4.dp).clip(CircleShape).background(Palette.TextDim))
    }
}

@Composable
private fun ScientificPad(vm: CalculatorViewModel, keyHeight: Dp, gap: Dp, modifier: Modifier = Modifier) {
    val s = vm.secondMode
    fun fn(normal: String, inverse: String, nDesc: String, iDesc: String, nTok: String, iTok: String) =
        KeySpec(if (s) inverse else normal, description = if (s) iDesc else nDesc) { vm.input(if (s) iTok else nTok) }

    val rows = listOf(
        listOf(
            KeySpec("2nd", KeyStyle.Toggle, "Second functions", active = s) { vm.toggleSecond() },
            fn("sin", "sin⁻¹", "sine", "arc sine", "sin(", "sin⁻¹("),
            fn("cos", "cos⁻¹", "cosine", "arc cosine", "cos(", "cos⁻¹("),
            fn("tan", "tan⁻¹", "tangent", "arc tangent", "tan(", "tan⁻¹("),
            KeySpec("π", description = "pi") { vm.input("π") },
        ),
        listOf(
            fn("x²", "x³", "square", "cube", "²", "³"),
            fn("√", "∛", "square root", "cube root", "√(", "∛("),
            if (s) KeySpec("eˣ", description = "e to the power") { vm.insert(listOf("e", "^", "(")) }
            else KeySpec("ln", description = "natural log") { vm.input("ln(") },
            if (s) KeySpec("10ˣ", description = "ten to the power") { vm.insert(listOf("10", "^", "(")) }
            else KeySpec("log", description = "log base 10") { vm.input("log(") },
            KeySpec("e", description = "Euler's number") { vm.input("e") },
        ),
        listOf(
            KeySpec("xʸ", description = "power") { vm.input("^") },
            fn("1/x", "|x|", "reciprocal", "absolute value", "⁻¹", "abs("),
            KeySpec("x!", description = "factorial") { vm.input("!") },
            KeySpec("%", description = "percent") { vm.input("%") },
            KeySpec("EXP", description = "times ten to the power") { vm.input("E") },
        ),
        listOf(
            fn("sinh", "sinh⁻¹", "hyperbolic sine", "inverse hyperbolic sine", "sinh(", "sinh⁻¹("),
            fn("cosh", "cosh⁻¹", "hyperbolic cosine", "inverse hyperbolic cosine", "cosh(", "cosh⁻¹("),
            fn("tanh", "tanh⁻¹", "hyperbolic tangent", "inverse hyperbolic tangent", "tanh(", "tanh⁻¹("),
            KeySpec("±", description = "toggle sign") { vm.toggleSign() },
            KeySpec("Ans", description = "previous answer") { vm.input("Ans") },
        ),
    )
    val fontSize = (keyHeight.value * 0.42f).coerceIn(13f, 20f).sp
    KeyGrid(rows, keyHeight, gap, fontSize, modifier)
}

@Composable
private fun MainPad(vm: CalculatorViewModel, keyHeight: Dp, gap: Dp, modifier: Modifier = Modifier) {
    fun num(d: String) = KeySpec(d, KeyStyle.Number) { vm.input(d) }
    fun op(sym: String, desc: String) = KeySpec(sym, KeyStyle.Operator, desc) { vm.input(sym) }
    val rows = listOf(
        listOf(
            KeySpec("AC", KeyStyle.Clear, "All clear") { vm.clear() },
            KeySpec("(", KeyStyle.Function, "open bracket") { vm.input("(") },
            KeySpec(")", KeyStyle.Function, "close bracket") { vm.input(")") },
            op("÷", "divide"),
        ),
        listOf(num("7"), num("8"), num("9"), op("×", "multiply")),
        listOf(num("4"), num("5"), num("6"), op("−", "minus")),
        listOf(num("1"), num("2"), num("3"), op("+", "plus")),
        listOf(
            num("0"),
            KeySpec(".", KeyStyle.Number, "decimal point") { vm.input(".") },
            KeySpec("⌫", KeyStyle.Number, "Backspace. Long press to clear", onLongClick = { vm.clear() }) { vm.backspace() },
            KeySpec("=", KeyStyle.Equals, "equals") { vm.equals() },
        ),
    )
    val fontSize = (keyHeight.value * 0.4f).coerceIn(18f, 30f).sp
    KeyGrid(rows, keyHeight, gap, fontSize, modifier)
}

/* ---------------------------------------------------------------- History */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(vm: CalculatorViewModel, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.Sheet,
        scrimColor = Color.Black.copy(alpha = 0.55f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("History", color = Palette.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (vm.history.isNotEmpty()) {
                TextButton(onClick = vm::clearHistory) {
                    Icon(Icons.Filled.Delete, null, tint = Palette.Coral, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear", color = Palette.Coral)
                }
            }
        }
        if (vm.history.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().height(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HistoryGlyph(Palette.TextDim, Modifier.size(44.dp))
                Spacer(Modifier.height(14.dp))
                Text("No calculations yet", color = Palette.TextSecondary, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text("Your results will show up here", color = Palette.TextDim, fontSize = 13.sp)
            }
        } else {
            Text(
                "Tap a result to use it",
                color = Palette.TextDim, fontSize = 12.sp,
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp),
            )
            LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = 520.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(vm.history) { item ->
                    HistoryRow(item) {
                        vm.useHistory(item)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    }
                }
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            item.expression, color = Palette.TextSecondary, fontSize = 15.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
        )
        Text(
            "= ${item.result}", color = Palette.TextPrimary, fontSize = 26.sp,
            fontWeight = FontWeight.Light, maxLines = 1,
        )
    }
}
