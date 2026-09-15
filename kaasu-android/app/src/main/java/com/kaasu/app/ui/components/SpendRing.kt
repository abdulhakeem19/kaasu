package com.kaasu.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaasu.app.core.util.toAmountDisplay
import com.kaasu.app.ui.theme.KaasuColors

/**
 * The spent-against-budget ring.
 *
 * Drawing happens inside a `DrawScope`, which is not composition, so every palette value is read
 * into a local first — `KaasuColors` properties are `@Composable @ReadOnlyComposable` and cannot be
 * touched from inside `Canvas`. The sparkline on the dashboard is the existing precedent.
 */
@Composable
fun SpendRing(
    spentInPaise: Long,
    budgetInPaise: Long,
    modifier: Modifier = Modifier,
    diameter: Dp = 168.dp,
    strokeWidth: Dp = 14.dp,
    label: String? = null,
) {
    // Hoisted out of DrawScope deliberately — see the class comment.
    val trackColor = KaasuColors.border
    val fillColor = KaasuColors.forest
    val overColor = KaasuColors.expense

    // A ring cannot draw less than empty, so a month whose refunds outran its purchases is clamped
    // for the drawing only. The figure printed in the middle keeps the truth.
    val hasBudget = budgetInPaise > 0
    val fraction = if (hasBudget) {
        (spentInPaise.toFloat() / budgetInPaise).coerceIn(0f, 1f)
    } else {
        0f
    }
    val isOver = hasBudget && spentInPaise > budgetInPaise
    val sweepColor = if (isOver) overColor else fillColor

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = sweepColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = spentInPaise.toAmountDisplay(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = KaasuColors.ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = when {
                    !hasBudget -> label ?: "spent"
                    isOver -> "${(spentInPaise - budgetInPaise).toAmountDisplay()} over"
                    else -> "of ${budgetInPaise.toAmountDisplay()}"
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = if (isOver) overColor else KaasuColors.muted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
