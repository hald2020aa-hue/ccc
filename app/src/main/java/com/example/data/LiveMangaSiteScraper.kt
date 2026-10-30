package com.example.data

import android.webkit.CookieManager
import com.example.data.local.SiteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class CatalogScrapeResult(
    val works: List<MangaWork>,
    val requiresCloudflare: Boolean = false,
    val errorMessage: String? = null
)

data class DetailsScrapeResult(
    val work: MangaWork,
    val requiresCloudflare: Boolean = false,
    val errorMessage: String? = null
)

data class ChapterPagesScrapeResult(
    val pageUrls: List<String>,
    val nextChapterUrl: String? = null,
    val prevChapterUrl: String? = null,
    val requiresCloudflare: Boolean = false,
    val errorMessage: String? = null
)

object LiveMangaSiteScraper {

    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    @Volatile
    var activeUserAgent: String = DEFAULT_USER_AGENT

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getCookiesForUrl(url: String): String {
        if (url.isBlank()) return ""
        return try {
            CookieManager.getInstance()?.getCookie(url) ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * يدمج ملفات تعريف الارتباط (Cookies) من رابط الصورة (CDN) ورابط الصفحة المرجعية (Referer) ورابط الموقع الأصلي
     * لضمان نجاح تحميل الصور التي تتطلب جلسة Cloudflare أو جلسة موقع نشطة.
     */
    fun getCombinedCookies(
        targetUrl: String,
        refererUrl: String? = null,
        siteUrl: String? = null
    ): String {
        val cookiePairs = LinkedHashMap<String, String>()
        listOfNotNull(siteUrl, refererUrl, targetUrl).forEach { url ->
            val rawCookie = getCookiesForUrl(url)
            if (rawCookie.isNotBlank()) {
                rawCookie.split(";").forEach { segment ->
                    val trimmed = segment.trim()
                    if (trimmed.isNotBlank()) {
                        val key = trimmed.substringBefore("=").trim()
                        if (key.isNotBlank()) {
                            cookiePairs[key] = trimmed
                        }
                    }
                }
            }
        }
        return cookiePairs.values.joinToString("; ")
    }

    fun buildRequest(url: String, referer: String? = null, postFormBody: String? = null): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", activeUserAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "ar,en-US;q=0.9,en;q=0.8")
        if (!referer.isNullOrBlank()) {
            builder.header("Referer", referer)
        }
        val cookie = getCombinedCookies(targetUrl = url, refererUrl = referer)
        if (cookie.isNotBlank()) {
            builder.header("Cookie", cookie)
        }
        if (postFormBody != null) {
            builder.header("X-Requested-With", "XMLHttpRequest")
            builder.post(postFormBody.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
        }
        return builder.build()
    }

    fun isCloudflareChallengeHtml(html: String, httpCode: Int = 200): Boolean {
        if (httpCode == 403 || httpCode == 503) {
            if (html.contains("cloudflare", ignoreCase = true) ||
                html.contains("cf-browser-verification", ignoreCase = true) ||
                html.contains("Just a moment", ignoreCase = true) ||
                html.contains("challenge-platform", ignoreCase = true)
            ) {
                return true
            }
        }
        return html.contains("<title>Just a moment...</title>", ignoreCase = true) ||
            html.contains("cf-challenge-running", ignoreCase = true) ||
            html.contains("Enable JavaScript and cookies to continue", ignoreCase = true) ||
            html.contains("تحقق من أنك إنسان", ignoreCase = true)
    }

    fun resolveUrl(baseUrl: String, rawHref: String): String {
        val cleaned = rawHref.trim()
            .replace("&amp;", "&")
            .removePrefix("url(")
            .removeSuffix(")")
            .trim('\'', '"', ' ')
        if (cleaned.isEmpty() || cleaned.startsWith("javascript:") || cleaned.startsWith("data:") || cleaned.startsWith("#")) {
            return ""
        }
        if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
            return cleaned.replace(" ", "%20")
        }
        if (cleaned.startsWith("//")) {
            return "https:${cleaned.replace(" ", "%20")}"
        }
        return try {
            val baseUri = URI(baseUrl.trimEnd('/'))
            val scheme = baseUri.scheme ?: "https"
            val host = baseUri.host ?: return cleaned.replace(" ", "%20")
            val portPart = if (baseUri.port != -1 && baseUri.port != 80 && baseUri.port != 443) ":${baseUri.port}" else ""
            val origin = "$scheme://$host$portPart"
            val normPath = cleaned.replace(" ", "%20")
            if (normPath.startsWith("/")) {
                "$origin$normPath"
            } else {
                "$origin/$normPath"
            }
        } catch (_: Exception) {
            cleaned.replace(" ", "%20")
        }
    }

    fun buildCatalogTargetUrl(
        site: SiteEntity,
        searchQuery: String = "",
        selectedGenre: String? = null
    ): String {
        val base = site.url.trimEnd('/')
        if (searchQuery.isNotBlank()) {
            val encoded = try {
                URLEncoder.encode(searchQuery.trim(), "UTF-8")
            } catch (_: Exception) {
                searchQuery.trim()
            }
            return when {
                base.contains("olympustaff", ignoreCase = true) || base.contains("team", ignoreCase = true) ->
                    "$base/series?search=$encoded"
                else ->
                    "$base/?s=$encoded&post_type=wp-manga"
            }
        }
        if (!selectedGenre.isNullOrBlank() && selectedGenre != "التصنيفات") {
            val encoded = try {
                URLEncoder.encode(selectedGenre.trim(), "UTF-8")
            } catch (_: Exception) {
                selectedGenre.trim()
            }
            return when {
                base.contains("olympustaff", ignoreCase = true) || base.contains("team", ignoreCase = true) ->
                    "$base/series?genre=$encoded"
                else ->
                    "$base/manga-genre/$encoded/"
            }
        }
        return base
    }

    suspend fun fetchSiteCatalog(
        site: SiteEntity,
        searchQuery: String = "",
        selectedGenre: String? = null
    ): CatalogScrapeResult = withContext(Dispatchers.IO) {
        val targetUrl = buildCatalogTargetUrl(site, searchQuery, selectedGenre)
        try {
            val request = buildRequest(targetUrl, referer = site.url)
            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: ""
                if (isCloudflareChallengeHtml(html, response.code)) {
                    return@withContext CatalogScrapeResult(
                        works = emptyList(),
                        requiresCloudflare = true,
                        errorMessage = "الموقع محمي بواسطة Cloudflare، جاري فتح جسر الحماية التلقائي..."
                    )
                }
                var works = parseCatalogHtml(html, site, targetUrl)
                // If homepage had few works and site is olympustaff/team-x or madara, also try /series or /manga
                if (works.size < 3 && searchQuery.isBlank() && selectedGenre == null) {
                    val altPaths = listOf("/series", "/manga", "/series/")
                    for (path in altPaths) {
                        val altUrl = site.url.trimEnd('/') + path
                        try {
                            httpClient.newCall(buildRequest(altUrl, referer = site.url)).execute().use { altResp ->
                                val altHtml = altResp.body?.string() ?: ""
                                val altWorks = parseCatalogHtml(altHtml, site, altUrl)
                                if (altWorks.size > works.size) {
                                    works = altWorks
                                }
                            }
                        } catch (_: Exception) {
                        }
                        if (works.size >= 6) break
                    }
                }
                CatalogScrapeResult(works = works, requiresCloudflare = false)
            }
        } catch (e: Exception) {
            CatalogScrapeResult(
                works = emptyList(),
                requiresCloudflare = false,
                errorMessage = e.localizedMessage ?: "تعذر الاتصال بالموقع"
            )
        }
    }

    fun parseCatalogHtml(html: String, site: SiteEntity, pageUrl: String = site.url): List<MangaWork> {
        if (html.isBlank()) return emptyList()
        val results = LinkedHashMap<String, MangaWork>()

        // Clean script and style blocks to speed up regex scanning
        val bodyHtml = html
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")

        // Strategy 1: Scan structured card blocks (Team-X .bs/.bsx/.box, Madara .page-item-detail/.c-tabs-item__content, MangaStream .utao/.bs)
        val blockRegex = Regex(
            """<(?:div|article|li)[^>]+class=["'][^"']*(?:page-item-detail|c-tabs-item__content|bsx|bs|manga__item|post-item|series-card|animepost|item-summary|utao|box)[^"']*["'][^>]*>([\s\S]{40,3500}?)</(?:div|article|li)>""",
            RegexOption.IGNORE_CASE
        )
        blockRegex.findAll(bodyHtml).forEach { match ->
            val block = match.groupValues[1]
            val work = extractWorkFromCardFragment(block, site, pageUrl)
            if (work != null && work.mangaUrl.isNotBlank() && !results.containsKey(work.mangaUrl)) {
                results[work.mangaUrl] = work
            }
        }

        // Strategy 2: Scan all anchor tags wrapping an <img> that point to manga/series/work paths
        val anchorWithImgRegex = Regex(
            """<a[^>]+href=["']([^"']+)["'][^>]*>([\s\S]{10,1800}?)</a>""",
            RegexOption.IGNORE_CASE
        )
        anchorWithImgRegex.findAll(bodyHtml).forEach { match ->
            val rawHref = match.groupValues[1]
            val inner = match.groupValues[2]
            if (!inner.contains("<img", ignoreCase = true) && !inner.contains("background-image", ignoreCase = true)) {
                return@forEach
            }
            val resolvedHref = resolveUrl(pageUrl, rawHref)
            if (!isLikelyMangaWorkUrl(resolvedHref, site.url)) return@forEach

            val imgUrl = extractBestImageUrl(inner, pageUrl)
            if (imgUrl.isBlank() || isIgnoredAssetImage(imgUrl)) return@forEach

            val rawTitle = extractAttribute(match.value, "title")
                .ifBlank { extractAttribute(inner, "alt") }
                .ifBlank { stripHtmlTags(inner) }
                .let { cleanMangaTitle(it) }

            if (rawTitle.length >= 2 && !results.containsKey(resolvedHref)) {
                val id = slugFromUrl(resolvedHref)
                results[resolvedHref] = MangaWork(
                    id = id,
                    title = rawTitle,
                    mangaUrl = resolvedHref,
                    siteName = site.name,
                    siteUrl = site.url,
                    siteIconKey = site.iconKey,
                    coverImageUrl = imgUrl
                )
            } else if (results.containsKey(resolvedHref)) {
                val existing = results[resolvedHref]!!
                if (existing.coverImageUrl.isNullOrBlank() && imgUrl.isNotBlank()) {
                    results[resolvedHref] = existing.copy(coverImageUrl = imgUrl)
                }
            }
        }

        // Strategy 3: Scan nearby titles for works whose title was only in a sibling heading
        return results.values.toList()
    }

    private fun extractWorkFromCardFragment(fragment: String, site: SiteEntity, baseUrl: String): MangaWork? {
        val hrefRegex = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val hrefs = hrefRegex.findAll(fragment)
            .map { resolveUrl(baseUrl, it.groupValues[1]) }
            .filter { isLikelyMangaWorkUrl(it, site.url) }
            .toList()

        val mangaUrl = hrefs.firstOrNull() ?: return null
        val imgUrl = extractBestImageUrl(fragment, baseUrl)
        if (imgUrl.isBlank() || isIgnoredAssetImage(imgUrl)) return null

        val headingRegex = Regex("""<(?:h2|h3|h4|h5|div|span)[^>]*class=["'][^"']*(?:post-title|tt|title|series-title|name)[^"']*["'][^>]*>([\s\S]*?)</(?:h2|h3|h4|h5|div|span)>""", RegexOption.IGNORE_CASE)
        val headingTitle = headingRegex.find(fragment)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) } ?: ""

        val titleAttr = extractAttribute(fragment, "title")
        val altAttr = extractAttribute(fragment, "alt")

        val title = listOf(headingTitle, titleAttr, altAttr)
            .map { cleanMangaTitle(it) }
            .firstOrNull { it.length >= 2 && !it.equals("مانجا", ignoreCase = true) }
            ?: cleanMangaTitle(slugFromUrl(mangaUrl).replace("-", " "))

        if (title.isBlank()) return null

        return MangaWork(
            id = slugFromUrl(mangaUrl),
            title = title,
            mangaUrl = mangaUrl,
            siteName = site.name,
            siteUrl = site.url,
            siteIconKey = site.iconKey,
            coverImageUrl = imgUrl
        )
    }

    suspend fun fetchMangaDetails(work: MangaWork): DetailsScrapeResult = withContext(Dispatchers.IO) {
        val targetUrl = work.mangaUrl.ifBlank { "${work.siteUrl.trimEnd('/')}/series/${work.id}" }
        try {
            val request = buildRequest(targetUrl, referer = work.siteUrl)
            httpClient.newCall(request).execute().use { response ->
                val html = response.body?.string() ?: ""
                if (isCloudflareChallengeHtml(html, response.code)) {
                    return@withContext DetailsScrapeResult(
                        work = work,
                        requiresCloudflare = true,
                        errorMessage = "يتطلب الموقع تخطي حماية Cloudflare لعرض الفصول"
                    )
                }
                var parsedWork = parseMangaDetailsHtml(html, work, targetUrl)

                // Madara WordPress AJAX chapters fallback if chapters weren't in initial HTML
                if (parsedWork.chapters.isEmpty()) {
                    val ajaxUrl = targetUrl.trimEnd('/') + "/ajax/chapters/"
                    try {
                        httpClient.newCall(buildRequest(ajaxUrl, referer = targetUrl, postFormBody = "")).execute().use { ajaxResp ->
                            val ajaxHtml = ajaxResp.body?.string() ?: ""
                            val ajaxChapters = parseChaptersFromHtmlFragment(ajaxHtml, work.id, targetUrl)
                            if (ajaxChapters.isNotEmpty()) {
                                parsedWork = parsedWork.copy(
                                    chapters = ajaxChapters,
                                    chaptersCount = ajaxChapters.size,
                                    isDetailsLoaded = true
                                )
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                DetailsScrapeResult(work = parsedWork, requiresCloudflare = false)
            }
        } catch (e: Exception) {
            DetailsScrapeResult(
                work = work,
                requiresCloudflare = false,
                errorMessage = e.localizedMessage ?: "تعذر جلب تفاصيل المانجا"
            )
        }
    }

    fun parseMangaDetailsHtml(html: String, baseWork: MangaWork, pageUrl: String): MangaWork {
        if (html.isBlank()) return baseWork

        // Title extraction
        val h1Title = Regex("""<h1[^>]*>([\s\S]*?)</h1>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.let { cleanMangaTitle(stripHtmlTags(it)) }
            ?.takeIf { it.isNotBlank() }
        val ogTitle = extractMetaProperty(html, "og:title")
            .let { cleanMangaTitle(it) }
            .takeIf { it.isNotBlank() }

        // Cover extraction
        val summaryImgBlock = Regex(
            """<(?:div|figure)[^>]+class=["'][^"']*(?:summary_image|thumb|white-bg|series-thumb|manga-cover|poster)[^"']*["'][^>]*>([\s\S]{10,1200}?)</(?:div|figure)>""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.getOrNull(1) ?: ""
        val coverUrl = extractBestImageUrl(summaryImgBlock, pageUrl)
            .ifBlank { extractMetaProperty(html, "og:image") }
            .let { resolveUrl(pageUrl, it) }
            .ifBlank { baseWork.coverImageUrl ?: "" }

        // Description / Story extraction
        val descRegex = Regex(
            """<(?:div|p)[^>]+class=["'][^"']*(?:summary__content|description-summary|entry-content-single|review-content|story|manga-excerpt|desc|synopsis)[^"']*["'][^>]*>([\s\S]{15,6000}?)</(?:div|p)>""",
            RegexOption.IGNORE_CASE
        )
        val rawDesc = descRegex.find(html)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it) }
            ?.ifBlank { extractMetaProperty(html, "og:description") }
            ?.ifBlank { extractMetaName(html, "description") }
            ?: baseWork.description

        // Status extraction
        val status = when {
            html.contains("مكتملة") || html.contains("Completed", ignoreCase = true) -> "مكتملة"
            html.contains("متوقفة") || html.contains("Hiatus", ignoreCase = true) -> "متوقفة"
            else -> "مستمرة"
        }

        // Type extraction
        val type = when {
            html.contains("مانها") || html.contains("Manhua", ignoreCase = true) || html.contains("صيني") -> "مانها\nصيني"
            html.contains("مانهوا") || html.contains("Manhwa", ignoreCase = true) || html.contains("كوري") -> "مانهوا\nكورية"
            html.contains("مانجا") || html.contains("Manga", ignoreCase = true) || html.contains("ياباني") -> "مانجا\nياباني"
            else -> baseWork.type
        }

        // Rating extraction
        val ratingMatch = Regex("""(?:post-total-rating|rating-prc|numscore|score)[^>]*>[\s\S]{0,120}?([0-9]\.[0-9])""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1) ?: baseWork.rating

        // Genres extraction
        val genres = mutableListOf<String>()
        val genreSectionRegex = Regex(
            """<(?:div|span|ul)[^>]+class=["'][^"']*(?:genres-content|mgen|wp-manga-tags-list|genre-info|review-author-info|seriestugenre)[^"']*["'][^>]*>([\s\S]{10,2500}?)</(?:div|span|ul)>""",
            RegexOption.IGNORE_CASE
        )
        genreSectionRegex.findAll(html).forEach { section ->
            Regex("""<a[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
                .findAll(section.groupValues[1])
                .forEach { aMatch ->
                    val g = stripHtmlTags(aMatch.groupValues[1]).trim()
                    if (g.length in 2..24 && g !in genres) {
                        genres.add(g)
                    }
                }
        }

        // Chapters extraction
        val chapters = parseChaptersFromHtmlFragment(html, baseWork.id, pageUrl)

        return baseWork.copy(
            title = h1Title ?: ogTitle ?: baseWork.title,
            coverImageUrl = coverUrl.ifBlank { baseWork.coverImageUrl },
            description = rawDesc.ifBlank { "لا يوجد وصف متاح لهذا العمل على الموقع." },
            status = status,
            type = type,
            rating = ratingMatch,
            genres = if (genres.isNotEmpty()) genres.take(8) else baseWork.genres,
            chapters = chapters,
            chaptersCount = chapters.size,
            isDetailsLoaded = true
        )
    }

    fun parseChaptersFromHtmlFragment(html: String, workId: String, pageUrl: String): List<MangaChapter> {
        val chapterMap = LinkedHashMap<String, MangaChapter>()

        // Match <li class="wp-manga-chapter ..."> or <li data-num="..."> or chapter list anchors
        val liRegex = Regex(
            """<li[^>]*class=["'][^"']*(?:wp-manga-chapter|ch-main-anchor|eph-num|chapter-li)[^"']*["'][^>]*>([\s\S]{10,1500}?)</li>""",
            RegexOption.IGNORE_CASE
        )
        liRegex.findAll(html).forEach { match ->
            val block = match.groupValues[1]
            val aMatch = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE).find(block)
            if (aMatch != null) {
                val chUrl = resolveUrl(pageUrl, aMatch.groupValues[1])
                val rawText = stripHtmlTags(aMatch.groupValues[2]).trim()
                val dateMatch = Regex("""<(?:span|i)[^>]+class=["'][^"']*(?:chapter-release-date|chapterdate|date)[^"']*["'][^>]*>([\s\S]*?)</(?:span|i)>""", RegexOption.IGNORE_CASE)
                    .find(block)?.groupValues?.getOrNull(1)?.let { stripHtmlTags(it).trim() } ?: ""
                if (chUrl.isNotBlank()) {
                    val chNum = extractChapterNumber(rawText, chUrl, chapterMap.size + 1)
                    val title = rawText.ifBlank { "الفصل $chNum" }
                    chapterMap[chUrl] = MangaChapter(
                        id = "${workId}_ch_${chNum}_${chapterMap.size}",
                        number = chNum,
                        title = title,
                        dateText = dateMatch,
                        chapterUrl = chUrl
                    )
                }
            }
        }

        // Broad scan for any chapter links on the page (Team-X / OlympuStaff / MangaStream / Custom)
        val anchorRegex = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>([\s\S]{1,600}?)</a>""", RegexOption.IGNORE_CASE)
        anchorRegex.findAll(html).forEach { match ->
            val chUrl = resolveUrl(pageUrl, match.groupValues[1])
            if (chUrl.isBlank() || chapterMap.containsKey(chUrl)) return@forEach
            val innerHtml = match.groupValues[2]
            val text = stripHtmlTags(innerHtml).replace(Regex("\\s+"), " ").trim()

            val looksLikeChapterUrl = isLikelyChapterUrl(chUrl, pageUrl)
            val looksLikeChapterText = text.contains("فصل") ||
                text.contains("الفصل") ||
                text.contains("Chapter", ignoreCase = true) ||
                text.contains("Ch.", ignoreCase = true)

            if (looksLikeChapterUrl || (looksLikeChapterText && chUrl.startsWith(pageUrl.substringBefore("/series")))) {
                val chNum = extractChapterNumber(text, chUrl, chapterMap.size + 1)
                val cleanTitle = text.ifBlank { "الفصل $chNum" }
                chapterMap[chUrl] = MangaChapter(
                    id = "${workId}_ch_${chNum}_${chapterMap.size}",
                    number = chNum,
                    title = cleanTitle,
                    dateText = "",
                    chapterUrl = chUrl
                )
            }
        }

        val list = chapterMap.values.toList()
        // Sort ascending by chapter number (1, 2, 3...) matching 00:25 of the reference video, or keep site order
        return if (list.size > 1) {
            list.sortedBy { it.number }
        } else {
            list
        }
    }

    suspend fun fetchChapterPages(
        work: MangaWork,
        chapter: MangaChapter
    ): ChapterPagesScrapeResult = withContext(Dispatchers.IO) {
        val targetUrl = chapter.chapterUrl.ifBlank {
            "${work.siteUrl.trimEnd('/')}/series/${work.id}/${chapter.number}"
        }
        try {
            val request = buildRequest(targetUrl, referer = work.mangaUrl.ifBlank { work.siteUrl })
            httpClient.newCall(request).execute().use { response ->
                val httpCode = response.code
                val html = response.body?.string() ?: ""
                if (isCloudflareChallengeHtml(html, httpCode)) {
                    DiagnosticLogger.logWarn(
                        category = DiagnosticCategory.SCRAPER,
                        message = "صفحة الفصل تتطلب تخطي حماية Cloudflare",
                        sourceId = work.sourceId.toString(),
                        sourceName = work.siteName,
                        workId = work.id,
                        chapterId = chapter.id,
                        url = targetUrl,
                        httpCode = httpCode
                    )
                    return@withContext ChapterPagesScrapeResult(
                        pageUrls = emptyList(),
                        requiresCloudflare = true,
                        errorMessage = "يتطلب الفصل تخطي حماية Cloudflare"
                    )
                }
                if (!response.isSuccessful && html.isBlank()) {
                    DiagnosticLogger.logError(
                        category = DiagnosticCategory.SCRAPER,
                        message = "فشل جلب صفحة الفصل (HTTP $httpCode)",
                        sourceId = work.sourceId.toString(),
                        sourceName = work.siteName,
                        workId = work.id,
                        chapterId = chapter.id,
                        url = targetUrl,
                        httpCode = httpCode
                    )
                    return@withContext ChapterPagesScrapeResult(
                        pageUrls = emptyList(),
                        requiresCloudflare = false,
                        errorMessage = "فشل الاتصال بصفحة الفصل (HTTP $httpCode)"
                    )
                }
                val pages = parseChapterPagesHtml(html, targetUrl)
                DiagnosticLogger.logInfo(
                    category = DiagnosticCategory.SCRAPER,
                    message = "تم استخراج ${pages.size} صفحة للفصل ${chapter.number} من HTML",
                    sourceId = work.sourceId.toString(),
                    sourceName = work.siteName,
                    workId = work.id,
                    chapterId = chapter.id,
                    url = targetUrl,
                    httpCode = httpCode
                )
                ChapterPagesScrapeResult(
                    pageUrls = pages,
                    requiresCloudflare = false,
                    errorMessage = if (pages.isEmpty()) "لم يتم العثور على صور داخل صفحة الفصل" else null
                )
            }
        } catch (e: Exception) {
            DiagnosticLogger.logError(
                category = DiagnosticCategory.SCRAPER,
                message = "استثناء أثناء جلب صفحات الفصل: ${e.localizedMessage}",
                sourceId = work.sourceId.toString(),
                sourceName = work.siteName,
                workId = work.id,
                chapterId = chapter.id,
                url = targetUrl
            )
            ChapterPagesScrapeResult(
                pageUrls = emptyList(),
                requiresCloudflare = false,
                errorMessage = e.localizedMessage ?: "تعذر تحميل صور الفصل"
            )
        }
    }

    /**
     * يستخرج جميع روابط صور الفصل بالترتيب الأصلي مع إزالة الروابط المكررة،
     * ويعالج حاويات القراءة المتداخلة (.reading-content / .page-break / #readerarea)
     * ومصفوفات JSON داخل السكربتات دون التوقف عند الصورة الأولى.
     */
    fun parseChapterPagesHtml(html: String, chapterUrl: String): List<String> {
        if (html.isBlank()) return emptyList()
        val images = LinkedHashSet<String>()

        // 1. Check ts_reader.run({... "images": [...] }), chapter_preloaded_images, or JSON arrays of page URLs
        val jsonArrayPatterns = listOf(
            Regex("""["'](?:images|pages|page_urls|chapter_images)["']\s*:\s*\[([\s\S]*?)\]""", RegexOption.IGNORE_CASE),
            Regex("""(?:chapter_preloaded_images|chapterImages|pagesList|image_list)\s*=\s*\[([\s\S]*?)\]""", RegexOption.IGNORE_CASE)
        )
        for (pattern in jsonArrayPatterns) {
            pattern.findAll(html).forEach { match ->
                val arrayContent = match.groupValues[1]
                Regex("""["']((?:https?:)?//[^"']+|/[^"']+\.(?:jpg|jpeg|png|webp)(?:\?[^"']*)?)["']""", RegexOption.IGNORE_CASE)
                    .findAll(arrayContent)
                    .forEach { urlMatch ->
                        val rawUrl = urlMatch.groupValues[1].replace("\\/", "/").trim()
                        val resolved = resolveUrl(chapterUrl, rawUrl)
                        if (resolved.isNotBlank() && !isIgnoredAssetImage(resolved) && isLikelyChapterPageImage(resolved)) {
                            images.add(resolved)
                        }
                    }
            }
        }

        // 2. Slice HTML from the reader container start up to comments/footer/related sections
        // (Avoids the non-greedy </div> bug where nested <div class="page-break"> stopped after page 1!)
        val readerStartRegex = Regex(
            """<(?:div|section|main|article)[^>]+(?:id|class)=["'][^"']*(?:reading-content|readerarea|chapter-content|images_chapter|vung-doc|container-chapter-reader|rdminimal|chapter-reader|reader-area|entry-content)[^"']*["'][^>]*>""",
            RegexOption.IGNORE_CASE
        )
        val readerStartMatch = readerStartRegex.find(html)
        if (readerStartMatch != null) {
            val startIndex = readerStartMatch.range.first
            val tailHtml = html.substring(startIndex)
            val footerBoundaryRegex = Regex(
                """<(?:div|section|footer|aside)[^>]+(?:id|class)=["'][^"']*(?:comments-area|disqus_thread|c-blog__heading|related-manga|manga-discussion|wp-manga-nav|site-footer|footer-area)[^"']*["']""",
                RegexOption.IGNORE_CASE
            )
            val endOffset = footerBoundaryRegex.find(tailHtml)?.range?.first ?: minOf(tailHtml.length, 350_000)
            val readerSectionHtml = tailHtml.substring(0, endOffset)
            extractAllImageUrlsFromFragment(readerSectionHtml, chapterUrl).forEach { url ->
                if (!isIgnoredAssetImage(url) && isLikelyChapterPageImage(url)) {
                    images.add(url)
                }
            }
        }

        // 3. Scan all .page-break blocks or explicit reader <img> tags across the entire document
        val pageBreakRegex = Regex(
            """<div[^>]+class=["'][^"']*(?:page-break|image-item|chapter-image|page-item)[^"']*["'][^>]*>([\s\S]*?)</div>""",
            RegexOption.IGNORE_CASE
        )
        pageBreakRegex.findAll(html).forEach { match ->
            extractAllImageUrlsFromFragment(match.groupValues[1], chapterUrl).forEach { url ->
                if (!isIgnoredAssetImage(url) && isLikelyChapterPageImage(url)) {
                    images.add(url)
                }
            }
        }

        val imgTagRegex = Regex("""<img[^>]+>""", RegexOption.IGNORE_CASE)
        imgTagRegex.findAll(html).forEach { match ->
            val tag = match.value
            val isExplicitReaderImg = tag.contains("wp-manga-chapter-img", ignoreCase = true) ||
                tag.contains("chapter-img", ignoreCase = true) ||
                tag.contains("page-img", ignoreCase = true) ||
                tag.contains("ts-main-image", ignoreCase = true) ||
                tag.contains("data-page", ignoreCase = true) ||
                tag.contains("data-index", ignoreCase = true) ||
                tag.contains("image-", ignoreCase = true)

            if (isExplicitReaderImg) {
                val url = extractBestImageUrl(tag, chapterUrl)
                if (url.isNotBlank() && !isIgnoredAssetImage(url) && isLikelyChapterPageImage(url)) {
                    images.add(url)
                }
            }
        }

        // 4. Fallback: If fewer than 2 images were found above, scan ALL <img> tags on the page
        // (Never use `images.isEmpty()` inside the loop condition, which previously caused stopping after 1 image!)
        if (images.size < 2) {
            imgTagRegex.findAll(html).forEach { match ->
                val url = extractBestImageUrl(match.value, chapterUrl)
                if (url.isNotBlank() && !isIgnoredAssetImage(url) && isLikelyChapterPageImage(url)) {
                    images.add(url)
                }
            }
        }

        return images.toList()
    }

    // ==================== JAVASCRIPT DOM BRIDGE EXTRACTORS ====================

    val JS_EXTRACT_CATALOG = """
        (function() {
            var title = document.title || "";
            var bodyText = document.body ? document.body.innerText : "";
            var isCf = title.indexOf("Just a moment") !== -1 ||
                       !!document.querySelector("#cf-wrapper, .cf-browser-verification, iframe[src*='challenges.cloudflare.com']");
            var items = [];
            var seen = {};
            var selectors = [
                ".page-item-detail", ".c-tabs-item__content", ".bs .bsx", ".bs", ".listupd .utao",
                ".box", ".manga__item", ".series-card", ".animepost", "article", ".item", ".post-item"
            ];
            var cards = document.querySelectorAll(selectors.join(","));
            function getImgUrl(el) {
                if (!el) return "";
                var img = el.tagName === "IMG" ? el : el.querySelector("img");
                if (img) {
                    var src = img.getAttribute("data-src") || img.getAttribute("data-lazy-src") ||
                              img.getAttribute("data-original") || img.currentSrc || img.src || "";
                    if (src && src.indexOf("data:image") === -1) return src;
                }
                var bgEl = el.querySelector("[style*='background-image']") || el;
                var style = window.getComputedStyle(bgEl).backgroundImage;
                if (style && style !== "none" && style.indexOf("url(") !== -1) {
                    return style.replace(/^url\(["']?/, "").replace(/["']?\)$/, "");
                }
                return "";
            }
            for (var i = 0; i < cards.length; i++) {
                var c = cards[i];
                var a = c.querySelector("a[href]");
                if (!a || !a.href) continue;
                var href = a.href;
                if (seen[href]) continue;
                var imgUrl = getImgUrl(c);
                if (!imgUrl) continue;
                var heading = c.querySelector("h2, h3, h4, h5, .post-title, .tt, .title, .name");
                var t = (heading ? heading.innerText : (a.getAttribute("title") || a.innerText || "")).trim();
                if (!t) {
                    var im = c.querySelector("img[alt]");
                    if (im) t = im.getAttribute("alt").trim();
                }
                if (t && t.length >= 2) {
                    seen[href] = true;
                    items.push({ title: t.split("\n")[0].trim(), url: href, cover: imgUrl });
                }
            }
            if (items.length < 3) {
                var links = document.querySelectorAll("a[href]");
                for (var j = 0; j < links.length; j++) {
                    var lnk = links[j];
                    var h = lnk.href;
                    if (!h || seen[h]) continue;
                    var im2 = lnk.querySelector("img");
                    if (!im2) continue;
                    var u2 = getImgUrl(im2);
                    if (!u2) continue;
                    var t2 = (lnk.getAttribute("title") || im2.getAttribute("alt") || lnk.innerText || "").trim();
                    if (!t2 && lnk.parentElement) {
                        t2 = lnk.parentElement.innerText.trim();
                    }
                    if (t2 && t2.length >= 2) {
                        seen[h] = true;
                        items.push({ title: t2.split("\n")[0].trim(), url: h, cover: u2 });
                    }
                }
            }
            return JSON.stringify({ cf: isCf, count: items.length, items: items.slice(0, 80) });
        })();
    """.trimIndent()

    val JS_EXTRACT_DETAILS = """
        (function() {
            var title = document.title || "";
            var isCf = title.indexOf("Just a moment") !== -1 ||
                       !!document.querySelector("#cf-wrapper, .cf-browser-verification, iframe[src*='challenges.cloudflare.com']");
            var h1 = document.querySelector("h1, .post-title h1, .entry-title");
            var workTitle = h1 ? h1.innerText.trim() : title;
            var coverEl = document.querySelector(".summary_image img, .thumb img, .white-bg img, .series-thumb img, .poster img, meta[property='og:image']");
            var cover = "";
            if (coverEl) {
                cover = coverEl.getAttribute("content") || coverEl.getAttribute("data-src") || coverEl.getAttribute("data-lazy-src") || coverEl.currentSrc || coverEl.src || "";
            }
            var descEl = document.querySelector(".summary__content, .description-summary, .review-content, .entry-content-single, .story, .desc, .synopsis");
            var desc = descEl ? descEl.innerText.trim() : "";
            var genres = [];
            var gLinks = document.querySelectorAll(".genres-content a, .mgen a, .wp-manga-tags-list a, .genre-info a, .review-author-info a, a[href*='genre']");
            for (var i = 0; i < gLinks.length; i++) {
                var gt = gLinks[i].innerText.trim();
                if (gt && gt.length >= 2 && gt.length <= 24 && genres.indexOf(gt) === -1) {
                    genres.push(gt);
                }
            }
            var chapters = [];
            var seenCh = {};
            var chSelectors = [
                ".wp-manga-chapter a", ".eplister ul li a", "#chapterlist li a",
                ".clstyle li a", ".chapters-list a", ".ch-main-anchor a",
                "a[href*='/chapter']", "a[href*='/%d8%a7%d9%84%d9%81%d8%b5%d9%84']"
            ];
            var chNodes = document.querySelectorAll(chSelectors.join(","));
            if (chNodes.length === 0) {
                chNodes = document.querySelectorAll("a[href]");
            }
            for (var j = 0; j < chNodes.length; j++) {
                var a = chNodes[j];
                var href = a.href;
                if (!href || seenCh[href]) continue;
                var txt = (a.innerText || "").replace(/\s+/g, " ").trim();
                var isCh = a.closest(".wp-manga-chapter, .eplister, #chapterlist, .clstyle") !== null ||
                           txt.indexOf("فصل") !== -1 || txt.indexOf("الفصل") !== -1 ||
                           /chapter|ch\./i.test(txt) || /\/[0-9]+(\/|$)/.test(href);
                if (isCh && href !== window.location.href) {
                    seenCh[href] = true;
                    var li = a.closest("li, tr, .chapter-item");
                    var dateEl = li ? li.querySelector(".chapter-release-date, .chapterdate, .date, span i") : null;
                    var dateTxt = dateEl ? dateEl.innerText.trim() : "";
                    chapters.push({ title: txt || ("الفصل " + (chapters.length + 1)), url: href, date: dateTxt });
                }
            }
            return JSON.stringify({
                cf: isCf,
                title: workTitle,
                cover: cover,
                desc: desc,
                genres: genres.slice(0, 8),
                chapters: chapters.slice(0, 500)
            });
        })();
    """.trimIndent()

    val JS_EXTRACT_CHAPTER_PAGES = """
        (function() {
            var title = document.title || "";
            var isCf = title.indexOf("Just a moment") !== -1 ||
                       !!document.querySelector("#cf-wrapper, .cf-browser-verification, iframe[src*='challenges.cloudflare.com']");
            var urls = [];
            var seen = {};
            function addUrl(raw) {
                if (!raw || typeof raw !== "string") return;
                var u = raw.trim();
                if (!u || u.indexOf("data:image") === 0 || u.indexOf("javascript:") === 0) return;
                var lower = u.toLowerCase();
                if (lower.indexOf("logo") !== -1 || lower.indexOf("avatar") !== -1 ||
                    lower.indexOf("favicon") !== -1 || lower.indexOf("gravatar") !== -1 ||
                    lower.indexOf("spinner") !== -1 || lower.indexOf("loading") !== -1 ||
                    lower.indexOf(".svg") !== -1) return;
                if (!seen[u]) {
                    seen[u] = true;
                    urls.push(u);
                }
            }
            try {
                if (window.chapter_preloaded_images && Array.isArray(window.chapter_preloaded_images)) {
                    for (var p = 0; p < window.chapter_preloaded_images.length; p++) {
                        addUrl(window.chapter_preloaded_images[p]);
                    }
                }
                if (window.chapterImages && Array.isArray(window.chapterImages)) {
                    for (var c = 0; c < window.chapterImages.length; c++) {
                        addUrl(window.chapterImages[c]);
                    }
                }
            } catch (e) {}
            var readerSelectors = [
                ".reading-content img", "#readerarea img", ".page-break img",
                ".chapter-content img", "#images_chapter img", "img.wp-manga-chapter-img",
                ".vung-doc img", ".container-chapter-reader img", ".rdminimal img",
                ".chapter-reader img", ".entry-content img"
            ];
            var readerImgs = document.querySelectorAll(readerSelectors.join(","));
            for (var i = 0; i < readerImgs.length; i++) {
                var im = readerImgs[i];
                var src = im.getAttribute("data-src") || im.getAttribute("data-lazy-src") ||
                          im.getAttribute("data-original") || im.getAttribute("data-url") ||
                          im.getAttribute("data-cfsrc") || im.getAttribute("data-pagespeed-lazy-src") ||
                          im.currentSrc || im.src || "";
                addUrl(src);
            }
            if (urls.length < 2) {
                var allImgs = document.querySelectorAll("main img, article img, img");
                for (var j = 0; j < allImgs.length; j++) {
                    var im2 = allImgs[j];
                    var src2 = im2.getAttribute("data-src") || im2.getAttribute("data-lazy-src") ||
                               im2.getAttribute("data-original") || im2.getAttribute("data-url") ||
                               im2.currentSrc || im2.src || "";
                    var hasLazyAttr = !!(im2.getAttribute("data-src") || im2.getAttribute("data-lazy-src") || im2.getAttribute("data-original"));
                    var w = im2.naturalWidth || im2.width || (hasLazyAttr ? 400 : 250);
                    if (w >= 160) {
                        addUrl(src2);
                    }
                }
            }
            return JSON.stringify({ cf: isCf, pages: urls });
        })();
    """.trimIndent()

    fun parseCatalogFromDomJson(rawJsonResult: String, site: SiteEntity): CatalogScrapeResult {
        val unwrapped = unwrapEvaluateJsString(rawJsonResult)
        if (unwrapped.isBlank() || unwrapped == "null") {
            return CatalogScrapeResult(emptyList())
        }
        return try {
            val obj = JSONObject(unwrapped)
            val isCf = obj.optBoolean("cf", false)
            val arr = obj.optJSONArray("items") ?: JSONArray()
            val list = mutableListOf<MangaWork>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val url = resolveUrl(site.url, item.optString("url"))
                val cover = resolveUrl(site.url, item.optString("cover"))
                val title = cleanMangaTitle(item.optString("title"))
                if (url.isNotBlank() && title.isNotBlank() && isLikelyMangaWorkUrl(url, site.url)) {
                    list.add(
                        MangaWork(
                            id = slugFromUrl(url),
                            title = title,
                            mangaUrl = url,
                            siteName = site.name,
                            siteUrl = site.url,
                            siteIconKey = site.iconKey,
                            coverImageUrl = cover.ifBlank { null }
                        )
                    )
                }
            }
            CatalogScrapeResult(works = list, requiresCloudflare = isCf && list.isEmpty())
        } catch (_: Exception) {
            CatalogScrapeResult(emptyList())
        }
    }

    fun parseDetailsFromDomJson(rawJsonResult: String, baseWork: MangaWork): DetailsScrapeResult {
        val unwrapped = unwrapEvaluateJsString(rawJsonResult)
        if (unwrapped.isBlank() || unwrapped == "null") {
            return DetailsScrapeResult(baseWork)
        }
        return try {
            val obj = JSONObject(unwrapped)
            val isCf = obj.optBoolean("cf", false)
            val title = cleanMangaTitle(obj.optString("title")).ifBlank { baseWork.title }
            val cover = resolveUrl(baseWork.siteUrl, obj.optString("cover")).ifBlank { baseWork.coverImageUrl ?: "" }
            val desc = obj.optString("desc").trim().ifBlank { baseWork.description }

            val genres = mutableListOf<String>()
            val gArr = obj.optJSONArray("genres")
            if (gArr != null) {
                for (i in 0 until gArr.length()) {
                    val g = gArr.optString(i).trim()
                    if (g.isNotBlank()) genres.add(g)
                }
            }

            val chList = mutableListOf<MangaChapter>()
            val cArr = obj.optJSONArray("chapters")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val cObj = cArr.getJSONObject(i)
                    val chUrl = resolveUrl(baseWork.siteUrl, cObj.optString("url"))
                    val chTitle = cObj.optString("title").trim()
                    val chDate = cObj.optString("date").trim()
                    if (chUrl.isNotBlank()) {
                        val num = extractChapterNumber(chTitle, chUrl, i + 1)
                        chList.add(
                            MangaChapter(
                                id = "${baseWork.id}_ch_${num}_$i",
                                number = num,
                                title = chTitle.ifBlank { "الفصل $num" },
                                dateText = chDate,
                                chapterUrl = chUrl
                            )
                        )
                    }
                }
            }

            val sortedChapters = if (chList.size > 1) chList.sortedBy { it.number } else chList
            val updated = baseWork.copy(
                title = title,
                coverImageUrl = cover.ifBlank { baseWork.coverImageUrl },
                description = desc.ifBlank { "لا يوجد وصف متاح لهذا العمل." },
                genres = if (genres.isNotEmpty()) genres else baseWork.genres,
                chapters = if (sortedChapters.isNotEmpty()) sortedChapters else baseWork.chapters,
                chaptersCount = if (sortedChapters.isNotEmpty()) sortedChapters.size else baseWork.chaptersCount,
                isDetailsLoaded = true
            )
            DetailsScrapeResult(work = updated, requiresCloudflare = isCf && sortedChapters.isEmpty())
        } catch (_: Exception) {
            DetailsScrapeResult(baseWork)
        }
    }

    fun parseChapterPagesFromDomJson(rawJsonResult: String, chapterUrl: String): ChapterPagesScrapeResult {
        val unwrapped = unwrapEvaluateJsString(rawJsonResult)
        if (unwrapped.isBlank() || unwrapped == "null") {
            return ChapterPagesScrapeResult(emptyList())
        }
        return try {
            val obj = JSONObject(unwrapped)
            val isCf = obj.optBoolean("cf", false)
            val pArr = obj.optJSONArray("pages") ?: JSONArray()
            val urls = mutableListOf<String>()
            for (i in 0 until pArr.length()) {
                val u = resolveUrl(chapterUrl, pArr.optString(i))
                if (u.isNotBlank() && !isIgnoredAssetImage(u)) {
                    urls.add(u)
                }
            }
            ChapterPagesScrapeResult(pageUrls = urls, requiresCloudflare = isCf && urls.isEmpty())
        } catch (_: Exception) {
            ChapterPagesScrapeResult(emptyList())
        }
    }

    private fun unwrapEvaluateJsString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return try {
                JSONArray("[$trimmed]").getString(0)
            } catch (_: Exception) {
                trimmed.substring(1, trimmed.length - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
            }
        }
        return trimmed
    }

    // ==================== HELPER UTILITIES ====================

    private fun extractBestImageUrl(fragment: String, baseUrl: String): String {
        val singleUrlAttributes = listOf(
            "data-src",
            "data-lazy-src",
            "data-original",
            "data-url",
            "data-cfsrc",
            "data-pagespeed-lazy-src",
            "data-echo",
            "data-full-url"
        )
        for (attr in singleUrlAttributes) {
            val raw = extractAttribute(fragment, attr).trim()
            if (raw.isNotBlank() && !raw.startsWith("data:image", ignoreCase = true)) {
                val resolved = resolveUrl(baseUrl, raw)
                if (resolved.isNotBlank()) return resolved
            }
        }
        val srcsetAttributes = listOf("data-srcset", "srcset")
        for (attr in srcsetAttributes) {
            val raw = extractAttribute(fragment, attr).trim()
            if (raw.isNotBlank() && !raw.startsWith("data:image", ignoreCase = true)) {
                val firstCandidate = raw.substringBefore(",").trim().substringBefore(" ")
                val resolved = resolveUrl(baseUrl, firstCandidate)
                if (resolved.isNotBlank()) return resolved
            }
        }
        val rawSrc = extractAttribute(fragment, "src").trim()
        if (rawSrc.isNotBlank() && !rawSrc.startsWith("data:image", ignoreCase = true)) {
            val resolved = resolveUrl(baseUrl, rawSrc)
            if (resolved.isNotBlank()) return resolved
        }
        // Check inline style background-image
        val bgRegex = Regex("""background-image\s*:\s*url\(([^)]+)\)""", RegexOption.IGNORE_CASE)
        val bgMatch = bgRegex.find(fragment)?.groupValues?.getOrNull(1)
        if (!bgMatch.isNullOrBlank()) {
            return resolveUrl(baseUrl, bgMatch)
        }
        return ""
    }

    private fun extractAllImageUrlsFromFragment(fragment: String, baseUrl: String): List<String> {
        val list = mutableListOf<String>()
        Regex("""<img[^>]+>""", RegexOption.IGNORE_CASE).findAll(fragment).forEach { m ->
            val u = extractBestImageUrl(m.value, baseUrl)
            if (u.isNotBlank()) list.add(u)
        }
        return list
    }

    private fun extractAttribute(tagOrFragment: String, attrName: String): String {
        val regex = Regex("""\b$attrName\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(tagOrFragment)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractMetaProperty(html: String, prop: String): String {
        val r1 = Regex("""<meta[^>]+property=["']$prop["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val r2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']$prop["']""", RegexOption.IGNORE_CASE)
        return (r1.find(html) ?: r2.find(html))?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractMetaName(html: String, name: String): String {
        val r1 = Regex("""<meta[^>]+name=["']$name["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val r2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+name=["']$name["']""", RegexOption.IGNORE_CASE)
        return (r1.find(html) ?: r2.find(html))?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun stripHtmlTags(input: String): String {
        return input
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .replace("&#8217;", "'")
            .replace("&#8211;", "-")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun cleanMangaTitle(raw: String): String {
        return raw
            .replace(Regex("\\s+"), " ")
            .removePrefix("مانجا ")
            .removePrefix("مانهوا ")
            .substringBefore(" - مانجا ليك")
            .substringBefore(" - أزورا")
            .substringBefore(" | Team-X")
            .substringBefore(" - Team-X")
            .trim()
    }

    private fun isLikelyMangaWorkUrl(url: String, siteUrl: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        val baseLower = siteUrl.trimEnd('/').lowercase(Locale.ROOT)
        if (lower == baseLower || lower == "$baseLower/") return false
        val rejectedSegments = listOf(
            "/wp-login", "/wp-admin", "/login", "/register", "/contact", "/privacy",
            "/terms", "/dmca", "/discord", "/facebook", "/twitter", "/tag/", "/manga-genre/",
            "/genre/", "/page/", "?s=", "/author/", "/artist/", "/bookmark", "/history"
        )
        if (rejectedSegments.any { lower.contains(it) }) return false
        // Exclude direct chapter links from the main catalog works list
        if (Regex("""/(?:chapter|ch|فصل|الفصل)[-_/\d]""").containsMatchIn(lower)) return false
        return lower.contains("/manga/") ||
            lower.contains("/series/") ||
            lower.contains("/work/") ||
            lower.contains("/comic/") ||
            lower.contains("/manhwa/") ||
            lower.startsWith(baseLower)
    }

    private fun isLikelyChapterUrl(url: String, mangaPageUrl: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        val basePage = mangaPageUrl.trimEnd('/').lowercase(Locale.ROOT)
        if (lower == basePage || lower == "$basePage/") return false
        if (lower.startsWith(basePage) && lower.length > basePage.length + 1) {
            return true
        }
        return lower.contains("/chapter") ||
            lower.contains("الفصل") ||
            Regex("""/series/[^/]+/\d+""").containsMatchIn(lower) ||
            Regex("""/manga/[^/]+/[^/]+""").containsMatchIn(lower)
    }

    private fun isIgnoredAssetImage(imgUrl: String): Boolean {
        val lower = imgUrl.lowercase(Locale.ROOT)
        return lower.contains("logo") ||
            lower.contains("favicon") ||
            lower.contains("avatar") ||
            lower.contains("gravatar") ||
            lower.contains("spinner") ||
            lower.contains("loading") ||
            lower.endsWith(".svg") ||
            lower.endsWith(".gif") && lower.contains("lazy")
    }

    private fun isLikelyChapterPageImage(imgUrl: String): Boolean {
        val lower = imgUrl.lowercase(Locale.ROOT)
        return lower.contains(".jpg") ||
            lower.contains(".jpeg") ||
            lower.contains(".png") ||
            lower.contains(".webp") ||
            lower.contains("wp-content/uploads") ||
            lower.contains("chapter") ||
            lower.contains("manga") ||
            lower.contains("cdn")
    }

    private fun slugFromUrl(url: String): String {
        val cleaned = url.trimEnd('/').substringBefore("?").substringAfterLast("/")
        return cleaned.ifBlank { "work_${url.hashCode()}" }
    }

    private fun extractChapterNumber(title: String, url: String, fallback: Int): Int {
        val numInTitle = Regex("""(\d+)""").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (numInTitle != null) return numInTitle
        val tail = url.trimEnd('/').substringAfterLast("/")
        val numInUrl = Regex("""(\d+)""").find(tail)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return numInUrl ?: fallback
    }
}
