package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.FavoriteCategoryEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SavedWorkEntity
import com.example.data.local.SiteEntity
import com.example.ui.components.SiteIconBadge
import com.example.ui.theme.AppCardSurface
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.AppDividerColor
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorageLibraryScreen(
    savedWorks: List<SavedWorkEntity>,
    showContinueReadingButton: Boolean,
    onOpenDrawer: () -> Unit,
    onImportUri: (Uri) -> Unit,
    onOpenWork: (SavedWorkEntity) -> Unit,
    onDeleteWork: (SavedWorkEntity) -> Unit,
    onContinueReadingClick: () -> Unit,
    onOpenDownloadsScreen: () -> Unit,
    onOpenStorageSettings: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("آخر الاضافات") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportUri(uri)
        }
    }

    val filteredWorks = remember(savedWorks, searchQuery, sortMode) {
        val matching = if (searchQuery.isBlank()) {
            savedWorks
        } else {
            savedWorks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.sourceUrl.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortMode) {
            "الاسم" -> matching.sortedBy { it.title }
            "الحجم" -> matching.sortedByDescending { it.fileSizeBytes }
            else -> matching.sortedByDescending { it.createdAt }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("storage_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar (01:39-01:40): Menu ☰ on Right, Search 🔍 + More ⋮ on Left
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
                    modifier = Modifier.testTag("open_drawer_button")
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
                        placeholder = { Text("ابحث في التخزين...", fontSize = 13.sp) },
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
                            modifier = Modifier.testTag("storage_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                tint = TextPrimaryWhite
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("storage_more_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "المزيد",
                                    tint = TextPrimaryWhite
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false },
                                containerColor = AppDialogSurface
                            ) {
                                DropdownMenuItem(
                                    text = { Text("استيراد ملف من الجهاز", color = TextPrimaryWhite) },
                                    leadingIcon = { Icon(Icons.Default.FolderOpen, null, tint = PeriwinklePrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        filePickerLauncher.launch(arrayOf("*/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إدارة التنزيلات", color = TextPrimaryWhite) },
                                    leadingIcon = { Icon(Icons.Default.Download, null, tint = PeriwinklePrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenDownloadsScreen()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إحصائيات وإعدادات التخزين", color = TextPrimaryWhite) },
                                    leadingIcon = { Icon(Icons.Default.Storage, null, tint = PeriwinklePrimary) },
                                    onClick = {
                                        showMoreMenu = false
                                        onOpenStorageSettings()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Title Row: "التخزين" on the right, "آخر الاضافات" filter on the left (01:39)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "التخزين",
                    color = TextPrimaryWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("storage_sort_button"),
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
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = AppDialogSurface
                    ) {
                        listOf("آخر الاضافات", "الاسم", "الحجم").forEach { mode ->
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

            if (filteredWorks.isEmpty()) {
                // Exact Empty State from 01:39-01:40:
                // Kaomoji ( ` ‸ ´ ), "لم تحفظ اي عمل في التخزين.", and Periwinkle "استيراد" button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "( ` ‸ ´ )",
                            color = TextPrimaryWhite,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "لم تحفظ اي عمل في التخزين.",
                            color = TextPrimaryWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        Button(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PeriwinklePrimary,
                                contentColor = PeriwinkleOnPrimary
                            ),
                            modifier = Modifier
                                .width(126.dp)
                                .height(46.dp)
                                .testTag("storage_import_button")
                        ) {
                            Text(
                                text = "استيراد",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredWorks, key = { it.id }) { work ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenWork(work) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF283044)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = PeriwinklePrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = work.title,
                                        color = TextPrimaryWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val typeLabel = when (work.fileType) {
                                        "PAGE" -> "صفحة محفوظة"
                                        "DOWNLOAD" -> "تنزيل"
                                        else -> "ملف مستورد"
                                    }
                                    Text(
                                        text = "$typeLabel • ${MainViewModel.formatBytesArabic(work.fileSizeBytes)}",
                                        color = TextSecondaryGrey,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = work.localFilePath,
                                        color = Color(0xFF73798B),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onDeleteWork(work) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف العمل",
                                        tint = Color(0xFFFF6B6B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Exact Bottom-Left "متابعة" Floating Button from 01:39-01:40 & 00:00
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
                    .testTag("continue_reading_button")
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
}

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTaskEntity>,
    onBackClick: () -> Unit,
    onStartNewDownload: (String, String) -> Unit,
    onDeleteTask: (DownloadTaskEntity) -> Unit
) {
    var showAddDownloadDialog by remember { mutableStateOf(false) }
    var downloadTitle by remember { mutableStateOf("") }
    var downloadUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("downloads_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("التنزيلات", color = TextPrimaryWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { showAddDownloadDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PeriwinklePrimary,
                    contentColor = PeriwinkleOnPrimary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تنزيل رابط", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF4D5260),
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("لا توجد عمليات تنزيل حاليًا", color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("يمكنك حفظ الصفحات من المتصفح أو بدء تنزيل مباشر بالرابط.", color = TextSecondaryGrey, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    color = TextPrimaryWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { onDeleteTask(task) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFFF6B6B))
                                }
                            }
                            val statusText = when (task.status) {
                                "DOWNLOADING" -> "جار التنزيل... (${task.progress}%)"
                                "COMPLETED" -> "مكتمل • ${MainViewModel.formatBytesArabic(task.bytesDownloaded)}"
                                "FAILED" -> "فشل التنزيل: ${task.errorMessage ?: ""}"
                                else -> task.status
                            }
                            Text(text = statusText, color = TextSecondaryGrey, fontSize = 13.sp)
                            if (task.status == "DOWNLOADING") {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { task.progress / 100f },
                                    color = PeriwinklePrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showAddDownloadDialog = false },
            containerColor = AppDialogSurface,
            title = { Text("تنزيل صفحة أو ملف", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = downloadTitle,
                        onValueChange = { downloadTitle = it },
                        label = { Text("اسم العمل أو الملف") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("الرابط المباشر (https://...)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (downloadUrl.isNotBlank()) {
                            onStartNewDownload(downloadTitle.ifBlank { downloadUrl }, downloadUrl.trim())
                            showAddDownloadDialog = false
                            downloadTitle = ""
                            downloadUrl = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PeriwinklePrimary, contentColor = PeriwinkleOnPrimary)
                ) {
                    Text("بدء التنزيل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDownloadDialog = false }) {
                    Text("إلغاء", color = PeriwinklePrimary)
                }
            }
        )
    }
}

@Composable
fun RecommendationsScreen(
    enabledSites: List<SiteEntity>,
    onOpenDrawer: () -> Unit,
    onOpenSite: (SiteEntity) -> Unit,
    onOpenManageSites: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("recommendations_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("التوصيات", color = TextPrimaryWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        if (enabledSites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFF4C505E),
                        modifier = Modifier.size(90.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد توصيات متاحة حاليًا",
                        color = TextPrimaryWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "أضف مواقعك المفضلة من شاشة «إدارة المواقع» لتظهر أحدث التوصيات والأعمال هنا.",
                        color = TextSecondaryGrey,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    Button(
                        onClick = onOpenManageSites,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PeriwinklePrimary,
                            contentColor = PeriwinkleOnPrimary
                        )
                    ) {
                        Icon(Icons.Default.Extension, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إدارة المواقع", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "تصفح التوصيات المباشرة من مواقعك المفعلة:",
                        color = TextSecondaryGrey,
                        fontSize = 14.sp
                    )
                }
                items(enabledSites, key = { it.id }) { site ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSite(site) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SiteIconBadge(iconKey = site.iconKey, siteName = site.name, size = 46.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(site.name, color = TextPrimaryWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(site.url, color = TextSecondaryGrey, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "فتح", tint = PeriwinklePrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatesScreen(
    enabledSites: List<SiteEntity>,
    isChecking: Boolean,
    onOpenDrawer: () -> Unit,
    onRefreshUpdates: () -> Unit,
    onOpenSite: (SiteEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("updates_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("التحديثات", color = TextPrimaryWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onRefreshUpdates, enabled = !isChecking) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = PeriwinklePrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = TextPrimaryWhite)
                }
            }
        }

        if (enabledSites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFF4C505E),
                        modifier = Modifier.size(88.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("لا توجد تحديثات فصول جديدة", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("أضف مواقع الأعمال والمفضلة لمتابعة الفصول الجديدة.", color = TextSecondaryGrey, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedButton(
                        onClick = onRefreshUpdates,
                        enabled = !isChecking,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, null, tint = PeriwinklePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isChecking) "جار التحقق من الفصول الجديدة..." else "فحص جميع المواقع المفعلة الآن",
                            color = PeriwinklePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                items(enabledSites, key = { it.id }) { site ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSite(site) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SiteIconBadge(iconKey = site.iconKey, siteName = site.name, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(site.name, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("متصل وجاهز لتصفح أحدث الفصول", color = Color(0xFF81C784), fontSize = 12.sp)
                            }
                            Text("فتح", color = PeriwinklePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteEntity>,
    categories: List<FavoriteCategoryEntity>,
    onOpenDrawer: () -> Unit,
    onOpenFavoriteUrl: (String, String) -> Unit,
    onRemoveFavorite: (FavoriteEntity) -> Unit,
    onManageCategories: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("الكل") }

    val filtered = remember(favorites, selectedCategory) {
        if (selectedCategory == "الكل") favorites
        else favorites.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("favorites_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("المفضلة", color = TextPrimaryWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onManageCategories) {
                Text("فئات المفضلة", color = PeriwinklePrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "الكل",
                        onClick = { selectedCategory = "الكل" },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PeriwinklePrimary,
                            selectedLabelColor = PeriwinkleOnPrimary
                        )
                    )
                }
                items(categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PeriwinklePrimary,
                            selectedLabelColor = PeriwinkleOnPrimary
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFF4C505E),
                        modifier = Modifier.size(88.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("قائمة المفضلة فارغة", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("يمكنك إضافة أي عمل أو صفحة إلى المفضلة من قائمة المتصفح.", color = TextSecondaryGrey, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { fav ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFavoriteUrl(fav.url, fav.title) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fav.title, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(fav.url, color = TextSecondaryGrey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onRemoveFavorite(fav) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFFF6B6B))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    historyItems: List<HistoryEntity>,
    onOpenDrawer: () -> Unit,
    onOpenHistoryUrl: (String, String) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    onClearAllHistory: () -> Unit
) {
    var showConfirmClear by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("history_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة", tint = TextPrimaryWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("السجل", color = TextPrimaryWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            if (historyItems.isNotEmpty()) {
                IconButton(
                    onClick = { showConfirmClear = true },
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "مسح السجل", tint = Color(0xFFFF6B6B))
                }
            }
        }

        if (historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF4C505E),
                        modifier = Modifier.size(88.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("سجل التصفح والقراءة فارغ", color = TextPrimaryWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ستظهر هنا جميع الصفحات والأعمال التي تقوم بزيارتها.", color = TextSecondaryGrey, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyItems, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppCardSurface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenHistoryUrl(item.url, item.title) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = PeriwinklePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = TextPrimaryWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.url, color = TextSecondaryGrey, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(dateFormat.format(Date(item.visitedAt)), color = Color(0xFF72788A), fontSize = 11.sp)
                            }
                            IconButton(onClick = { onDeleteHistoryItem(item.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "حذف", tint = TextSecondaryGrey, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmClear) {
        AlertDialog(
            onDismissRequest = { showConfirmClear = false },
            containerColor = AppDialogSurface,
            title = { Text("مسح السجل", color = TextPrimaryWhite, fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد مسح جميع عناصر سجل التصفح؟", color = TextSecondaryGrey) },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClear = false
                        onClearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE05252), contentColor = Color.White)
                ) {
                    Text("مسح الكل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClear = false }) {
                    Text("إلغاء", color = PeriwinklePrimary)
                }
            }
        )
    }
}
