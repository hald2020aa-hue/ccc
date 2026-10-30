package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaCatalogData
import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.download.MangaDownloadManager
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadedChapterEntity
import com.example.data.local.DownloadedWorkEntity
import com.example.data.local.SiteEntity
import com.example.data.repository.ReadingProgressRepository
import com.example.data.repository.SourceRepository
import com.example.data.source.MangaSourceAdapterRegistry
import com.example.data.storage.LocalStorageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

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
                <div class="reading-content">
                    <div class="page-break no-gaps">
                        <img id="image-0" data-src="https://olympustaff.com/uploads/chapters/ch1/01.jpg" class="wp-manga-chapter-img" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-1" data-lazy-src="https://olympustaff.com/uploads/chapters/ch1/02.jpg" class="wp-manga-chapter-img" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-2" data-original="https://olympustaff.com/uploads/chapters/ch1/03.jpg" class="wp-manga-chapter-img" />
                    </div>
                    <div class="page-break no-gaps">
                        <img id="image-3" src="https://olympustaff.com/uploads/chapters/ch1/04.webp" class="wp-manga-chapter-img" />
                    </div>
                </div>
                <div class="comments-area">
                    <img src="https://olympustaff.com/uploads/avatars/user1.jpg" class="avatar" />
                </div>
            </body>
            </html>
        """.trimIndent()

        val pageUrls = LiveMangaSiteScraper.parseChapterPagesHtml(
            html = sampleChapterHtml,
            chapterUrl = enrichedWork.chapters.first().chapterUrl
        )
        assertEquals(4, pageUrls.size)
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/01.jpg", pageUrls[0])
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/02.jpg", pageUrls[1])
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/03.jpg", pageUrls[2])
        assertEquals("https://olympustaff.com/uploads/chapters/ch1/04.webp", pageUrls[3])
    }

    @Test
    fun verifySourceRepositoryLocalStorageOfflineReaderProgressAndDeletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.appDao()
        val storageManager = LocalStorageManager(context)
        val sourceRepository = SourceRepository(dao)
        val progressRepository = ReadingProgressRepository(dao)

        // 1. Add a new source and verify persistence, capabilities, and lastVisitedUrl
        val siteId = sourceRepository.addSource(
            name = "Azora",
            url = "azoramoon.com",
            domain = "azoramoon.com",
            iconKey = "azora",
            supportsChapterDownload = true
        )
        assertTrue(siteId > 0L)
        val savedSite = sourceRepository.getSourceById(siteId)
        assertNotNull(savedSite)
        assertEquals("https://azoramoon.com", savedSite?.url)
        assertEquals("azoramoon.com", savedSite?.domain)

        sourceRepository.updateLastVisitedUrl(siteId, "https://azoramoon.com/series/solo-leveling")
        val updatedSite = sourceRepository.getSourceById(siteId)
        assertEquals("https://azoramoon.com/series/solo-leveling", updatedSite?.lastVisitedUrl)

        val capabilities = MangaSourceAdapterRegistry.getCapabilitiesForSite(updatedSite!!)
        assertTrue(capabilities.supportsWebViewBrowsing)
        assertTrue(capabilities.supportsChapterDetection)
        assertTrue(capabilities.supportsChapterDownload)
        assertTrue(capabilities.supportsOfflineReader)

        // 2. Save real local chapter page images and cover on disk via LocalStorageManager
        val workId = "azora_solo_leveling"
        val ch1Key = "${workId}_ch_1"
        val ch2Key = "${workId}_ch_2"

        // Clean any prior test artifacts
        storageManager.deleteWorkFiles(workId)

        val fakeJpegBytes = storageManager.generateValidJpegBytes("Solo Leveling", "Ch 1", 1)
        val coverPath = storageManager.saveCoverImage(workId, fakeJpegBytes)
        assertTrue(File(coverPath).exists())

        val ch1Page1 = storageManager.saveChapterPageImage(workId, ch1Key, 0, fakeJpegBytes)
        val ch1Page2 = storageManager.saveChapterPageImage(workId, ch1Key, 1, fakeJpegBytes)
        val ch1Page3 = storageManager.saveChapterPageImage(workId, ch1Key, 2, fakeJpegBytes)

        val ch2Page1 = storageManager.saveChapterPageImage(workId, ch2Key, 0, fakeJpegBytes)
        val ch2Page2 = storageManager.saveChapterPageImage(workId, ch2Key, 1, fakeJpegBytes)

        // 3. Verify complete vs incomplete chapter file verification
        assertTrue(
            storageManager.verifyChapterFilesComplete(
                workId = workId,
                chapterKey = ch1Key,
                expectedPages = 3,
                pagePaths = listOf(ch1Page1, ch1Page2, ch1Page3)
            )
        )
        // If expectedPages is 4 but only 3 files exist, verification MUST return false (INCOMPLETE)
        assertFalse(
            storageManager.verifyChapterFilesComplete(
                workId = workId,
                chapterKey = ch1Key,
                expectedPages = 4,
                pagePaths = listOf(ch1Page1, ch1Page2, ch1Page3)
            )
        )

        // 4. Store DownloadedWorkEntity and DownloadedChapterEntity in Room (COMPLETED & INCOMPLETE)
        dao.upsertDownloadedWork(
            DownloadedWorkEntity(
                workId = workId,
                sourceId = siteId,
                sourceName = "Azora",
                sourceUrl = "https://azoramoon.com",
                title = "ارتقاء المستوى المنفرد",
                subtitle = "Solo Leveling",
                author = "Chugong",
                statusText = "مكتمل",
                status = "مكتمل",
                synopsis = "قصة صياد يرتقي بمفرده.",
                genresCsv = "أكشن,خيال",
                mangaUrl = "https://azoramoon.com/series/solo-leveling",
                remoteCoverUrl = "https://azoramoon.com/uploads/solo.jpg",
                localCoverPath = coverPath,
                savedChaptersCount = 1,
                totalChaptersOnSite = 2,
                totalStorageBytes = 1536L
            )
        )

        dao.upsertDownloadedChapter(
            DownloadedChapterEntity(
                chapterKey = ch1Key,
                workId = workId,
                sourceId = siteId,
                sourceName = "Azora",
                workTitle = "ارتقاء المستوى المنفرد",
                chapterId = "ch_1",
                chapterNumber = 1,
                chapterTitle = "الفصل 1",
                dateText = "2026/10/29",
                chapterUrl = "https://azoramoon.com/series/solo-leveling/1",
                localPagesJson = storageManager.encodeLocalPathsJson(listOf(ch1Page1, ch1Page2, ch1Page3)),
                pageCount = 3,
                downloadedPagesCount = 3,
                totalBytes = 1536L,
                status = MangaDownloadManager.STATUS_COMPLETED
            )
        )

        dao.upsertDownloadedChapter(
            DownloadedChapterEntity(
                chapterKey = ch2Key,
                workId = workId,
                sourceId = siteId,
                sourceName = "Azora",
                workTitle = "ارتقاء المستوى المنفرد",
                chapterId = "ch_2",
                chapterNumber = 2,
                chapterTitle = "الفصل 2",
                dateText = "2026/10/30",
                chapterUrl = "https://azoramoon.com/series/solo-leveling/2",
                localPagesJson = storageManager.encodeLocalPathsJson(listOf(ch2Page1, ch2Page2)),
                pageCount = 3, // Expected 3 pages, only 2 downloaded -> INCOMPLETE
                downloadedPagesCount = 2,
                totalBytes = 1024L,
                status = MangaDownloadManager.STATUS_INCOMPLETE
            )
        )

        val storedCh1 = dao.getDownloadedChapterByKey(ch1Key)
        val storedCh2 = dao.getDownloadedChapterByKey(ch2Key)
        assertNotNull(storedCh1)
        assertNotNull(storedCh2)
        assertEquals(MangaDownloadManager.STATUS_COMPLETED, storedCh1?.status)
        assertEquals(MangaDownloadManager.STATUS_INCOMPLETE, storedCh2?.status)

        // 5. Save and restore reading progress in ReadingProgressRepository
        progressRepository.saveProgress(
            chapterKey = ch1Key,
            workId = workId,
            chapterNumber = 1,
            chapterTitle = "الفصل 1",
            lastPageIndex = 2,
            scrollOffset = 140,
            totalPages = 3
        )

        val restoredProgress = progressRepository.getChapterProgress(ch1Key)
        assertNotNull(restoredProgress)
        assertEquals(2, restoredProgress?.lastPageIndex)
        assertEquals(140, restoredProgress?.scrollOffset)
        assertTrue(restoredProgress?.isCompleted == true)

        val latestWorkProgress = progressRepository.getLatestWorkProgress(workId)
        assertNotNull(latestWorkProgress)
        assertEquals(1, latestWorkProgress?.chapterNumber)
        val formattedProgress = progressRepository.formatRelativeProgressText(latestWorkProgress!!)
        assertTrue(formattedProgress.contains("الصفحة 3"))

        // 6. Delete Chapter 2 only and verify Chapter 1 files and cover remain intact
        storageManager.deleteChapterFiles(workId, ch2Key)
        dao.deleteDownloadedChapterByKey(ch2Key)

        assertNull(dao.getDownloadedChapterByKey(ch2Key))
        assertFalse(File(ch2Page1).exists())
        assertTrue(File(ch1Page1).exists())
        assertTrue(File(ch1Page2).exists())
        assertTrue(File(ch1Page3).exists())
        assertTrue(File(coverPath).exists())

        // Clean up test files
        storageManager.deleteWorkFiles(workId)
        db.close()
    }
}
