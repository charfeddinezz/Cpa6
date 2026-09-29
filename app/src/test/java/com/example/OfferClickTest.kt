package com.example

import com.example.data.model.GeneratedIdentity
import com.example.data.model.OfferClickItem
import com.example.service.AutomationScriptBuilder
import com.example.service.TaskCategoryPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfferClickTest {

    @Test
    fun testOfferClickIsPriorityOne() {
        // Test that "offer_click" is always assigned priority = 1
        val def = TaskCategoryPlanner.findDefinition("Offer Click")
        assertNotNull(def)
        assertEquals(1, def?.priority)
        assertEquals("offer_click", def?.id)
    }

    @Test
    fun testPlannerSortingWithOfferClick() {
        // Ensure that Offer Click is always sorted first before any other categories
        val rawCategories = listOf(
            "Survey / Quiz",
            "Confirmation",
            "Offer Click",
            "Email Submit",
            "Skip Upsells"
        )
        val orderedSteps = TaskCategoryPlanner.orderCategories(rawCategories)
        assertEquals("offer_click", orderedSteps.first().id)
        assertEquals(1, orderedSteps.first().priority)
    }

    @Test
    fun testOfferClickSequenceSelection() {
        // Test sequential round-robin execution through enabled items only
        val items = listOf(
            OfferClickItem(id = 1L, text = "Get \$1000 Walmart gift card", enabled = true, orderIndex = 0),
            OfferClickItem(id = 2L, text = "Claim \$750 Cash App Reward", enabled = false, orderIndex = 1),
            OfferClickItem(id = 3L, text = "Get \$500 Amazon Gift Card", enabled = true, orderIndex = 2)
        )

        val activeList = items.filter { it.enabled }.sortedBy { it.orderIndex }
        assertEquals(2, activeList.size)
        assertEquals("Get \$1000 Walmart gift card", activeList[0].text)
        assertEquals("Get \$500 Amazon Gift Card", activeList[1].text)

        // Loop index 0:
        val pick0 = activeList[0 % activeList.size]
        assertEquals("Get \$1000 Walmart gift card", pick0.text)

        // Loop index 1:
        val pick1 = activeList[1 % activeList.size]
        assertEquals("Get \$500 Amazon Gift Card", pick1.text)

        // Loop index 2 (wraps around):
        val pick2 = activeList[2 % activeList.size]
        assertEquals("Get \$1000 Walmart gift card", pick2.text)
    }

    @Test
    fun testScriptBuilderContainsOfferClickLogic() {
        val identity = GeneratedIdentity(
            firstName = "John",
            lastName = "Smith",
            fullName = "John Smith",
            email = "john.smith@gmail.com",
            phone = "2125550199",
            address = "123 Main St",
            city = "New York",
            state = "NY",
            postalCode = "10001",
            country = "United States",
            birthDate = "1990-05-12",
            gender = "Male"
        )

        val script = AutomationScriptBuilder.buildSmartFormFillScript(
            identity = identity,
            categories = "Offer Click, Survey / Quiz, Email Submit",
            clickTexts = listOf("Get \$1000 Walmart gift card", "Claim \$750 Cash App Reward"),
            activeClickText = "Get \$1000 Walmart gift card"
        )

        // Verify key components exist in generated script
        assertTrue(script.contains("handleOfferClick"))
        assertTrue(script.contains("Get $1000 Walmart gift card"))
        assertTrue(script.contains("checkMatch"))
        assertTrue(script.contains("window.AndroidBridge.onOfferClicked"))
        assertTrue(script.contains("PRIORITY #1 - OFFER CLICK"))
    }

    @Test
    fun testLockerOfferClickComboFunnel() {
        val categories = listOf("Content / Link Locker", "Offer Click", "Email Submit", "Confirmation")
        assertTrue(TaskCategoryPlanner.isLockerOfferClickCombo(categories))

        val ordered = TaskCategoryPlanner.orderCategories(categories)
        assertEquals(5, ordered.size)
        assertEquals("wait_locker", ordered[0].id)
        assertEquals("click_locker_offer", ordered[1].id)
        assertEquals("new_tab_transition", ordered[2].id)
        assertEquals("email_submit", ordered[3].id)
        assertEquals("completion_confirm", ordered[4].id)

        val summary = TaskCategoryPlanner.formatPlanSummary(categories)
        assertTrue(summary.contains("Wait for Locker & Detection"))
        assertTrue(summary.contains("Click Matching Locker Offer"))
        assertTrue(summary.contains("Open in New Tab & Switch Work"))
    }

    @Test
    fun lockerActivatorWaitsForAsynchronousLockerScripts() {
        val script = AutomationScriptBuilder.buildCpaLockerDetectorAndActivatorScript()

        assertTrue(script.contains("var triggerAttempts = 0"))
        assertTrue(script.contains("triggerAttempts < 20"))
        assertTrue(script.contains("clearInterval(triggerTimer)"))
    }

    @Test
    fun testLockerAutoClickScriptContainsAsynchronousScannerAndNewTabBridge() {
        val script = AutomationScriptBuilder.buildLockerOfferAutoClickScript(
            clickTexts = listOf("Get \$1000 Walmart gift card", "Claim \$750 Cash App Reward"),
            activeTargetText = "Get \$1000 Walmart gift card",
            openInNewTab = true
        )

        assertTrue(script.contains("window.AndroidBridge.onOfferClickedInNewTab"))
        assertTrue(script.contains("MutationObserver"))
        assertTrue(script.contains("window.__cpa_locker_poll_interval"))
        assertTrue(script.contains("waiting_for_locker_offers"))
    }

    @Test
    fun testInfoScreenDataInjection() {
        val identity = GeneratedIdentity(
            firstName = "Alex",
            lastName = "Mercer",
            fullName = "Alex Mercer",
            email = "alex.mercer99@gmail.com",
            phone = "4155552671",
            address = "742 Evergreen Terrace",
            city = "Springfield",
            state = "OR",
            postalCode = "97477",
            country = "United States",
            birthDate = "1994-08-20",
            gender = "Male",
            username = "alexmercer99",
            password = "SecurePassword123!",
            cardNumber = "4532 1198 2234 5678",
            cardExpiry = "09/28",
            cardCvv = "882",
            cardHolder = "Alex Mercer",
            cardType = "Visa",
            bankName = "Chase Bank"
        )

        val extractedInfo = com.example.data.model.ExtractedInfo(
            ip = "198.51.100.42",
            country = "United States",
            countryCode = "US",
            region = "Oregon",
            city = "Springfield",
            postalCode = "97477",
            timezone = "America/Los_Angeles",
            language = "en-US",
            isp = "Comcast Cable",
            latitude = 44.0462,
            longitude = -123.0220
        )

        val script = AutomationScriptBuilder.buildSmartFormFillScript(
            identity = identity,
            categories = "Offer Click, Form Fill, Survey / Quiz",
            clickTexts = listOf("Claim Reward Now"),
            activeClickText = "Claim Reward Now",
            extractedInfo = extractedInfo
        )

        // Verify identity and extracted info are serialized into the browser script
        assertTrue(script.contains("alex.mercer99@gmail.com"))
        assertTrue(script.contains("alexmercer99"))
        assertTrue(script.contains("SecurePassword123!"))
        assertTrue(script.contains("4532 1198 2234 5678"))
        assertTrue(script.contains("09/28") || script.contains("09\\/28"))
        assertTrue(script.contains("882"))
        assertTrue(script.contains("Chase Bank"))
        assertTrue(script.contains("198.51.100.42"))
        assertTrue(script.contains("America/Los_Angeles") || script.contains("America\\/Los_Angeles"))

        // Verify matchField and field handlers cover all fields
        assertTrue(script.contains("field === 'username'"))
        assertTrue(script.contains("field === 'password'"))
        assertTrue(script.contains("field === 'cardHolder'"))
        assertTrue(script.contains("field === 'cardExpMonth'"))
        assertTrue(script.contains("field === 'cardExpYear'"))
        assertTrue(script.contains("field === 'bankName'"))
    }
}
