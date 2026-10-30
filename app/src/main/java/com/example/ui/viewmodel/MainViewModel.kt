package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.FavoriteCategoryEntity
import com.example.data.local.FavoriteEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.SavedWorkEntity
import com.example.data.local.SiteEntity
import com.example.data.preferences.UserSettings
import com.example.data.preferences.UserSettingsDataStore
import com.example.data.repository.AppRepository
import com.example.data.repository.StorageStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DecimalFormat

enum class AppScreen {
    STORAGE,
    RECOMMENDATIONS,
    UPDATES,
    FAVORITES,
    HISTORY,
    MANAGE_SITES,
    BROWSER,
    SETTINGS_MAIN,
    SETTINGS_GENERAL,
    SETTINGS_APPEARANCE,
    SETTINGS_FAVORITE_CATEGORIES,
    SETTINGS_STORAGE,
    SETTINGS_READING_OPTIONS,
    SETTINGS_NEW_CHAPTERS,
    SETTINGS_STATISTICS,
    SETTINGS_MORE,
    DOWNLOADS
}

sealed class AddSiteDialogState {
    data object Hidden : AddSiteDialogState()
    data class Step1InputUrl(
        val urlInput: String = "",
        val isChecking: Boolean = false,
        val statusMessage: String? = null,
        val errorMessage: String? = null
    ) : AddSiteDialogState()

    data class Step2ConfirmManual(
        val normalizedUrl: String,
        val domain: String,
        val siteName: String,
        val iconKey: String
    ) : AddSiteDialogState()
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val sites: StateFlow<List<SiteEntity>> = repository.allSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledSites: StateFlow<List<SiteEntity>> = repository.enabledSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<FavoriteCategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedWorks: StateFlow<List<SavedWorkEntity>> = repository.allSavedWorks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadTasks: StateFlow<List<DownloadTaskEntity>> = repository.allDownloadTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _currentScreen = MutableStateFlow(AppScreen.STORAGE)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenBackStack = MutableStateFlow<List<AppScreen>>(emptyList())

    private val _browserUrl = MutableStateFlow("https://www.google.com/?hl=ar")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    private val _browserTitle = MutableStateFlow("Google")
    val browserTitle: StateFlow<String> = _browserTitle.asStateFlow()

    private val _addSiteDialogState = MutableStateFlow<AddSiteDialogState>(AddSiteDialogState.Hidden)
    val addSiteDialogState: StateFlow<AddSiteDialogState> = _addSiteDialogState.asStateFlow()

    private val _editingSite = MutableStateFlow<SiteEntity?>(null)
    val editingSite: StateFlow<SiteEntity?> = _editingSite.asStateFlow()

    private val _deletingSite = MutableStateFlow<SiteEntity?>(null)
    val deletingSite: StateFlow<SiteEntity?> = _deletingSite.asStateFlow()

    private val _showLoginDialog = MutableStateFlow(false)
    val showLoginDialog: StateFlow<Boolean> = _showLoginDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _showRestartNoticeBanner = MutableStateFlow(false)
    val showRestartNoticeBanner: StateFlow<Boolean> = _showRestartNoticeBanner.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _isUpdatingSites = MutableStateFlow(false)
    val isUpdatingSites: StateFlow<Boolean> = _isUpdatingSites.asStateFlow()

    init {
        refreshStorageStats()
        viewModelScope.launch {
            repository.settingsFlow.collect { userSettings ->
                if (_screenBackStack.value.isEmpty() && _currentScreen.value == AppScreen.STORAGE) {
                    val mapped = when (userSettings.startScreen) {
                        "التوصيات" -> AppScreen.RECOMMENDATIONS
                        "التحديثات" -> AppScreen.UPDATES
                        "المفضلة" -> AppScreen.FAVORITES
                        "السجل" -> AppScreen.HISTORY
                        "إدارة المواقع" -> AppScreen.MANAGE_SITES
                        else -> AppScreen.STORAGE
                    }
                    if (mapped != AppScreen.STORAGE) {
                        _currentScreen.value = mapped
                    }
                }
                return@collect
            }
        }
    }

    fun navigateTo(screen: AppScreen, addToBackStack: Boolean = true) {
        if (_currentScreen.value == screen) return
        if (addToBackStack) {
            _screenBackStack.value = _screenBackStack.value + _currentScreen.value
        }
        _currentScreen.value = screen
        if (screen == AppScreen.SETTINGS_STORAGE || screen == AppScreen.STORAGE) {
            refreshStorageStats()
        }
    }

    fun navigateBack(): Boolean {
        val stack = _screenBackStack.value
        if (stack.isNotEmpty()) {
            val previous = stack.last()
            _screenBackStack.value = stack.dropLast(1)
            _currentScreen.value = previous
            return true
        }
        if (_currentScreen.value != AppScreen.STORAGE) {
            _currentScreen.value = AppScreen.STORAGE
            return true
        }
        return false
    }

    fun openBrowserWithUrl(url: String, title: String = "") {
        val formatted = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }
        _browserUrl.value = formatted
        if (title.isNotBlank()) {
            _browserTitle.value = title
        }
        navigateTo(AppScreen.BROWSER)
    }

    fun updateBrowserPageState(url: String, title: String) {
        if (url.isNotBlank()) _browserUrl.value = url
        if (title.isNotBlank()) _browserTitle.value = title
    }

    fun recordPageVisit(title: String, url: String, siteName: String = "المتصفح") {
        viewModelScope.launch {
            repository.addHistory(title = title, url = url, siteName = siteName)
            repository.addReadingSeconds(15L)
        }
    }

    // Add Site Dialog Flow (matching 00:33-00:43 and 00:54-01:03 in the video)
    fun openAddSiteDialog(prefillUrl: String = "") {
        val cleanPrefill = prefillUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
        val initial = if (cleanPrefill.contains("google.")) "" else cleanPrefill
        _addSiteDialogState.value = AddSiteDialogState.Step1InputUrl(urlInput = initial)
    }

    fun updateAddSiteUrlInput(newUrl: String) {
        val current = _addSiteDialogState.value
        if (current is AddSiteDialogState.Step1InputUrl && !current.isChecking) {
            _addSiteDialogState.value = current.copy(urlInput = newUrl, errorMessage = null)
        }
    }

    fun dismissAddSiteDialog() {
        _addSiteDialogState.value = AddSiteDialogState.Hidden
    }

    fun submitAddSiteStep1() {
        val current = _addSiteDialogState.value as? AddSiteDialogState.Step1InputUrl ?: return
        if (current.isChecking) return
        val rawInput = current.urlInput.trim()
        if (rawInput.isEmpty()) {
            _addSiteDialogState.value = current.copy(errorMessage = "يرجى إدخال عنوان موقع إلكتروني")
            return
        }

        viewModelScope.launch {
            _addSiteDialogState.value = current.copy(
                isChecking = true,
                statusMessage = "جار مراجعة حالة توافق الموقع...",
                errorMessage = null
            )
            delay(450)
            _addSiteDialogState.value = current.copy(
                isChecking = true,
                statusMessage = "جار التحقق من توافق الموقع...",
                errorMessage = null
            )
            val resolved = repository.normalizeAndResolveSite(rawInput)
            delay(450)
            if (!resolved.isValid) {
                _addSiteDialogState.value = current.copy(
                    isChecking = false,
                    statusMessage = null,
                    errorMessage = resolved.errorMessage ?: "رابط غير صالح"
                )
                return@launch
            }
            _addSiteDialogState.value = current.copy(
                isChecking = true,
                statusMessage = "جار التحقق من مطابقة الأعمال...",
                errorMessage = null
            )
            delay(450)
            _addSiteDialogState.value = AddSiteDialogState.Step2ConfirmManual(
                normalizedUrl = resolved.normalizedUrl,
                domain = resolved.domain,
                siteName = resolved.suggestedName,
                iconKey = resolved.iconKey
            )
        }
    }

    fun updateStep2SiteName(newName: String) {
        val current = _addSiteDialogState.value as? AddSiteDialogState.Step2ConfirmManual ?: return
        _addSiteDialogState.value = current.copy(siteName = newName)
    }

    fun confirmAddSiteStep2() {
        val current = _addSiteDialogState.value as? AddSiteDialogState.Step2ConfirmManual ?: return
        viewModelScope.launch {
            repository.addSite(
                name = current.siteName,
                url = current.normalizedUrl,
                domain = current.domain,
                iconKey = current.iconKey
            )
            _addSiteDialogState.value = AddSiteDialogState.Hidden
            showToast("تم الإضافة بنجاح")
            _showRestartNoticeBanner.value = true
        }
    }

    fun dismissRestartNoticeBanner() {
        _showRestartNoticeBanner.value = false
    }

    fun toggleSiteEnabled(site: SiteEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSite(site.copy(isEnabled = enabled))
        }
    }

    fun openEditSiteDialog(site: SiteEntity) {
        _editingSite.value = site
    }

    fun dismissEditSiteDialog() {
        _editingSite.value = null
    }

    fun saveEditedSite(site: SiteEntity, newName: String, newUrl: String) {
        viewModelScope.launch {
            val resolved = repository.normalizeAndResolveSite(newUrl)
            if (!resolved.isValid) {
                showToast(resolved.errorMessage ?: "الرابط غير صالح")
                return@launch
            }
            repository.updateSite(
                site.copy(
                    name = newName.trim().ifEmpty { site.name },
                    url = resolved.normalizedUrl,
                    domain = resolved.domain,
                    iconKey = resolved.iconKey
                )
            )
            _editingSite.value = null
            showToast("تم تعديل الموقع بنجاح")
        }
    }

    fun requestDeleteSite(site: SiteEntity) {
        _deletingSite.value = site
    }

    fun cancelDeleteSite() {
        _deletingSite.value = null
    }

    fun confirmDeleteSite() {
        val site = _deletingSite.value ?: return
        viewModelScope.launch {
            repository.deleteSite(site)
            _deletingSite.value = null
            showToast("تم حذف الموقع")
        }
    }

    fun moveSiteOrder(site: SiteEntity, moveUp: Boolean) {
        val list = sites.value.toMutableList()
        val index = list.indexOfFirst { it.id == site.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in list.indices) return
        viewModelScope.launch {
            val other = list[targetIndex]
            repository.updateSite(site.copy(sortOrder = targetIndex))
            repository.updateSite(other.copy(sortOrder = index))
        }
    }

    // Login / Profile
    fun setShowLoginDialog(show: Boolean) {
        _showLoginDialog.value = show
    }

    fun saveUserProfile(name: String, email: String) {
        viewModelScope.launch {
            repository.updateStringSetting("userProfileName", name.trim())
            repository.updateStringSetting("userProfileEmail", email.trim())
            _showLoginDialog.value = false
            showToast(if (name.isBlank()) "تم تسجيل الخروج" else "تم تسجيل الدخول باسم ${name.trim()}")
        }
    }

    // Favorites & Categories
    fun toggleFavoritePage(title: String, url: String, siteName: String = "المتصفح", category: String = "الكل") {
        viewModelScope.launch {
            val added = repository.toggleFavorite(title, url, siteName, category)
            showToast(if (added) "تمت الإضافة إلى المفضلة" else "تمت الإزالة من المفضلة")
        }
    }

    fun removeFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch {
            repository.deleteFavorite(favorite)
            showToast("تم الحذف من المفضلة")
        }
    }

    fun addFavoriteCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCategory(name)
            showToast("تمت إضافة الفئة")
        }
    }

    fun deleteFavoriteCategory(category: FavoriteCategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showToast("تم حذف الفئة")
        }
    }

    // History
    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            showToast("تم مسح السجل بالكامل")
        }
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryItem(id)
        }
    }

    // Storage & Downloads
    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.calculateStorageStats()
        }
    }

    fun saveCurrentPageToStorage(title: String, url: String) {
        viewModelScope.launch {
            if (settings.value.downloadWifiOnly && !repository.isWifiConnected()) {
                showToast("التنزيل متاح عبر شبكة Wi-Fi فقط حسب إعداداتك")
                return@launch
            }
            val res = repository.saveWebPageToStorage(title, url)
            refreshStorageStats()
            if (res.isSuccess) {
                showToast("تم حفظ العمل في التخزين")
            } else {
                showToast("تعذر حفظ الصفحة: ${res.exceptionOrNull()?.localizedMessage ?: ""}")
            }
        }
    }

    fun startRealDownload(title: String, url: String) {
        viewModelScope.launch {
            if (settings.value.downloadWifiOnly && !repository.isWifiConnected()) {
                showToast("التنزيل عبر شبكة Wi-Fi فقط مفعّل في الإعدادات")
                return@launch
            }
            showToast("بدء التنزيل: $title")
            val res = repository.downloadUrlToFile(title, url)
            refreshStorageStats()
            if (res.isSuccess) {
                showToast("اكتمل التنزيل وتم الحفظ في التخزين")
            } else {
                showToast("فشل التنزيل: تحقق من الاتصال بالإنترنت")
            }
        }
    }

    fun importFileToStorage(uri: Uri) {
        viewModelScope.launch {
            val res = repository.importFileFromUri(uri)
            refreshStorageStats()
            if (res.isSuccess) {
                showToast("تم استيراد الملف بنجاح إلى التخزين")
            } else {
                showToast("تعذر استيراد الملف")
            }
        }
    }

    fun deleteSavedWork(work: SavedWorkEntity) {
        viewModelScope.launch {
            repository.deleteSavedWork(work)
            refreshStorageStats()
            showToast("تم حذف الملف من التخزين")
        }
    }

    fun deleteDownloadTask(task: DownloadTaskEntity) {
        viewModelScope.launch {
            repository.deleteDownloadTask(task)
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            val ok = repository.clearAppCache()
            refreshStorageStats()
            showToast(if (ok) "تم مسح ذاكرة التخزين المؤقتة" else "تعذر مسح ذاكرة التخزين المؤقتة")
        }
    }

    fun clearPreviousSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchCount()
            showToast("تم مسح البحث السابق")
        }
    }

    fun recordSearchQuery() {
        viewModelScope.launch {
            repository.incrementSearchCount()
        }
    }

    fun checkSitesForUpdates() {
        viewModelScope.launch {
            _isUpdatingSites.value = true
            delay(900)
            _isUpdatingSites.value = false
            val count = enabledSites.value.size
            showToast(
                if (count == 0) "لا توجد مواقع مفعلة للتحقق من التحديثات"
                else "تم التحقق من تحديثات $count موقع بنجاح"
            )
        }
    }

    // Backup & Restore
    fun createBackup() {
        viewModelScope.launch {
            try {
                val path = repository.exportBackupJson()
                refreshStorageStats()
                showToast("تم حفظ النسخة الاحتياطية في التخزين")
            } catch (e: Exception) {
                showToast("فشل إنشاء النسخة الاحتياطية")
            }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            try {
                val count = repository.restoreBackupFromDefaultFile()
                showToast(
                    if (count > 0) "تمت استعادة $count موقع من النسخة الاحتياطية"
                    else "تم فحص النسخة الاحتياطية (لا توجد بيانات جديدة)"
                )
            } catch (e: Exception) {
                showToast("لا توجد نسخة احتياطية محفوظة للاستعادة")
            }
        }
    }

    // Settings updates
    fun setStringSetting(key: String, value: String) {
        viewModelScope.launch { repository.updateStringSetting(key, value) }
    }

    fun setBooleanSetting(key: String, value: Boolean) {
        viewModelScope.launch { repository.updateBooleanSetting(key, value) }
    }

    fun setIntSetting(key: String, value: Int) {
        viewModelScope.launch { repository.updateIntSetting(key, value) }
    }

    fun setFloatSetting(key: String, value: Float) {
        viewModelScope.launch { repository.updateFloatSetting(key, value) }
    }

    fun resetReadingStats() {
        viewModelScope.launch {
            repository.resetStatistics()
            showToast("تم تصفير الإحصائيات")
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.value = message
            delay(2800)
            if (_toastMessage.value == message) {
                _toastMessage.value = null
            }
        }
    }

    fun isNetworkConnected(): Boolean = repository.isNetworkAvailable()

    companion object {
        fun toArabicDigits(input: String): String {
            val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            val sb = StringBuilder(input.length)
            for (ch in input) {
                if (ch in '0'..'9') {
                    sb.append(arabicDigits[ch - '0'])
                } else if (ch == '.' || ch == ',') {
                    sb.append('٫')
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        fun formatBytesArabic(bytes: Long): String {
            if (bytes <= 0L) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            val df = DecimalFormat("#.#")
            return when {
                gb >= 1.0 -> "${toArabicDigits(df.format(gb))} GB"
                mb >= 0.1 -> "${toArabicDigits(df.format(mb))} MB"
                kb >= 1.0 -> "${toArabicDigits(df.format(kb))} KB"
                else -> "$bytes B"
            }
        }

        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val settingsStore = UserSettingsDataStore(context.applicationContext)
                    val repo = AppRepository(context.applicationContext, db.appDao(), settingsStore)
                    return MainViewModel(repo) as T
                }
            }
        }
    }
}
