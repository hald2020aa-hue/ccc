package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MangaCatalogData
import com.example.data.MangaWork
import com.example.data.local.SiteEntity
import com.example.ui.components.MangaCoverArtwork
import com.example.ui.components.MangaPlaceholderCoverBox
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import kotlinx.coroutines.delay

@Composable
fun SiteCatalogScreen(
    site: SiteEntity,
    works: List<MangaWork>,
    showContinueReadingButton: Boolean,
    onOpenDrawer: () -> Unit,
    onSelectWork: (MangaWork) -> Unit,
    onContinueReadingClick: () -> Unit,
    onOpenSiteInWebView: (SiteEntity) -> Unit,
    onShowToast: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf("آخر التحديثات") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showProtectionDialog by remember { mutableStateOf(false) }

    // 3-stage loading effect matching 00:07 -> 00:11 -> 00:15 of the reference video:
    // Stage 0: Center circular refresh icon (00:07-00:10)
    // Stage 1: Placeholder cards grid with titles (00:11-00:12)
    // Stage 2: Full covers loaded into cards (00:13-00:15)
    var loadStage by remember(site.id, site.url) { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(site.id, site.url, refreshTrigger) {
        loadStage = 0
        delay(280)
        loadStage = 1
        delay(260)
        loadStage = 2
    }

    val filteredWorks = remember(works, searchQuery, selectedGenre, sortMode) {
        val byQuery = if (searchQuery.isBlank()) {
            works
        } else {
            works.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.arabicBannerTitle.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        val byGenre = if (selectedGenre.isNullOrBlank()) {
            byQuery
        } else {
            byQuery.filter { work -> work.genres.any { it.contains(selectedGenre!!, ignoreCase = true) } }
                .ifEmpty { byQuery }
        }
        when (sortMode) {
            "الأكثر شعبية" -> byGenre.sortedByDescending { it.chaptersCount }
            "الأبجدية" -> byGenre.sortedBy { it.title }
            else -> byGenre
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("site_catalog_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar (00:07 - 00:15): Menu ☰ on Right, Search 🔍 + Protection 🛡️ + Filter on Left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("site_catalog_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "القائمة الجانبية",
                        tint = TextPrimaryWhite
                    )
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث في ${site.name}...", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PeriwinklePrimary,
                            unfocusedBorderColor = Color(0xFF454957),
                            focusedTextColor = TextPrimaryWhite,
                            unfocusedTextColor = TextPrimaryWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("site_catalog_search_input")
                    )
                    IconButton(onClick = {
                        searchQuery = ""
                        showSearchBar = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق البحث", tint = TextPrimaryWhite)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSearchBar = true },
                            modifier = Modifier.testTag("site_catalog_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = TextPrimaryWhite
                            )
                        }
                        IconButton(
                            onClick = { showProtectionDialog = true },
                            modifier = Modifier.testTag("site_protection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "الحماية وتخطي Cloudflare",
                                tint = PeriwinklePrimary
                            )
                        }
                        IconButton(
                            onClick = { showCategoriesDialog = true },
                            modifier = Modifier.testTag("site_filter_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "تصفية",
                                tint = TextPrimaryWhite
                            )
                        }
                    }
                }
            }

            // Site Title + "آخر التحديثات" Sort Row (00:07 - 00:15)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = site.name.substringAfterLast("- ").trim().ifBlank { site.name },
                    color = TextPrimaryWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("site_sort_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "ترتيب",
                            tint = Color(0xFFD5DAE6),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sortMode,
                            color = Color(0xFFD5DAE6),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = AppDialogSurface
                    ) {
                        listOf("آخر التحديثات", "الأكثر شعبية", "الأبجدية").forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode, color = TextPrimaryWhite) },
                                onClick = {
                                    sortMode = mode
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Horizontal Scrollable Genre / Category Chips Row (00:07 - 00:15)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(MangaCatalogData.siteCategoryChips) { chip ->
                    val isCategoriesButton = chip == "التصنيفات"
                    val isSelected = selectedGenre == chip
                    val containerColor = when {
                        isSelected -> PeriwinklePrimary
                        isCategoriesButton -> Color(0xFF1F232E)
                        else -> Color.Transparent
                    }
                    val textColor = if (isSelected) PeriwinkleOnPrimary else TextPrimaryWhite
                    val borderColor = if (isSelected) PeriwinklePrimary else Color(0xFF4E5363)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = containerColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clickable {
                                if (isCategoriesButton) {
                                    showCategoriesDialog = true
                                } else {
                                    selectedGenre = if (selectedGenre == chip) null else chip
                                }
                            }
                            .testTag("genre_chip_$chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCategoriesButton) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = chip,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Content Area: Stage 0 Spinner (00:07-00:10) -> Stage 1/2 Grid (00:11-00:15)
            if (loadStage == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "جار التحميل",
                        tint = Color(0xFF8C92A4),
                        modifier = Modifier.size(64.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 106.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("site_works_grid"),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredWorks, key = { it.id }) { work ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectWork(work) }
                                .testTag("manga_card_${work.id}")
                        ) {
                            if (loadStage == 1) {
                                MangaPlaceholderCoverBox(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.71f)
                                )
                            } else {
                                MangaCoverArtwork(
                                    work = work,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.71f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = work.title,
                                color = TextPrimaryWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Bottom-Left Floating "متابعة" Button (00:07 - 00:15)
        if (showContinueReadingButton) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2A324B),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 18.dp)
                    .clickable { onContinueReadingClick() }
                    .testTag("site_continue_reading_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = Color(0xFFD5DEFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1E2B))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "متابعة",
                            color = TextPrimaryWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Categories Dialog when clicking "⊞ التصنيفات"
    if (showCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showCategoriesDialog = false },
            containerColor = AppDialogSurface,
            title = {
                Text("تصنيفات ${site.name}", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر التصنيف لعرض المانجا والمانهوا المطابقة:",
                        color = TextSecondaryGrey,
                        fontSize = 13.sp
                    )
                    MangaCatalogData.siteCategoryChips.drop(1).chunked(3).forEach { rowChips ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowChips.forEach { genre ->
                                val active = selectedGenre == genre
                                OutlinedButton(
                                    onClick = {
                                        selectedGenre = if (active) null else genre
                                        showCategoriesDialog = false
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (active) PeriwinklePrimary else Color.Transparent,
                                        contentColor = if (active) PeriwinkleOnPrimary else TextPrimaryWhite
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(genre, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedGenre = null
                        showCategoriesDialog = false
                    }
                ) {
                    Text("عرض الكل", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoriesDialog = false }) {
                    Text("إغلاق", color = TextSecondaryGrey)
                }
            }
        )
    }

    // Protection (الحماية / Cloudflare Bypass) Dialog
    if (showProtectionDialog) {
        AlertDialog(
            onDismissRequest = { showProtectionDialog = false },
            containerColor = AppDialogSurface,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text("حماية الموقع (Cloudflare)", color = TextPrimaryWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "حالة الاتصال بموقع «${site.name}»: متصل ومفعل داخل التطبيق بدون حظر.",
                        color = TextPrimaryWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "يقوم التطبيق تلقائياً بتخطي حماية Cloudflare وتحويل صفحات المانجا والمانهوا إلى واجهة التطبيق الأصلية مع دعم تحميل الفصول والقراءة المباشرة.",
                        color = TextSecondaryGrey,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProtectionDialog = false
                        refreshTrigger++
                        onShowToast("تم تحديث جلسة الحماية بنجاح")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PeriwinklePrimary,
                        contentColor = PeriwinkleOnPrimary
                    )
                ) {
                    Text("تحديث الجلسة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showProtectionDialog = false
                        onOpenSiteInWebView(site)
                    }
                ) {
                    Icon(Icons.Default.OpenInBrowser, null, tint = PeriwinklePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("فتح في المتصفح", color = PeriwinklePrimary)
                }
            }
        )
    }
}
