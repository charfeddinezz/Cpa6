package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationState
import com.example.data.model.CampaignStat
import com.example.data.model.TaskEntity
import com.example.ui.theme.CpaAccent
import com.example.ui.theme.CpaAccentDim
import com.example.ui.theme.CpaBg
import com.example.ui.theme.CpaBorder
import com.example.ui.theme.CpaCard
import com.example.ui.theme.CpaCardElevated
import com.example.ui.theme.CpaError
import com.example.ui.theme.CpaPrimary
import com.example.ui.theme.CpaPrimaryBorder
import com.example.ui.theme.CpaPrimaryDim
import com.example.ui.theme.CpaSuccess
import com.example.ui.theme.CpaSuccessDim
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    stats: List<CampaignStat>,
    tasks: List<TaskEntity>,
    automationState: AutomationState,
    onClearStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalRuns = stats.size
    val totalLeads = stats.count { it.leadDetected }
    val conversionRate = if (totalRuns > 0) (totalLeads.toFloat() / totalRuns * 100) else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // High-level KPI Matrix
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "TOTAL RUNS",
                    value = "$totalRuns",
                    subtext = "Executed tasks",
                    valueColor = CpaText,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "CONFIRMED LEADS",
                    value = "$totalLeads",
                    subtext = "CPA conversions",
                    valueColor = CpaSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(
                    title = "CONVERSION RATE",
                    value = String.format(Locale.US, "%.1f%%", conversionRate),
                    subtext = "Overall efficiency",
                    valueColor = CpaPrimary,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "ACTIVE SESSION",
                    value = "${automationState.leadsThisSession} / ${automationState.completedThisSession}",
                    subtext = "Leads / Runs this run",
                    valueColor = CpaAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action: Reset Stats
        if (stats.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onClearStats,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CpaError.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = CpaError, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Campaign Stats", color = CpaError, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Recent Conversion History Logs
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = "History", tint = CpaPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RECENT RUN & CONVERSION HISTORY",
                    color = CpaPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (stats.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = "Empty", tint = CpaTextDim, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Runs Recorded Yet", color = CpaTextMuted, fontSize = 14.sp)
                        Text("Start automation to generate campaign results", color = CpaTextDim, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(stats, key = { it.id }) { log ->
                val timeFormat = SimpleDateFormat("HH:mm:ss · MM/dd", Locale.US).format(Date(log.timestamp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CpaCard)
                        .border(
                            1.dp,
                            if (log.leadDetected) CpaSuccess.copy(alpha = 0.4f) else CpaBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (log.leadDetected) CpaSuccessDim else CpaCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.leadDetected) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = "Status",
                                    tint = if (log.leadDetected) CpaSuccess else CpaTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = log.taskName,
                                    color = CpaText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${log.ip} · ${log.country} · ${log.details}",
                                    color = if (log.leadDetected) CpaSuccess else CpaTextDim,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = timeFormat,
                            color = CpaTextDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CpaCard)
            .border(1.dp, CpaBorder, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                color = CpaTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = CpaTextDim,
                fontSize = 10.sp
            )
        }
    }
}
