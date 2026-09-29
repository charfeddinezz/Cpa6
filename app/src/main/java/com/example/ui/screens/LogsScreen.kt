package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LogEntry
import com.example.ui.theme.CpaBg
import com.example.ui.theme.CpaBorder
import com.example.ui.theme.CpaCard
import com.example.ui.theme.CpaCardElevated
import com.example.ui.theme.CpaError
import com.example.ui.theme.CpaPrimary
import com.example.ui.theme.CpaPrimaryDim
import com.example.ui.theme.CpaSuccess
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted
import com.example.ui.theme.CpaWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    logs: List<LogEntry>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = logs.filter { entry ->
        val matchesLevel = if (selectedFilter == "all") true else entry.level.equals(selectedFilter, ignoreCase = true)
        val matchesQuery = if (searchQuery.isBlank()) true else {
            entry.message.contains(searchQuery, ignoreCase = true) ||
                    (entry.taskName?.contains(searchQuery, ignoreCase = true) == true)
        }
        matchesLevel && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CpaBg)
            .padding(14.dp)
    ) {
        // Search & Clear Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs...", color = CpaTextDim) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CpaTextMuted, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CpaPrimary,
                    unfocusedBorderColor = CpaBorder,
                    focusedTextColor = CpaText,
                    unfocusedTextColor = CpaText,
                    focusedContainerColor = CpaCard,
                    unfocusedContainerColor = CpaCard
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(46.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onClearLogs,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CpaCard)
                    .border(1.dp, CpaBorder, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear", tint = CpaTextMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Level Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "all" to "All (${logs.size})",
                "success" to "Success",
                "info" to "Info",
                "warning" to "Warn",
                "error" to "Error"
            ).forEach { (lvl, title) ->
                val isSel = selectedFilter == lvl
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) CpaPrimaryDim else CpaCard)
                        .border(1.dp, if (isSel) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                        .clickable { selectedFilter = lvl }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSel) CpaPrimary else CpaTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs Output Console List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(CpaCard)
                .border(1.dp, CpaBorder, RoundedCornerShape(8.dp)),
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filteredLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No logs matching criteria", color = CpaTextDim, fontSize = 12.sp)
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    val (dotColor, textColor) = when (log.level.lowercase()) {
                        "success" -> Pair(CpaSuccess, CpaSuccess)
                        "warning" -> Pair(CpaWarning, CpaWarning)
                        "error" -> Pair(CpaError, CpaError)
                        else -> Pair(CpaPrimary, CpaText)
                    }

                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = timeStr,
                            color = CpaTextDim,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(60.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (log.taskName != null) {
                                Text(
                                    text = "[${log.taskName}]",
                                    color = CpaPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = log.message,
                                color = textColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
