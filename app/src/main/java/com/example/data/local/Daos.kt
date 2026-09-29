package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CampaignStat
import com.example.data.model.EmailItem
import com.example.data.model.OfferClickItem
import com.example.data.model.ProxyItem
import com.example.data.model.ScriptItem
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE enabled = 1 ORDER BY createdAt ASC")
    suspend fun getEnabledTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    suspend fun getAllTasksList(): List<TaskEntity>

    @Query("UPDATE tasks SET enabled = :enabled WHERE id = :id")
    suspend fun updateTaskEnabled(id: String, enabled: Boolean)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String)

    @Query("UPDATE tasks SET completedRuns = completedRuns + 1, lastRunAt = :timestamp WHERE id = :id")
    suspend fun incrementCompletedRuns(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = 'pending', completedRuns = 0")
    suspend fun resetAllTaskStatuses()
}

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails ORDER BY id ASC")
    fun getAllEmails(): Flow<List<EmailItem>>

    @Query("SELECT COUNT(*) FROM emails")
    fun getEmailCount(): Flow<Int>

    @Query("SELECT * FROM emails ORDER BY id ASC LIMIT 1")
    suspend fun getNextEmail(): EmailItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmails(emails: List<EmailItem>)

    @Delete
    suspend fun deleteEmail(email: EmailItem)

    @Query("DELETE FROM emails WHERE id = :id")
    suspend fun deleteEmailById(id: Long)

    @Query("DELETE FROM emails")
    suspend fun clearAllEmails()
}

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY createdAt ASC")
    fun getAllScripts(): Flow<List<ScriptItem>>

    @Query("SELECT * FROM scripts WHERE enabled = 1")
    suspend fun getActiveScripts(): List<ScriptItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultScripts(scripts: List<ScriptItem>)

    @Update
    suspend fun updateScript(script: ScriptItem)

    @Delete
    suspend fun deleteScript(script: ScriptItem)

    @Query("UPDATE scripts SET enabled = :enabled WHERE id = :id")
    suspend fun toggleScript(id: String, enabled: Boolean)
}

@Dao
interface LeadLogDao {
    @Query("SELECT * FROM lead_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<CampaignStat>>

    @Query("SELECT COUNT(*) FROM lead_logs")
    fun getTotalRunsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM lead_logs WHERE leadDetected = 1")
    fun getLeadsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CampaignStat)

    @Query("DELETE FROM lead_logs")
    suspend fun clearAllLogs()
}

@Dao
interface ProxyDao {
    @Query("SELECT * FROM proxies ORDER BY id DESC")
    fun getAllProxies(): Flow<List<ProxyItem>>

    @Query("SELECT * FROM proxies ORDER BY id DESC")
    suspend fun getAllProxiesList(): List<ProxyItem>

    @Query("SELECT COUNT(*) FROM proxies")
    fun getProxyCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM proxies")
    suspend fun getProxyCountOnce(): Int

    @Query("SELECT * FROM proxies ORDER BY lastUsedAt ASC LIMIT 1")
    suspend fun getNextProxy(): ProxyItem?

    @Query("SELECT * FROM proxies WHERE status != 'failed' ORDER BY lastUsedAt ASC LIMIT 1")
    suspend fun getNextWorkingProxy(): ProxyItem?

    @Query("SELECT * FROM proxies WHERE id = :id LIMIT 1")
    suspend fun getProxyById(id: Long): ProxyItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxies(proxies: List<ProxyItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxy(proxy: ProxyItem)

    @Update
    suspend fun updateProxy(proxy: ProxyItem)

    @Delete
    suspend fun deleteProxy(proxy: ProxyItem)

    @Query("DELETE FROM proxies WHERE id = :id")
    suspend fun deleteProxyById(id: Long)

    @Query("DELETE FROM proxies")
    suspend fun clearAllProxies()

    @Query("UPDATE proxies SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun markProxyUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE proxies SET status = :status, lastPingMs = :ping WHERE id = :id")
    suspend fun updateProxyStatus(id: Long, status: String, ping: Long)

    @Query("UPDATE proxies SET status = :status, lastPingMs = :ping, country = :country WHERE id = :id")
    suspend fun updateProxyStatusAndCountry(id: Long, status: String, ping: Long, country: String)

    @Query("UPDATE proxies SET status = :status, lastPingMs = :ping, country = :country, city = :city, isp = :isp, score = :score, lastCheckedAt = :checkedAt WHERE id = :id")
    suspend fun updateProxyFullDetails(id: Long, status: String, ping: Long, country: String, city: String, isp: String, score: Int, checkedAt: Long = System.currentTimeMillis())

    @Query("UPDATE proxies SET successCount = successCount + 1, score = MIN(100, score + 5), lastUsedAt = :timestamp WHERE id = :id")
    suspend fun recordProxySuccess(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE proxies SET failCount = failCount + 1, score = MAX(0, score - 25), status = CASE WHEN failCount >= 2 THEN 'failed' ELSE status END WHERE id = :id")
    suspend fun recordProxyFailure(id: Long)

    @Query("UPDATE proxies SET type = :newType WHERE id = :id")
    suspend fun updateProxyType(id: Long, newType: String)

    @Query("UPDATE proxies SET status = 'active', failCount = 0 WHERE status = 'failed'")
    suspend fun resetFailedProxies(): Int

    @Query("DELETE FROM proxies WHERE status = 'failed'")
    suspend fun deleteFailedProxies(): Int

    @Query("SELECT * FROM proxies WHERE status = 'working' ORDER BY score DESC, lastPingMs ASC")
    suspend fun getWorkingProxies(): List<ProxyItem>

    @Query("SELECT * FROM proxies WHERE status != 'failed' ORDER BY score DESC, lastPingMs ASC")
    suspend fun getAvailableProxies(): List<ProxyItem>

    @Query("SELECT * FROM proxies WHERE status != 'failed' ORDER BY score DESC, lastPingMs ASC LIMIT 1")
    suspend fun getBestWorkingProxy(): ProxyItem?

    @Query("SELECT * FROM proxies WHERE country = :countryCode AND status != 'failed' ORDER BY score DESC, lastPingMs ASC LIMIT 1")
    suspend fun getBestProxyForCountry(countryCode: String): ProxyItem?
}

@Dao
interface OfferClickDao {
    @Query("SELECT * FROM offer_click_items ORDER BY orderIndex ASC, id ASC")
    fun getAllClickItems(): Flow<List<OfferClickItem>>

    @Query("SELECT * FROM offer_click_items ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllClickItemsList(): List<OfferClickItem>

    @Query("SELECT * FROM offer_click_items WHERE enabled = 1 ORDER BY orderIndex ASC, id ASC")
    fun getEnabledClickItems(): Flow<List<OfferClickItem>>

    @Query("SELECT * FROM offer_click_items WHERE enabled = 1 ORDER BY orderIndex ASC, id ASC")
    suspend fun getEnabledClickItemsList(): List<OfferClickItem>

    @Query("SELECT * FROM offer_click_items WHERE id = :id LIMIT 1")
    suspend fun getClickItemById(id: Long): OfferClickItem?

    @Query("SELECT MAX(orderIndex) FROM offer_click_items")
    suspend fun getMaxOrderIndex(): Int?

    @Query("SELECT COUNT(*) FROM offer_click_items")
    fun getClickItemsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClickItem(item: OfferClickItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClickItems(items: List<OfferClickItem>)

    @Update
    suspend fun updateClickItem(item: OfferClickItem)

    @Delete
    suspend fun deleteClickItem(item: OfferClickItem)

    @Query("DELETE FROM offer_click_items WHERE id = :id")
    suspend fun deleteClickItemById(id: Long)

    @Query("UPDATE offer_click_items SET enabled = :enabled WHERE id = :id")
    suspend fun toggleClickItem(id: Long, enabled: Boolean)

    @Query("UPDATE offer_click_items SET clickCount = clickCount + 1, lastClickedAt = :timestamp WHERE text = :text")
    suspend fun recordClickByText(text: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE offer_click_items SET clickCount = clickCount + 1, lastClickedAt = :timestamp WHERE id = :id")
    suspend fun recordClick(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE offer_click_items SET orderIndex = :newIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, newIndex: Int)

    @Query("DELETE FROM offer_click_items")
    suspend fun clearAll()
}

@Dao
interface WorkTemplateDao {
    @Query("SELECT * FROM work_templates ORDER BY updatedAt DESC")
    fun getAllTemplates(): Flow<List<com.example.data.model.WorkTemplateEntity>>

    @Query("SELECT * FROM work_templates ORDER BY updatedAt DESC")
    suspend fun getAllTemplatesList(): List<com.example.data.model.WorkTemplateEntity>

    @Query("SELECT * FROM work_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: String): com.example.data.model.WorkTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: com.example.data.model.WorkTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<com.example.data.model.WorkTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: com.example.data.model.WorkTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: com.example.data.model.WorkTemplateEntity)

    @Query("DELETE FROM work_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: String)
}


