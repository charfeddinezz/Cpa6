package com.example.service

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

data class CategoryDefinition(
    val id: String,
    val labelEn: String,
    val labelAr: String,
    val emoji: String,
    val priority: Int, // Lower number executes earlier in funnel
    val description: String,
    val matchingKeywords: List<String>
)

data class PlannedStep(
    val order: Int,
    val id: String,
    val labelEn: String,
    val labelAr: String,
    val emoji: String,
    val priority: Int,
    val description: String
)

data class ExtractedPlanResult(
    val detectedName: String,
    val targetUrl: String,
    val categories: List<String>,
    val orderedSteps: List<PlannedStep>,
    val summary: String,
    val recommendedMode: String, // "mode1", "mode2", "mode3"
    val recommendedDuration: Int,
    val recommendedKeywords: String,
    val recommendedReferer: String,
    val explanationAr: String,
    val explanationEn: String
)

data class PresetOfferPlan(
    val id: String,
    val name: String,
    val url: String,
    val categories: String,
    val mode: String,
    val duration: Int,
    val completionKeywords: String,
    val descriptionAr: String,
    val descriptionEn: String
)

object TaskCategoryPlanner {

    val PRESET_CATEGORIES = listOf(
        CategoryDefinition(
            id = "offer_click",
            labelEn = "Offer Click",
            labelAr = "النقرة على العرض",
            emoji = "🖱️",
            priority = 1, // ALWAYS #1 in execution order: auto-clicks designated text/button to redirect to the offer site
            description = "Scan landing page for specified target text (e.g., 'Get $1000 Walmart gift card'), auto-click to transfer to the main destination offer site",
            matchingKeywords = listOf("offer click", "click offer", "النقرة على العرض", "النقرة", "نقرة", "click", "نقر", "offer_click", "عرض")
        ),
        CategoryDefinition(
            id = "email_submit",
            labelEn = "Email Submit",
            labelAr = "إدخال البريد",
            emoji = "✉️",
            priority = 10,
            description = "Detect single email input field on landing page, enter email, and proceed",
            matchingKeywords = listOf("email", "email submit", "mail", "بريد", "ايميل")
        ),
        CategoryDefinition(
            id = "zip_submit",
            labelEn = "Zip Submit",
            labelAr = "رمز بريدي",
            emoji = "📍",
            priority = 20,
            description = "Detect postal/zip code input for geo-targeting and submit",
            matchingKeywords = listOf("zip", "postal", "zip submit", "رمز بريدي", "كود بريدي")
        ),
        CategoryDefinition(
            id = "lead_gen",
            labelEn = "Lead Gen Form",
            labelAr = "استمارة بيانات",
            emoji = "📋",
            priority = 30,
            description = "Fill complete contact info (first & last name, address, phone, city, state)",
            matchingKeywords = listOf("lead", "lead gen", "form", "contact", "بيانات", "استمارة")
        ),
        CategoryDefinition(
            id = "survey_quiz",
            labelEn = "Survey / Quiz",
            labelAr = "استبيان وأسئلة",
            emoji = "📝",
            priority = 40,
            description = "Answer qualification questions, select interactive button answers and radios",
            matchingKeywords = listOf("survey", "quiz", "questions", "poll", "استبيان", "اسئلة", "كويز")
        ),
        CategoryDefinition(
            id = "skip_upsells",
            labelEn = "Skip Upsells",
            labelAr = "تخطي العروض",
            emoji = "⏭️",
            priority = 50,
            description = "Identify sponsored co-reg offers and click 'No Thanks', 'Skip', 'Not interested'",
            matchingKeywords = listOf("skip", "upsell", "no thanks", "sponsor", "تخطي", "عروض اضافية")
        ),
        CategoryDefinition(
            id = "sign_up",
            labelEn = "Sign Up",
            labelAr = "تسجيل حساب",
            emoji = "👤",
            priority = 60,
            description = "Fill account registration fields, generate secure password, and submit register",
            matchingKeywords = listOf("sign up", "signup", "register", "create account", "تسجيل", "انشاء حساب")
        ),
        CategoryDefinition(
            id = "pin_submit",
            labelEn = "PIN Submit",
            labelAr = "تأكيد الهاتف",
            emoji = "📱",
            priority = 70,
            description = "Enter mobile phone number and prepare for carrier verification / SMS",
            matchingKeywords = listOf("pin", "pin submit", "sms", "phone verify", "تاكيد هاتف", "رقم الهاتف")
        ),
        CategoryDefinition(
            id = "terms_agreement",
            labelEn = "Terms Agreement",
            labelAr = "موافقة الشروط",
            emoji = "📜",
            priority = 80,
            description = "Ensure terms, privacy policy, and 18+ majority age checkboxes are checked",
            matchingKeywords = listOf("terms", "agree", "checkbox", "18", "شروط", "موافقة")
        ),
        CategoryDefinition(
            id = "sweepstakes",
            labelEn = "Sweepstakes Claim",
            labelAr = "مسابقات وجوائز",
            emoji = "🎁",
            priority = 15,
            description = "Sweepstakes entry, gift card claim verification and prize draw opt-in",
            matchingKeywords = listOf("sweepstakes", "sweep", "prize", "gift card", "reward", "win", "draw", "مسابقة", "سحب", "جائزة", "بطاقة هدية")
        ),
        CategoryDefinition(
            id = "app_install",
            labelEn = "App Install",
            labelAr = "تثبيت التطبيقات",
            emoji = "📲",
            priority = 35,
            description = "Detect app store / APK download redirect buttons and trigger install flow",
            matchingKeywords = listOf("install", "download", "app", "play store", "apk", "تثبيت", "تنزيل", "تطبيق")
        ),
        CategoryDefinition(
            id = "financial_quote",
            labelEn = "Financial & Insurance",
            labelAr = "التأمين والقروض",
            emoji = "💼",
            priority = 48,
            description = "Multi-step quote funnels (auto insurance, life, personal loans, solar energy)",
            matchingKeywords = listOf("insurance", "quote", "loan", "mortgage", "solar", "credit", "قرض", "تأمين", "عرض سعر")
        ),
        CategoryDefinition(
            id = "gaming_reward",
            labelEn = "Gaming & Reward",
            labelAr = "ألعاب ومكافآت",
            emoji = "🎮",
            priority = 45,
            description = "Gaming reward portals, unlock coins/tokens, level completion and play bonuses",
            matchingKeywords = listOf("game", "gaming", "play", "coins", "points", "reward zone", "لعبة", "العاب", "نقاط")
        ),
        CategoryDefinition(
            id = "ecommerce_trial",
            labelEn = "Free Trial & Shipping",
            labelAr = "تجربة مجانية وشحن",
            emoji = "📦",
            priority = 75,
            description = "Sample trial checkout (shipping and handling address entry)",
            matchingKeywords = listOf("trial", "sample", "free sample", "shipping", "handling", "تجربة", "عينة")
        ),
        CategoryDefinition(
            id = "content_locker",
            labelEn = "Content / Link Locker",
            labelAr = "قفل المحتوى والروابط",
            emoji = "🔒",
            priority = 5,
            description = "Detect link locker or offer wall, trigger unlock script or select unlocked task",
            matchingKeywords = listOf("locker", "content locker", "link locker", "unlock", "locked", "قفل", "فك القفل", "رابط مقفل")
        ),
        CategoryDefinition(
            id = "push_subscribe",
            labelEn = "Push Notifications",
            labelAr = "اشتراك الإشعارات",
            emoji = "🔔",
            priority = 8,
            description = "Detect push notification prompt, allow or bypass alert to proceed to offer",
            matchingKeywords = listOf("push", "notification", "subscribe", "allow", "alert", "اشعار", "اشعارات", "اشتراك")
        ),
        CategoryDefinition(
            id = "crypto_faucet",
            labelEn = "Crypto & Airdrop",
            labelAr = "عملات رقمية وإيردروب",
            emoji = "🪙",
            priority = 42,
            description = "Claim free crypto reward, submit wallet address or airdrop entry",
            matchingKeywords = listOf("crypto", "faucet", "airdrop", "bitcoin", "usdt", "wallet", "claim crypto", "عملات", "محفظة", "بيتكوين")
        ),
        CategoryDefinition(
            id = "dating_casual",
            labelEn = "Dating & Social",
            labelAr = "تعارف وتواصل",
            emoji = "❤️",
            priority = 43,
            description = "Age 18+ check, preference quiz, location radius and matching profile submit",
            matchingKeywords = listOf("dating", "meet", "single", "chat", "match", "casual", "تعارف", "لقاء", "علاقات")
        ),
        CategoryDefinition(
            id = "coupons_deals",
            labelEn = "Coupons & Discounts",
            labelAr = "كوبونات وتخفيضات",
            emoji = "🏷️",
            priority = 32,
            description = "Reveal discount coupon code, click 'Get Deal' or 'Copy & Continue'",
            matchingKeywords = listOf("coupon", "promo", "discount", "code", "deal", "voucher", "كوبون", "خصم", "تخفيض")
        ),
        CategoryDefinition(
            id = "job_application",
            labelEn = "Remote Work & Jobs",
            labelAr = "وظائف وعمل عن بعد",
            emoji = "💼",
            priority = 47,
            description = "Employment questionnaire, schedule availability, and career lead submit",
            matchingKeywords = listOf("job", "career", "work", "remote", "hire", "employment", "وظيفة", "عمل عن بعد", "توظيف")
        ),
        CategoryDefinition(
            id = "credit_score",
            labelEn = "Credit Score & Report",
            labelAr = "فحص السجل الائتماني",
            emoji = "📊",
            priority = 49,
            description = "Credit score tier selection, free credit check qualification and report lookup",
            matchingKeywords = listOf("credit score", "credit report", "credit", "score", "ائتمان", "سجل ائتماني", "نقاط الائتمان")
        ),
        CategoryDefinition(
            id = "instant_scratch",
            labelEn = "Instant Win & Wheel",
            labelAr = "عجلة الحظ وربح فوري",
            emoji = "🎡",
            priority = 16,
            description = "Spin reward wheel, scratch virtual card, and instant prize claim",
            matchingKeywords = listOf("spin", "wheel", "scratch", "instant win", "lucky", "عجلة", "حك", "ربح فوري", "دولاب")
        ),
        CategoryDefinition(
            id = "software_download",
            labelEn = "Software & Utilities",
            labelAr = "تحميل برامج وتطبيقات",
            emoji = "💾",
            priority = 38,
            description = "Trigger installer download button, bypass bundled offers and start setup",
            matchingKeywords = listOf("download software", "installer", "setup", "exe", "dmg", "تحميل برنامج", "تنزيل برنامج", "تثبيت البرنامج")
        ),
        CategoryDefinition(
            id = "video_stream",
            labelEn = "Video & Watch",
            labelAr = "مشاهدة فيديو ومكافأة",
            emoji = "▶️",
            priority = 36,
            description = "Start video player, watch sponsored clip and confirm view credit",
            matchingKeywords = listOf("video", "watch", "stream", "clip", "فيديو", "مشاهدة", "مقطع")
        ),
        CategoryDefinition(
            id = "completion_confirm",
            labelEn = "Confirmation",
            labelAr = "تأكيد الإكمال",
            emoji = "🏆",
            priority = 90,
            description = "Confirm reward claim, detect thank-you/success receipt and finalize conversion",
            matchingKeywords = listOf("confirm", "claim", "thank you", "complete", "تاكيد", "مكافأة")
        )
    )

    val PRESET_OFFER_PLANS = listOf(
        PresetOfferPlan(
            id = "plan_gdfqo_blogspot",
            name = "Gdfqo Blogspot Landing Page (User CPA Offer)",
            url = "https://gdfqo.blogspot.com",
            categories = "Content / Link Locker, Offer Click, Email Submit, Terms Agreement, Survey / Quiz, Lead Gen Form, Skip Upsells, Confirmation",
            mode = "mode1",
            duration = 55,
            completionKeywords = "thank you, congratulations, success, confirmed, completed, verified, reward",
            descriptionAr = "خطة موقعك (gdfqo.blogspot.com): تعتمد قمع اللوكر والنقر التلقائي؛ انتظار ظهور اللوكر، النقر على العرض المناسب باستخدام نصوص شاشة Offer Click، فتح العرض تلقائياً في تبويب جديد والانتقال للعمل فيه لإكمال التحويل.",
            descriptionEn = "Your Blogspot funnel: Combines Locker + Offer Click; waits for locker, auto-clicks matching offer using Offer Click keywords, opens target offer in a new tab and transitions work context to it."
        ),
        PresetOfferPlan(
            id = "plan_ctc_100gc",
            name = "ConsumerTestConnect ($100 GC)",
            url = "https://consumertestconnect.com/ctc-100gcsweep",
            categories = "Email Submit, Terms Agreement, Survey / Quiz, Lead Gen Form, Skip Upsells, Confirmation",
            mode = "mode1",
            duration = 45,
            completionKeywords = "thank you, congratulations, success, confirmed, sweepstakes, completed",
            descriptionAr = "خطة متكاملة تشمل البريد، الموافقة على الشروط وتأكيد السن (18+)، استبيان التأهيل، ملء الاستمارة، وتخطي العروض الدعائية تلقائياً للوصول للتحويل.",
            descriptionEn = "Complete funnel: Email opt-in, 18+ terms agreement, qualification survey, lead gen contact info, upsell wall bypass, and claim confirmation."
        ),
        PresetOfferPlan(
            id = "plan_amazon_500",
            name = "Amazon $500 Card Survey",
            url = "https://nationalconsumercenter.com/amazon-500-sweep",
            categories = "Email Submit, Survey / Quiz, Terms Agreement, Lead Gen Form, Skip Upsells, Confirmation",
            mode = "mode3",
            duration = 50,
            completionKeywords = "thank you, congratulations, success, claim, verified, reward",
            descriptionAr = "خطة قسيمة أمازون: إدخال البريد، أسئلة التسوق المفضلة، تعبئة بيانات الشحن وتأكيد المطالبة الذكي.",
            descriptionEn = "Amazon Gift Card: Email submission, shopping preference quiz, address demographic submission, and keyword verification."
        ),
        PresetOfferPlan(
            id = "plan_cashapp_750",
            name = "Cash App $750 Fast Reward",
            url = "https://rewardzoneusa.com/cash-app-750-reward",
            categories = "Email Submit, Terms Agreement, Survey / Quiz, Skip Upsells, Confirmation",
            mode = "mode1",
            duration = 40,
            completionKeywords = "thank you, completed, reward credited, congratulations, transfer",
            descriptionAr = "مسار Cash App السريع: تسجيل البريد، تخطي عروض الشركات الراعية، وإتمام التأكيد التلقائي.",
            descriptionEn = "Cash App reward path: Email submission, terms consent, sponsor deal bypass, and instant completion verification."
        ),
        PresetOfferPlan(
            id = "plan_sweep_win",
            name = "US Sweepstakes Prize Draw ($1,500)",
            url = "https://instant-rewards.us/sweep-entry",
            categories = "Sweepstakes Claim, Email Submit, Zip Submit, Survey / Quiz, Skip Upsells, Confirmation",
            mode = "mode1",
            duration = 45,
            completionKeywords = "entry confirmed, congratulations, official rules, ticket number",
            descriptionAr = "مسار مسابقات الجوائز: فحص الرمز البريدي، تسجيل البريد، استبيان المستهلكين، وتأكيد رقم تذكرة السحب.",
            descriptionEn = "Sweepstakes Funnel: Postal code entry, email registration, demographic quiz, and draw ticket verification."
        ),
        PresetOfferPlan(
            id = "plan_quote_lead",
            name = "Homeowners Solar & Insurance Quote",
            url = "https://quote-generator.us/solar-quote-lead",
            categories = "Zip Submit, Financial & Insurance, Lead Gen Form, Survey / Quiz, Terms Agreement, Confirmation",
            mode = "mode1",
            duration = 55,
            completionKeywords = "quote ready, thank you, representative, estimate, confirmed",
            descriptionAr = "مسار عروض التأمين والطاقة: فحص الرمز البريدي أولاً، ملء استمارة بيانات العقار، وتأكيد طلب التقدير.",
            descriptionEn = "Quote funnel: ZIP targeting, property details questionnaire, contact info lead generation, and quote confirmation."
        )
    )

    data class PageAnalysisReport(
        val url: String = "",
        val title: String = "",
        val detectedCategory: String = "general",
        val detectedCategoryAr: String = "عام",
        val confidence: Int = 50,
        val summary: String = "",
        val fieldsCount: Int = 0,
        val emailFields: Int = 0,
        val textFields: Int = 0,
        val passwordFields: Int = 0,
        val checkboxesCount: Int = 0,
        val radioGroupsCount: Int = 0,
        val selectCount: Int = 0,
        val buttonsCount: Int = 0,
        val hasSkipButtons: Boolean = false,
        val hasLocker: Boolean = false,
        val hasOfferClickCandidate: Boolean = false,
        val isConfirmationPage: Boolean = false,
        val recommendedNextAction: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )

    data class TacticalDirective(
        val archetype: String, // "LOCKER", "SURVEY", "LEAD_FORM", "UPSELL", "CONFIRMATION", "GENERAL"
        val titleAr: String,
        val titleEn: String,
        val reasonAr: String,
        val actionPlanSteps: List<String>,
        val priorityCode: Int
    )

    fun deriveTacticalDirective(
        report: PageAnalysisReport,
        configuredCategories: List<String>,
        activeClickTarget: String?
    ): TacticalDirective {
        val hasLockerCombo = isLockerOfferClickCombo(configuredCategories) ||
                report.hasLocker ||
                report.detectedCategory.lowercase().contains("locker")

        return when {
            report.isConfirmationPage || report.detectedCategory.lowercase().contains("confirm") -> {
                TacticalDirective(
                    archetype = "CONFIRMATION",
                    titleAr = "🏆 تأكيد التحويل وحفظ صفحة الإنجاز",
                    titleEn = "Confirm Lead & Preserve Conversion Screen",
                    reasonAr = "تم رصد رسالة الشكر والإكمال في الصفحة بنجاح. التحليل يثبت تسجيل التحويل.",
                    actionPlanSteps = listOf(
                        "التحقق من كلمات الشكر والإنجاز",
                        "تثبيت صفحة المتصفح للمعاينة البصرية",
                        "تسجيل الإحصائيات في قاعدة البيانات",
                        "إنهاء التكرار بنجاح وبدء التبريد"
                    ),
                    priorityCode = 1
                )
            }
            hasLockerCombo -> {
                val targetText = activeClickTarget ?: "العرض الأول المتاح"
                TacticalDirective(
                    archetype = "LOCKER",
                    titleAr = "🔒 مراقبة اللوكر والنقر التلقائي لفتح العرض في تبويب جديد",
                    titleEn = "Solve Locker & Auto-Click Offer into New Tab",
                    reasonAr = "تم رصد إطار/نافذة Content Locker. التوجيه هو مطابقة نصوص شاشة Offer Click، النقر على '$targetText'، ونقل مسار العمل فورياً إلى التبويب الجديد.",
                    actionPlanSteps = listOf(
                        "مراقبة استجابة وظهور إطار اللوكر",
                        "مسح قائمة العروض ومقارنتها بنصوص Offer Click",
                        "النقر التلقائي على العرض المستهدف ($targetText)",
                        "اعتراض الرابط وفتحه تلقائياً في تبويب جديد",
                        "تحويل تركيز الأتمتة فوراً إلى تبويب العرض الجديد"
                    ),
                    priorityCode = 2
                )
            }
            report.hasSkipButtons || report.detectedCategory.lowercase().contains("skip") -> {
                TacticalDirective(
                    archetype = "UPSELL",
                    titleAr = "⚡ تخطي العروض الترويجية الإضافية (Skip Upsells)",
                    titleEn = "Bypass Co-Reg / Upsell Sponsor Wall",
                    reasonAr = "تم كشف عروض ترويجية مشروطة بالدفع. التوجيه التكتيكي هو النقر الفوري على No Thanks / Skip لتجاوز الجدار دون دفع.",
                    actionPlanSteps = listOf(
                        "تحديد عناصر وأزرار التخطي (No Thanks / Skip)",
                        "النقر التلقائي وتجاوز العرض الترويجي",
                        "التقدم إلى المرحلة الأساسية التالية"
                    ),
                    priorityCode = 3
                )
            }
            report.radioGroupsCount >= 1 || report.selectCount >= 1 || report.detectedCategory.lowercase().contains("survey") -> {
                TacticalDirective(
                    archetype = "SURVEY",
                    titleAr = "📝 حل استبيان الأسئلة والتأهيل التلقائي",
                    titleEn = "Qualify Demographics & Answer Survey",
                    reasonAr = "تم كشف استبيان تفاعلي (${report.radioGroupsCount} مجموعة خيارات). التوجيه هو الإجابة بنمط متسق مع سن 18+ والإقامة المؤهلة.",
                    actionPlanSteps = listOf(
                        "تحليل نص وسياق السؤال الحالي",
                        "اختيار الإجابة الأكثر مطابقة لشروط التأهل",
                        "تفعيل أحداث التغيير والنقر (Click & Change)",
                        "المتابعة والانتقال للسؤال التالي"
                    ),
                    priorityCode = 4
                )
            }
            report.textFields >= 1 || report.emailFields >= 1 || report.detectedCategory.lowercase().contains("lead") || report.detectedCategory.lowercase().contains("email") -> {
                TacticalDirective(
                    archetype = "LEAD_FORM",
                    titleAr = "📋 تعبئة نموذج البيانات والاشتراك من شاشة المعلومات",
                    titleEn = "Fill Lead Form from InfoScreen Persona",
                    reasonAr = "تم كشف نموذج تسجيل/طلب (${report.fieldsCount} حقول). التوجيه هو ضخ بيانات الهوية المتكاملة من شاشة المعلومات والموافقة على الشروط.",
                    actionPlanSteps = listOf(
                        "فحص نوع ومطابقة كل حقل إدخال (اسم، إيميل، هاتف، عنوان)",
                        "ضخ بيانات الهوية المحددة وتحديث الـ Trackers",
                        "الموافقة على مربعات الشروط والأحكام (Terms & Conditions)",
                        "النقر على زر المتابعة والإرسال (Submit / Continue)"
                    ),
                    priorityCode = 5
                )
            }
            else -> {
                TacticalDirective(
                    archetype = "GENERAL",
                    titleAr = "🌐 محاكاة التصفح واستكشاف عناصر التقدم",
                    titleEn = "Natural Browsing & Progress Exploration",
                    reasonAr = "صفحة هبوط تفاعلية. التوجيه هو محاكاة السلوك البشري بالتمرير واستكشاف الأزرار لتحريك المسار.",
                    actionPlanSteps = listOf(
                        "التمرير الطبيعي لأسفل وأعلى الصفحة",
                        "فحص عناصر التحويل المتاحة",
                        "النقر على زر المتابعة والانتقال"
                    ),
                    priorityCode = 6
                )
            }
        }
    }

    fun parseAnalysisReport(rawJson: String): PageAnalysisReport {
        return try {
            val obj = JSONObject(rawJson)
            val cat = obj.optString("detectedCategory", "general")
            val catDef = findDefinition(cat)
            PageAnalysisReport(
                url = obj.optString("url", ""),
                title = obj.optString("title", ""),
                detectedCategory = catDef?.labelEn ?: cat,
                detectedCategoryAr = catDef?.labelAr ?: cat,
                confidence = obj.optInt("confidence", 70),
                summary = obj.optString("summary", "Page analyzed"),
                fieldsCount = obj.optInt("fieldsCount", 0),
                emailFields = obj.optInt("emailFields", 0),
                textFields = obj.optInt("textFields", 0),
                passwordFields = obj.optInt("passwordFields", 0),
                checkboxesCount = obj.optInt("checkboxesCount", 0),
                radioGroupsCount = obj.optInt("radioGroupsCount", 0),
                selectCount = obj.optInt("selectCount", 0),
                buttonsCount = obj.optInt("buttonsCount", 0),
                hasSkipButtons = obj.optBoolean("hasSkipButtons", false),
                hasLocker = obj.optBoolean("hasLocker", false),
                hasOfferClickCandidate = obj.optBoolean("hasOfferClickCandidate", false),
                isConfirmationPage = obj.optBoolean("isConfirmationPage", false),
                recommendedNextAction = obj.optString("recommendedNextAction", "Continue automation flow"),
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            PageAnalysisReport(summary = "Analysis parsed with fallback")
        }
    }

    fun parseCategories(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun findDefinition(categoryName: String): CategoryDefinition? {
        val normalized = categoryName.trim().lowercase()
        return PRESET_CATEGORIES.firstOrNull { def ->
            def.id == normalized ||
            def.labelEn.lowercase() == normalized ||
            def.labelAr.lowercase() == normalized ||
            def.matchingKeywords.any { kw -> normalized.contains(kw) }
        }
    }

    /**
     * Checks if the task categories combine both Offer Click and Content Locker.
     * When both are present, the AI automator coordinates:
     * 1. Waiting for the Locker to appear / activating it.
     * 2. Auto-clicking a matching locker offer using Offer Click screen texts.
     * 3. Intercepting and opening the offer site in a new tab.
     * 4. Seamlessly transitioning the active working context to the new tab.
     */
    fun isLockerOfferClickCombo(categories: List<String>): Boolean {
        if (categories.isEmpty()) return false
        val hasClick = categories.any { cat ->
            val norm = cat.trim().lowercase()
            norm == "offer_click" || norm == "offer click" || norm.contains("click") || norm.contains("نقرة") || norm.contains("نقر") || norm == "عرض"
        }
        val hasLocker = categories.any { cat ->
            val norm = cat.trim().lowercase()
            norm == "content_locker" || norm == "locker" || norm.contains("locker") || norm.contains("لوكر") || norm.contains("قفل")
        }
        return hasClick && hasLocker
    }

    /**
     * Intelligently arranges and orders the user-supplied categories into
     * an optimal CPA conversion funnel execution pipeline.
     */
    fun orderCategories(categories: List<String>): List<PlannedStep> {
        if (categories.isEmpty()) return emptyList()

        if (isLockerOfferClickCombo(categories)) {
            val lockerStep = PlannedStep(
                order = 1,
                id = "wait_locker",
                labelEn = "Wait for Locker & Detection",
                labelAr = "انتظار ورصد اللوكر",
                emoji = "🔒",
                priority = 1,
                description = "انتظار ظهور لوكر العروض (Content Locker / Script / Overlay) وتنشيطه تلقائياً وفك حظر document.write"
            )
            val clickStep = PlannedStep(
                order = 2,
                id = "click_locker_offer",
                labelEn = "Click Matching Locker Offer",
                labelAr = "نقر عرض اللوكر التلقائي",
                emoji = "🖱️",
                priority = 2,
                description = "فحص عروض اللوكر واختيار العرض المطابق لنصوص شاشة Offer Click والنقر التلقائي عليه"
            )
            val tabStep = PlannedStep(
                order = 3,
                id = "new_tab_transition",
                labelEn = "Open in New Tab & Switch Work",
                labelAr = "فتح العرض بتبويب جديد ونقل العمل",
                emoji = "🌐",
                priority = 3,
                description = "فتح موقع العرض تلقائياً في تبويب جديد منفصل ونقل تركيز وبيئة العمل التلقائية إليه"
            )

            val remainingUserCats = categories.filterNot { cat ->
                val norm = cat.trim().lowercase()
                norm == "offer_click" || norm == "offer click" || norm.contains("click") || norm.contains("نقرة") || norm.contains("نقر") ||
                norm == "content_locker" || norm == "locker" || norm.contains("locker") || norm.contains("لوكر") || norm.contains("قفل")
            }

            val otherSteps = remainingUserCats.mapNotNull { userCat ->
                val def = findDefinition(userCat)
                if (def != null) {
                    PlannedStep(
                        order = 0,
                        id = def.id,
                        labelEn = def.labelEn,
                        labelAr = def.labelAr,
                        emoji = def.emoji,
                        priority = def.priority,
                        description = "${def.description} (في التبويب الجديد للعرض)"
                    )
                } else null
            }.sortedBy { it.priority }

            return (listOf(lockerStep, clickStep, tabStep) + otherSteps).mapIndexed { idx, s ->
                s.copy(order = idx + 1)
            }
        }

        // Match each user category to known definitions or assign dynamic priority
        val matchedList = categories.map { userCat ->
            val def = findDefinition(userCat)
            if (def != null) {
                PlannedStep(
                    order = 0,
                    id = def.id,
                    labelEn = def.labelEn,
                    labelAr = def.labelAr,
                    emoji = def.emoji,
                    priority = def.priority,
                    description = def.description
                )
            } else {
                // Custom user category
                val inferredPriority = when {
                    userCat.contains("click", ignoreCase = true) || userCat.contains("نقرة", ignoreCase = true) || userCat.contains("نقر", ignoreCase = true) -> 1
                    userCat.contains("locker", ignoreCase = true) || userCat.contains("قفل", ignoreCase = true) -> 5
                    userCat.contains("push", ignoreCase = true) || userCat.contains("اشعار", ignoreCase = true) -> 8
                    userCat.contains("email", ignoreCase = true) || userCat.contains("mail", ignoreCase = true) -> 10
                    userCat.contains("scratch", ignoreCase = true) || userCat.contains("wheel", ignoreCase = true) || userCat.contains("spin", ignoreCase = true) -> 16
                    userCat.contains("zip", ignoreCase = true) || userCat.contains("postal", ignoreCase = true) -> 20
                    userCat.contains("coupon", ignoreCase = true) || userCat.contains("deal", ignoreCase = true) || userCat.contains("خصم", ignoreCase = true) -> 32
                    userCat.contains("form", ignoreCase = true) || userCat.contains("info", ignoreCase = true) || userCat.contains("lead", ignoreCase = true) -> 30
                    userCat.contains("video", ignoreCase = true) || userCat.contains("watch", ignoreCase = true) -> 36
                    userCat.contains("download", ignoreCase = true) || userCat.contains("software", ignoreCase = true) -> 38
                    userCat.contains("survey", ignoreCase = true) || userCat.contains("quiz", ignoreCase = true) -> 40
                    userCat.contains("crypto", ignoreCase = true) || userCat.contains("wallet", ignoreCase = true) -> 42
                    userCat.contains("dating", ignoreCase = true) || userCat.contains("تعارف", ignoreCase = true) -> 43
                    userCat.contains("job", ignoreCase = true) || userCat.contains("work", ignoreCase = true) -> 47
                    userCat.contains("credit", ignoreCase = true) || userCat.contains("score", ignoreCase = true) -> 49
                    userCat.contains("skip", ignoreCase = true) || userCat.contains("pass", ignoreCase = true) -> 50
                    userCat.contains("sign", ignoreCase = true) || userCat.contains("reg", ignoreCase = true) -> 60
                    userCat.contains("pin", ignoreCase = true) || userCat.contains("sms", ignoreCase = true) -> 70
                    userCat.contains("term", ignoreCase = true) || userCat.contains("agree", ignoreCase = true) -> 80
                    userCat.contains("confirm", ignoreCase = true) || userCat.contains("complete", ignoreCase = true) -> 90
                    else -> 55 // Default mid-priority
                }
                PlannedStep(
                    order = 0,
                    id = userCat.lowercase().replace(" ", "_"),
                    labelEn = userCat,
                    labelAr = userCat,
                    emoji = "🎯",
                    priority = inferredPriority,
                    description = "Custom automated step for '$userCat'"
                )
            }
        }

        // Sort by funnel priority and re-index
        return matchedList
            .distinctBy { it.id }
            .sortedBy { it.priority }
            .mapIndexed { index, step ->
                step.copy(order = index + 1)
            }
    }

    /**
     * Builds a human-readable summary of the AI execution plan.
     * E.g.: "1. ✉️ Email Submit ➔ 2. 📝 Survey / Quiz ➔ 3. 👤 Sign Up"
     */
    fun formatPlanSummary(categories: List<String>): String {
        val steps = orderCategories(categories)
        if (steps.isEmpty()) return "Standard Full-Auto Flow"
        return steps.joinToString(" ➔ ") { "${it.order}. ${it.emoji} ${it.labelEn}" }
    }

    /**
     * Builds a JSON string representing the ordered plan for JavaScript automation injection.
     */
    fun buildPlanJson(categories: List<String>): String {
        val steps = orderCategories(categories)
        val jsonArray = JSONArray()
        for (s in steps) {
            val obj = JSONObject()
            obj.put("order", s.order)
            obj.put("id", s.id)
            obj.put("label", s.labelEn)
            obj.put("priority", s.priority)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    /**
     * Extracts and optimizes the CPA conversion funnel plan from ANY URL or offer link.
     * Performs comprehensive domain, path, query and archetype analysis.
     */
    fun extractFunnelPlanFromUrl(rawUrl: String): ExtractedPlanResult {
        var cleanUrl = rawUrl.trim()
        if (cleanUrl.isNotBlank() && !cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        var host = ""
        var path = ""
        var query = ""
        try {
            val uri = URI.create(cleanUrl)
            host = (uri.host ?: "").lowercase()
            path = (uri.path ?: "").lowercase()
            query = (uri.query ?: "").lowercase()
        } catch (e: Exception) {
            host = cleanUrl.lowercase()
        }

        val fullText = "$cleanUrl $host $path $query".lowercase()

        // 0. User Landing Page / Blogspot / Bridge / Locker archetype (Combines Locker + Offer Click with New Tab Transition)
        if (fullText.contains("gdfqo") || fullText.contains("blogspot") || fullText.contains("landing") || fullText.contains("bridge") || fullText.contains("alignmentfiles") || fullText.contains("cpagrip") || fullText.contains("locker")) {
            val cats = listOf("Content / Link Locker", "Offer Click", "Email Submit", "Terms Agreement", "Survey / Quiz", "Lead Gen Form", "Skip Upsells", "Confirmation")
            val ordered = orderCategories(cats)
            return ExtractedPlanResult(
                detectedName = if (fullText.contains("gdfqo")) "Gdfqo Blogspot CPA Locker Funnel" else "CPA Content Locker & Offer Click Funnel",
                targetUrl = cleanUrl,
                categories = cats,
                orderedSteps = ordered,
                summary = formatPlanSummary(cats),
                recommendedMode = "mode1",
                recommendedDuration = 55,
                recommendedKeywords = "thank you, congratulations, success, confirmed, completed, verified, reward, claim",
                recommendedReferer = "https://www.google.com",
                explanationAr = "تم الكشف عن قمع لوكر وصفحة هبوط: انتظار ظهور اللوكر، النقر على العرض المناسب باستخدام نصوص شاشة Offer Click، فتح موقع العرض تلقائياً في تبويب جديد والانتقال الفوري للعمل فيه لإكمال التحويل.",
                explanationEn = "Locker + Offer Click Funnel detected: Automatically waits for Content Locker to appear, clicks matching offer using Offer Click screen texts, opens offer in a new tab, and transitions work context to it."
            )
        }

        // 1. ConsumerTestConnect ($100 GC) archetype check
        if (fullText.contains("consumertestconnect") || (fullText.contains("ctc") && fullText.contains("100gc"))) {
            val cats = listOf("Email Submit", "Terms Agreement", "Survey / Quiz", "Lead Gen Form", "Skip Upsells", "Confirmation")
            val ordered = orderCategories(cats)
            return ExtractedPlanResult(
                detectedName = "ConsumerTestConnect ($100 GC)",
                targetUrl = cleanUrl,
                categories = cats,
                orderedSteps = ordered,
                summary = formatPlanSummary(cats),
                recommendedMode = "mode1",
                recommendedDuration = 45,
                recommendedKeywords = "thank you, congratulations, success, confirmed, sweepstakes entry, verified, completed",
                recommendedReferer = "https://www.google.com",
                explanationAr = "تم استخراج خطة عرض ConsumerTestConnect بنجاح: مسار تحويل كامل يبدأ بتسجيل البريد الإلكتروني، تأكيد الموافقة على الشروط وبلوغ سن 18 عاماً، الإجابة الذكية على أسئلة الاستبيان التأهيلية، ملء استمارة البيانات الديموغرافية، التخطي التلقائي لعروض الرعاة (Skip Upsells)، والوصول لصفحة تأكيد المكافأة.",
                explanationEn = "Successfully extracted ConsumerTestConnect ($100 GC) plan: Full funnel with Email Submit, Terms & 18+ Age agreement, Qualification Survey, Demographic Lead Gen, Sponsor Deal Skip, and Confirmation claim."
            )
        }

        // 2. Gift Card / Reward / Sweepstakes archetype
        if (fullText.contains("sweep") || fullText.contains("giftcard") || fullText.contains("reward") || fullText.contains("voucher") || fullText.contains("100gc") || fullText.contains("500") || fullText.contains("750") || fullText.contains("1000")) {
            val brand = when {
                fullText.contains("amazon") -> "Amazon $500"
                fullText.contains("walmart") -> "Walmart $100"
                fullText.contains("target") -> "Target $100"
                fullText.contains("cash") || fullText.contains("cashapp") -> "Cash App $750"
                fullText.contains("apple") -> "Apple $500"
                else -> "Rewards Card"
            }
            val cats = listOf("Email Submit", "Terms Agreement", "Survey / Quiz", "Lead Gen Form", "Skip Upsells", "Confirmation")
            val ordered = orderCategories(cats)
            return ExtractedPlanResult(
                detectedName = "$brand Sweepstakes Funnel",
                targetUrl = cleanUrl,
                categories = cats,
                orderedSteps = ordered,
                summary = formatPlanSummary(cats),
                recommendedMode = "mode1",
                recommendedDuration = 45,
                recommendedKeywords = "thank you, congratulations, success, confirmed, claim reward, entry submitted",
                recommendedReferer = "https://www.google.com",
                explanationAr = "خطة مسار جوائز وقسائم: إدخال البريد، الموافقة على الشروط وتأكيد السن، استبيان تأهيلي سريع، إدخال عنوان الشحن والبيانات، وتخطي العروض الدعائية للوصول لصفحة الفوز.",
                explanationEn = "Extracted Reward/Sweepstakes funnel: Email opt-in, terms acceptance, interactive quiz, contact information submit, upsell bypass, and final confirmation."
            )
        }

        // 3. Survey / Opinion / Quiz archetype
        if (fullText.contains("survey") || fullText.contains("quiz") || fullText.contains("opinion") || fullText.contains("poll") || fullText.contains("feedback")) {
            val cats = listOf("Survey / Quiz", "Terms Agreement", "Email Submit", "Confirmation")
            val ordered = orderCategories(cats)
            return ExtractedPlanResult(
                detectedName = "Interactive Survey & Quiz",
                targetUrl = cleanUrl,
                categories = cats,
                orderedSteps = ordered,
                summary = formatPlanSummary(cats),
                recommendedMode = "mode3",
                recommendedDuration = 60,
                recommendedKeywords = "survey completed, thank you, congratulations, success, responses recorded, rewards credited",
                recommendedReferer = "https://www.google.com",
                explanationAr = "خطة استبيان وأسئلة: إجابة الأسئلة التأهيلية بأعلى تقييم وبما يطابق الهوية، الموافقة على الشروط، إدخال البريد الإلكتروني، وانتظار رسالة اكتمال الاستبيان.",
                explanationEn = "Extracted Survey Funnel: High-qualifying question answering, terms agreement, email submission, and smart keyword completion."
            )
        }

        // 4. Lead Gen / Quotes / Insurance / Finance archetype
        if (fullText.contains("quote") || fullText.contains("insurance") || fullText.contains("solar") || fullText.contains("loan") || fullText.contains("mortgage") || fullText.contains("finance") || fullText.contains("credit")) {
            val cats = listOf("Zip Submit", "Lead Gen Form", "Survey / Quiz", "Terms Agreement", "Confirmation")
            val ordered = orderCategories(cats)
            return ExtractedPlanResult(
                detectedName = "Quote & Lead Gen Funnel",
                targetUrl = cleanUrl,
                categories = cats,
                orderedSteps = ordered,
                summary = formatPlanSummary(cats),
                recommendedMode = "mode1",
                recommendedDuration = 55,
                recommendedKeywords = "quote ready, estimate, thank you, successfully submitted, verified",
                recommendedReferer = "https://www.bing.com",
                explanationAr = "خطة مسار بيانات وتقدير عروض: استخراج الرمز البريدي أولاً، تعبئة استمارة بيانات الاتصال، الإجابة على أسئلة المتطلبات، والموافقة على الشروط للحصول على العرض.",
                explanationEn = "Extracted Lead Gen funnel: Geo-targeted Zip code entry, full lead information autofill, qualification questions, and terms agreement."
            )
        }

        // 5. Sign Up / Account Registration archetype
        if (fullText.contains("signup") || fullText.contains("sign-up") || fullText.contains("register") || fullText.contains("join") || fullText.contains("create-account")) {
            val cats = listOf("Sign Up", "Email Submit", "Terms Agreement", "Confirmation")
            val ordered = orderCategories(cats)
            return ExtractedPlanResult(
                detectedName = "Account Registration Flow",
                targetUrl = cleanUrl,
                categories = cats,
                orderedSteps = ordered,
                summary = formatPlanSummary(cats),
                recommendedMode = "mode1",
                recommendedDuration = 40,
                recommendedKeywords = "welcome, account created, verification, success, dashboard",
                recommendedReferer = "https://www.google.com",
                explanationAr = "خطة تسجيل حساب: تعبئة البريد الإلكتروني واسم المستخدم، توليد كلمة مرور معقدة وآمنة تلقائياً، والموافقة على الشروط.",
                explanationEn = "Extracted Sign-Up flow: Email and username detection, secure password generation, terms agreement, and welcome confirmation."
            )
        }

        // 6. Mobile PIN / SMS submit archetype
        if (fullText.contains("pin") || fullText.contains("sms") || fullText.contains("mobile") || fullText.contains("carrier") || fullText.contains("phone")) {
            val cats = listOf("PIN Submit", "Terms Agreement", "Confirmation")
            val ordered = orderCategories(cats)
            return ExtractedPlanResult(
                detectedName = "Mobile PIN Submit Flow",
                targetUrl = cleanUrl,
                categories = cats,
                orderedSteps = ordered,
                summary = formatPlanSummary(cats),
                recommendedMode = "mode1",
                recommendedDuration = 35,
                recommendedKeywords = "pin sent, code verified, success, thank you, subscribed",
                recommendedReferer = "https://m.facebook.com",
                explanationAr = "خطة تأكيد الهاتف والـ PIN: إدخال رقم هاتف أمريكي متطابق مع شبكة المشغل والموافقة على الشروط.",
                explanationEn = "Extracted Mobile PIN submit flow: Carrier detection, phone number entry, and verification confirmation."
            )
        }

        // 7. General High-Converting CPA Offer Fallback
        val cleanDomain = host.removePrefix("www.").substringBefore(".")
        val formattedTitle = if (cleanDomain.isNotBlank() && cleanDomain.length > 2) {
            cleanDomain.replaceFirstChar { it.uppercase() } + " CPA Offer"
        } else {
            "CPA Smart Funnel"
        }

        val defaultCats = listOf("Email Submit", "Terms Agreement", "Survey / Quiz", "Lead Gen Form", "Skip Upsells", "Confirmation")
        val defaultOrdered = orderCategories(defaultCats)
        return ExtractedPlanResult(
            detectedName = formattedTitle,
            targetUrl = cleanUrl,
            categories = defaultCats,
            orderedSteps = defaultOrdered,
            summary = formatPlanSummary(defaultCats),
            recommendedMode = "mode1",
            recommendedDuration = 45,
            recommendedKeywords = "thank you, congratulations, success, confirmed, completed, verified",
            recommendedReferer = "https://www.google.com",
            explanationAr = "تم فحص الرابط وتوليد خطة تحويل شاملة ومحسنة: مسار تلقائي يتضمن إدخال البريد، الموافقة على الشروط، الإجابة على أي أسئلة أو اختيارات، تعبئة بيانات الاتصال، وتخطي العروض الدعائية لضمان إتمام الإحالة بنجاح.",
            explanationEn = "Extracted and optimized robust CPA conversion funnel: Universal flow covering email submit, terms consent, interactive question answering, lead generation, and upsell wall bypass."
        )
    }
}

