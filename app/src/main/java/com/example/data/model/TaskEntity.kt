package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskMode(val id: String, val label: String, val desc: String) {
    MODE1("mode1", "Mode 1 – Timer", "Browser open duration & repeat cycle count"),
    MODE2("mode2", "Mode 2 – Repeat", "Repeat task within same browser session"),
    MODE3("mode3", "Mode 3 – Smart", "Auto-detect completion via page keywords")
}

enum class TaskStatus(val id: String, val label: String) {
    PENDING("pending", "Pending"),
    RUNNING("running", "Running"),
    COMPLETED("completed", "Done"),
    ERROR("error", "Error"),
    PAUSED("paused", "Paused")
}

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val referer: String = "https://www.google.com",
    val userAgent: String = "random",
    val mode: String = TaskMode.MODE1.id,
    val repeatCount: Int = 1,
    val completedRuns: Int = 0,
    val status: String = TaskStatus.PENDING.id,
    val enabled: Boolean = true,
    // Mode 1 config
    val browserDuration: Int = 60,
    val mode1RepeatCount: Int = 1,
    // Mode 2 config
    val taskDuration: Int = 60,
    val taskRepeatCount: Int = 3,
    val operationRepeatCount: Int = 1,
    // Mode 3 config
    val completionKeywords: String = "thank you, congratulations, success, completed, order received, confirmation",
    // Intelligent Intent Categories (e.g. "Email Submit, Survey, Sign Up")
    val categories: String = "Email Submit, Survey",
    // Work Template & Sequential Repeat Automation
    val workTemplateId: String? = null,
    val sequentialProxyRotation: Boolean = true,
    val clearCacheOnRepeat: Boolean = true,
    val newIdentityOnRepeat: Boolean = true,
    val lastRunAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
