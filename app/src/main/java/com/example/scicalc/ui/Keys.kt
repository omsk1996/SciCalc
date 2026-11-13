package com.example.scicalc.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

enum class KeyStyle { Number, Operator, Function, Equals, Clear, Toggle }

data class KeySpec(
    val label: String,
    val style: KeyStyle = KeyStyle.Function,
    val description: String = label,
    val active: Boolean = false,
    val weight: Float = 1f,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

@Composable
fun KeyGrid(
    rows: List<List<KeySpec>>,
    keyHeight: Dp,
    gap: Dp,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth().height(keyHeight),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { k -> CalcKey(k, Modifier.weight(k.weight).fillMaxHeight(), fontSize) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalcKey(spec: KeySpec, modifier: Modifier, fontSize: TextUnit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "keyScale",
    )
    val view = LocalView.current
    val shape = RoundedCornerShape(percent = 32)

    val (bg: Brush, fg: Color) = when (spec.style) {
        KeyStyle.Number -> Brush.verticalGradient(listOf(Palette.SurfaceHi, Palette.Surface)) to Palette.TextPrimary
        KeyStyle.Operator -> Brush.verticalGradient(listOf(Palette.SurfaceHi, Palette.Surface)) to Palette.Accent
        KeyStyle.Function -> Brush.verticalGradient(listOf(Palette.FnKeyHi, Palette.FnKey)) to Palette.Teal
        KeyStyle.Clear -> Brush.verticalGradient(listOf(Palette.SurfaceHi, Palette.Surface)) to Palette.Coral
        KeyStyle.Equals -> Brush.linearGradient(listOf(Palette.Accent, Palette.AccentDeep)) to Color(0xFF1A1205)
        KeyStyle.Toggle ->
            if (spec.active) Brush.linearGradient(listOf(Palette.Teal, Color(0xFF3FB8D9))) to Color(0xFF06221E)
            else Brush.verticalGradient(listOf(Palette.FnKeyHi, Palette.FnKey)) to Palette.TextSecondary
    }
    val size = when {
        spec.style == KeyStyle.Operator || spec.style == KeyStyle.Equals -> fontSize * 1.18f
        spec.label.length >= 5 -> fontSize * 0.72f
        spec.label.length >= 4 -> fontSize * 0.82f
        else -> fontSize
    }
    val weight = when (spec.style) {
        KeyStyle.Number -> FontWeight.Normal
        KeyStyle.Function -> FontWeight.Medium
        else -> FontWeight.SemiBold
    }

    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (spec.style == KeyStyle.Equals)
                    Modifier.shadow(14.dp, shape, ambientColor = Palette.AccentDeep, spotColor = Palette.AccentDeep)
                else Modifier
            )
            .clip(shape)
            .background(bg)
            .border(1.dp, Color.White.copy(alpha = if (spec.style == KeyStyle.Equals) 0.18f else 0.05f), shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(color = fg),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    spec.onClick()
                },
                onLongClick = spec.onLongClick?.let { long ->
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        long()
                    }
                },
            )
            .semantics { contentDescription = spec.description },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = spec.label,
            transitionSpec = {
                (slideInVertically(tween(180)) { it / 2 } + fadeIn(tween(180))) togetherWith
                    (slideOutVertically(tween(150)) { -it / 2 } + fadeOut(tween(150)))
            },
            label = "keyLabel",
        ) { label ->
            Text(label, color = fg, fontSize = size, fontWeight = weight, maxLines = 1)
        }
    }
}
