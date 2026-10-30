package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MangaChapter
import com.example.data.MangaCatalogData
import com.example.data.MangaWork
import com.example.ui.components.MangaCoverArtwork
import com.example.ui.components.SiteIconBadge
import com.example.ui.theme.AppBackgroundDark
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.AppDividerColor
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MangaDetailsScreen(
    work: MangaWork,
    isFavorite: Boolean,
    downloadedChapterNumbers: Set<Int>,
    onBackClick: () -> Unit,
    onOpenChapter: (MangaWork, MangaChapter) -> Unit,
    onToggleFavorite: (MangaWork) -> Unit,
    onSaveWorkToStorage: (MangaWork) -> Unit,
    onDownloadChapters: (MangaWork, List<MangaChapter>, String) -> Unit,
    onOpenInWebView: (String, String) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoadingChapters by remember(work.id) { mutableStateOf(true) }
    var isChaptersReversed by remember { mutableStateOf(false) }
    var activeChapterMenuId by remember { mutableStateOf<String?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSearchInSitesDialog by remember { mutableStateOf(false) }
    var showDownloadMangaDialog by remember { mutableStateOf(false) }
    var bottomActionHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(work.id) {
        isLoadingChapters = true
        delay(240)
        isLoadingChapters = false
    }

    fun triggerActionHint(label: String) {
        scope.launch {
            bottomActionHint = label
            delay(1600)
            if (bottomActionHint == label) {
                bottomActionHint = null
            }
        }
    }

    val displayedChapters = remember(work.chapters, isChaptersReversed) {
        if (isChaptersReversed) work.chapters.reversed() else work.chapters
    }

    // Two-Pane Split Layout matching 00:16 - 00:38 of the reference video:
    // In RTL:
    // - Right Pane (weight 0.44f): Manga Info, Cover, Status, Type, Rating, Chapters Count, Site, Genres, Story
    // - Left Pane (weight 0.56f): "الفصول" Tab, Chapters List with context menu, and Bottom 5-icon Bar
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundDark)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("manga_details_screen")
    ) {
        // ==================== RIGHT PANE: MANGA INFO & SYNOPSIS ====================
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
        ) {
            // Blurred Cover Artwork Backdrop (00:17 - 00:38)
            MangaCoverArtwork(
                work = work,
                showArabicTitleBanner = false,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(26.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0D1017),
                                Color(0xB310141F),
                                Color(0xD90B0E15)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                // Top Row: Back Arrow + Title (00:16 - 00:38)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("manga_details_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimaryWhite
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = work.title,
                        color = TextPrimaryWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cover Image (Right) + Status & Type Boxes (Left) (00:25)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        MangaCoverArtwork(
                            work = work,
                            modifier = Modifier
                                .width(88.dp)
                                .height(126.dp)
                                .border(1.dp, Color(0xFF343B4D), RoundedCornerShape(8.dp))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xD9171A22),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E3342)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "الحالة: ${work.status}",
                                    color = TextPrimaryWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                )
                            }

                            Surface(
                                color = Color(0xD9171A22),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF2E3342)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "النوع:\n${work.type}",
                                    color = TextPrimaryWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 15.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    // Rating ★ + Read 📖 + Web 🌐 Bar (00:17 - 00:38)
                    Surface(
                        color = Color(0xD9171A22),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E3342)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onShowToast("تقييم العمل: ${work.rating}")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "التقييم",
                                    tint = Color(0xFF9EA4B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = work.rating,
                                    color = TextPrimaryWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = {
                                    val firstChapter = work.chapters.firstOrNull()
                                    if (firstChapter != null) {
                                        onOpenChapter(work, firstChapter)
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("start_reading_icon_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "قراءة الآن",
                                    tint = Color(0xFF9EA4B8),
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onOpenInWebView(work.siteUrl, work.title)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("open_web_protection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "فتح الموقع / الحماية",
                                    tint = Color(0xFF9EA4B8),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    // Chapters Count + Source Site Row (00:25 - 00:38)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xD9171A22),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E3342)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = "عدد الفصول: ${work.chaptersCount}",
                                    color = TextPrimaryWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Surface(
                            color = Color(0xD9171A22),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF2E3342)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = work.siteName.substringAfterLast("- ").trim().ifBlank { work.siteName },
                                    color = TextPrimaryWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                SiteIconBadge(
                                    iconKey = work.siteIconKey,
                                    siteName = work.siteName,
                                    size = 16.dp
                                )
                            }
                        }
                    }

                    // Genre Chips (00:25 - 00:38)
                    work.genres.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            pair.forEach { genre ->
                                Surface(
                                    color = Color(0xD9171A22),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF3E4352)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = genre,
                                        color = TextPrimaryWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Full Arabic Story Synopsis (00:25 - 00:38)
                    Surface(
                        color = Color(0xB312151E),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = work.description,
                            color = TextPrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // ==================== LEFT PANE: CHAPTERS LIST & BOTTOM ACTION BAR ====================
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .background(AppBackgroundDark)
        ) {
            // Top "الفصول" Tab Header (00:16 - 00:38)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "الفصول",
                    color = PeriwinklePrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(PeriwinklePrimary)
                )
                HorizontalDivider(color = AppDividerColor, thickness = 1.dp)
            }

            // Chapters Content Area (00:16-00:24 Skeleton -> 00:25-00:38 Chapters List)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoadingChapters) {
                    // Skeleton Loading State matching 00:16 - 00:24
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        repeat(6) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF282C37))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(26.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF282C37))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF8C92A4),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("chapters_lazy_column"),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(displayedChapters, key = { it.id }) { chapter ->
                            val isSaved = chapter.number in downloadedChapterNumbers
                            val showMenuForThis = activeChapterMenuId == chapter.id

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { onOpenChapter(work, chapter) },
                                            onLongClick = { activeChapterMenuId = chapter.id }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                        .testTag("chapter_row_${chapter.number}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Right side in RTL: Circular Chapter Number Badge (clickable to open download menu too!)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSaved) Color(0xFF1E3B2B) else Color(0xFF1A1D26)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSaved) Color(0xFF4CAF50) else Color(0xFF3E4352),
                                                shape = CircleShape
                                            )
                                            .clickable { activeChapterMenuId = chapter.id }
                                            .testTag("chapter_badge_${chapter.number}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSaved) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "محفوظ",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = chapter.number.toString(),
                                                color = TextPrimaryWhite,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = chapter.title,
                                            color = TextPrimaryWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isSaved) "${chapter.dateText} • تم التنزيل" else chapter.dateText,
                                            color = if (isSaved) Color(0xFF81C784) else TextSecondaryGrey,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Exact Chapter Context Menu from 00:27 - 00:28:
                                // "حفظ هذا الفصل"
                                // "تنزيل هذا الفصل والفصول السابقة"
                                // "تنزيل هذا الفصل والفصول التالية"
                                DropdownMenu(
                                    expanded = showMenuForThis,
                                    onDismissRequest = { activeChapterMenuId = null },
                                    containerColor = Color(0xFF22252F)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "حفظ هذا الفصل",
                                                color = TextPrimaryWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        onClick = {
                                            activeChapterMenuId = null
                                            onDownloadChapters(
                                                work,
                                                listOf(chapter),
                                                "تم حفظ ${chapter.title} في التخزين"
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "تنزيل هذا الفصل والفصول السابقة",
                                                color = TextPrimaryWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        onClick = {
                                            activeChapterMenuId = null
                                            val previousAndCurrent = work.chapters.filter { it.number <= chapter.number }
                                            onDownloadChapters(
                                                work,
                                                previousAndCurrent,
                                                "تم تنزيل ${previousAndCurrent.size} فصل (الفصل ${chapter.number} والسابقة)"
                                            )
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "تنزيل هذا الفصل والفصول التالية",
                                                color = TextPrimaryWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        onClick = {
                                            activeChapterMenuId = null
                                            val nextAndCurrent = work.chapters.filter { it.number >= chapter.number }
                                            onDownloadChapters(
                                                work,
                                                nextAndCurrent,
                                                "تم تنزيل ${nextAndCurrent.size} فصل (الفصل ${chapter.number} والتالية)"
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Floating Tooltip Pill above Bottom Action Bar (00:32, 00:33, 00:34, 00:35)
                androidx.compose.animation.AnimatedVisibility(
                    visible = bottomActionHint != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Surface(
                        color = Color(0xFF282C37),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 6.dp
                    ) {
                        Text(
                            text = bottomActionHint ?: "",
                            color = TextPrimaryWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Bottom 5-Icon Bar matching 00:16 - 00:38 (♡, ⇅, 💾, 🔄, ⋮ from left to right)
            HorizontalDivider(color = AppDividerColor, thickness = 1.dp)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color(0xFF12151E))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Heart Icon: أضف إلى المفضلة (00:35)
                    IconButton(
                        onClick = {
                            triggerActionHint("أضف إلى المفضلة")
                            onToggleFavorite(work)
                        },
                        modifier = Modifier.testTag("details_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "أضف إلى المفضلة",
                            tint = if (isFavorite) PeriwinklePrimary else TextPrimaryWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 2. Reverse Chapters Icon: عكس الفصول (00:34)
                    IconButton(
                        onClick = {
                            isChaptersReversed = !isChaptersReversed
                            triggerActionHint("عكس الفصول")
                        },
                        modifier = Modifier.testTag("details_reverse_chapters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "عكس الفصول",
                            tint = if (isChaptersReversed) PeriwinklePrimary else TextPrimaryWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // 3. Save / Download Icon: حفظ (00:33)
                    IconButton(
                        onClick = {
                            triggerActionHint("حفظ")
                            showDownloadMangaDialog = true
                        },
                        modifier = Modifier.testTag("details_save_manga_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "حفظ وتنزيل المانجا",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // 4. Search in Added Sites / Protection Icon: البحث عن هذا العمل في المواقع المضافة (00:32)
                    IconButton(
                        onClick = {
                            triggerActionHint("البحث عن هذا العمل في المواقع المضافة")
                            showSearchInSitesDialog = true
                        },
                        modifier = Modifier.testTag("details_search_other_sites_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FindReplace,
                            contentDescription = "البحث عن هذا العمل في المواقع المضافة",
                            tint = TextPrimaryWhite,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // 5. Three-dots Overflow Menu (00:30):
                    // تحديث الصفحة / انشاء اختصار / الإبلاغ / شارك / فتح في ...
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("details_more_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "المزيد",
                                tint = TextPrimaryWhite,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            containerColor = Color(0xFF232630)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "تحديث الصفحة",
                                        color = TextPrimaryWhite,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    scope.launch {
                                        isLoadingChapters = true
                                        delay(300)
                                        isLoadingChapters = false
                                        onShowToast("تم تحديث فصول العمل وحالة الحماية")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "انشاء اختصار",
                                        color = TextPrimaryWhite,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onSaveWorkToStorage(work)
                                    onShowToast("تم إنشاء اختصار للعمل في التخزين")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "الإبلاغ",
                                        color = TextPrimaryWhite,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onShowToast("تم إرسال بلاغ فحص الفصل")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "شارك",
                                        color = TextPrimaryWhite,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "${work.title} - ${work.siteUrl}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة العمل"))
                                    } catch (_: Exception) {
                                        onShowToast("تم نسخ رابط المشاركة")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "فتح في ...",
                                        color = TextPrimaryWhite,
                                        fontSize = 14.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.End
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenInWebView(work.siteUrl, work.title)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog for Save / Download Manga or Chapters ("حفظ" button at 00:33)
    if (showDownloadMangaDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadMangaDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("حفظ وتنزيل المانجا أو الفصول", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اختر طريقة حفظ العمل «${work.title}» في التخزين المحلي:",
                        color = TextSecondaryGrey,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            showDownloadMangaDialog = false
                            onSaveWorkToStorage(work)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PeriwinklePrimary,
                            contentColor = PeriwinkleOnPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ المانجا في مكتبة التخزين", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showDownloadMangaDialog = false
                            val firstFive = work.chapters.take(5)
                            onDownloadChapters(work, firstFive, "تم تنزيل أول 5 فصول وحفظ العمل في التخزين")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF283044),
                            contentColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تنزيل أول 5 فصول للقراءة بدون إنترنت", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = {
                            showDownloadMangaDialog = false
                            onDownloadChapters(work, work.chapters, "تم بدء تنزيل جميع الفصول (${work.chapters.size} فصل)")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF283044),
                            contentColor = TextPrimaryWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تنزيل جميع الفصول (${work.chapters.size} فصل)", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDownloadMangaDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }

    // Dialog for Cross-Site Search ("البحث عن هذا العمل في المواقع المضافة" at 00:32)
    if (showSearchInSitesDialog) {
        AlertDialog(
            onDismissRequest = { showSearchInSitesDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("البحث في المواقع المضافة", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "نتائج مطابقة العمل «${work.title}» عبر المواقع المضافة:",
                        color = TextSecondaryGrey,
                        fontSize = 13.sp
                    )
                    MangaCatalogData.defaultAddedSites.take(4).forEach { s ->
                        Surface(
                            color = Color(0xFF232733),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSearchInSitesDialog = false
                                    onShowToast("تم التبديل إلى مصدر: ${s.name}")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SiteIconBadge(iconKey = s.iconKey, siteName = s.name, size = 28.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.name, color = TextPrimaryWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("متوفر • ${work.chaptersCount} فصل • الحماية: متجاوزة ✓", color = Color(0xFF81C784), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchInSitesDialog = false }) {
                    Text("إغلاق", color = PeriwinklePrimary)
                }
            }
        )
    }
}
