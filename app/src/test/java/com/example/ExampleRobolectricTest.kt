package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaCatalogData
import com.example.data.local.SiteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun verifyZeroPreAddedSitesAndLiveScraperExtraction() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مكتبة المانجا والمواقع", appName)

        // Ensure no pre-added demo sites or demo storage works exist
        assertTrue(MangaCatalogData.defaultAddedSites.isEmpty())
        assertTrue(MangaCatalogData.storageLibraryWorks.isEmpty())

        // Drawer is open initially
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

        // Verify live website scraper extracts real covers, details, chapters, and page images
        val site = SiteEntity(
            id = 1L,
            name = "Team-X",
            url = "https://olympustaff.com",
            domain = "olympustaff.com",
            iconKey = "teamx"
        )

        val sampleCatalogHtml = """
            <html>
            <body>
                <div class="bs">
                    <div class="bsx">
                        <a href="https://olympustaff.com/series/high-martial-world" title="بيد واحدة يكتسح آلاف الأنماط">
                            <div class="limit">
                                <img data-src="https://olympustaff.com/uploads/covers/high-martial.jpg" alt="بيد واحدة يكتسح آلاف الأنماط" />
                            </div>
                            <div class="tt">بيد واحدة يكتسح آلاف الأنماط</div>
                        </a>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val catalogWorks = LiveMangaSiteScraper.parseCatalogHtml(
            html = sampleCatalogHtml,
            site = site,
            pageUrl = site.url
        )
        assertEquals(1, catalogWorks.size)
        assertEquals("بيد واحدة يكتسح آلاف الأنماط", catalogWorks.first().title)
        assertEquals("https://olympustaff.com/uploads/covers/high-martial.jpg", catalogWorks.first().coverImageUrl)
        assertEquals("https://olympustaff.com/series/high-martial-world", catalogWorks.first().mangaUrl)

        val sampleDetailsHtml = """
            <html>
            <head>
                <meta property="og:title" content="بيد واحدة يكتسح آلاف الأنماط" />
                <meta property="og:image" content="https://olympustaff.com/uploads/covers/high-martial.jpg" />
            </head>
            <body>
                <div class="entry-content"><p>قصة المانهوا الحقيقية مباشرة من الموقع.</p></div>
                <div class="mgen"><a href="/genre/action">أكشن</a></div>
                <div class="eplister">
                    <ul>
                        <li>
                            <a href="https://olympustaff.com/series/high-martial-world/chapter-2">
                                <span class="chapternum">الفصل 2</span>
                                <span class="chapterdate">2026/10/30</span>
                            </a>
                        </li>
                        <li>
                            <a href="https://olympustaff.com/series/high-martial-world/chapter-1">
                                <span class="chapternum">الفصل 1</span>
                                <span class="chapterdate">2026/10/29</span>
                            </a>
                        </li>
                    </ul>
                </div>
            </body>
            </html>
        """.trimIndent()

        val enrichedWork = LiveMangaSiteScraper.parseMangaDetailsHtml(
            html = sampleDetailsHtml,
            baseWork = catalogWorks.first(),
            pageUrl = catalogWorks.first().mangaUrl
        )
        assertEquals(2, enrichedWork.chapters.size)
        assertEquals("https://olympustaff.com/series/high-martial-world/chapter-1", enrichedWork.chapters.first().chapterUrl)

        val sampleChapterHtml = """
            <html>
            <body>
                <div id="readerarea">
                    <img data-src="https://olympustaff.com/uploads/chapters/ch1/01.jpg" />
                    <img data-src="https://olympustaff.com/uploads/chapters/ch1/02.jpg" />
                    <img data-src="https://olympustaff.com/uploads/chapters/ch1/03.jpg" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val pageUrls = LiveMangaSiteScraper.parseChapterPagesHtml(
            html = sampleChapterHtml,
            chapterUrl = enrichedWork.chapters.first().chapterUrl
        )
        assertEquals(3, pageUrls.size)
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/01.jpg", pageUrls[0])
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/02.jpg", pageUrls[1])
    }
}
