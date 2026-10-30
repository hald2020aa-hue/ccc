package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
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
import com.example.data.MangaChapter
import com.example.data.MangaWork
import com.example.data.preferences.UserSettings
import com.example.ui.components.ChapterPageIllustration
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
    onBackClick: () -> Unit,
    onSelectChapter: (MangaChapter) -> Unit,
    onToggleFavorite: (MangaWork) -> Unit,
    onDownloadChapter: (MangaWork, MangaChapter) -> Unit,
    onUpdateWebtoonMode: (Boolean) -> Unit,
    onOpenReadingSettings: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalPages = chapter.pageCount.coerceAtLeast(8)

    // Smooth loading sequence matching 00:39 - 00:47:
    // ٣٪ -> ١٦٪ -> ٢٧٪ -> ٥١٪ -> ٧٤٪ -> ٩٧٪ -> Loaded!
    var loadingProgress by remember(chapter.id) { mutableIntStateOf(3) }
    var isChapterLoaded by remember(chapter.id) { mutableStateOf(false) }

    LaunchedEffect(chapter.id) {
        isChapterLoaded = false
        val steps = listOf(3, 16, 27, 51, 57, 74, 79, 97, 100)
        for (pct in steps) {
            loadingProgress = pct
            delay(110)
        }
        isChapterLoaded = true
    }

    var showReaderHud by remember { mutableStateOf(true) }
    var showQuickTunePopup by remember { mutableStateOf(false) }
    var showCommentsDialog by remember { mutableStateOf(false) }
    var showPagesGridDialog by remember { mutableStateOf(false) }
    var isBookmarked by remember(chapter.id) { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var isWebtoonMode by remember(settings.webtoonAlwaysOn) { mutableStateOf(settings.webtoonAlwaysOn) }

    val webtoonListState = rememberLazyListState()
    val pagerState = rememberPagerState(pageCount = { totalPages })

    val currentPageIndex by remember {
        derivedStateOf {
            if (isWebtoonMode) {
                webtoonListState.firstVisibleItemIndex.coerceIn(0, totalPages - 1)
            } else {
                pagerState.currentPage.coerceIn(0, totalPages - 1)
            }
        }
    }

    // Auto-scroll support if enabled in settings
    LaunchedEffect(settings.autoScroll, isChapterLoaded, isWebtoonMode) {
        if (settings.autoScroll && isChapterLoaded && isWebtoonMode) {
            while (true) {
                delay(1800)
                val next = (webtoonListState.firstVisibleItemIndex + 1).coerceAtMost(totalPages - 1)
                webtoonListState.animateScrollToItem(next)
            }
        }
    }

    val commentsList = remember {
        mutableStateListOf(
            "أسطورة الفصل! الرسم والحماس في هذا العمل خيالي 🔥" to "منذ ساعة",
            "شكراً لفريق Team-X على الترجمة السريعة والجودة العالية" to "منذ 3 ساعات",
            "تقنية الرمح المائي في الصفحة الثانية رهيبة جداً!" to "منذ 5 ساعات"
        )
    }
    var newCommentText by remember { mutableStateOf("") }

    fun jumpToPage(targetIndex: Int) {
        val clamped = targetIndex.coerceIn(0, totalPages - 1)
        scope.launch {
            if (isWebtoonMode) {
                webtoonListState.animateScrollToItem(clamped)
            } else {
                pagerState.animateScrollToPage(clamped)
            }
        }
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
        // ==================== MAIN READER AREA (LOADING OR PAGES) ====================
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
        } else {
            if (isWebtoonMode) {
                // Continuous Vertical Webtoon Strip (00:48 - 00:52)
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
                    items(totalPages, key = { "page_$it" }) { pageIdx ->
                        ChapterPageIllustration(
                            work = work,
                            chapterNumber = chapter.number,
                            pageIndex = pageIdx,
                            totalPages = totalPages
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

                            if (settings.showCommentsAtEnd) {
                                OutlinedButton(
                                    onClick = { showCommentsDialog = true },
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Comment, null, tint = PeriwinklePrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "عرض التعليقات (${commentsList.size})",
                                        color = PeriwinklePrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val nextChapter = work.chapters.find { it.number == chapter.number + 1 }
                                val prevChapter = work.chapters.find { it.number == chapter.number - 1 }

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
                                totalPages = totalPages
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

                    Text(
                        text = work.title,
                        color = TextPrimaryWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

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
                .align(Alignment.BottomStart) // In RTL, BottomStart is Bottom-Right!
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
                        // Row 1: Next/Prev Chapter Circular Buttons + Dotted Page Slider (00:41 - 00:52)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left circular button: Previous Chapter
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF232734),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            val prev = work.chapters.find { it.number == chapter.number - 1 }
                                            if (prev != null) {
                                                onSelectChapter(prev)
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
                                            tint = TextPrimaryWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Middle Pill Slider with Dots (00:41 - 00:52)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF232734))
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Subtle page dots along the track
                                    Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                                        val count = totalPages.coerceAtLeast(2)
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

                                // Right circular button: Next Chapter
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF232734),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable {
                                            val next = work.chapters.find { it.number == chapter.number + 1 }
                                            if (next != null) {
                                                onSelectChapter(next)
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
                                            tint = TextPrimaryWhite,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: 5 Action Icons from Left to Right (♡, 💾, 🔖, 🔄, 🎛) matching 00:41 - 00:52
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
                                    onClick = { onDownloadChapter(work, chapter) },
                                    modifier = Modifier.testTag("reader_save_chapter_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "حفظ الفصل",
                                        tint = TextPrimaryWhite,
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

            // Bottom Mini Status Strip (00:40 - 00:52): Time on Right, Page "١/١٢" in Center, Battery "68%" on Left
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
                            fontWeight = FontWeight.Bold
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

    // Chapter Comments Dialog ("💬" button in Top Bar or End of Chapter)
    if (showCommentsDialog) {
        AlertDialog(
            onDismissRequest = { showCommentsDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("تعليقات ${chapter.title}", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    // Pages & Chapters Grid Jumper Dialog ("⊞" button in Top Bar)
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
                    (1..totalPages).chunked(4).forEach { rowPages ->
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
}
