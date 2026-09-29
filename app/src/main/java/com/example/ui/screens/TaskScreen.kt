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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationState
import com.example.data.model.TaskEntity
import com.example.service.TaskCategoryPlanner
import com.example.ui.components.AddTaskDialog
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
import com.example.ui.theme.CpaText
import com.example.ui.theme.CpaTextDim
import com.example.ui.theme.CpaTextMuted
import com.example.ui.theme.CpaWarning

@Composable
fun TaskScreen(
    tasks: List<TaskEntity>,
    automationState: AutomationState,
    workTemplates: List<com.example.data.model.WorkTemplateEntity> = emptyList(),
    onSaveTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onRunTask: (TaskEntity) -> Unit,
    onOptimizeTask: (TaskEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    val enabledCount = tasks.count { it.enabled }
    val totalRuns = tasks.sumOf { it.completedRuns }

    Box(modifier = modifier.fillMaxSize().background(CpaBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CpaCard)
                        .border(1.dp, CpaBorder, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("TOTAL TASKS", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${tasks.size}", color = CpaText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(CpaBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("ENABLED", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$enabledCount", color = CpaPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(1.dp).height(32.dp).background(CpaBorder))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("TOTAL RUNS", color = CpaTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$totalRuns", color = CpaSuccess, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = "Empty", tint = CpaTextDim, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No CPA Tasks Added", color = CpaTextMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tap the + button to add an offer automation", color = CpaTextDim, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                itemsIndexed(tasks, key = { _, t -> t.id }) { index, task ->
                    TaskCard(
                        task = task,
                        index = index,
                        isCurrentlyExecuting = automationState.currentTaskId == task.id,
                        workTemplates = workTemplates,
                        onRun = { onRunTask(task) },
                        onEdit = {
                            taskToEdit = task
                            showDialog = true
                        },
                        onOptimize = { onOptimizeTask(task) },
                        onDelete = { onDeleteTask(task) },
                        onToggle = { onToggleTask(task) }
                    )
                }
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                taskToEdit = null
                showDialog = true
            },
            containerColor = CpaPrimary,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    if (showDialog) {
        AddTaskDialog(
            taskToEdit = taskToEdit,
            workTemplates = workTemplates,
            onDismiss = {
                showDialog = false
                taskToEdit = null
            },
            onSave = {
                onSaveTask(it)
                showDialog = false
                taskToEdit = null
            }
        )
    }
}

@Composable
fun TaskCard(
    task: TaskEntity,
    index: Int,
    isCurrentlyExecuting: Boolean,
    workTemplates: List<com.example.data.model.WorkTemplateEntity> = emptyList(),
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onOptimize: () -> Unit = {},
    onDelete: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor, statusIcon) = when {
        isCurrentlyExecuting -> Triple("Running", CpaPrimary, Icons.Default.PlayCircle)
        task.status == "completed" -> Triple("Done", CpaSuccess, Icons.Default.CheckCircle)
        task.status == "error" -> Triple("Error", CpaError, Icons.Default.Error)
        task.status == "paused" -> Triple("Paused", CpaWarning, Icons.Default.PauseCircle)
        else -> Triple("Pending", CpaTextMuted, Icons.Default.HourglassEmpty)
    }

    val modeLabel = when (task.mode) {
        "mode1" -> "Mode 1 · Timer"
        "mode2" -> "Mode 2 · Repeat"
        "mode3" -> "Mode 3 · Smart"
        else -> "Mode 1"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CpaCard)
            .border(
                1.dp,
                if (isCurrentlyExecuting) CpaPrimaryBorder else CpaBorder,
                RoundedCornerShape(10.dp)
            )
            .then(if (!task.enabled) Modifier.alpha(0.6f) else Modifier)
            .padding(14.dp)
    ) {
        Column {
            // Top Row: Index badge, Name & URL, Toggle switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CpaPrimaryDim)
                        .border(1.dp, CpaPrimaryBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = CpaPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.name,
                        color = CpaText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.url,
                        color = CpaTextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Switch(
                    checked = task.enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CpaPrimary,
                        checkedTrackColor = CpaPrimaryBorder,
                        uncheckedThumbColor = CpaTextDim,
                        uncheckedTrackColor = CpaCardElevated
                    ),
                    modifier = Modifier.testTag("task_toggle_switch_${task.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Status, Mode, Repeat Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(statusIcon, contentDescription = statusLabel, tint = statusColor, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Mode badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CpaAccentDim)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(modeLabel, color = CpaAccent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }

                // Repeat badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CpaCardElevated)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = "Runs", tint = CpaTextMuted, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${task.completedRuns}/${if (task.repeatCount == 0) "∞" else task.repeatCount}",
                        color = CpaTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Random User Agent Badge
                if (task.userAgent.equals("random", ignoreCase = true) || task.userAgent.startsWith("random")) {
                    val uaLabel = when {
                        task.userAgent.contains("mobile", ignoreCase = true) -> "📱 UA: عشوائي موبايل"
                        task.userAgent.contains("desktop", ignoreCase = true) -> "💻 UA: عشوائي كمبيوتر"
                        else -> "🎲 UA: عشوائي متجدد"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CpaPrimaryDim)
                            .border(0.5.dp, CpaPrimaryBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(uaLabel, color = CpaPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Attached Work Template Badge
                if (!task.workTemplateId.isNullOrBlank()) {
                    val tmplName = workTemplates.firstOrNull { it.id == task.workTemplateId }?.name ?: "قالب مخصص"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CpaAccent.copy(alpha = 0.15f))
                            .border(0.5.dp, CpaAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("📋 $tmplName", color = CpaAccent, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // AI Intent & Funnel Categories
            val parsedCats = TaskCategoryPlanner.parseCategories(task.categories)
            val orderedSteps = TaskCategoryPlanner.orderCategories(parsedCats)
            if (orderedSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(CpaPrimaryDim)
                        .border(1.dp, CpaPrimaryBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Funnel",
                        tint = CpaPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Funnel:",
                        color = CpaPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = orderedSteps.joinToString(" ➔ ") { "${it.order}.${it.emoji} ${it.labelEn}" },
                        color = CpaText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Progress bar if running
            if (isCurrentlyExecuting) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = CpaPrimary,
                    trackColor = CpaCardElevated
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CpaBorder))
            Spacer(modifier = Modifier.height(6.dp))

            // Action buttons: Run Now, Edit, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Run in Browser Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCurrentlyExecuting) CpaPrimaryDim else CpaCardElevated)
                        .border(1.dp, if (isCurrentlyExecuting) CpaPrimary else CpaBorder, RoundedCornerShape(6.dp))
                        .clickable { onRun() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run",
                            tint = CpaPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isCurrentlyExecuting) "Running..." else "Run in Browser",
                            color = CpaPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clickable { onOptimize() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Optimize", tint = CpaPrimary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("تحسين", color = CpaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        modifier = Modifier
                            .clickable { onEdit() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CpaTextDim, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Edit", color = CpaTextDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Row(
                        modifier = Modifier
                            .clickable { onDelete() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CpaError, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Delete", color = CpaError, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
