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
        SUBMIT_ADVANCE("submit_advance"),
        // إضافات الذكاء المتقدم
        OPTIMIZE_STRATEGY("optimize_strategy"),
        ADAPT_DELAY("adapt_delay"),
        RECOVER_STUCK("recover_stuck"),
        PRIORITIZE_TASKS_UCB2("prioritize_ucb2")
    }

    data class SmartDecision(
        val action: NextAction,
        val titleAr: String,
        titleEn: String,
        val reasonAr: String,
        val reasonEn: String,
        val confidence: Int, // 0..100
        val delayMsBeforeNextCycle: Long, // توقيت بشري تكيفي
        val prioritySteps: List<String>,
        val needsReload: Boolean = false,
        val needsProxySwitch: Boolean = false,
        val isStuck: Boolean = false,
        // حالات الذكاء المتقدم
        val strategyAdjustment: String? = null,
        val adaptiveDelayFactor: Double = 1.0,
        val explorationBoost: Double = 0.0,
        val personalization: Map<String, Any> = emptyMap()
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
     * القرار الرئيسي: ماذا نفعل الآن؟ — تحليل شامل Multi-Layer
     * Layer 1: State Detection — ما نشاهده الآن
     * Layer 2: Pattern Analysis — ماpatterns نلاحظها
     * Layer 3: Predictive Analysis — ماذا سيتشكّل لاحقاً
     * Layer 4: Strategy Synthesis — الاستراتيجية الأمثل
     */
    fun decide(
        report: TaskCategoryPlanner.PageAnalysisReport,
        rawJson: String,
        configuredCategories: List<String>,
        activeClickTarget: String?,
        memory: BrainMemory,
        taskLearning: TaskLearningStats? = null,
        userContext: Map<String, Any>? = null
    ): SmartDecision {
        // ── Layer 1: State Detection — State of the Page Now ──
        val extras = parseExtras(rawJson)
        val isBlocked = detectBlocked(report, extras)
        val isCaptcha = detectCaptcha(report, extras)
        val isLoading = detectLoading(report, extras)

        val samePageRepeat = memory.consecutiveSamePage
        val noProgress = memory.consecutiveNoProgress
        val isStuck = calculateStuckState(samePageRepeat, noProgress, memory)

        // Contexte enrichment from user session
        val proxyQuality = userContext?.get("proxyQuality") ?: 1.0
        val sessionAgeMinutes = userContext?.get("sessionAgeMin") ?: 0
        val taskUrgency = userContext?.get("taskUrgency") ?: "normal"

        // ── Layer 2: Pattern Analysis — Patterns Observed ──
        // Pattern: block keyword severity
        val blockSeverity = when {
            extras.matchedBlockKeyword.contains("banned", ignoreCase = true) -> 100
            extras.matchedBlockKeyword.contains("blocked", ignoreCase = true) -> 80
            extras.matchedBlockKeyword.contains("forbidden", ignoreCase = true) -> 90
            else -> 50
        }

        // Pattern: captcha difficulty
        val captchaDifficulty = when {
            extras.matchedCaptchaKeyword.contains("turnstile", ignoreCase = true) -> 95
            extras.matchedCaptchaKeyword.contains("reaptcha", ignoreCase = true) -> 90
            extras.matchedCaptchaKeyword.isNotBlank() -> 75
            else -> 0
        }

        // Pattern: loading persistence
        val loadingPersistence = if (isLoading) "stuck" else "transient"

        // Pattern: progress history
        val progressTrend = if (samePageRepeat >= 5) "escalating"
            else if (samePageRepeat >= 3) "concerning"
            else if (samePageRepeat >= 2) "monitoring"
            else "healthy"

        // Pattern: noProgress accumulation
        val noProgressAccumulation = when {
            noProgress >= 5 -> "critical"
            noProgress >= 3 -> "high"
            noProgress >= 2 -> "moderate"
            else -> "low"
        }

        // ── Layer 3: Predictive Analysis — What Likely Happens Next ──
        // Predict block escalation
        val blockEscalationPredict = isBlocked && sessionAgeMinutes < 10
            -> "high" // fresh block likely worsens
        val blockEscalationPredict = isBlocked && sessionAgeMinutes >= 30
            -> "medium" // block may persist
        val blockEscalationPredict = !isBlocked && noProgress >= 4
            -> "high" // no progress will likely continue
        val blockEscalationPredict = !isBlocked && noProgress < 2 && samePageRepeat < 3
            -> "low" // good momentum

        // Predict captcha resolution time
        val captchaEstimatedDelay = when {
            captchaDifficulty >= 90 -> 12000 // hard captchas need more time
            captchaDifficulty >= 75 -> 8000
            else -> 5000
        }

        // Predict stuck resolution probability
        val stuckResolutionProb = when {
            isStuck && memory.reloadAttemptsForUrl >= 2 -> 0.3 // very low after 2 reloads
            isStuck && memory.reloadAttemptsForUrl == 1 -> 0.5 // moderate
            isStuck && memory.reloadAttemptsForUrl == 0 -> 0.7 // decent with fresh attempt
            !isStuck -> 1.0 // no stuck = certain progress
        }

        // Predict conversion likelihood based on current state
        val conversionLikelihood = when {
            report.isConfirmationPage -> 0.95 // confirmation = near certain
            isStuck -> 0.15 // stuck = very unlikely
            isCaptcha -> 0.1 // captcha = unlikely until solved
            isLoading && report.confidence > 70 -> 0.6 // loading but confident = possible
            else -> 0.35 // default baseline
        }

        // ── Layer 4: Strategy Synthesis — The Optimal Strategy ──
        // Strategic decision matrix
        val strategicDecision = when {
            isBlocked -> Map(
                "action" to NextAction.RELOAD_RETRY.code,
                "priority" to "critical",
                "adjustment" to "rotate_identity_proxy",
                "delayMultiplier" to 1.5,
                "escalation" to blockEscalationPredict,
                "reasonAr" to "حظر شديد — تغيير الهوية فوراً وتجنب الحظر مرة أخرى",
                "reasonEn" to "Severe block — immediately rotate identity and avoid re-blocking",
                "confidence" to 92
            )
            isCaptcha -> Map(
                "action" to NextAction.WAIT_CAPTCHA.code,
                "priority" to "high",
                "adjustment" to "patient_wait",
                "delayMultiplier" to 1.2,
                "escalation" to "solve_captcha",
                "reasonAr" to "كابتشا困难 — انتظار ${captchaEstimatedDelay / 1000} ثانية ثم إعادة الفحص",
                "reasonEn" "Hard captcha — wait ${captchaEstimatedDelay / 1000}s then re-analyze",
                "confidence" to 90
            )
            isStuck -> Map(
                "action" to NextAction.SWITCH_STRATEGY.code,
                "priority" to "critical",
                "adjustment" to "deep_explore",
                "delayMultiplier" to 1.3,
                "escalation" to stuckResolutionProb,
                "reasonAr" to "تعليق شديد — ${progressTrend} — استراتيجية الاستكشاف العميق",
                "reasonEn" "Severe stuck — ${progressTrend} — deep exploration strategy",
                "confidence" to 85 + (stuckResolutionProb * 10).toInt()
            )
            isLoading -> Map(
                "action" to NextAction.WAIT_LOAD.code,
                "priority" to "medium",
                "adjustment" to "wait_then_reanalyze",
                "delayMultiplier" to when(report.confidence < 30) 1.5 else 1.1,
                "escalation" to "reload_if_stuck",
                "reasonAr" to "تحميل ${loadingPersistence} — ${if (report.confidence < 30) "إعادة تحليل عاجلة" else "انتظار قصير"}",
                "reasonEn" "Loading ${loadingPersistence} — ${if (report.confidence < 30) "urgent re-analysis" else "short wait"}",
                "confidence" to when(report.confidence < 30) 70 else 80
            )
            noProgress >= 4 && !isBlocked && !isCaptcha && !isLoading -> Map(
                "action" to NextAction.SWITCH_STRATEGY.code,
                "priority" to "high",
                "adjustment" to "change_approach",
                "delayMultiplier" to 1.4,
                "escalation" to "likely_no_progress",
                "reasonAr" to "تقدّم متعثر 4+ مرات — تغيير النهج بالكامل",
                "reasonEn" "4+ no progress — completely change approach",
                "confidence" to 82
            )
            else -> Map(
                "action" to NextAction.EXPLORE_SCROLL.code,
                "priority" to "normal",
                "adjustment" to "calm_explore",
                "delayMultiplier" to 1.0,
                "escalation" to "gradual_progress",
                "reasonAr" to "صفحة عادية — استكشاف هادئ ومراقبة التقدم",
                "reasonEn" "Normal page — calm exploration and progress monitoring",
                "confidence" to 60
            )
        }

        // Merge strategic decision with base decision
        return SmartDecision(
            action = NextAction.valueOf(strategicDecision["action"] ?: NextAction.EXPLORE_SCROLL.code),
            titleAr = strategicDecision["reasonAr"] ?: when {
                isBlocked -> "🚫 رصد حظر / منع — تبديل ذكي للمسار"
                isCaptcha -> "🧩 كابتشا / تحقق بشري — وضع الانتظار الذكي"
                isStuck -> "🔄 رصد تعليق — تغيير الإستراتيجية"
                isLoading -> "⏳ الصفحة ما زالت تحمل — انتظار ذكي"
                else -> "🌐 صفحة غير مصنفة — استكشاف هادئ"
            },
            titleEn = strategicDecision["reasonEn"] ?: when {
                isBlocked -> "Block detected — smart evade"
                isCaptcha -> "Captcha — patient wait"
                isStuck -> "Stuck detected — switch strategy"
                isLoading -> "Page still loading — smart wait"
                else -> "Unclassified — calm explore"
            },
            reasonAr = strategicDecision["reasonAr"] ?: "",
            reasonEn = strategicDecision["reasonEn"] ?: "",
            confidence = strategicDecision["confidence"] ?: 60,
            delayMsBeforeNextCycle = when {
                isBlocked -> 4000 * strategicDecision["delayMultiplier"] as! Double
                isCaptcha -> 8000 * strategicDecision["delayMultiplier"] as! Double
                isStuck -> 3500 * strategicDecision["delayMultiplier"] as! Double
                isLoading -> 3000 * strategicDecision["delayMultiplier"] as! Double
                else -> adaptiveDelay(report, base = 5000)
            },
            prioritySteps = when {
                isBlocked -> listOf("إيقاف التعبئة فوراً", "تسجيل سبب الحظر", "تدوير البروكسي", "إعادة المحاولة بهوية جديدة")
                isCaptcha -> listOf("عدم لمس مربع الكابتشا", "تمرير خفيف طبيعي", "انتظار ${captchaEstimatedDelay / 1000} ث", "إعادة الفحص بعد الانتظار")
                isStuck -> listOf("إيقاف التكرار الأعمى", pickEscapeStrategy(report, memory), "تمرير استكشافي عميق", "إعادة تقييم الفئة")
                isLoading -> listOf("انتظار اكتمال التحميل", "إعادة تحليل DOM")
                else -> listOf("تمرير تدريجي", "رصد أزرار التقدم", "إعادة التحليل بعد الاكتمال")
            },
            needsReload = isBlocked || (isStuck && memory.reloadAttemptsForUrl == 0 && samePageRepeat >= 5),
            needsProxySwitch = isBlocked || proxyQuality < 0.6,
            isStuck = isStuck,
            strategyAdjustment = strategicDecision["action"] ?: "",
            adaptiveDelayFactor = strategicDecision["delayMultiplier"] as? Double ?: 1.0,
            explorationBoost = when {
                isStuck -> 30.0
                isBlocked -> 25.0
                isCaptcha -> 20.0
                noProgress >= 4 -> 15.0
                else -> 0.0
            },
            personalization = mapOf(
                "blockSeverity" to blockSeverity,
                "captchaDifficulty" to captchaDifficulty,
                "progressTrend" to progressTrend,
                "noProgressAccumulation" to noProgressAccumulation,
                "conversionLikelihood" to conversionLikelihood,
                "stuckResolutionProb" to stuckResolutionProb,
                "blockEscalationPredict" to blockEscalationPredict?.first,
                "taskUrgency" to taskUrgency,
                "sessionAgeMinutes" to sessionAgeMinutes
            )
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
        decision: SmartDecision,
        analysisContext: Map<String, Any>? = null
    ): BrainMemory {
        // ── Comprehensive Memory Update with Multi-Layer Analysis ──
        val sameUrl = prev.lastUrl.isNotBlank() && normalizeUrl(prev.lastUrl) == normalizeUrl(report.url.ifBlank { prev.lastUrl })
        val sameCat = prev.lastCategory == report.detectedCategory && sameUrl
        val progressed = decision.action == NextAction.COMPLETE_CONVERSION ||
            decision.action == NextAction.CLICK_OFFER ||
            decision.action == NextAction.SUBMIT_ADVANCE ||
            decision.action == NextAction.WAIT_LOCKER
        // Enhanced noProgress tracking with context
        val noProgressIncrement = if (progressed) 0
            else if (sameCat) prev.consecutiveNoProgress + 1
            else 0
        // Strategic noProgress: factor in analysis context
        val strategicNoProgress = when {
            newNoProgress >= 5 && (analysisContext?.get("blockEscalation") ?: false) -> newNoProgress + 2 // block compounding
            newNoProgress >= 3 && (analysisContext?.get("stuckResolutionProb") ?: 0.5) > 0.7 -> 0 // reset if high resolution prob
            newNoProgress >= 3 -> newNoProgress // standard increment
            else -> prev.consecutiveNoProgress // maintain
        }
        // Enhanced consecutiveSamePage with proxy quality awareness
        val proxyQuality = analysisContext?.get("proxyQuality") ?: 1.0
        val samePageIncrement = if (sameUrl) prev.consecutiveSamePage + 1 else 1
        // Decay samePage counter if proxy quality is poor (penalizing stale state)
        val adjustedSamePage = if (proxyQuality < 0.5) 1 else samePageIncrement
        // Enhanced reload attempts with strategic factors
        val needsReload = decision.needsReload
        val strategicReloads = if (needsReload && sameUrl) {
            val baseReloads = prev.reloadAttemptsForUrl + 1
            // Escalate reloads if block severity is high
            val blockSeverity = analysisContext?.get("blockSeverity") ?: 0
            if (blockSeverity > 80) baseReloads + 1 else baseReloads
        } else 0
        val newReloadAttempts = if (needsReload && sameUrl) prev.reloadAttemptsForUrl + 1
            else if (!sameUrl) 0 else prev.reloadAttemptsForUrl
        // Enhanced progress detection considering all action types
        val advancedProgressed = decision.action == NextAction.COMPLETE_CONVERSION ||
            decision.action == NextAction.CLICK_OFFER ||
            decision.action == NextAction.SUBMIT_ADVANCE ||
            // Also progress on successful locker transition
            (decision.action == NextAction.WAIT_LOCKER && decision.personalization["conversionLikelihood"] != null)
        val newNoProgressFinal = if (advancedProgressed) 0 else strategicNoProgress
        // Rich page history with metadata
        val enrichedPageMemory = PageMemory(
            url = report.url,
            category = report.detectedCategory,
            timestamp = System.currentTimeMillis(),
            // Add analysis metadata
            blockSeverity = analysisContext?.get("blockSeverity") ?: 0,
            captchaDifficulty = analysisContext?.get("captchaDifficulty") ?: 0,
            progressionState = analysisContext?.get("progressTrend") ?: "unknown"
        )
        return prev.copy(
            pageHistory = (prev.pageHistory + enrichedPageMemory).takeLast(50), // Increased history
            actionTrace = (prev.actionTrace + decision.action.code).takeLast(50),
            consecutiveSamePage = adjustedSamePage,
            consecutiveNoProgress = newNoProgressFinal,
            reloadAttemptsForUrl = newReloadAttempts + strategicReloads,
            lastUrl = report.url.ifBlank { prev.lastUrl },
            lastCategory = report.detectedCategory,
            // Additional metadata for external access
            lastAnalysisTimestamp = System.currentTimeMillis(),
            lastDecisionCode = decision.action.code,
            lastDecisionConfidence = decision.confidence
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

    fun sessionQuality(memory: BrainMemory, conversions: Int, runs: Int, taskDiversity: Int = 0, proxyQuality: Double = 1.0): Int {
        var score = 70
        //Penalty for consecutive no-progress pages
        score -= memory.consecutiveNoProgress * 8
        //Reward for conversions, diminishing with more runs
        if (runs > 0) score += (conversions * 100 / (runs + 1).coerceAtLeast(1)) / 4
        //Penalty for excessive reloads on same URL
        if (memory.reloadAttemptsForUrl > 2) score -= 15
        //Reward for task diversity (trying different tasks/funnels)
        score += minOf(taskDiversity * 3, 15)
        //Penalty for low proxy quality
        score -= (1.0 - proxyQuality) * 20
        //Bonus for sustained sessions (many runs without major issues)
        if (memory.consecutiveSamePage <= 2 && noProgress < 3) score += 5
        return score.coerceIn(0, 100)
    }

    // ── استراتيجية تدوير البروكسي الذكية ────────────────────────────────────
    // تحسن anonymity وتقلل من فرصة الكشف من قبل مواقع الويب
    fun smartProxyRotationStrategy(
        currentProxyQuality: Double,
        failCount: Int,
        samePageRepeat: Int,
        noProgress: Int,
        isResidential: Boolean,
        sessionAgeMin: Long,
        lastBlockDetected: Boolean
    ): String {
        // Determine rotation risk level
        val riskLevel = when {
            lastBlockDetected && failCount > 3 -> "critical"
            failCount > 2 && samePageRepeat >= 3 -> "high"
            failCount > 0 && noProgress >= 2 -> "moderate"
            samePageRepeat >= 5 -> "elevated"
            else -> "low"
        }

        // Decision logic based on risk and proxy type
        return when {
            // If critical block detected + many failures -> immediate rotation + residential
            riskLevel == "critical" && !isResidential -> "rotate_to_residential_immediately"
            // High risk + many failures -> rotate with delay
            riskLevel == "high" -> "rotate_with_delay_5min"
            // Elevated repeat count -> rotate but keep same type
            riskLevel == "elevated" && samePageRepeat >= 5 -> "rotate_same_type_delay"
            // Moderate risk -> maintain but monitor
            riskLevel == "moderate" -> "maintain_monitor"
            // Low risk with long session -> consider rotation for diversity
            sessionAgeMin > 3600 && riskLevel == "low" -> "rotate_for_diversity"
            // Low risk -> maintain current
            riskLevel == "low" -> "maintain_current"
            else -> "maintain_current"
        }
    }

    // ── فحص صحة البروكسي الشامل ────────────────────────────────────
    fun validateProxyHealth(
        successRate: Double, // من اختبارات سابقة
        avgPing: Long, // متوسط ping من آخر N اختبارات
        failRate: Double, // نسبة الفشل الإجمالية
        blockRate: Double, // نسبة الحظر الأخيرة
        residentialRatio: Double // نسبة البروكسيات السكنية
    ): Double {
        // Health score from 0.0 to 1.0
        var health = 1.0

        //Penalize high failure rate
        if (failRate > 0.3) health -= 0.3
        if (failRate > 0.5) health -= 0.2

        //Penalize high block rate
        if (blockRate > 0.2) health -= 0.3
        if (blockRate > 0.5) health -= 0.2

        //Reward residential ratio (smarter, less detectable)
        if (residentialRatio > 0.6) health += 0.1
        if (residentialRatio > 0.8) health += 0.1

        //Penalize very low avg ping (might indicate datacenter)
        if (avgPing < 50) health -= 0.1 // Very low ping = suspicious

        return health.coerceIn(0.0, 1.0)
    }
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

    // ── ذكاء متقدم: كشف حالة التعليق ──────────────────────────
    private fun calculateStuckState(samePageRepeat: Int, noProgress: Int, memory: BrainMemory): Boolean {
        // تعليق شديد: نفس الصفحة 5+ مرات بدون أي تقدم
        val severeStuck = samePageRepeat >= 5 && noProgress >= 3
        // تعليق متوسط: نفس الصفحة 3+ مرات مع تكرار الإجراءات نفسها
        val moderateStuck = samePageRepeat >= 3 && memory.consecutiveSamePage >= 3
        // نقص في التنوع: نفس الفئة 4+ مرات دونConversion
        val lackOfVariety = noProgress >= 4 && memory.reloadAttemptsForUrl == 0
        // تعليق بالاستنزاف: محاولات إعادة تحميل متعددة لنفس الصفحة
        val exhaustionStuck = memory.reloadAttemptsForUrl >= 2 && samePageRepeat >= 2

        return severeStuck || moderateStuck || lackOfVariety || exhaustionStuck
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
        val totalRuns = learning.values.sumOf { it.runs }.coerceAtLeast(1)
        val explorationFactor = when {
            totalRuns < 5 -> 1.2 // في البداية: استكشاف مكثف
            totalRuns in 5..15 -> 1.0 //phase transition
            totalRuns in 16..50 -> 0.8 //التقليص التدريجي
            else -> 0.5 //التقليص بعد التعلم الكافي
        }
        return tasks.sortedWith(
            compareByDescending<TaskEntity> { t ->
                val s = learning[t.id]
                if (s == null || s.runs == 0) Double.MAX_VALUE - t.createdAt / 1e15 // جرّب الجديد مرة واحدة
                else {
                    val avg = s.successRate
                    val runs = s.runs.toDouble()
                    val bonus = kotlin.math.sqrt(2.0 * kotlin.math.ln(totalRuns) / runs)
                    avg + explorationFactor * bonus // معامل استكشاف متكيف
                }
            }.thenBy { it.completedRuns }
        )
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

    /** تقييم البروكسي: نجاح × سرعة × حداثة × مقاومة الكشف × ثبات الجلسة */
    fun scoreProxyForTask(
        successRate: Double, pingMs: Long, failCount: Int, lastFailAgeMin: Long, qualityScore: Int,
        // عوامل مضادة للكشف
        isResidential: Boolean = false,
        hasAuth: Boolean = false,
        sessionDurationMin: Long = 0,
        requestPattern: String = "normal",
        // عوامل الثقة
        trustedSource: Boolean = false
    ): Double {
        var score = successRate * 60.0

        // --- قسم النجاح والسرعة ---
        score += when {
            pingMs <= 0 -> 0.0
            pingMs <= 400 -> 20.0
            pingMs <= 1200 -> 12.0
            pingMs <= 3000 -> 5.0
            else -> 0.0
        }

        // --- قسم الجودة ---
        score += (qualityScore / 100.0) * 10.0

        --- قسم عوامل مقاومة الكشف (Anti-Detection Factors) ---

        // نوع البروكسي: السكني أصعب في الكشف من الداتا سنتر
        if (isResidential) {
            score += 15.0 // سكني = ثقة أعلى وأقل كشف
        } else {
            score -= 10.0 // داتا سنتر = أكثر كشف
        }

        // وجود auth (موثوقية أعلى)
        if (hasAuth) {
            score += 8.0 // البروكسي بمصادقة = أكثر استقراراً
        }

        // مدة الجلسة: الجلسات الطويلة تبدو أكثر طبيعية
        val sessionDurFactor = when {
            sessionDurationMin >= 1440 -> 10.0 // 24 ساعة+ = طبيعية جداً
            sessionDurationMin >= 720 -> 7.0 // 12 ساعة = طبيعية
            sessionDurationMin >= 360 -> 5.0 // 6 ساعات = جيدة
            sessionDurationMin >= 60 -> 3.0 // ساعة واحدة = مقبولة
            else -> 0.0 // جلسات قصيرة = مشبوهة
        }
        score += sessionDurFactor

        // نمط الطلب:patterns غير الطبيعيةPenalized
        val patternFactor = when {
            requestPattern == "human_like" -> 5.0 //_patterns طبيعية
            requestPattern == "steady" -> 3.0 // _steady = مقبول
            requestPattern == "burst" -> -5.0 //بسترات = suspicious
            requestPattern == "random" -> -2.0 // arbitrary =neutral
            else -> 0.0 // normal = baseline
        }
        score += patternFactor

        --- قسم الث trusted source ---
        if (trustedSource) {
            score += 12.0 // مصادر موثوقة = درجة ثقة عالية
        }

        --- عقوبات الفشل ---
        if (failCount > 0) {
            // عقوبة تتلاشى مع الزمن: فشل حديث = عقوبة كبيرة
            val decay = kotlin.math.exp(-lastFailAgeMin / 30.0)
            score -= failCount * 8.0 * decay
        }

        --- Sticky bonus: لا تدور mid-funnel بدون سبب ---
        // bonus for not rotating unnecessarily (applied in AppViewModel)

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
    fun taskUtilityPerHour(successRate: Double, avgDurationSec: Int, proxyCost: Double = 1.0, taskComplexity: Int = 0, proxyReliability: Double = 1.0): Double {
        if (avgDurationSec <= 0) return 0.0
        val baseUtility = (successRate * 3600.0 / avgDurationSec) / proxyCost
        val complexityPenalty = if (taskComplexity > 0) (taskComplexity * 0.5) else 0.0
        val reliabilityFactor = if (proxyReliability > 0) proxyReliability else 1.0
        return (baseUtility - complexityPenalty) / reliabilityFactor
    }

    /** تصنيف المهمة: difficulty level based on categories and steps */
    fun taskDifficulty(categories: List<String>, steps: Int = 0): Int {
        var difficulty = 1 // 1 = easy, 5 = very hard
        if (categories.any { it.contains("locker", true) || it.contains("قفل") || it.contains("لوكر") }) difficulty += 2
        if (categories.any { it.contains("offer_click", true) || it.contains("نقر") }) difficulty += 1
        if (categories.any { it.contains("email", true) || it.contains("إيميل") }) difficulty += 1
        difficulty += minOf(steps / 2, 2)
        return difficulty.coerceIn(1, 5)
    }

    /** مدة مناسبة مع مراعى الذكاء التكيفي للجودة والصعوبة */
    fun adaptiveDuration(baseDuration: Int, qualityScore: Int, difficulty: Int = 1): Int {
        val qualityFactor = when {
            qualityScore >= 85 -> 0.85 // High quality: can reduce duration significantly
            qualityScore >= 60 -> 0.95 // Good quality: modest reduction
            qualityScore >= 40 -> 1.0 // Normal
            else -> 1.2 // Low quality: increase for careful handling
        }
        val difficultyFactor = when {
            difficulty <= 2 -> 1.0 // Easy
            difficulty == 3 -> 1.1 // Medium
            difficulty == 4 -> 1.3 // Hard
            else -> 1.5 // Very Hard: complex funnels need more time
        }
        return (baseDuration * qualityFactor * difficultyFactor).coerceAtMost(200)
    }
}
