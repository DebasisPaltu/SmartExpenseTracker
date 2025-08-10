package com.example.smartexpensetracker.screen

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartexpensetracker.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(vm: ExpenseViewModel, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val last7Days by vm.expensesForLast7Days().collectAsState()

    val end = ExpenseViewModel.endOfToday()
    val weekStart = end - 6 * 24 * 60 * 60 * 1000
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val rangeText = "${dateFormat.format(Date(weekStart))} - ${dateFormat.format(Date(end))}"

    val dailyTotals = (0..6).map { i ->
        val dayStart = ExpenseViewModel.today() + i * 24L * 60 * 60 * 1000 - 6 * 24L * 60 * 60 * 1000
        val total = last7Days.filter { ExpenseViewModel.isSameDay(it.date, dayStart) }.sumOf { it.amount }
        dayStart to total
    }
    val weeklyTotal = dailyTotals.sumOf { it.second }
    val dailyAverage = weeklyTotal / 7.0

    val categoryTotals = last7Days.groupBy { it.category }
        .mapValues { it.value.sumOf { e -> e.amount } }
        .toList()
        .sortedByDescending { it.second }

    Column(
        Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("7-Day Expense Report", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.height(6.dp))
                Text(rangeText, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatSmallCard(
                modifier = Modifier.weight(1f),
                title = "Weekly Total",
                value = "₹" + formatMoney(weeklyTotal),
                leadingIcon = Icons.Filled.Done
            )
            StatSmallCard(
                modifier = Modifier.weight(1f),
                title = "Daily Average",
                value = "₹" + formatMoney(dailyAverage),
                leadingIcon = Icons.Filled.Done
            )
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Daily Expenses (Last 7 Days)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                LineChart(
                    data = dailyTotals.map { it.second },
                    labels = dailyTotals.map { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it.first)) },
                    height = 160.dp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (categoryTotals.isEmpty()) {
                    Text("No data available")
                } else {
                    categoryTotals.forEach { (cat, total) ->
                        val pct = if (weeklyTotal > 0) (total / weeklyTotal).toFloat() else 0f
                        CategoryRow(
                            category = cat,
                            amountText = "₹" + formatMoney(total),
                            progress = pct
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Export Report", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        Toast.makeText(context, "Export PDF (mock)", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Done, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export PDF")
                    }
                    OutlinedButton(onClick = {
                        Toast.makeText(context, "Export CSV (mock)", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Done, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export CSV")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Exports will be downloaded to your device and can be shared",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { onNavigate("entry") }, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Entry")
        }
    }
}

@Composable
private fun StatSmallCard(modifier: Modifier = Modifier, title: String, value: String, leadingIcon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LineChart(data: List<Double>, labels: List<String>, height: Dp, strokeColor: Color = MaterialTheme.colorScheme.primary) {
    val maxVal = (data.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val points = data.map { (it / maxVal).toFloat() }
    Column(Modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val w = size.width
            val h = size.height
            val stepX = if (points.size > 1) w / (points.size - 1) else w

            drawLine(color = strokeColor.copy(alpha = 0.3f), start = Offset(0f, h - 4f), end = Offset(w, h - 4f), strokeWidth = 4f)

            val path = Path()
            points.forEachIndexed { idx, p ->
                val x = stepX * idx
                val y = h - p * (h - 16f)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path = path, color = strokeColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEachIndexed { idx, lbl ->
                if (idx % 1 == 0) Text(lbl, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CategoryRow(category: String, amountText: String, progress: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            LinearProgressIndicator(progress = progress.coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(amountText, style = MaterialTheme.typography.bodyMedium)
            Text("${"%.1f".format(progress * 100)}%", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatMoney(value: Double): String {
    val nf = java.text.NumberFormat.getInstance(Locale("en", "IN"))
    return nf.format(value)
}
