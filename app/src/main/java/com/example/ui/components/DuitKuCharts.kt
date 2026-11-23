package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CurrencyOption
import com.example.data.model.WalletColor
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SlateViolet
import com.example.util.CurrencyFormatter

data class CategorySpendSlice(
    val category: String,
    val amount: Double,
    val color: Color
)

val ChartSliceColors = listOf(
    EmeraldPrimary,
    GoldSecondary,
    OceanBlue,
    ExpenseCoral,
    Color(0xFF0F766E),
    Color(0xFF7C3AED),
    SlateViolet
)

fun walletGradientBrush(color: WalletColor): Brush {
    val stops = when (color) {
        WalletColor.EMERALD -> listOf(EmeraldDeep, EmeraldPrimary, EmeraldBright)
        WalletColor.GOLD -> listOf(Color(0xFF78350F), GoldSecondary, Color(0xFFF59E0B))
        WalletColor.OCEAN -> listOf(Color(0xFF0C4A6E), OceanBlue, Color(0xFF38BDF8))
        WalletColor.SLATE -> listOf(Color(0xFF1E293B), SlateViolet, Color(0xFF64748B))
        WalletColor.CORAL -> listOf(Color(0xFF881337), ExpenseCoral, Color(0xFFFB7185))
    }
    return Brush.linearGradient(stops)
}

@Composable
fun SpendingDonutChart(
    slices: List<CategorySpendSlice>,
    currency: CurrencyOption,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.amount }.coerceAtLeast(0.01)
    val sweepFraction = remember { Animatable(0f) }

    LaunchedEffect(slices) {
        sweepFraction.snapTo(0f)
        sweepFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(196.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokePx = 24.dp.toPx()
                val arcSize = Size(size.width - strokePx, size.height - strokePx)
                val topLeft = Offset(strokePx / 2f, strokePx / 2f)

                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )

                var startAngle = -90f
                val gapDegrees = if (slices.size > 1) 4f else 0f
                slices.forEach { slice ->
                    val rawSweep = ((slice.amount / total) * 360.0).toFloat() * sweepFraction.value
                    val adjustedSweep = (rawSweep - gapDegrees).coerceAtLeast(1f)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = adjustedSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    startAngle += rawSweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.format(slices.sumOf { it.amount }, currency, hidden = hideAmounts),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            slices.forEach { slice ->
                val pct = ((slice.amount / total) * 100.0).toInt().coerceIn(1, 100)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = slice.category,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = CurrencyFormatter.format(slice.amount, currency, hidden = hideAmounts),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
