package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.LiveMangaSiteScraper
import com.example.data.MangaWork
import com.example.ui.theme.AppDialogSurface
import com.example.ui.theme.PeriwinkleOnPrimary
import com.example.ui.theme.PeriwinklePrimary
import com.example.ui.theme.TextPrimaryWhite
import com.example.ui.theme.TextSecondaryGrey

@Composable
fun MangaPlaceholderCoverBox(
    modifier: Modifier = Modifier
) {
    // Neutral dark placeholder card matching 00:11-00:12 while real website covers load
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222630)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Image,
            contentDescription = null,
            tint = Color(0xFF4D5366),
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
fun MangaCoverArtwork(
    work: MangaWork,
    modifier: Modifier = Modifier,
    showArabicTitleBanner: Boolean = false
) {
    val context = LocalContext.current
    val coverUrl = work.coverImageUrl?.trim().orEmpty()

    if (coverUrl.isBlank()) {
        MangaPlaceholderCoverBox(modifier = modifier)
        return
    }

    val imageRequest = remember(coverUrl, work.siteUrl) {
        val builder = ImageRequest.Builder(context)
            .data(coverUrl)
            .crossfade(true)
            .addHeader("User-Agent", LiveMangaSiteScraper.activeUserAgent)

        if (work.siteUrl.isNotBlank()) {
            builder.addHeader("Referer", work.siteUrl)
        }
        val cookie = LiveMangaSiteScraper.getCookiesForUrl(coverUrl)
        if (cookie.isNotBlank()) {
            builder.addHeader("Cookie", cookie)
        }
        builder.build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = work.title,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        loading = {
            MangaPlaceholderCoverBox(modifier = Modifier.fillMaxSize())
        },
        error = {
            MangaPlaceholderCoverBox(modifier = Modifier.fillMaxSize())
        }
    )
}

@Composable
fun ChapterPageIllustration(
    work: MangaWork,
    chapterNumber: Int,
    pageIndex: Int,
    totalPages: Int,
    pageImageUrl: String? = null,
    refererUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var retryNonce by remember { mutableIntStateOf(0) }
    val cleanUrl = pageImageUrl?.trim().orEmpty()

    if (cleanUrl.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(Color(0xFF0E1118)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    color = PeriwinklePrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "جاري جلب الصفحة ${pageIndex + 1} من الموقع...",
                    color = TextSecondaryGrey,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    val imageRequest = remember(cleanUrl, refererUrl, retryNonce) {
        val builder = ImageRequest.Builder(context)
            .data(cleanUrl)
            .crossfade(true)
            .addHeader("User-Agent", LiveMangaSiteScraper.activeUserAgent)
            .setParameter("retry_nonce", retryNonce)

        val effectiveReferer = refererUrl?.ifBlank { work.mangaUrl.ifBlank { work.siteUrl } } ?: work.siteUrl
        if (effectiveReferer.isNotBlank()) {
            builder.addHeader("Referer", effectiveReferer)
        }
        val cookie = LiveMangaSiteScraper.getCookiesForUrl(cleanUrl)
        if (cookie.isNotBlank()) {
            builder.addHeader("Cookie", cookie)
        }
        builder.build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "صفحة ${pageIndex + 1}",
        contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth(),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(Color(0xFF0E1118)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = PeriwinklePrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${pageIndex + 1} / $totalPages",
                        color = TextSecondaryGrey,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFF131620))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = TextSecondaryGrey,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "تعذر تحميل صورة الصفحة ${pageIndex + 1}",
                        color = TextPrimaryWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { retryNonce++ },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PeriwinklePrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعادة المحاولة", fontSize = 12.sp)
                    }
                }
            }
        }
    )
}

/**
 * Hidden 1x1 WebView DOM Bridge that loads a live manga site URL with full JS & CookieManager enabled,
 * executes the provided extraction script once the DOM settles, and passes the JSON result back to the screen.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HiddenSiteWebViewBridge(
    targetUrl: String,
    jsExtractorScript: String,
    reloadTrigger: Int = 0,
    onResultJson: (String) -> Unit
) {
    if (targetUrl.isBlank() || !targetUrl.startsWith("http")) return

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(targetUrl, reloadTrigger) {
        onDispose {
            try {
                webViewRef?.stopLoading()
            } catch (_: Exception) {
            }
        }
    }

    Box(
        modifier = Modifier
            .size(1.dp)
            .alpha(0.01f)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(1, 1)
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = false
                    settings.blockNetworkImage = true
                    LiveMangaSiteScraper.activeUserAgent = settings.userAgentString ?: LiveMangaSiteScraper.DEFAULT_USER_AGENT

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            CookieManager.getInstance().flush()
                            view?.postDelayed({
                                try {
                                    view.evaluateJavascript(jsExtractorScript) { rawJson ->
                                        if (!rawJson.isNullOrBlank() && rawJson != "null") {
                                            onResultJson(rawJson)
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            }, 950L)
                        }
                    }
                    webViewRef = this
                    loadUrl(targetUrl)
                }
            },
            update = { view ->
                if (view.url != targetUrl) {
                    view.loadUrl(targetUrl)
                }
            }
        )
    }
}

/**
 * Interactive Cloudflare Protection & Live Site Verification Modal ("الحماية").
 * Lets the user solve Cloudflare challenges or inspect the live site connection, automatically syncing cookies
 * and extracting the rendered DOM back into the native app UI.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CloudflareProtectionDialog(
    url: String,
    siteName: String,
    jsExtractorScript: String? = null,
    onExtractedDomJson: ((String) -> Unit)? = null,
    onDismissAndRefresh: () -> Unit
) {
    var isLoadingPage by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismissAndRefresh,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(18.dp),
            color = AppDialogSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161922))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PeriwinklePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "حماية الموقع وتخطي Cloudflare",
                                color = TextPrimaryWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = siteName,
                                color = TextSecondaryGrey,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = TextPrimaryWhite)
                        }
                        IconButton(onClick = onDismissAndRefresh) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextPrimaryWhite)
                        }
                    }
                }

                // Live WebView for solving Cloudflare / loading session cookies
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                val cm = CookieManager.getInstance()
                                cm.setAcceptCookie(true)
                                cm.setAcceptThirdPartyCookies(this, true)

                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                LiveMangaSiteScraper.activeUserAgent =
                                    settings.userAgentString ?: LiveMangaSiteScraper.DEFAULT_USER_AGENT

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoadingPage = true
                                    }

                                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                        super.onPageFinished(view, finishedUrl)
                                        isLoadingPage = false
                                        CookieManager.getInstance().flush()
                                        if (jsExtractorScript != null && onExtractedDomJson != null) {
                                            view?.postDelayed({
                                                try {
                                                    view.evaluateJavascript(jsExtractorScript) { rawJson ->
                                                        if (!rawJson.isNullOrBlank() && rawJson != "null") {
                                                            onExtractedDomJson(rawJson)
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }, 700L)
                                        }
                                    }
                                }
                                webViewInstance = this
                                loadUrl(url)
                            }
                        }
                    )

                    if (isLoadingPage) {
                        CircularProgressIndicator(
                            color = PeriwinklePrimary,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .size(28.dp)
                        )
                    }
                }

                // Footer action button to return to the native app screen after verification
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161922))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            CookieManager.getInstance().flush()
                            val view = webViewInstance
                            if (view != null && jsExtractorScript != null && onExtractedDomJson != null) {
                                view.evaluateJavascript(jsExtractorScript) { rawJson ->
                                    if (!rawJson.isNullOrBlank() && rawJson != "null") {
                                        onExtractedDomJson(rawJson)
                                    }
                                    onDismissAndRefresh()
                                }
                            } else {
                                onDismissAndRefresh()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PeriwinklePrimary,
                            contentColor = PeriwinkleOnPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تم التحقق — عرض المانجا داخل التطبيق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
