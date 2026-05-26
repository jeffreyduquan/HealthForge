package de.healthforge.presentation.log

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogChartsScreen(
    onBack: () -> Unit,
    vm: LogChartsViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30).forEach { d ->
                    FilterChip(
                        selected = s.rangeDays == d,
                        onClick = { vm.setRange(d) },
                        label = { Text("$d Tage") },
                    )
                }
            }
            if (s.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ChartCard(
                    title = "Symptom-Severity (Ø 1–5)",
                    values = s.data.map { it.severityAvg },
                    labels = s.data.map { it.date.format(DateTimeFormatter.ofPattern("dd.MM")) },
                    yMin = 1f,
                    yMax = 5f,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                ChartCard(
                    title = "Einträge pro Tag",
                    values = s.data.map { it.entryCount.toDouble() },
                    labels = s.data.map { it.date.format(DateTimeFormatter.ofPattern("dd.MM")) },
                    yMin = 0f,
                    yMax = (s.data.maxOfOrNull { it.entryCount }?.toFloat()?.coerceAtLeast(1f) ?: 1f),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    values: List<Double?>,
    labels: List<String>,
    yMin: Float,
    yMax: Float,
    color: Color,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            val hasData = values.any { it != null }
            if (!hasData) {
                Text(
                    "Keine Daten im Zeitraum.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                ) {
                    val w = size.width
                    val h = size.height
                    val n = values.size
                    if (n < 2) return@Canvas
                    val stepX = w / (n - 1)
                    val range = (yMax - yMin).coerceAtLeast(0.0001f)

                    // baseline (yMin)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, h),
                        end = Offset(w, h),
                        strokeWidth = 2f,
                    )

                    val path = Path()
                    var started = false
                    values.forEachIndexed { i, v ->
                        if (v == null) {
                            started = false
                            return@forEachIndexed
                        }
                        val x = stepX * i
                        val y = h - ((v.toFloat() - yMin) / range) * h
                        if (!started) {
                            path.moveTo(x, y)
                            started = true
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    drawPath(path = path, color = color, style = Stroke(width = 4f))

                    // points
                    values.forEachIndexed { i, v ->
                        if (v != null) {
                            val x = stepX * i
                            val y = h - ((v.toFloat() - yMin) / range) * h
                            drawCircle(color = color, radius = 6f, center = Offset(x, y))
                        }
                    }
                }
                // X-axis labels (compressed to show first/last/middle if many)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (labels.isNotEmpty()) {
                        Text(labels.first(), style = MaterialTheme.typography.labelSmall)
                        if (labels.size > 2) {
                            Text(labels[labels.size / 2], style = MaterialTheme.typography.labelSmall)
                        }
                        Text(labels.last(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
