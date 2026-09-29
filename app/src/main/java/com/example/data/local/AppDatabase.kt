package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CampaignStat
import com.example.data.model.EmailItem
import com.example.data.model.OfferClickItem
import com.example.data.model.ProxyItem
import com.example.data.model.ScriptItem
import com.example.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        TaskEntity::class,
        EmailItem::class,
        ScriptItem::class,
        CampaignStat::class,
        ProxyItem::class,
        OfferClickItem::class,
        com.example.data.model.WorkTemplateEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun emailDao(): EmailDao
    abstract fun scriptDao(): ScriptDao
    abstract fun leadLogDao(): LeadLogDao
    abstract fun proxyDao(): ProxyDao
    abstract fun offerClickDao(): OfferClickDao
    abstract fun workTemplateDao(): WorkTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cpa_automator_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Default sample tasks including user's Blogspot offer click landing page
            val sampleTasks = listOf(
                TaskEntity(
                    id = "task_gdfqo_blogspot",
                    name = "Gdfqo Blogspot (Offer Click Funnel)",
                    url = "https://gdfqo.blogspot.com",
                    referer = "https://www.google.com",
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                    mode = "mode1",
                    repeatCount = 5,
                    browserDuration = 45,
                    categories = "Offer Click, Email Submit, Terms Agreement, Survey / Quiz, Lead Gen Form, Skip Upsells, Confirmation",
                    completionKeywords = "thank you, congratulations, success, confirmed, completed, claim",
                    enabled = true
                ),
                TaskEntity(
                    id = "task_browserleaks_ip",
                    name = "BrowserLeaks IP & WebRTC Audit",
                    url = "https://browserleaks.com/ip",
                    referer = "https://www.google.com",
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                    mode = "mode1",
                    repeatCount = 1,
                    browserDuration = 30,
                    categories = "Proxy Audit, WebRTC Leak Test, Fingerprint Validation",
                    completionKeywords = "WebRTC, IP Address, Leak Test",
                    enabled = true
                ),
                TaskEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Gift Card Rewards Survey",
                    url = "https://example.com/cpa/giftcard-offer",
                    referer = "https://www.google.com",
                    userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                    mode = "mode1",
                    repeatCount = 3,
                    browserDuration = 45,
                    mode1RepeatCount = 3,
                    categories = "Offer Click, Email Submit, Survey / Quiz, Terms Agreement"
                ),
                TaskEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Gaming Subscription Offer",
                    url = "https://example.com/cpa/game-signup",
                    referer = "https://www.facebook.com",
                    userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1",
                    mode = "mode3",
                    repeatCount = 2,
                    completionKeywords = "thank you, congratulations, welcome, completed, verified",
                    categories = "Sign Up, Lead Gen Form, Skip Upsells"
                )
            )
            for (task in sampleTasks) {
                database.taskDao().insertTask(task)
            }

            // Default system scripts
            val defaultScripts = listOf(
                ScriptItem(
                    id = "script_webrtc",
                    name = "Disable WebRTC & Leak Protection",
                    timing = "before",
                    execMode = "sequential",
                    code = """
(function() {
  try {
    if (window.RTCPeerConnection) window.RTCPeerConnection = undefined;
    if (window.webkitRTCPeerConnection) window.webkitRTCPeerConnection = undefined;
    if (window.mozRTCPeerConnection) window.mozRTCPeerConnection = undefined;
    if (navigator.mediaDevices) {
      Object.defineProperty(navigator, 'mediaDevices', {
        get: () => ({ getUserMedia: () => Promise.reject(new Error('Blocked')) }),
        configurable: true
      });
    }
    console.log('[CPA] WebRTC disabled');
  } catch(e) {}
})();
true;
                    """.trimIndent(),
                    enabled = true,
                    isSystemPreset = true
                ),
                ScriptItem(
                    id = "script_antidetect",
                    name = "Anti-Bot & Hardware Fingerprint Spoof",
                    timing = "before",
                    execMode = "sequential",
                    code = """
(function() {
  try {
    Object.defineProperty(navigator, 'webdriver', { get: () => undefined, configurable: true });
    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8, configurable: true });
    Object.defineProperty(navigator, 'deviceMemory', { get: () => 8, configurable: true });
  } catch(e) {}
})();
true;
                    """.trimIndent(),
                    enabled = true,
                    isSystemPreset = true
                ),
                ScriptItem(
                    id = "script_canvas_noise",
                    name = "Canvas Fingerprint Randomized Noise",
                    timing = "before",
                    execMode = "sequential",
                    code = """
(function() {
  try {
    function addNoise(imgData) {
      if (!imgData || !imgData.data) return;
      var d = imgData.data;
      var step = d.length > 40000 ? 16 : 4;
      for (var i = 0; i < d.length; i += step) {
        if (d[i + 3] > 5) {
          var noise = (Math.random() < 0.5 ? 1 : -1) * (1 + Math.floor(Math.random() * 2));
          var c = i % 3;
          var v = d[i + c] + noise;
          d[i + c] = v < 0 ? 0 : (v > 255 ? 255 : v);
        }
      }
    }
    if (window.CanvasRenderingContext2D) {
      var origGet = CanvasRenderingContext2D.prototype.getImageData;
      CanvasRenderingContext2D.prototype.getImageData = function() {
        var res = origGet.apply(this, arguments);
        addNoise(res);
        return res;
      };
    }
    if (window.HTMLCanvasElement) {
      var origURL = HTMLCanvasElement.prototype.toDataURL;
      HTMLCanvasElement.prototype.toDataURL = function() {
        try {
          var ctx = this.getContext('2d');
          if (ctx && this.width > 0 && this.height > 0) {
            var s = ctx.getImageData(0, 0, Math.min(this.width, 32), Math.min(this.height, 32));
            addNoise(s);
            ctx.putImageData(s, 0, 0);
          }
        } catch(e) {}
        return origURL.apply(this, arguments);
      };
    }
  } catch(e) {}
})();
true;
                    """.trimIndent(),
                    enabled = true,
                    isSystemPreset = true
                ),
                ScriptItem(
                    id = "script_human",
                    name = "Human Simulation & Scroll",
                    timing = "after",
                    execMode = "parallel",
                    code = """
(function() {
  function randomScroll() {
    var delta = (Math.random() - 0.5) * 250;
    window.scrollBy({ top: delta, behavior: 'smooth' });
    setTimeout(randomScroll, 2500 + Math.random() * 3500);
  }
  setTimeout(randomScroll, 1500);
})();
true;
                    """.trimIndent(),
                    enabled = true,
                    isSystemPreset = true
                ),
                ScriptItem(
                    id = "script_cpa_locker",
                    name = "CPA Locker & AlignmentFiles Auto-Activator",
                    timing = "after",
                    execMode = "sequential",
                    code = """
(function() {
  try {
    if (!window.__cpa_write_patched) {
      window.__cpa_write_patched = true;
      var ow = document.write;
      document.write = function(c) {
        if (typeof c === 'string' && c.indexOf('<script') !== -1) {
          try {
            var p = new DOMParser();
            var d = p.parseFromString(c, 'text/html');
            d.querySelectorAll('script').forEach(function(s) {
              var n = document.createElement('script');
              if (s.src) { n.src = s.src; n.async = false; } else { n.textContent = s.textContent; }
              (document.head || document.documentElement).appendChild(n);
            });
            return;
          } catch(e) {}
        }
        try { ow.apply(document, arguments); } catch(ex) {}
      };
    }
    setTimeout(function() {
      if (typeof window.call_locker === 'function') window.call_locker();
      else if (typeof window.DisplayInlineBox === 'function') window.DisplayInlineBox();
      else if (typeof window.call1 === 'function') window.call1();
    }, 1200);
  } catch(e) {}
})();
true;
                    """.trimIndent(),
                    enabled = true,
                    isSystemPreset = true
                )
            )
            database.scriptDao().insertDefaultScripts(defaultScripts)

            // Seed some sample emails
            val sampleEmails = listOf(
                EmailItem(email = "marketing.alex89@gmail.com"),
                EmailItem(email = "tech.david.williams@outlook.com"),
                EmailItem(email = "samuel.jackson.media@yahoo.com")
            )
            database.emailDao().insertEmails(sampleEmails)

            // Seed user's configured US Proxy
            val userProxy = ProxyItem(
                host = "185.100.232.75",
                port = 9999,
                type = "socks5",
                username = "bqjcykpcvw-a2c12283-3e44-4012-b6c0-7a476bef9351",
                password = "DKiwjdv40s2tG3Jk",
                country = "US",
                status = "working",
                lastPingMs = 120L
            )
            database.proxyDao().insertProxy(userProxy)

            // Seed initial Offer Click texts (with user's exact requested text at orderIndex 0)
            val initialClickTexts = listOf(
                OfferClickItem(
                    text = "Get \$1000 Walmart gift card",
                    enabled = true,
                    orderIndex = 0,
                    tagOrNote = "Walmart $1000 GC Offer"
                ),
                OfferClickItem(
                    text = "Claim \$750 Cash App Reward",
                    enabled = true,
                    orderIndex = 1,
                    tagOrNote = "Cash App Reward"
                ),
                OfferClickItem(
                    text = "Get \$500 Amazon Gift Card",
                    enabled = true,
                    orderIndex = 2,
                    tagOrNote = "Amazon $500 Sweep"
                ),
                OfferClickItem(
                    text = "Win \$100 Target Gift Card",
                    enabled = true,
                    orderIndex = 3,
                    tagOrNote = "Target Gift Card"
                ),
                OfferClickItem(
                    text = "Claim \$500 Apple Store Card",
                    enabled = false, // Demonstrates toggled/closed text as requested
                    orderIndex = 4,
                    tagOrNote = "Apple Store Voucher"
                )
            )
            database.offerClickDao().insertClickItems(initialClickTexts)
            
            // Seed intelligent preset work templates
            val defaultTemplates = listOf(
                com.example.data.model.WorkTemplateEntity(
                    id = "template_smart_auto",
                    name = "قالب العمل التلقائي الذكي (Smart Auto)",
                    description = "يحلل الصفحة تلقائياً ويكتشف الأزرار والحقول والسكرول وينشئ خريطة عمل ديناميكية بدون ترتيب مسبق",
                    targetCategory = "Smart Auto",
                    isAutoGenerated = false,
                    stepsJson = """[
                        {"id":"s1","type":"SCROLL","targetType":"auto","scrollDirection":"down","scrollAmount":300,"delayMs":800,"description":"تمرير طبيعي لمحاكاة المستخدم"},
                        {"id":"s2","type":"EXTRACT_ADAPT","targetType":"auto","fieldSource":"identity","fieldKey":"email","delayMs":1000,"description":"استخراج ذكي لأسئلة الصفحة وحقولها وملئها حسب الهوية"},
                        {"id":"s3","type":"CHECK_BOX","targetType":"auto","targetValue":"terms","delayMs":600,"description":"موافقة الشروط وتأكيد السن القانوني"},
                        {"id":"s4","type":"CLICK_BUTTON","targetType":"auto","targetValue":"submit","delayMs":1200,"description":"نقر زر المتابعة / الإرسال"}
                    ]"""
                ),
                com.example.data.model.WorkTemplateEntity(
                    id = "template_email_optin",
                    name = "قالب إدخال البريد والموافقة (Email & Terms)",
                    description = "يفهم حقل الإيميل من شاشة المعلومات، يمرر الصفحة، يوافق على الشروط وينقر زر المتابعة",
                    targetCategory = "Email Submit",
                    isAutoGenerated = false,
                    stepsJson = """[
                        {"id":"s1","type":"SCROLL","targetType":"auto","scrollDirection":"down","scrollAmount":250,"delayMs":700,"description":"تمرير نحو نموذج البريد"},
                        {"id":"s2","type":"FILL_FIELD","targetType":"auto","fieldSource":"identity","fieldKey":"email","delayMs":900,"description":"ملء البريد الإلكتروني من شاشة المعلومات"},
                        {"id":"s3","type":"CHECK_BOX","targetType":"auto","targetValue":"agree","delayMs":500,"description":"تحديد مربع الموافقة على الشروط"},
                        {"id":"s4","type":"CLICK_BUTTON","targetType":"text","targetValue":"Continue, Submit, Next, Start","delayMs":1000,"description":"نقر زر الإرسال"}
                    ]"""
                ),
                com.example.data.model.WorkTemplateEntity(
                    id = "template_survey_lead",
                    name = "قالب الاستبيان والبيانات (Survey & Lead)",
                    description = "يجيب على أسئلة الاستبيان بذكاء، يملأ البيانات الكاملة (الاسم، الهاتف، العنوان، الرمز البريدي)",
                    targetCategory = "Survey / Quiz",
                    isAutoGenerated = false,
                    stepsJson = """[
                        {"id":"s1","type":"EXTRACT_ADAPT","targetType":"auto","delayMs":1200,"description":"استخراج أسئلة الاستبيان واختيار الإجابات المؤهلة"},
                        {"id":"s2","type":"FILL_FIELD","targetType":"auto","fieldSource":"identity","fieldKey":"fullName","delayMs":800,"description":"ملء الاسم الكامل"},
                        {"id":"s3","type":"FILL_FIELD","targetType":"auto","fieldSource":"identity","fieldKey":"phone","delayMs":800,"description":"ملء رقم الهاتف"},
                        {"id":"s4","type":"FILL_FIELD","targetType":"auto","fieldSource":"identity","fieldKey":"postalCode","delayMs":800,"description":"ملء الرمز البريدي"},
                        {"id":"s5","type":"CLICK_BUTTON","targetType":"auto","targetValue":"No Thanks, Skip","delayMs":1000,"description":"تخطي العروض الدعائية المزعجة"},
                        {"id":"s6","type":"CLICK_BUTTON","targetType":"auto","targetValue":"Claim, Finish, Confirm","delayMs":1200,"description":"نقر زر تأكيد الإكمال"}
                    ]"""
                )
            )
            database.workTemplateDao().insertTemplates(defaultTemplates)
        }
    }
}
