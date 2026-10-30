package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChapterPageIllustration
import com.example.ui.components.CloudflareProtectionDialog
import com.example.ui.components.HiddenSiteWebViewBridge
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaChapterReaderScreen(
    work: MangaWork,
    chapter: MangaChapter,
    settings: UserSettings,
    isFavorite: Boolean,
    initialPageIndex: Int = 0,
    onSaveReadingProgress: (Int) -> Unit = {},
    onBackClick: () -> Unit,
    onSelectChapter: (MangaChapter) -> Unit,
    onToggleFavorite: (MangaWork) -> Unit,
    onDownloadChapter: (MangaWork, MangaChapter) -> Unit,
    onUpdateWebtoonMode: (Boolean) -> Unit,
    onOpenReadingSettings: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    val isOfflineComplete = remember(chapter.id, chapter.localPagePaths, chapter.downloadStatus) {
        chapter.localPagePaths.isNotEmpty() && chapter.downloadStatus == "COMPLETED"
    }
    val isOfflineIncomplete = remember(chapter.id, chapter.downloadStatus) {
        chapter.downloadStatus == "INCOMPLETE"
    }

    var localPagePaths by remember(chapter.id, chapter.localPagePaths) {
        mutableStateOf(chapter.localPagePaths)
    }
    var livePageUrls by remember(chapter.id, chapter.chapterUrl, chapter.pageImageUrls) {
        mutableStateOf(chapter.pageImageUrls)
    }
    var loadingProgress by remember(chapter.id, isOfflineComplete) {
        mutableIntStateOf(if (isOfflineComplete || chapter.pageImageUrls.isNotEmpty()) 100 else 3)
    }
    var isChapterLoaded by remember(chapter.id, isOfflineComplete) {
        mutableStateOf(isOfflineComplete || chapter.pageImageUrls.isNotEmpty())
    }
    var requiresCloudflare by remember(chapter.id, chapter.chapterUrl) { mutableStateOf(false) }
    var showProtectionModal by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val targetChapterUrl = remember(chapter.chapterUrl, work.siteUrl, work.id, chapter.number) {
        chapter.chapterUrl.ifBlank {
            "${work.siteUrl.trimEnd('/')}/series/${work.id}/${chapter.number}"
        }
    }

    // Only fetch from network if NOT reading a completed local chapter (100% offline support!)
    LaunchedEffect(chapter.id, targetChapterUrl, isOfflineComplete, refreshTrigger) {
        if (isOfflineComplete && refreshTrigger == 0) {
            loadingProgress = 100
            isChapterLoaded = true
            return@LaunchedEffect
        }
        if ((livePageUrls.isEmpty() && localPagePaths.isEmpty()) || refreshTrigger > 0) {
            isChapterLoaded = false
            loadingProgress = 3
            delay(90)
            loadingProgress = 16
            delay(90)
            loadingProgress = 27
            val res = LiveMangaSiteScraper.fetchChapterPages(work, chapter.copy(chapterUrl = targetChapterUrl))
            loadingProgress = 57
            delay(90)
            loadingProgress = 79
            if (res.pageUrls.isNotEmpty()) {
                livePageUrls = res.pageUrls
                requiresCloudflare = false
                loadingProgress = 100
                isChapterLoaded = true
            } else if (res.requiresCloudflare) {
                requiresCloudflare = true
                loadingProgress = 97
                isChapterLoaded = true
            } else {
                loadingProgress = 97
                delay(1200)
                isChapterLoaded = true
            }
        }
    }

    // Hidden WebView DOM Bridge only when online and not reading from local disk
    if (!isOfflineComplete || refreshTrigger > 0) {
        HiddenSiteWebViewBridge(
            targetUrl = targetChapterUrl,
            jsExtractorScript = LiveMangaSiteScraper.JS_EXTRACT_CHAPTER_PAGES,
            reloadTrigger = refreshTrigger,
            onResultJson = { rawJson ->
                val domRes = LiveMangaSiteScraper.parseChapterPagesFromDomJson(rawJson, targetChapterUrl)
                if (domRes.pageUrls.isNotEmpty()) {
                    livePageUrls = domRes.pageUrls
                    requiresCloudflare = false
                    loadingProgress = 100
                    isChapterLoaded = true
                } else if (domRes.requiresCloudflare && livePageUrls.isEmpty() && localPagePaths.isEmpty()) {
                    requiresCloudflare = true
                    isChapterLoaded = true
                }
            }
        )
    }

    val effectivePageCount = when {
        localPagePaths.isNotEmpty() -> localPagePaths.size
        livePageUrls.isNotEmpty() -> livePageUrls.size
        else -> 0
    }
    val totalPages = effectivePageCount.coerceAtLeast(1)

    var showReaderHud by remember { mutableStateOf(true) }
    var showQuickTunePopup by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showPagesGridDialog by remember { mutableStateOf(false) }
    var isBookmarked by remember(chapter.id) { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var isWebtoonMode by remember(settings.webtoonAlwaysOn) { mutableStateOf(settings.webtoonAlwaysOn) }

    val webtoonListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPageIndex.coerceAtLeast(0)
    )
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceAtLeast(0),
        pageCount = { totalPages }
    )

    val currentPageIndex by remember {
        derivedStateOf {
            if (isWebtoonMode) {
                webtoonListState.firstVisibleItemIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            } else {
                pagerState.currentPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            }
        }
    }

    LaunchedEffect(currentPageIndex) {
        onSaveReadingProgress(currentPageIndex)
    }

    // Auto-scroll support if enabled in settings
    LaunchedEffect(settings.autoScroll, isChapterLoaded, isWebtoonMode, totalPages) {
        if (settings.autoScroll && isChapterLoaded && isWebtoonMode && effectivePageCount > 0) {
            while (true) {
                delay(1800)
                val next = (webtoonListState.firstVisibleItemIndex + 1).coerceAtMost(totalPages - 1)
                webtoonListState.animateScrollToItem(next)
            }
        }
    }

    val commentsList = remember {
        mutableStateListOf<Pair<String, String>>()
    }
    var newCommentText by remember { mutableStateOf("") }

    fun jumpToPage(targetIndex: Int) {
        val clamped = targetIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
        scope.launch {
            if (isWebtoonMode) {
                webtoonListState.animateScrollToItem(clamped)
            } else {
                pagerState.animateScrollToPage(clamped)
            }
        }
    }

    val prevChapter = remember(work.chapters, chapter.number) {
        work.chapters.find { it.number == chapter.number - 1 }
    }
    val nextChapter = remember(work.chapters, chapter.number) {
        work.chapters.find { it.number == chapter.number + 1 }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C12))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (showQuickTunePopup) {
                            showQuickTunePopup = false
                        } else {
                            showReaderHud = !showReaderHud
                        }
                    },
                    onDoubleTap = {
                        if (settings.webtoonZoom) {
                            zoomScale = if (zoomScale > 1.05f) 1f else 1.65f
                        }
                    }
                )
            }
            .testTag("manga_chapter_reader_screen")
    ) {
        // ==================== MAIN READER AREA (OFFLINE LOCAL FILES OR LIVE CHAPTER PAGES) ====================
        if (!isChapterLoaded) {
            // Exact Loading Screen from 00:39 - 00:47:
            // "التحميل: ٢٧٪" + Horizontal LinearProgressIndicator
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (loadingProgress <= 5) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF8C92A4),
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "التحميل: ${MainViewModel.toArabicDigits(loadingProgress.toString())}٪",
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { loadingProgress / 100f },
                            color = PeriwinklePrimary,
                            trackColor = Color(0xFF2D313E),
                            modifier = Modifier
                                .width(156.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        } else if (effectivePageCount == 0) {
            // Prompt to verify Cloudflare or reload chapter images from website
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PeriwinklePrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (requiresCloudflare) {
                            "يتطلب الفصل تخطي حماية Cloudflare لتحميل صور المانجا من الموقع"
                        } else {
                            "تعذر استخراج صور الفصل تلقائياً — افتح نافذة الحماية لتحميل الصفحات مباشرة"
                        },
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showProtectionModal = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PeriwinklePrimary,
                                contentColor = PeriwinkleOnPrimary
                            )
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تخطي الحماية وجلب الصور", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { refreshTrigger++ }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextPrimaryWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إعادة المحاولة", color = TextPrimaryWhite)
                        }
                    }
                }
            }
        } else {
            if (isWebtoonMode) {
                // Continuous Vertical Webtoon Strip displaying local files or live website images
                LazyColumn(
                    state = webtoonListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale
                        )
                        .testTag("reader_webtoon_list"),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (isOfflineIncomplete) {
                        item(key = "incomplete_warning") {
                            Surface(
                                color = Color(0xFF3E2723),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("reader_incomplete_warning_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF8A80))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "هذا الفصل غير مكتمل (${localPagePaths.size}/${chapter.pageCount} صور)",
                                            color = TextPrimaryWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    TextButton(onClick = { onDownloadChapter(work, chapter) }) {
                                        Text("إعادة التنزيل", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    items(effectivePageCount, key = { "page_$it" }) { pageIdx ->
                        ChapterPageIllustration(
                            work = work,
                            chapterNumber = chapter.number,
                            pageIndex = pageIdx,
                            totalPages = effectivePageCount,
                            pageImageUrl = livePageUrls.getOrNull(pageIdx),
                            localFilePath = localPagePaths.getOrNull(pageIdx),
                            refererUrl = targetChapterUrl
                        )
                    }

                    // End of Chapter Summary & Next Chapter Card
                    item(key = "chapter_footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF12151E))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "نهاية ${chapter.title}",
                                color = TextPrimaryWhite,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (prevChapter != null) {
                                    OutlinedButton(
                                        onClick = { onSelectChapter(prevChapter) },
                                        shape = RoundedCornerShape(22.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("الفصل السابق", color = TextPrimaryWhite)
                                    }
                                }
                                if (nextChapter != null) {
                                    Button(
                                        onClick = { onSelectChapter(nextChapter) },
                                        shape = RoundedCornerShape(22.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PeriwinklePrimary,
                                            contentColor = PeriwinkleOnPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("الفصل التالي", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Horizontal Paged Mode when "وضع الويبتون" is unchecked
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale
                        )
                        .testTag("reader_horizontal_pager")
                ) { pageIdx ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            ChapterPageIllustration(
                                work = work,
                                chapterNumber = chapter.number,
                                pageIndex = pageIdx,
                                totalPages = effectivePageCount,
                                pageImageUrl = livePageUrls.getOrNull(pageIdx),
                                localFilePath = localPagePaths.getOrNull(pageIdx),
                                refererUrl = targetChapterUrl
                            )
                        }
                    }
                }
            }
        }

        // Custom Brightness Dimming Overlay if enabled in settings
        if (settings.customBrightnessEnabled) {
            val dimAlpha = ((100 - settings.customBrightnessLevel).coerceIn(0, 80)) / 120f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        // ==================== TOP READER BAR (00:41 - 00:52) ====================
        AnimatedVisibility(
            visible = showReaderHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = Color(0xF2151821),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimaryWhite
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = work.title,
                            color = TextPrimaryWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isOfflineComplete) "${chapter.title} • بدون إنترنت" else chapter.title,
                            color = if (isOfflineComplete) Color(0xFF81C784) else TextSecondaryGrey,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { showProtectionModal = true },
                        modifier = Modifier.testTag("reader_protection_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "حماية الموقع",
                            tint = if (requiresCloudflare) Color(0xFFFFB74D) else PeriwinklePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { showCommentsDialog = true },
                        modifier = Modifier.testTag("reader_comments_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = "التعليقات",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    IconButton(
                        onClick = { showPagesGridDialog = true },
                        modifier = Modifier.testTag("reader_pages_grid_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = "صفحات وفصول العمل",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ==================== QUICK TUNE POPUP (00:44 - 00:46: وضع الويبتون ☑ / الاعدادات) ====================
        AnimatedVisibility(
            visible = showReaderHud && showQuickTunePopup,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 132.dp)
        ) {
            Surface(
                color = Color(0xFF1F232E),
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .width(205.dp)
                    .testTag("reader_quick_tune_popup")
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updated = !isWebtoonMode
                                isWebtoonMode = updated
                                onUpdateWebtoonMode(updated)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("toggle_webtoon_mode_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Checkbox(
                            checked = isWebtoonMode,
                            onCheckedChange = {
                                isWebtoonMode = it
                                onUpdateWebtoonMode(it)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = PeriwinklePrimary,
                                checkmarkColor = PeriwinkleOnPrimary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "وضع الويبتون",
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showQuickTunePopup = false
                                onOpenReadingSettings()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("open_reading_settings_from_reader"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "الاعدادات",
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==================== BOTTOM READER BAR (00:41 - 00:52) ====================
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = showReaderHud,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xF2141720),
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp)
                    ) {
                        // Row 1: Next/Prev Chapter Circular Buttons + Dotted Page Slider
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (prevChapter != null) Color(0xFF232734) else Color(0xFF191C24),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            if (prevChapter != null) {
                                                onSelectChapter(prevChapter)
                                            } else {
                                                onShowToast("أنت في الفصل الأول")
                                            }
                                        }
                                        .testTag("reader_prev_chapter_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "الفصل السابق",
                                            tint = if (prevChapter != null) TextPrimaryWhite else TextSecondaryGrey.copy(alpha = 0.45f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF232734))
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                        val count = totalPages.coerceIn(2, 24)
                                        val step = size.width / (count - 1)
                                        for (i in 0 until count) {
                                            drawCircle(
                                                color = Color(0xFF6E758A),
                                                radius = 2.5f,
                                                center = Offset(i * step, size.height / 2f)
                                            )
                                        }
                                    }

                                    Slider(
                                        value = currentPageIndex.toFloat(),
                                        onValueChange = { jumpToPage(it.roundToInt()) },
                                        valueRange = 0f..(totalPages - 1).coerceAtLeast(1).toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = PeriwinklePrimary,
                                            activeTrackColor = PeriwinklePrimary,
                                            inactiveTrackColor = Color(0xFF394054)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = if (nextChapter != null) Color(0xFF232734) else Color(0xFF191C24),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            if (nextChapter != null) {
                                                onSelectChapter(nextChapter)
                                            } else {
                                                onShowToast("هذا هو أحدث فصل متاح")
                                            }
                                        }
                                        .testTag("reader_next_chapter_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "الفصل التالي",
                                            tint = if (nextChapter != null) TextPrimaryWhite else TextSecondaryGrey.copy(alpha = 0.45f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: 5 Action Icons from Left to Right (♡, 💾, 🔖, 🔄, 🎛)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleFavorite(work) },
                                    modifier = Modifier.testTag("reader_favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "المفضلة",
                                        tint = if (isFavorite) PeriwinklePrimary else TextPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onDownloadChapter(
                                            work,
                                            chapter.copy(
                                                pageImageUrls = livePageUrls,
                                                pageCount = effectivePageCount.coerceAtLeast(chapter.pageCount)
                                            )
                                        )
                                    },
                                    modifier = Modifier.testTag("reader_save_chapter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "حفظ الفصل",
                                        tint = if (isOfflineComplete) Color(0xFF81C784) else TextPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        isBookmarked = !isBookmarked
                                        onShowToast(
                                            if (isBookmarked) "تم حفظ علامة مرجعية عند الصفحة ${currentPageIndex + 1}"
                                            else "تمت إزالة العلامة المرجعية"
                                        )
                                    },
                                    modifier = Modifier.testTag("reader_bookmark_button")
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "علامة مرجعية",
                                        tint = if (isBookmarked) PeriwinklePrimary else TextPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        zoomScale = if (zoomScale == 1f) 1.25f else 1f
                                        onShowToast(if (zoomScale > 1f) "تم تكبير عرض الصفحة" else "تمت استعادة الحجم الطبيعي")
                                    },
                                    modifier = Modifier.testTag("reader_rotate_fit_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ScreenRotation,
                                        contentDescription = "تدوير وملاءمة الشاشة",
                                        tint = TextPrimaryWhite,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showQuickTunePopup = !showQuickTunePopup },
                                    modifier = Modifier.testTag("reader_tune_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "إعدادات القراءة السريعة",
                                        tint = if (showQuickTunePopup) PeriwinklePrimary else TextPrimaryWhite,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Mini Status Strip: Time on Right, Page "١/١٢" in Center, Battery "68%" on Left
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xCC0A0C12))
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "68%",
                        color = TextPrimaryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (settings.showPageNumber) {
                        val curAr = MainViewModel.toArabicDigits((currentPageIndex + 1).toString())
                        val totAr = MainViewModel.toArabicDigits(totalPages.toString())
                        Text(
                            text = "$curAr/$totAr",
                            color = TextPrimaryWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("reader_page_indicator_text")
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = "٠٨:١٩ م",
                        color = TextPrimaryWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Chapter Comments Dialog
    if (showCommentsDialog) {
        AlertDialog(
            onDismissRequest = { showCommentsDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("تعليقات ${chapter.title}", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (commentsList.isEmpty()) {
                        Text(
                            text = "لا توجد تعليقات مضافة بعد على هذا الفصل.",
                            color = TextSecondaryGrey,
                            fontSize = 13.sp
                        )
                    } else {
                        commentsList.forEach { (comment, time) ->
                            Surface(
                                color = Color(0xFF232734),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(comment, color = TextPrimaryWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(time, color = TextSecondaryGrey, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("أضف تعليقك على الفصل...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            commentsList.add(0, newCommentText.trim() to "الآن")
                            newCommentText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PeriwinklePrimary,
                        contentColor = PeriwinkleOnPrimary
                    )
                ) {
                    Text("إرسال", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentsDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }

    // Pages Grid Jumper Dialog ("⊞" button in Top Bar)
    if (showPagesGridDialog) {
        AlertDialog(
            onDismissRequest = { showPagesGridDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("الانتقال السريع بين الصفحات", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${chapter.title} • اختر الصفحة للانتقال المباشر:",
                        color = TextSecondaryGrey,
                        fontSize = 13.sp
                    )
                    (1..totalPages.coerceAtMost(40)).chunked(4).forEach { rowPages ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowPages.forEach { pageNum ->
                                val isCur = (currentPageIndex + 1) == pageNum
                                OutlinedButton(
                                    onClick = {
                                        showPagesGridDialog = false
                                        jumpToPage(pageNum - 1)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isCur) PeriwinklePrimary else Color.Transparent,
                                        contentColor = if (isCur) PeriwinkleOnPrimary else TextPrimaryWhite
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ص $pageNum", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPagesGridDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }

    // Interactive Cloudflare Protection & Live DOM Extraction Modal
    if (showProtectionModal) {
        CloudflareProtectionDialog(
            url = targetChapterUrl,
            siteName = work.siteName,
            jsExtractorScript = LiveMangaSiteScraper.JS_EXTRACT_CHAPTER_PAGES,
            onExtractedDomJson = { rawJson ->
                val domRes = LiveMangaSiteScraper.parseChapterPagesFromDomJson(rawJson, targetChapterUrl)
                if (domRes.pageUrls.isNotEmpty()) {
                    livePageUrls = domRes.pageUrls
                    requiresCloudflare = false
                    isChapterLoaded = true
                }
            },
            onDismissAndRefresh = {
                showProtectionModal = false
                refreshTrigger++
            }
        )
    }
}
