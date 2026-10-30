package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyAppNameAndDrawerNavigation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مكتبة المانجا والمواقع", appName)

        // Drawer is open initially matching 00:00 of the reference video
        composeTestRule.onNodeWithTag("navigation_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithText("انقر هنا لتسجيل الدخول").assertIsDisplayed()

        // Navigate to Manage Sites ("إدارة المواقع")
        composeTestRule.onNodeWithTag("drawer_item_manage_sites")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("manage_sites_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("لم تضف أي مواقع بعد").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_add_site_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_browser_button").assertIsDisplayed()
    }
}
