package de.healthforge.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.healthforge.presentation.theme.LocalHmTokens
import de.healthforge.presentation.theme.StatusOverUl
import kotlin.math.roundToInt

/**
 * P7.S3a v2 / REQ-HOME-WATER-BAR-001 — Wasser-Zeile in der PinnedNutrientCard.
 *
 * Optisch wie ein normaler Pinned-Nährstoff (Label / Wert/Ziel / Prozent /
 * gefüllte Bar), aber die Bar IST ein Slider mit Stufen-Logik + Ghost-Marker.
 *
 * ### Stufen-Modell
 * - Eine **Stufe** entspricht 1×Tagesziel. Stufe 0 = `0..goal`, Stufe N =
 *   `N*goal..(N+1)*goal`. Stufen sind endlos.
 * - Bar zeigt immer 0–100 % der **aktuell angezeigten Stufe**.
 * - **Stufen 0..9** haben je ein eigenes Farb-Paar aus der Histamind-Palette
 *   (siehe [waterStageGradient]). Ab Stufe 10+ bleibt Stufe-9-Farbe.
 *
 * ### Drag-Logik (in-drag + on-release stage transitions)
 * Material3-Slider clampt seinen `value` an die `valueRange`. Wir verwalten
 * daher die Stufe lokal als `displayedStage` zusätzlich zum relativen Wert
 * 0..goal innerhalb der Stufe:
 *
 * - **In-Drag**: erreicht der Drag-Wert das obere Ende (`relativeMl == goal`)
 *   von einem niedrigeren Wert kommend, wird `displayedStage++` sofort
 *   inkrementiert und `relativeMl = 0` → die Bar zeigt sofort die nächste
 *   Stufe in neuer Farbe. Symmetrisch für das untere Ende (Stage-Down).
 *
 * - **On-Release**: wenn der finale Wert genau auf einer Stufengrenze liegt
 *   (oben oder unten), rückt der lokale State nochmals um eine Stufe vor (oben)
 *   bzw. zurück (unten), sodass der nächste Drag direkt in der neuen Stufe
 *   beginnt.
 *
 * ### Commit
 * Beim Loslassen wird `displayedStage * goal + relativeMl` über [onCommit]
 * persistiert (Day-Aggregate via `WaterIntakeRepository.setDayTotal`).
 *
 * ### Ghost-Soll
 * Wenn `ghostMl` im sichtbaren Bereich der angezeigten Stufe liegt
 * (`displayedStage*goal..(displayedStage+1)*goal`), wird ein feiner weißer
 * vertikaler Strich an dieser Position gezeichnet.
 *
 * @param currentMl persistierter Tageskonsum.
 * @param ghostMl   lineares Tages-Soll bis jetzt (für visuellen Marker).
 * @param goalMl    Tagesziel = Größe einer Stufe.
 * @param reminderEnabled Toggle-State für die Defizit-Erinnerung.
 * @param onCommit  wird beim Loslassen mit dem neuen absoluten Wert gerufen.
 * @param onToggleReminder Toggle-Callback für die Reminder-Bell.
 */
@Composable
fun WaterStageSlider(
    currentMl: Int,
    ghostMl: Int,
    goalMl: Int,
    reminderEnabled: Boolean,
    onCommit: (Int) -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hm = LocalHmTokens.current
    val safeGoal = goalMl.coerceAtLeast(1)

    // ─── Initialen (displayedStage, relativeMl) aus currentMl ableiten ───
    // currentMl = N*goal: zeige neue Stufe N als 0% (empty). User kann durch
    // Drag-Through-Zero zurück in Stufe N-1 (kommt dann automatisch bei 100%
    // dieser Stufe an, sodass weiter-runter direkt möglich ist).
    val initialStage: Int = if (currentMl <= 0) 0 else currentMl / safeGoal
    val initialRelative: Int = if (currentMl <= 0) 0 else currentMl - initialStage * safeGoal

    var displayedStage by remember(safeGoal) { mutableIntStateOf(initialStage) }
    var relativeMl by remember(safeGoal) { mutableIntStateOf(initialRelative) }
    // Touch-Reset-Strategie für Stufenwechsel: nach jedem In-Drag Stage-Up/Down
    // wird der Slider per `key(sliderResetKey)` remounted → die aktive
    // Drag-Geste wird abgebrochen. Der User MUSS den Finger heben und neu
    // tippen, um eine weitere Stufe zu wechseln. Das ist die explizite
    // User-Direktive: "sobald eine stufe hoch oder runter geht, muss der touch
    // disconnected werden und nicht mehr von der app erkannt werden, bis ein
    // neues touch event kommt."
    var sliderResetKey by remember(safeGoal) { mutableIntStateOf(0) }

    // Resync NUR wenn der lokale (displayedStage, relativeMl) nicht mehr
    // mit currentMl konsistent ist (z.B. nach externem Reset, Tageswechsel,
    // Daten-Reload). Im Normalfall haben wir lokal das User-Intent (z.B.
    // "Bar gerade in neue Stufe vorgerückt") UND brauchen daher nicht jedes
    // Mal auf `boundary == vorige Stufe voll` zurückzufallen, wenn der User
    // einfach nur das Tagesziel exakt erreicht hat.
    LaunchedEffect(currentMl, safeGoal) {
        val localAbsolute = displayedStage * safeGoal + relativeMl
        if (localAbsolute != currentMl) {
            displayedStage = initialStage
            relativeMl = initialRelative
        }
    }

    val absoluteDrag = displayedStage * safeGoal + relativeMl
    val frac = relativeMl.toFloat() / safeGoal.toFloat()
    val percent = (frac * 100).roundToInt().coerceIn(0, 100)

    val gradient = remember(displayedStage) { waterStageGradient(displayedStage) }
    val accent = remember(displayedStage) { waterStageAccent(displayedStage) }

    // Ghost-Marker-Position innerhalb der angezeigten Stufe (oder null).
    val ghostInStage: Float? = run {
        val stageStart = displayedStage * safeGoal
        val stageEnd = stageStart + safeGoal
        if (ghostMl in stageStart..stageEnd) {
            ((ghostMl - stageStart).toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
        } else null
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Wasser",
                style = MaterialTheme.typography.bodyMedium,
                color = hm.fgPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (displayedStage >= 1) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "×${displayedStage + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "$absoluteDrag / $safeGoal ml",
                style = MaterialTheme.typography.bodySmall,
                color = hm.fgSecondary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.labelSmall,
                color = hm.fgTertiary,
            )
            Spacer(Modifier.width(4.dp))
            IconToggleButton(
                checked = reminderEnabled,
                onCheckedChange = onToggleReminder,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = if (reminderEnabled) Icons.Filled.Notifications
                                  else Icons.Outlined.NotificationsNone,
                    contentDescription = "Wasser-Erinnerung",
                    tint = if (reminderEnabled) accent else hm.fgTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Bar (Canvas) + Slider (overlay) ──
        Box(modifier = Modifier.fillMaxWidth()) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.Center),
            ) {
                val w = size.width
                val h = size.height
                val corner = CornerRadius(h / 2f, h / 2f)
                // Track-Farbe: Akzent der Vorgängerstufe × 0.25 Alpha — zeigt
                // "wo komme ich her". Stufe 0 → neutraler `hm.barTrack`.
                val trackColor = waterStageTrackColor(displayedStage) ?: hm.barTrack
                drawRoundRect(color = trackColor, size = Size(w, h), cornerRadius = corner)
                // Defizit-Rot: zwischen aktueller Füllung und Ghost-Soll, wenn
                // Soll vor uns liegt (current < ghost) UND beide im selben
                // sichtbaren Stufen-Bereich.
                if (ghostInStage != null && frac < ghostInStage) {
                    val redStart = w * frac
                    val redEnd = w * ghostInStage
                    drawRoundRect(
                        color = StatusOverUl.copy(alpha = 0.55f),
                        topLeft = Offset(redStart, 0f),
                        size = Size((redEnd - redStart).coerceAtLeast(0f), h),
                        cornerRadius = corner,
                    )
                }
                val fillW = (w * frac).coerceIn(0f, w)
                if (fillW > 0f) {
                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(0f, 0f),
                        size = Size(fillW, h),
                        cornerRadius = corner,
                    )
                }
                ghostInStage?.let { gf ->
                    val gx = w * gf
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(gx, 0f),
                        end = Offset(gx, h),
                        strokeWidth = 2f,
                    )
                }
            }

            key(sliderResetKey) {
                Slider(
                    value = relativeMl.toFloat(),
                    onValueChange = { v ->
                        val snapped = (v / 50f).roundToInt() * 50
                        val target = snapped.coerceIn(0, safeGoal)
                        val prev = relativeMl
                        when {
                            // Stage-Up: max-Treffer → transitionieren UND Slider
                            // resetten (cancelt aktive Geste; User muss neu tippen).
                            target == safeGoal && prev < safeGoal -> {
                                displayedStage += 1
                                relativeMl = 0
                                // Commit sofort, damit der Persistenz-Stand stimmt.
                                val newAbs = displayedStage * safeGoal
                                if (newAbs != currentMl) onCommit(newAbs)
                                sliderResetKey += 1
                            }
                            // Stage-Down: 0-Treffer in Stufe>0 → transitionieren + reset.
                            target == 0 && prev > 0 && displayedStage > 0 -> {
                                displayedStage -= 1
                                relativeMl = safeGoal
                                val newAbs = displayedStage * safeGoal + safeGoal
                                if (newAbs != currentMl) onCommit(newAbs)
                                sliderResetKey += 1
                            }
                            else -> {
                                relativeMl = target
                            }
                        }
                    },
                    onValueChangeFinished = {
                        val newAbsolute = displayedStage * safeGoal + relativeMl
                        if (newAbsolute != currentMl) onCommit(newAbsolute)
                    },
                    valueRange = 0f..safeGoal.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                )
            }
        }
    }
}
