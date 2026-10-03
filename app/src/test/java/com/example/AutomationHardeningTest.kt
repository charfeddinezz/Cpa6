package com.example

import com.example.data.model.OfferClickItem
import com.example.service.AutomationScriptBuilder
import com.example.service.SmartAutomationBrain
import com.example.service.TaskCategoryPlanner
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AutomationHardeningTest {

    @Test
    fun nikeText_isValidPriorityTarget() {
        val nike = "Get a \$100 Nike Gift Card!"
        assertTrue(nike.contains("Nike", ignoreCase = true))
        // Planner must keep locker+click combo for GDFQO funnel
        val cats = listOf("Content / Link Locker", "Offer Click", "Email Submit", "Confirmation")
        assertTrue(TaskCategoryPlanner.isLockerOfferClickCombo(cats))
        val ordered = TaskCategoryPlanner.orderCategories(cats)
        assertEquals("wait_locker", ordered[0].id)
        assertEquals("click_locker_offer", ordered[1].id)
        assertEquals("new_tab_transition", ordered[2].id)
    }

    @Test
    fun lockerScript_hasNikeSafeFixes() {
        val script = AutomationScriptBuilder.buildLockerOfferAutoClickScript(
            clickTexts = listOf("Get a \$100 Nike Gift Card!", "Get \$1000 Walmart gift card"),
            activeTargetText = "Get a \$100 Nike Gift Card!",
            openInNewTab = true
        )
        // Bug fix: no Kotlin .take() in JS
        assertFalse(script.contains(".take("))
        assertTrue(script.contains("substring(0, 30)"))
        // Nike-safe fuzzy matching + URL resolution + overlay dismissal
        assertTrue(script.contains("fuzzyOfferMatch"))
        assertTrue(script.contains("normalizeOfferText"))
        assertTrue(script.contains("resolveOfferUrl"))
        assertTrue(script.contains("dismissLandingOverlays"))
        assertTrue(script.contains("script_include.php"))
        assertTrue(script.contains("onOfferClickedInNewTab"))
        assertTrue(script.contains("MutationObserver"))
    }

    @Test
    fun lockerScript_defaultTargetsIncludeNike() {
        val script = AutomationScriptBuilder.buildLockerOfferAutoClickScript(
            clickTexts = emptyList(),
            activeTargetText = null,
            openInNewTab = true
        )
        assertTrue(script.lowercase().contains("nike"))
    }

    @Test
    fun offerClickItem_ctrTracking() {
        val fresh = OfferClickItem(text = "Get a \$100 Nike Gift Card!", enabled = true, orderIndex = 0)
        assertEquals(0.5, fresh.ctr, 0.001)
        val shown = fresh.copy(showCount = 9, clickCount = 3)
        // 3 clicks / (9+1) impressions
        assertEquals(0.3, shown.ctr, 0.001)
    }

    @Test
    fun proxyScoring_prefersHealthyFast() {
        val good = SmartAutomationBrain.scoreProxyForTask(0.9, 200L, 0, 9999L, 90)
        val bad = SmartAutomationBrain.scoreProxyForTask(0.2, 5000L, 3, 1L, 20)
        assertTrue(good > bad)
        assertTrue(good in 0.0..100.0)
        assertTrue(bad in 0.0..100.0)
    }

    @Test
    fun brainUcb_exploresNewTasks() {
        val t1 = com.example.data.model.TaskEntity(id = "a", name = "A", url = "https://a.com")
        val t2 = com.example.data.model.TaskEntity(id = "b", name = "B", url = "https://b.com")
        val learning = mapOf(
            "a" to SmartAutomationBrain.TaskLearningStats("a", runs = 10, conversions = 9)
        )
        val ranked = SmartAutomationBrain.prioritizeTasksUCB(listOf(t1, t2), learning)
        // Unseen task B must be tried first (no local-optimum trap)
        assertEquals("b", ranked.first().id)
    }

    @Test
    fun smartDuration_addsLockerBonus() {
        val base = com.example.data.model.TaskEntity(
            id = "g", name = "G", url = "https://gdfqo.blogspot.com",
            browserDuration = 45,
            categories = "Content / Link Locker, Offer Click, Email Submit"
        )
        val cats = TaskCategoryPlanner.parseCategories(base.categories)
        val smart = SmartAutomationBrain.smartDurationForTask(base, cats)
        assertTrue(smart > 45)
        assertTrue(smart <= 120)
    }

    @Test
    fun gdfqoPlan_detectsLockerFunnel() {
        val plan = TaskCategoryPlanner.extractFunnelPlanFromUrl(
            "https://gdfqo.blogspot.com/?utm_source=facebook&utm_medium=cpc&utm_campaign=tools&utm_content=tools_ad_1"
        )
        assertTrue(plan.categories.any { it.contains("Locker", ignoreCase = true) })
        assertTrue(plan.categories.any { it.contains("Offer Click", ignoreCase = true) })
        assertEquals("mode1", plan.recommendedMode)
    }
}
