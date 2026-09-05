package com.ridecopilot.app.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridecopilot.app.domain.ProfitabilityLevel
import com.ridecopilot.app.domain.ProfitabilityResult
import com.ridecopilot.app.domain.RideOffer
import com.ridecopilot.app.domain.TrafficEstimate
import kotlinx.coroutines.flow.StateFlow

data class OverlayUiState(
    val loading: Boolean = false,
    val offer: RideOffer? = null,
    val traffic: TrafficEstimate? = null,
    val profitability: ProfitabilityResult? = null,
    val errorMessage: String? = null
)

@Composable
fun OverlayCard(stateFlow: StateFlow<OverlayUiState>, onDismiss: () -> Unit) {
    val state by stateFlow.collectAsState()
    var expanded by remember { mutableStateOf(true) }

    val levelColor = when (state.profitability?.level) {
        ProfitabilityLevel.GOOD -> Color(0xFF2E7D32)
        ProfitabilityLevel.OK -> Color(0xFFF9A825)
        ProfitabilityLevel.BAD -> Color(0xFFC62828)
        else -> Color(0xFF546E7A)
    }

    if (!expanded) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(levelColor)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text("RC", color = Color.White, fontWeight = FontWeight.Bold)
        }
        return
    }

    Card(
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2101317))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ride Copilot", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row {
                    Text(
                        "—",
                        color = Color.White,
                        modifier = Modifier
                            .clickable { expanded = false }
                            .padding(horizontal = 8.dp)
                    )
                    Text(
                        "✕",
                        color = Color.White,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                state.loading -> Text("Analyse en cours...", color = Color.White)
                state.errorMessage != null -> Text(state.errorMessage!!, color = Color(0xFFFFAB91))
                state.traffic != null -> {
                    val traffic = state.traffic!!
                    val announced = state.offer?.announcedDurationMinutes

                    Text(
                        "Temps reel total : ${traffic.totalDurationMinutes.toInt()} min",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (traffic.approachDurationMinutes != null) {
                        Text(
                            "dont ${traffic.approachDurationMinutes.toInt()} min pour venir vous chercher",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                    }
                    if (announced != null) {
                        val delta = traffic.totalDurationMinutes - announced
                        val deltaText = if (delta >= 0) {
                            "+${delta.toInt()} min vs annonce app"
                        } else {
                            "${delta.toInt()} min vs annonce app"
                        }
                        Text(
                            deltaText,
                            color = if (delta > 3) Color(0xFFFF8A65) else Color(0xFFB0BEC5),
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "Distance totale : ${"%.1f".format(traffic.totalDistanceKm)} km",
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val profit = state.profitability
                    if (profit?.hardRuleReason != null) {
                        Text(
                            "🚫 REFUS AUTO",
                            color = levelColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(profit.hardRuleReason, color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    } else if (profit?.hourlyRateEuros != null) {
                        Text(
                            "%.2f €/h".format(profit.hourlyRateEuros),
                            color = levelColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Net estime : %.2f € (energie %.2f €)".format(
                                profit.netEarningsEuros,
                                profit.energyCostEuros
                            ),
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                    } else {
                        Text("Prix course non detecte a l'ecran", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                    }
                }
                else -> Text("En attente d'une course...", color = Color(0xFFB0BEC5))
            }
        }
    }
}
