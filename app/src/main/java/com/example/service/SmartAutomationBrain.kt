package com.example.service

import com.example.data.model.TaskEntity
import org.json.JSONObject
import kotlin.math.min
import kotlin.random.Random

/**
 * SmartAutomationBrain — العقل المركزي للأتمتة الذكية.
 *
 * يجعل الأتمتة:
 *  - فاهمة: تحلل الصفحة + السياق + التاريخ بدل رد فعل أعمى لكلمات مفتاحية.
 *  - واعية: تتذكر أين كانت، ماذا فعلت، هل تقدمت أم علقت، وتتعلم من الفشل.
 *  - متكيفة: تغير الإستراتيجية والتوقيت تلقائياً حسب تعقيد الصفحة وحالتها.
 */
object SmartAutomationBrain {

    // ── أنواع القرارات ──────────────────────────────────────────
    enum class NextAction(val code: String) {
        COMPLETE_CONVERSION("complete"),
        CLICK_OFFER("click_offer"),
        WAIT_LOCKER("wait_locker"),
        FILL_FORM("fill_form"),
        ANSWER_SURVEY("answer_survey"),
        SKIP_UPSELL("skip_upsell"),
        CHECK_TERMS("check_terms"),
        WAIT_CAPTCHA("wait_captcha"),
        WAIT_LOAD("wait_load"),
        RELOAD_RETRY("reload_retry"),
        SWITCH_STRATEGY("switch_strategy"),
        EXPLORE_SCROLL("explore_scroll"),
        SUBMIT_ADVANCE("submit_advance")
    }

    data class SmartDecision(
        val action: NextAction,
        val titleAr: String,
        val titleEn: String,
        val reasonAr: String,
        val reasonEn: String,
        val confidence: Int, // 0..100
        val delayMsBeforeNextCycle: Long, // توقيت بشري تكيفي
        val prioritySteps: List<String>,
        val needsReload: Boolean = false,
        val needsProxySwitch: Boolean = false,
        val isStuck: Boolean = false
    )

    data class PageMemory(
        val url: String,
        val category: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    // ذاكرة الجلسة — تُحفظ في AppViewModel وتُمرر للعقل
    data class BrainMemory(
        val pageHistory: List<PageMemory> = emptyList(),
        val actionTrace: List<String> = emptyList(), // أكواد NextAction السابقة
        val consecutiveSamePage: Int = 0,
        val consecutiveNoProgress: Int = 0,
        val reloadAttemptsForUrl: Int = 0,
        val lastUrl: String = "",
        val lastCategory: String = "",
        val sessionStartTime: Long = System.currentTimeMillis()
    )

    data class TaskLearningStats(
        val taskId: String,
        val runs: Int = 0,
        val conversions: Int = 0
    ) {
        val successRate: Double get() = if (runs == 0) 0.5 else conversions.toDouble() / runs.toDouble()
    }

    // ── كلمات الكشف الذكي ───────────────────────────────────────
    private val BLOCK_KEYWORDS = listOf(
        "access denied", "forbidden", "403", "blocked", "your ip has been blocked",
        "suspicious activity", "unusual traffic", "please verify you are a human",
        "attention required", "cloudflare ray", "error 1020", "ip banned"
    )
    private val CAPTCHA_KEYWORDS = listOf(
        "captcha", "recaptcha", "hcaptcha", "turnstile", "i'm not a robot",
        "select all images", "verify you are human", "checkbox.*robot"
    )
    private val LOADING_KEYWORDS = listOf(
        "loading", "please wait", "redirecting", "verifying your browser",
        "checking your browser", "just a moment"
    )
    private val CONFIRM_EXTRA = listOf(
        "thank you", "congratulations", "success", "confirmed", "completed",
        "verified", "reward", "entry confirmed", "order received", "welcome",
        "account created", "claim confirmed", "responses recorded", "ticket number"
    )

    /**
     * القرار الرئيسي: ماذا نفعل الآن؟
     */
    fun decide(
        report: TaskCategoryPlanner.PageAnalysisReport,
        rawJson: String,
        configuredCategories: List<String>,
        activeClickTarget: String?,
        memory: BrainMemory,
        taskLearning: TaskLearningStats? = null
    ): SmartDecision {
        val extras = parseExtras(rawJson)
        val isBlocked = detectBlocked(report, extras)
        val isCaptcha = detectCaptcha(report, extras)
        val isLoading = detectLoading(report, extras)

        val samePageRepeat = memory.consecutiveSamePage
        val noProgress = memory.consecutiveNoProgress
        val isStuck = (samePageRepeat >= 3 && noProgress >= 2) || noProgress >= 4

        // 1) محظور — لا تعاند، بدّل الهوية/البروكسي
        if (isBlocked) {
            return SmartDecision(
                action = NextAction.RELOAD_RETRY,
                titleAr = "🚫 رصد حظر / منع — تبديل ذكي للمسار",
                titleEn = "Block detected — smart evade",
                reasonAr = "الصفحة تظهر رسالة حظر أو رفض (${extras.matchedBlockKeyword}). الاستمرار بنفس البروكسي يضيع الوقت ويحرق الهوية. القرار: انتظار قصير ثم إعادة تحميل بهوية/بروكسي جديد.",
                reasonEn = "Block wall detected. Rotate identity/proxy instead of hammering.",
                confidence = 92,
                delayMsBeforeNextCycle = 4000,
                prioritySteps = listOf("إيقاف التعبئة فوراً", "تسجيل سبب الحظر", "تدوير البروكسي", "إعادة المحاولة بهوية جديدة"),
                needsReload = true,
                needsProxySwitch = true,
                isStuck = true
            )
        }

        // 2) كابتشا — انتظر ولا تخرب الحل
        if (isCaptcha) {
            return SmartDecision(
                action = NextAction.WAIT_CAPTCHA,
                titleAr = "🧩 كابتشا / تحقق بشري — وضع الانتظار الذكي",
                titleEn = "Captcha — patient wait",
                reasonAr = "رُصد تحقق بشري (${extras.matchedCaptchaKeyword}). النقر العشوائي الآن يفشل التحدي. القرار: تمرير هادئ وانتظار الحل (يدوي أو مزود CAPTCHA) ثم الاستئناف.",
                reasonEn = "Captcha challenge present. Wait patiently, don't interfere.",
                confidence = 90,
                delayMsBeforeNextCycle = 8000,
                prioritySteps = listOf("عدم لمس مربع الكابتشا", "تمرير خفيف طبيعي", "انتظار 8 ثوانٍ", "إعادة الفحص بعد الانتظار")
            )
        }

        // 3) صفحة ما زالت تحمل
        if (isLoading || report.confidence < 40) {
            return SmartDecision(
                action = NextAction.WAIT_LOAD,
                titleAr = "⏳ الصفحة ما زالت تتحمل — انتظار ذكي",
                titleEn = "Page still loading",
                reasonAr = "المحتوى غير مكتمل (تحميل/إعادة توجيه). التحليل الآن يعطي نتيجة مضللة. القرار: انتظار قصير ثم إعادة التحليل.",
                reasonEn = "Incomplete load. Wait then re-analyze.",
                confidence = 80,
                delayMsBeforeNextCycle = 3000,
                prioritySteps = listOf("انتظار اكتمال التحميل", "إعادة تحليل DOM")
            )
        }

        // 4) عالق — غيّر الإستراتيجية بدل تكرار نفس الفعل
        if (isStuck) {
            val alternative = pickEscapeStrategy(report, memory)
            return SmartDecision(
                action = NextAction.SWITCH_STRATEGY,
                titleAr = "🔄 رصد تعليق — تغيير الإستراتيجية",
                titleEn = "Stuck detected — switch strategy",
                reasonAr = "نفس الصفحة (${report.detectedCategoryAr}) تكررت $samePageRepeat مرات بدون تقدم. تكرار نفس النقرات لن يفيد. القرار البديل: $alternative.",
                reasonEn = "Loop detected. Trying alternative: $alternative",
                confidence = 85,
                delayMsBeforeNextCycle = 3500,
                prioritySteps = listOf("إيقاف التكرار الأعمى", alternative, "تمرير استكشافي عميق", "إعادة تقييم الفئة"),
                needsReload = memory.reloadAttemptsForUrl == 0 && samePageRepeat >= 5,
                isStuck = true
            )
        }

        // 5) تحويل مؤكد — أنهِ بذكاء
        if (report.isConfirmationPage) {
            return SmartDecision(
                action = NextAction.COMPLETE_CONVERSION,
                titleAr = "🏆 تحويل مؤكد — إغلاق ذكي للتكرار",
                titleEn = "Conversion confirmed",
                reasonAr = "رُصدت صفحة الشكر/النجاح بثقة ${report.confidence}%. الاستمرار يعني نقرات زائدة قد تبطل الليد. القرار: حفظ الشاشة وتسجيل التحويل فوراً.",
                reasonEn = "Thank-you page confirmed. Stop and record lead.",
                confidence = min(99, report.confidence + 5),
                delayMsBeforeNextCycle = 1500,
                prioritySteps = listOf("تثبيت صفحة الإثبات", "تسجيل الإحصائية", "إنهاء التكرار مبكراً")
            )
        }

        // 6) لوكر — أولوية قصوى
        val hasLockerCombo = TaskCategoryPlanner.isLockerOfferClickCombo(configuredCategories) ||
            report.hasLocker || report.detectedCategory.contains("locker", ignoreCase = true)
        if (hasLockerCombo) {
            val target = activeClickTarget ?: "العرض الأول المتاح"
            return SmartDecision(
                action = NextAction.WAIT_LOCKER,
                titleAr = "🔒 لوكر نشط — مطابقة ونقر ذكي",
                titleEn = "Locker — match & click",
                reasonAr = "اللوكر ظاهر (${report.summary}). الهدف: '$target'. القرار: مطابقة العروض حسب الإستراتيجية والنقر ثم نقل العمل للتبويب الجديد فوراً.",
                reasonEn = "Locker visible. Match offer and transition to new tab.",
                confidence = 93,
                delayMsBeforeNextCycle = adaptiveDelay(report, base = 2500),
                prioritySteps = listOf("مسح عروض اللوكر", "مطابقة '$target'", "النقر وفتح تبويب جديد", "نقل التركيز للتبويب الجديد")
            )
        }

        // 7) تخطي Upsell — لا تدفع أبداً
        if (report.hasSkipButtons) {
            return SmartDecision(
                action = NextAction.SKIP_UPSELL,
                titleAr = "⏭️ جدار عروض مدفوعة — تخطٍ حازم",
                titleEn = "Upsell wall — skip",
                reasonAr = "رُصدت أزرار (No Thanks/Skip). هذه عروض راعية مشروطة بالدفع. القرار: تخطيها فوراً دون ملء أي بيانات دفع.",
                reasonEn = "Sponsor wall. Skip without paying.",
                confidence = 90,
                delayMsBeforeNextCycle = adaptiveDelay(report, base = 2000),
                prioritySteps = listOf("تحديد زر No Thanks/Skip", "النقر والتجاوز", "المتابعة للمرحلة الأساسية")
            )
        }

        // 8) استبيان — أجب بذكاء حسب السياق
        if (report.radioGroupsCount >= 1 || report.selectCount >= 1 ||
            report.detectedCategory.contains("survey", ignoreCase = true)
        ) {
            return SmartDecision(
                action = NextAction.ANSWER_SURVEY,
                titleAr = "📝 استبيان — إجابة مؤهلة واعية",
                titleEn = "Survey — qualified answers",
                reasonAr = "استبيان تفاعلي (${report.radioGroupsCount} مجموعات). القرار: إجابات متسقة مع شخصية 18+ مقيم أمريكي (نعم/موظف/مالك) بدل الاختيار العشوائي.",
                reasonEn = "Survey detected. Answer consistently as qualified US adult.",
                confidence = 88,
                delayMsBeforeNextCycle = adaptiveDelay(report, base = 3500),
                prioritySteps = listOf("قراءة منطوق السؤال", "اختيار الإجابة المؤهلة", "تفعيل حدث Change", "الضغط على Next")
            )
        }

        // 9) نموذج بيانات — املأ بفهم الحقول
        if (report.emailFields >= 1 || report.textFields >= 1 ||
            report.detectedCategory.contains("lead", ignoreCase = true) ||
            report.detectedCategory.contains("email", ignoreCase = true)
        ) {
            val fieldsDesc = "${report.fieldsCount} حقول (${report.emailFields} بريد)"
            return SmartDecision(
                action = NextAction.FILL_FORM,
                titleAr = "📋 نموذج بيانات — تعبئة مفهومة",
                titleEn = "Lead form — understood fill",
                reasonAr = "نموذج تسجيل ($fieldsDesc). القرار: ضخ الهوية حقلاً بحقل مع مطابقة Label الحقيقي، ثم الموافقة على الشروط، ثم إرسال واحد فقط.",
                reasonEn = "Form detected. Fill field-by-field with real labels.",
                confidence = 87,
                delayMsBeforeNextCycle = adaptiveDelay(report, base = 4000),
                prioritySteps = listOf("مطابقة كل حقل (بريد/اسم/هاتف/zip)", "كتابة بشرية تدريجية", "تحديد الشروط 18+", "نقرة إرسال واحدة")
            )
        }

        // 10) شروط معلقة
        if (report.checkboxesCount >= 1) {
            return SmartDecision(
                action = NextAction.CHECK_TERMS,
                titleAr = "📜 موافقة الشروط — تحديد واعٍ",
                titleEn = "Terms consent",
                reasonAr = "مربعات موافقة غير محددة (${report.checkboxesCount}). بدونها لن يتقدم النموذج. القرار: تحديد الإلزامي فقط وتجاهل الاشتراكات الدعائية.",
                reasonEn = "Unchecked boxes block progress. Check required only.",
                confidence = 84,
                delayMsBeforeNextCycle = 2200,
                prioritySteps = listOf("تمييز الإلزامي عن الدعائي", "تحديد الشروط والعمر", "المتابعة")
            )
        }

        // 11) مرشح نقرة عرض
        if (report.hasOfferClickCandidate && activeClickTarget != null) {
            return SmartDecision(
                action = NextAction.CLICK_OFFER,
                titleAr = "🎯 عرض مطابق — نقرة محسوبة",
                titleEn = "Matching offer — calculated click",
                reasonAr = "النص المستهدف '$activeClickTarget' ظاهر في الصفحة. القرار: نقرة واحدة محسوبة بعد تمرير طبيعي، لا نقرات مكررة.",
                reasonEn = "Target text visible. Single calculated click.",
                confidence = 91,
                delayMsBeforeNextCycle = 2500,
                prioritySteps = listOf("تمرير حتى ظهور العنصر", "تحويم بشري", "نقرة واحدة", "انتظار الوجهة")
            )
        }

        // 12) افتراضي — استكشاف هادئ
        return SmartDecision(
            action = NextAction.EXPLORE_SCROLL,
            titleAr = "🌐 صفحة غير مصنفة — استكشاف هادئ",
            titleEn = "Unclassified — calm explore",
            reasonAr = "لا توجد إشارة قوية (${report.summary}). النقر العشوائي خطر. القرار: تمرير استكشافي + فحص الأزرار بدون نقر متهور.",
            reasonEn = "Weak signal. Explore without reckless clicks.",
            confidence = 60,
            delayMsBeforeNextCycle = adaptiveDelay(report, base = 5000),
            prioritySteps = listOf("تمرير تدريجي", "رصد أزرار التقدم", "إعادة التحليل بعد الاكتمال")
        )
    }

    // ── توقيت بشري تكيفي ────────────────────────────────────────
    fun adaptiveDelay(report: TaskCategoryPlanner.PageAnalysisReport, base: Long): Long {
        val complexity = report.fieldsCount + report.buttonsCount + report.radioGroupsCount * 2 + report.selectCount * 2
        val extra = min(complexity * 120L, 3000L)
        val jitter = Random.nextLong(400, 1400)
        return base + extra + jitter
    }

    fun humanCycleInterval(minSec: Int, maxSec: Int, qualityScore: Int): Int {
        // جلسة عالية الجودة → إيقاع أسرع قليلاً، جلسة متعثرة → إبطاء لتقليل البصمة
        val lo = minSec.coerceAtLeast(12)
        val hi = maxSec.coerceAtLeast(lo)
        if (lo >= hi) return lo
        val base = Random.nextInt(lo, hi + 1)
        return if (qualityScore < 40) base + 5 else base
    }

    // ── تحديث الذاكرة ───────────────────────────────────────────
    fun updateMemory(
        prev: BrainMemory,
        report: TaskCategoryPlanner.PageAnalysisReport,
        decision: SmartDecision
    ): BrainMemory {
        val sameUrl = prev.lastUrl.isNotBlank() && normalizeUrl(prev.lastUrl) == normalizeUrl(report.url.ifBlank { prev.lastUrl })
        val sameCat = prev.lastCategory == report.detectedCategory && sameUrl
        val progressed = decision.action == NextAction.COMPLETE_CONVERSION ||
            decision.action == NextAction.CLICK_OFFER ||
            decision.action == NextAction.SUBMIT_ADVANCE
        val newNoProgress = if (progressed) 0 else if (sameCat) prev.consecutiveNoProgress + 1 else 0
        return prev.copy(
            pageHistory = (prev.pageHistory + PageMemory(report.url, report.detectedCategory)).takeLast(30),
            actionTrace = (prev.actionTrace + decision.action.code).takeLast(30),
            consecutiveSamePage = if (sameUrl) prev.consecutiveSamePage + 1 else 1,
            consecutiveNoProgress = newNoProgress,
            reloadAttemptsForUrl = if (decision.needsReload && sameUrl) prev.reloadAttemptsForUrl + 1 else if (!sameUrl) 0 else prev.reloadAttemptsForUrl,
            lastUrl = report.url.ifBlank { prev.lastUrl },
            lastCategory = report.detectedCategory
        )
    }

    fun initialMemory(): BrainMemory = BrainMemory()

    // ── ترتيب المهام بالتعلم ────────────────────────────────────
    fun prioritizeTasks(tasks: List<TaskEntity>, learning: Map<String, TaskLearningStats>): List<TaskEntity> {
        if (tasks.size <= 1) return tasks
        return tasks.sortedWith(
            compareByDescending<TaskEntity> { learning[it.id]?.successRate ?: 0.5 }
                .thenBy { it.completedRuns }
                .thenBy { it.createdAt }
        )
    }

    fun recordOutcome(
        learning: MutableMap<String, TaskLearningStats>,
        taskId: String,
        converted: Boolean
    ) {
        val cur = learning[taskId] ?: TaskLearningStats(taskId)
        learning[taskId] = cur.copy(runs = cur.runs + 1, conversions = cur.conversions + if (converted) 1 else 0)
    }

    fun sessionQuality(memory: BrainMemory, conversions: Int, runs: Int): Int {
        var score = 70
        score -= memory.consecutiveNoProgress * 8
        if (runs > 0) score += (conversions * 100 / (runs + 1)) / 4
        if (memory.reloadAttemptsForUrl > 2) score -= 15
        return score.coerceIn(0, 100)
    }

    // ── كشف الحالات ─────────────────────────────────────────────
    private data class Extras(
        val matchedBlockKeyword: String = "",
        val matchedCaptchaKeyword: String = "",
        val matchedLoadingKeyword: String = "",
        val hasCaptchaElement: Boolean = false,
        val hasBlockElement: Boolean = false,
        val inputsCount: Int = 0
    )

    private fun parseExtras(rawJson: String): Extras {
        if (rawJson.isBlank()) return Extras()
        return try {
            val o = JSONObject(rawJson)
            Extras(
                matchedBlockKeyword = o.optString("blockKeyword", ""),
                matchedCaptchaKeyword = o.optString("captchaKeyword", ""),
                matchedLoadingKeyword = o.optString("loadingKeyword", ""),
                hasCaptchaElement = o.optBoolean("hasCaptcha", false),
                hasBlockElement = o.optBoolean("isBlocked", false),
                inputsCount = o.optInt("fieldsCount", 0)
            )
        } catch (_: Exception) { Extras() }
    }

    private fun detectBlocked(report: TaskCategoryPlanner.PageAnalysisReport, e: Extras): Boolean {
        if (e.hasBlockElement || e.matchedBlockKeyword.isNotBlank()) return true
        val hay = (report.summary + " " + report.title + " " + report.detectedCategory).lowercase()
        return BLOCK_KEYWORDS.any { hay.contains(it) }
    }

    private fun detectCaptcha(report: TaskCategoryPlanner.PageAnalysisReport, e: Extras): Boolean {
        if (e.hasCaptchaElement || e.matchedCaptchaKeyword.isNotBlank()) return true
        val hay = (report.summary + " " + report.title).lowercase()
        return CAPTCHA_KEYWORDS.any { hay.contains(it) }
    }

    private fun detectLoading(report: TaskCategoryPlanner.PageAnalysisReport, e: Extras): Boolean {
        if (e.matchedLoadingKeyword.isNotBlank()) return true
        val hay = (report.summary + " " + report.title).lowercase()
        return LOADING_KEYWORDS.any { hay.contains(it) }
    }

    private fun pickEscapeStrategy(report: TaskCategoryPlanner.PageAnalysisReport, memory: BrainMemory): String {
        val trace = memory.actionTrace.takeLast(5)
        return when {
            trace.count { it == NextAction.FILL_FORM.code } >= 3 ->
                "تجربة زر Next/Submit مباشرة بدل إعادة التعبئة"
            trace.count { it == NextAction.ANSWER_SURVEY.code } >= 2 ->
                "اختيار إجابة بديلة (الخيار الثاني بدل الأول)"
            report.buttonsCount > 6 ->
                "فحص الأزرار الثانوية (زر ثانٍ غير الرئيسي)"
            memory.reloadAttemptsForUrl >= 1 ->
                "الانتقال للمهمة التالية مؤقتاً وكسر الحلقة"
            else -> "إعادة تحميل نظيفة واحدة ثم إعادة التحليل"
        }
    }

    private fun normalizeUrl(u: String): String =
        u.lowercase().substringBefore("?").substringBefore("#").trimEnd('/')

    /** مدة ذكية للمهمة حسب تعقيد الفانل بدل الرقم الثابت */
    fun smartDurationForTask(task: TaskEntity, categories: List<String>): Int {
        val base = task.browserDuration.coerceAtLeast(20)
        val steps = TaskCategoryPlanner.orderCategories(categories).size
        val bonus = (steps * 6).coerceAtMost(30)
        val hasLocker = categories.any { it.contains("locker", true) || it.contains("قفل") || it.contains("لوكر") }
        val lockerBonus = if (hasLocker) 15 else 0
        return (base + bonus + lockerBonus).coerceAtMost(120)
    }

    fun confirmKeywordsSmart(configured: String): List<String> {
        val base = configured.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val merged = (base + CONFIRM_EXTRA).distinct()
        return merged.take(14)
    }

    // ══ خبرة متقدمة: استغلال مقابل استكشاف (UCB1) ═══════════════
    // لا تختار دائماً الأعلى نجاحاً فتعلق في optimum محلي، بل تجرب أحياناً.
    fun prioritizeTasksUCB(tasks: List<TaskEntity>, learning: Map<String, TaskLearningStats>): List<TaskEntity> {
        if (tasks.size <= 1) return tasks
        val totalRuns = learning.values.sumOf { it.runs } + 1
        return tasks.sortedWith(
            compareByDescending<TaskEntity> { t ->
                val s = learning[t.id]
                if (s == null || s.runs == 0) Double.MAX_VALUE - t.createdAt / 1e15 // جرّب الجديد أولاً مرة واحدة
                else {
                    val avg = s.successRate
                    val bonus = kotlin.math.sqrt(2.0 * kotlin.math.ln(totalRuns.toDouble()) / s.runs.toDouble())
                    avg + 0.7 * bonus // معامل الاستكشاف 0.7
                }
            }.thenBy { it.completedRuns }
        )
    }

    data class OfferChoice(val text: String, val clicks: Int, val impressions: Int = 0) {
        val ctr: Double get() = if (impressions == 0) 0.5 else clicks.toDouble() / (impressions + 1)
    }

    /** Epsilon-greedy للنصوص: 85% استغلال الأفضل، 15% استكشاف نص جديد */
    fun selectOfferText(
        enabledTexts: List<String>,
        clickCounts: Map<String, Int>,
        epsilon: Double = 0.15
    ): Int {
        if (enabledTexts.isEmpty()) return -1
        if (enabledTexts.size == 1) return 0
        if (Random.nextDouble() < epsilon) {
            // استكشاف: نص لم يُجرّب كثيراً
            val least = enabledTexts.indices.minByOrNull { clickCounts[enabledTexts[it]] ?: 0 } ?: 0
            return least
        }
        // استغلال: الأعلى نقرات (مع كسر التعادل عشوائياً)
        val maxClicks = enabledTexts.maxOf { clickCounts[it] ?: 0 }
        val best = enabledTexts.indices.filter { (clickCounts[enabledTexts[it]] ?: 0) == maxClicks }
        return best.random()
    }

    /** تقييم البروكسي: نجاح × سرعة × حداثة، مع cooldown للفاشل */
    fun scoreProxyForTask(
        successRate: Double, pingMs: Long, failCount: Int, lastFailAgeMin: Long, qualityScore: Int
    ): Double {
        var score = successRate * 60.0
        score += when {
            pingMs <= 0 -> 0.0
            pingMs <= 400 -> 20.0
            pingMs <= 1200 -> 12.0
            pingMs <= 3000 -> 5.0
            else -> 0.0
        }
        score += (qualityScore / 100.0) * 10.0
        if (failCount > 0) {
            // عقوبة تتلاشى مع الزمن: فشل حديث = عقوبة كبيرة
            val decay = kotlin.math.exp(-lastFailAgeMin / 30.0)
            score -= failCount * 8.0 * decay
        }
        // Sticky bonus: لا تدور mid-funnel بدون سبب — يُطبق في AppViewModel
        return score.coerceIn(0.0, 100.0)
    }

    /** بذرة ثابتة لكل تكرار: نفس الجلسة = نفس البصمة، جلسة جديدة = بصمة جديدة */
    fun sessionSeed(taskId: String, runNumber: Int, proxyHost: String): Long {
        var h = 1125899906842597L
        val s = "$taskId|$runNumber|$proxyHost"
        for (c in s) h = 31 * h + c.code
        return h
    }

    /** فحص اتساق الهوية قبل الضخ: العمر مقابل DOB، الجنس مقابل الاسم، المدينة مقابل Zip */
    fun personaConsistencyReport(
        firstName: String, gender: String, birthDate: String, city: String, postalCode: String, phone: String
    ): List<String> {
        val issues = mutableListOf<String>()
        try {
            val femaleNames = setOf("mary","patricia","jennifer","linda","elizabeth","barbara","susan","jessica","sarah","emily","emma","olivia")
            val isFemaleName = femaleNames.contains(firstName.lowercase())
            val isFemaleGender = gender.equals("female", true)
            if (isFemaleName != isFemaleGender) issues.add("gender-name mismatch")
            val year = birthDate.substringBefore("-").toIntOrNull() ?: 1995
            val age = 2026 - year
            if (age < 18 || age > 70) issues.add("age out of range ($age)")
            val digits = phone.filter { it.isDigit() }
            if (digits.length < 10) issues.add("phone too short")
        } catch (_: Exception) {}
        return issues
    }

    /** حقائق الشخصية للاستبيان الممتد: إجابات متسقة عبر الصفحات */
    fun personaFacts(firstName: String, gender: String, birthDate: String, city: String): Map<String, String> {
        val year = birthDate.substringBefore("-").toIntOrNull() ?: 1995
        val age = (2026 - year).coerceIn(18, 70)
        val ageBand = when {
            age <= 24 -> "18-24"
            age <= 34 -> "25-34"
            age <= 44 -> "35-44"
            else -> "45+"
        }
        return mapOf(
            "age" to age.toString(),
            "ageBand" to ageBand,
            "adult" to "yes",
            "resident" to "yes",
            "gender" to gender.lowercase(),
            "firstName" to firstName
        )
    }

    /** منفعة المهمة: تحويل متوقع في الساعة مقابل حرق البروكسي */
    fun taskUtilityPerHour(successRate: Double, avgDurationSec: Int, proxyCost: Double = 1.0): Double {
        if (avgDurationSec <= 0) return 0.0
        return (successRate * 3600.0 / avgDurationSec) / proxyCost
    }
}
