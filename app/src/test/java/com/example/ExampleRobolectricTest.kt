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

    @Test
    fun verifyNativeSiteCatalogDetailsAndChapterReaderFlow() {
        // Drawer is open initially showing المواقع المضافة including Team-X (id = 107L)
        composeTestRule.onNodeWithTag("navigation_drawer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("drawer_added_site_107")
            .performScrollTo()
            .performClick()

        // Advance clock so the 3-stage loading completes and shows the native SiteCatalogScreen grid
        composeTestRule.mainClock.advanceTimeBy(1200)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("site_catalog_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("site_works_grid").assertIsDisplayed()

        // Tap "High-martial world..." card to open MangaDetailsScreen
        composeTestRule.onNodeWithTag("manga_card_high_martial_world")
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("manga_details_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("details_favorite_button").assertIsDisplayed()

        // Tap Chapter 1 to open MangaChapterReaderScreen
        composeTestRule.onNodeWithTag("chapter_row_1")
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(1600)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("manga_chapter_reader_screen").assertIsDisplayed()
    }
}
