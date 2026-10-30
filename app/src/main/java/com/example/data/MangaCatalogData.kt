package com.example.data

import com.example.data.local.SiteEntity

data class MangaChapter(
    val id: String,
    val number: Int,
    val title: String,
    val dateText: String,
    val chapterUrl: String = "",
    val pageImageUrls: List<String> = emptyList(),
    val pageCount: Int = 0,
    val isDownloaded: Boolean = false,
    val isRead: Boolean = false
)

data class MangaWork(
    val id: String,
    val title: String,
    val mangaUrl: String = "",
    val arabicBannerTitle: String = "",
    val siteName: String,
    val siteUrl: String,
    val siteIconKey: String,
    val status: String = "مستمرة",
    val type: String = "مانهوا",
    val rating: String = "0.0",
    val chaptersCount: Int = 0,
    val genres: List<String> = emptyList(),
    val description: String = "",
    val coverStyleIndex: Int = 0,
    val coverImageUrl: String? = null,
    val chapters: List<MangaChapter> = emptyList(),
    val isDetailsLoaded: Boolean = false
)

object MangaCatalogData {

    val siteCategoryChips: List<String> = listOf(
        "التصنيفات",
        "أكشن",
        "إثارة",
        "إيسيكاي",
        "بطل غير إعتيادي",
        "خيال",
        "دموي",
        "نظام",
        "سحر",
        "مغامرات",
        "فنون قتال",
        "مانهوا"
    )

    // Empty as requested: user adds their own manga sites manually
    val defaultAddedSites: List<SiteEntity> = emptyList()

    // Empty as requested: storage only displays works saved/downloaded by the user
    val storageLibraryWorks: List<MangaWork> = emptyList()

    fun getCatalogForSite(site: SiteEntity): List<MangaWork> = emptyList()
}
