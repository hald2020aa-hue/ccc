package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddSiteFlowDialog
import com.example.ui.components.AppNavigationDrawerContent
import com.example.ui.components.BottomToastAndNoticeOverlay
import com.example.ui.components.DeleteSiteConfirmDialog
import com.example.ui.components.EditSiteDialog
import com.example.ui.components.LoginProfileDialog
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FavoriteCategoriesSettingsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.GeneralSettingsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InAppBrowserScreen
import com.example.ui.screens.ManageSitesScreen
import com.example.ui.screens.MoreSettingsScreen
import com.example.ui.screens.NewChaptersSettingsScreen
import com.example.ui.screens.ReadingOptionsSettingsScreen
import com.example.ui.screens.RecommendationsScreen
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.screens.StatisticsSettingsScreen
import com.example.ui.screens.StorageLibraryScreen
import com.example.ui.screens.StorageSettingsScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(context)
            )
            val userSettings by mainViewModel.settings.collectAsStateWithLifecycle()

            MyApplicationTheme(
                appThemeKey = userSettings.appTheme,
                pureDarkMode = userSettings.pureDarkMode
            ) {
                MangaSitesReaderApp(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun MangaSitesReaderApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val enabledSites by viewModel.enabledSites.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val savedWorks by viewModel.savedWorks.collectAsStateWithLifecycle()
    val downloadTasks by viewModel.downloadTasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val addSiteDialogState by viewModel.addSiteDialogState.collectAsStateWithLifecycle()
    val editingSite by viewModel.editingSite.collectAsStateWithLifecycle()
    val deletingSite by viewModel.deletingSite.collectAsStateWithLifecycle()
    val showLoginDialog by viewModel.showLoginDialog.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val showRestartBanner by viewModel.showRestartNoticeBanner.collectAsStateWithLifecycle()
    val browserUrl by viewModel.browserUrl.collectAsStateWithLifecycle()
    val browserTitle by viewModel.browserTitle.collectAsStateWithLifecycle()
    val isUpdatingSites by viewModel.isUpdatingSites.collectAsStateWithLifecycle()

    // Start with the drawer open on first launch so it matches 00:00 of the reference video
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()

    val isDrawerGestureEnabled = currentScreen in setOf(
        AppScreen.STORAGE,
        AppScreen.RECOMMENDATIONS,
        AppScreen.UPDATES,
        AppScreen.FAVORITES,
        AppScreen.HISTORY,
        AppScreen.MANAGE_SITES
    )

    BackHandler(enabled = drawerState.isOpen || currentScreen != AppScreen.STORAGE) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateBack()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isDrawerGestureEnabled,
        drawerContent = {
            AppNavigationDrawerContent(
                currentScreen = currentScreen,
                enabledSites = enabledSites,
                userProfileName = settings.userProfileName,
                showSiteIcons = settings.showSiteIcons,
                onSelectScreen = { target ->
                    scope.launch { drawerState.close() }
                    viewModel.navigateTo(target)
                },
                onOpenSiteInBrowser = { site ->
                    scope.launch { drawerState.close() }
                    viewModel.openBrowserWithUrl(site.url, site.name)
                },
                onLoginClick = {
                    viewModel.setShowLoginDialog(true)
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                AppScreen.STORAGE -> {
                    StorageLibraryScreen(
                        savedWorks = savedWorks,
                        showContinueReadingButton = settings.showContinueReadingButton,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onImportUri = { uri -> viewModel.importFileToStorage(uri) },
                        onOpenWork = { work ->
                            if (work.sourceUrl.startsWith("http")) {
                                viewModel.openBrowserWithUrl(work.sourceUrl, work.title)
                            } else {
                                viewModel.showToast("الملف محفوظ في: ${work.localFilePath}")
                            }
                        },
                        onDeleteWork = { work -> viewModel.deleteSavedWork(work) },
                        onContinueReadingClick = {
                            val lastHistory = history.firstOrNull()
                            if (lastHistory != null) {
                                viewModel.openBrowserWithUrl(lastHistory.url, lastHistory.title)
                            } else {
                                val firstSite = enabledSites.firstOrNull()
                                if (firstSite != null) {
                                    viewModel.openBrowserWithUrl(firstSite.url, firstSite.name)
                                } else {
                                    viewModel.navigateTo(AppScreen.MANAGE_SITES)
                                }
                            }
                        },
                        onOpenDownloadsScreen = { viewModel.navigateTo(AppScreen.DOWNLOADS) },
                        onOpenStorageSettings = { viewModel.navigateTo(AppScreen.SETTINGS_STORAGE) }
                    )
                }

                AppScreen.MANAGE_SITES -> {
                    ManageSitesScreen(
                        sites = sites,
                        onBackClick = {
                            if (!viewModel.navigateBack()) {
                                scope.launch { drawerState.open() }
                            }
                        },
                        onAddSiteClick = { viewModel.openAddSiteDialog() },
                        onOpenBrowserClick = {
                            viewModel.openBrowserWithUrl("https://www.google.com", "Google")
                        },
                        onOpenSiteInBrowser = { site ->
                            viewModel.openBrowserWithUrl(site.url, site.name)
                        },
                        onToggleSiteEnabled = { site, enabled ->
                            viewModel.toggleSiteEnabled(site, enabled)
                        },
                        onEditSite = { site -> viewModel.openEditSiteDialog(site) },
                        onDeleteSite = { site -> viewModel.requestDeleteSite(site) },
                        onMoveSite = { site, up -> viewModel.moveSiteOrder(site, up) }
                    )
                }

                AppScreen.BROWSER -> {
                    InAppBrowserScreen(
                        initialUrl = browserUrl,
                        currentTitle = browserTitle,
                        coachmarkAlreadyShown = settings.browserCoachmarkShown,
                        onUpdatePageState = { url, title ->
                            viewModel.updateBrowserPageState(url, title)
                        },
                        onRecordVisit = { title, url ->
                            viewModel.recordPageVisit(title, url)
                        },
                        onMarkCoachmarkShown = {
                            viewModel.setBooleanSetting("browserCoachmarkShown", true)
                        },
                        onAddSiteFromBrowser = { currentUrl ->
                            viewModel.openAddSiteDialog(prefillUrl = currentUrl)
                        },
                        onToggleFavorite = { title, url ->
                            viewModel.toggleFavoritePage(title, url)
                        },
                        onSavePageToStorage = { title, url ->
                            viewModel.saveCurrentPageToStorage(title, url)
                        },
                        onShowToast = { msg -> viewModel.showToast(msg) },
                        onRecordSearch = { viewModel.recordSearchQuery() },
                        onCloseBrowser = { viewModel.navigateBack() }
                    )
                }

                AppScreen.RECOMMENDATIONS -> {
                    RecommendationsScreen(
                        enabledSites = enabledSites,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenSite = { site -> viewModel.openBrowserWithUrl(site.url, site.name) },
                        onOpenManageSites = { viewModel.navigateTo(AppScreen.MANAGE_SITES) }
                    )
                }

                AppScreen.UPDATES -> {
                    UpdatesScreen(
                        enabledSites = enabledSites,
                        isChecking = isUpdatingSites,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onRefreshUpdates = { viewModel.checkSitesForUpdates() },
                        onOpenSite = { site -> viewModel.openBrowserWithUrl(site.url, site.name) }
                    )
                }

                AppScreen.FAVORITES -> {
                    FavoritesScreen(
                        favorites = favorites,
                        categories = categories,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenFavoriteUrl = { url, title -> viewModel.openBrowserWithUrl(url, title) },
                        onRemoveFavorite = { fav -> viewModel.removeFavorite(fav) },
                        onManageCategories = { viewModel.navigateTo(AppScreen.SETTINGS_FAVORITE_CATEGORIES) }
                    )
                }

                AppScreen.HISTORY -> {
                    HistoryScreen(
                        historyItems = history,
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenHistoryUrl = { url, title -> viewModel.openBrowserWithUrl(url, title) },
                        onDeleteHistoryItem = { id -> viewModel.deleteHistoryEntry(id) },
                        onClearAllHistory = { viewModel.clearAllHistory() }
                    )
                }

                AppScreen.DOWNLOADS -> {
                    DownloadsScreen(
                        tasks = downloadTasks,
                        onBackClick = { viewModel.navigateBack() },
                        onStartNewDownload = { title, url -> viewModel.startRealDownload(title, url) },
                        onDeleteTask = { task -> viewModel.deleteDownloadTask(task) }
                    )
                }

                AppScreen.SETTINGS_MAIN -> {
                    SettingsMainScreen(
                        onBackClick = {
                            if (!viewModel.navigateBack()) {
                                scope.launch { drawerState.open() }
                            }
                        },
                        onNavigateSubScreen = { sub -> viewModel.navigateTo(sub) },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }

                AppScreen.SETTINGS_GENERAL -> {
                    GeneralSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateStartScreen = { valStr -> viewModel.setStringSetting("startScreen", valStr) },
                        onUpdateRecNotifications = { enabled ->
                            viewModel.setBooleanSetting("recommendationNotifications", enabled)
                        }
                    )
                }

                AppScreen.SETTINGS_APPEARANCE -> {
                    AppearanceSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onSelectTheme = { themeKey -> viewModel.setStringSetting("appTheme", themeKey) },
                        onSelectNightMode = { mode -> viewModel.setStringSetting("nightMode", mode) },
                        onToggleBoolean = { key, value -> viewModel.setBooleanSetting(key, value) }
                    )
                }

                AppScreen.SETTINGS_FAVORITE_CATEGORIES -> {
                    FavoriteCategoriesSettingsScreen(
                        categories = categories,
                        onBackClick = { viewModel.navigateBack() },
                        onAddCategory = { name -> viewModel.addFavoriteCategory(name) },
                        onDeleteCategory = { cat -> viewModel.deleteFavoriteCategory(cat) }
                    )
                }

                AppScreen.SETTINGS_STORAGE -> {
                    StorageSettingsScreen(
                        stats = storageStats,
                        searchHistoryCount = settings.searchHistoryCount,
                        onBackClick = { viewModel.navigateBack() },
                        onClearCache = { viewModel.clearAppCache() },
                        onClearSearchHistory = { viewModel.clearPreviousSearchHistory() },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }

                AppScreen.SETTINGS_READING_OPTIONS -> {
                    ReadingOptionsSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateString = { k, v -> viewModel.setStringSetting(k, v) },
                        onUpdateBoolean = { k, v -> viewModel.setBooleanSetting(k, v) },
                        onUpdateInt = { k, v -> viewModel.setIntSetting(k, v) },
                        onUpdateFloat = { k, v -> viewModel.setFloatSetting(k, v) }
                    )
                }

                AppScreen.SETTINGS_NEW_CHAPTERS -> {
                    NewChaptersSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateBoolean = { k, v -> viewModel.setBooleanSetting(k, v) }
                    )
                }

                AppScreen.SETTINGS_STATISTICS -> {
                    StatisticsSettingsScreen(
                        totalReadingSeconds = settings.totalReadingSeconds,
                        sitesCount = sites.size,
                        historyCount = history.size,
                        favoritesCount = favorites.size,
                        onBackClick = { viewModel.navigateBack() },
                        onResetStats = { viewModel.resetReadingStats() }
                    )
                }

                AppScreen.SETTINGS_MORE -> {
                    MoreSettingsScreen(
                        settings = settings,
                        onBackClick = { viewModel.navigateBack() },
                        onUpdateInt = { k, v -> viewModel.setIntSetting(k, v) },
                        onUpdateBoolean = { k, v -> viewModel.setBooleanSetting(k, v) },
                        onCreateBackup = { viewModel.createBackup() },
                        onRestoreBackup = { viewModel.restoreBackup() },
                        onShowToast = { msg -> viewModel.showToast(msg) }
                    )
                }
            }

            // Global Add Site 2-Step Dialog (00:33-00:43, 00:54-01:03)
            AddSiteFlowDialog(
                state = addSiteDialogState,
                onUrlChange = { viewModel.updateAddSiteUrlInput(it) },
                onStep1Submit = { viewModel.submitAddSiteStep1() },
                onStep2NameChange = { viewModel.updateStep2SiteName(it) },
                onStep2Confirm = { viewModel.confirmAddSiteStep2() },
                onDismiss = { viewModel.dismissAddSiteDialog() }
            )

            // Global Edit & Delete Site Dialogs
            EditSiteDialog(
                site = editingSite,
                onSave = { site, newName, newUrl -> viewModel.saveEditedSite(site, newName, newUrl) },
                onDismiss = { viewModel.dismissEditSiteDialog() }
            )

            DeleteSiteConfirmDialog(
                site = deletingSite,
                onConfirm = { viewModel.confirmDeleteSite() },
                onDismiss = { viewModel.cancelDeleteSite() }
            )

            // Global Login / Profile Dialog
            LoginProfileDialog(
                visible = showLoginDialog,
                currentName = settings.userProfileName,
                currentEmail = settings.userProfileEmail,
                onSave = { name, email -> viewModel.saveUserProfile(name, email) },
                onDismiss = { viewModel.setShowLoginDialog(false) }
            )

            // Global Bottom Pill Toast ("تم الإضافة بنجاح") and Restart Notice Banner (00:44, 00:47, 01:04)
            BottomToastAndNoticeOverlay(
                toastMessage = toastMessage,
                showRestartBanner = showRestartBanner && currentScreen != AppScreen.BROWSER,
                onDismissRestartBanner = { viewModel.dismissRestartNoticeBanner() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
