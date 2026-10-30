package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MangaWork
import com.example.ui.theme.TextPrimaryWhite

private data class CoverPalette(
    val topBg: Color,
    val midBg: Color,
    val bottomBg: Color,
    val auraPrimary: Color,
    val auraSecondary: Color,
    val eyeGlow: Color,
    val titleColor: Color,
    val titleStroke: Color
)

private val coverPalettes = listOf(
    // 0: High-martial world (Deep navy + electric cyan/blue water spear + gold tip + red eye glow)
    CoverPalette(
        topBg = Color(0xFF0A1936),
        midBg = Color(0xFF103468),
        bottomBg = Color(0xFF060B16),
        auraPrimary = Color(0xFF29B6F6),
        auraSecondary = Color(0xFFFFD54F),
        eyeGlow = Color(0xFFFF1744),
        titleColor = Color(0xFFE1F5FE),
        titleStroke = Color(0xFF0277BD)
    ),
    // 1: Damn Necromancer (Dark purple/crimson shadow flames)
    CoverPalette(
        topBg = Color(0xFF1F0C29),
        midBg = Color(0xFF3B1345),
        bottomBg = Color(0xFF0D0712),
        auraPrimary = Color(0xFFCE93D8),
        auraSecondary = Color(0xFFFF5252),
        eyeGlow = Color(0xFFE040FB),
        titleColor = Color(0xFFF3E5F5),
        titleStroke = Color(0xFF6A1B9A)
    ),
    // 2: Nightwatcher (Dark crimson & gold imperial night)
    CoverPalette(
        topBg = Color(0xFF241114),
        midBg = Color(0xFF4A1C21),
        bottomBg = Color(0xFF0E080A),
        auraPrimary = Color(0xFFFFB74D),
        auraSecondary = Color(0xFFEF5350),
        eyeGlow = Color(0xFFFFCA28),
        titleColor = Color(0xFFFFF8E1),
        titleStroke = Color(0xFFB71C1C)
    ),
    // 3: Green Skin (Emerald & forest battle aura)
    CoverPalette(
        topBg = Color(0xFF0C2416),
        midBg = Color(0xFF1B4B2E),
        bottomBg = Color(0xFF07120B),
        auraPrimary = Color(0xFF66BB6A),
        auraSecondary = Color(0xFFAEEA00),
        eyeGlow = Color(0xFFFFAB00),
        titleColor = Color(0xFFE8F5E9),
        titleStroke = Color(0xFF1B5E20)
    ),
    // 4: The Dark Magician 66666 (Abyssal violet & silver)
    CoverPalette(
        topBg = Color(0xFF14112B),
        midBg = Color(0xFF2B2259),
        bottomBg = Color(0xFF080712),
        auraPrimary = Color(0xFF9575CD),
        auraSecondary = Color(0xFF80DEEA),
        eyeGlow = Color(0xFFFF1744),
        titleColor = Color(0xFFEDE7F6),
        titleStroke = Color(0xFF311B92)
    ),
    // 5: A Regressor's Tale (Celestial teal & gold cultivation)
    CoverPalette(
        topBg = Color(0xFF0D262B),
        midBg = Color(0xFF174A54),
        bottomBg = Color(0xFF061114),
        auraPrimary = Color(0xFF4DD0E1),
        auraSecondary = Color(0xFFFFD740),
        eyeGlow = Color(0xFF00E5FF),
        titleColor = Color(0xFFE0F7FA),
        titleStroke = Color(0xFF006064)
    ),
    // 6: Blade of the Demon (Blood red & obsidian slash)
    CoverPalette(
        topBg = Color(0xFF2B0B0E),
        midBg = Color(0xFF5C141B),
        bottomBg = Color(0xFF120506),
        auraPrimary = Color(0xFFFF5252),
        auraSecondary = Color(0xFFFF8A80),
        eyeGlow = Color(0xFFFF1744),
        titleColor = Color(0xFFFFEBEE),
        titleStroke = Color(0xFF880E4F)
    ),
    // 7: Limitless Predator (Dark indigo & neon magenta)
    CoverPalette(
        topBg = Color(0xFF1B0F2E),
        midBg = Color(0xFF3A1C61),
        bottomBg = Color(0xFF0B0614),
        auraPrimary = Color(0xFFFF4081),
        auraSecondary = Color(0xFF7C4DFF),
        eyeGlow = Color(0xFFFF4081),
        titleColor = Color(0xFFFCE4EC),
        titleStroke = Color(0xFF4A148C)
    ),
    // 8: Return of Abyss Ruler (Royal crimson & dark gold)
    CoverPalette(
        topBg = Color(0xFF26121A),
        midBg = Color(0xFF4F1D32),
        bottomBg = Color(0xFF0F070A),
        auraPrimary = Color(0xFFF06292),
        auraSecondary = Color(0xFFFFD54F),
        eyeGlow = Color(0xFFFF5252),
        titleColor = Color(0xFFFCE4EC),
        titleStroke = Color(0xFF880E4F)
    )
)

@Composable
fun MangaPlaceholderCoverBox(
    modifier: Modifier = Modifier
) {
    // Exact dark placeholder card from 00:11-00:12 of the reference video
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
            modifier = Modifier.size(42.dp)
        )
    }
}

@Composable
fun MangaCoverArtwork(
    work: MangaWork,
    modifier: Modifier = Modifier,
    showArabicTitleBanner: Boolean = true
) {
    val palette = coverPalettes[work.coverStyleIndex % coverPalettes.size]

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.topBg, palette.midBg, palette.bottomBg)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Radiant energy backdrop
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.auraPrimary.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.42f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.65f
            )

            // Swirling water/energy arcs
            val arcPath = Path().apply {
                moveTo(w * 0.08f, h * 0.72f)
                cubicTo(
                    w * 0.25f, h * 0.45f,
                    w * 0.75f, h * 0.58f,
                    w * 0.92f, h * 0.22f
                )
            }
            drawPath(
                path = arcPath,
                color = palette.auraPrimary.copy(alpha = 0.7f),
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
            )

            // Character silhouette shoulders & cloak
            val cloakPath = Path().apply {
                moveTo(w * 0.12f, h * 0.95f)
                quadraticBezierTo(w * 0.22f, h * 0.52f, w * 0.5f, h * 0.48f)
                quadraticBezierTo(w * 0.78f, h * 0.52f, w * 0.88f, h * 0.95f)
                close()
            }
            drawPath(path = cloakPath, color = Color(0xFF0A0E18))

            // Inner collar highlight
            val collarPath = Path().apply {
                moveTo(w * 0.35f, h * 0.48f)
                lineTo(w * 0.5f, h * 0.68f)
                lineTo(w * 0.65f, h * 0.48f)
            }
            drawPath(
                path = collarPath,
                color = palette.auraPrimary.copy(alpha = 0.85f),
                style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
            )

            // Character head & spiky manhwa hair silhouette
            drawCircle(
                color = Color(0xFFF5D6C6),
                radius = w * 0.14f,
                center = Offset(w * 0.5f, h * 0.34f)
            )
            val hairPath = Path().apply {
                moveTo(w * 0.30f, h * 0.36f)
                lineTo(w * 0.34f, h * 0.18f)
                lineTo(w * 0.44f, h * 0.22f)
                lineTo(w * 0.52f, h * 0.14f)
                lineTo(w * 0.60f, h * 0.21f)
                lineTo(w * 0.70f, h * 0.19f)
                lineTo(w * 0.68f, h * 0.37f)
                quadraticBezierTo(w * 0.5f, h * 0.27f, w * 0.30f, h * 0.36f)
                close()
            }
            drawPath(path = hairPath, color = Color(0xFF0E1320))

            // Glowing protagonist eye trail (matching 00:50 red eye glow!)
            drawCircle(
                color = palette.eyeGlow,
                radius = w * 0.022f,
                center = Offset(w * 0.55f, h * 0.34f)
            )
            drawLine(
                color = palette.eyeGlow.copy(alpha = 0.85f),
                start = Offset(w * 0.53f, h * 0.34f),
                end = Offset(w * 0.68f, h * 0.31f),
                strokeWidth = w * 0.018f,
                cap = StrokeCap.Round
            )

            // Diagonal Spear / Weapon slash with golden tip
            drawLine(
                color = Color(0xFFCFD8DC),
                start = Offset(w * 0.18f, h * 0.82f),
                end = Offset(w * 0.82f, h * 0.18f),
                strokeWidth = w * 0.03f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = palette.auraSecondary,
                radius = w * 0.045f,
                center = Offset(w * 0.82f, h * 0.18f)
            )

            // Bottom dark gradient vignette for Arabic cover title art
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xE6070910)),
                    startY = h * 0.52f,
                    endY = h
                )
            )
        }

        if (showArabicTitleBanner) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = work.arabicBannerTitle,
                    color = palette.titleColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Renders the authentic Chapter Pages shown in 00:48 - 00:52 of the reference video:
 * - Page 1 (index 0): Team-X / Straw Hat Credit & Warning Banner Page ("رسالة هامة إلى قرائنا الأعزاء" + HTTPS://OLYMPUSTAFF.COM)
 * - Page 2 (index 1): Full-bleed Blue Water Spear Martial Arts Hero Scene (00:50 - 00:51)
 * - Page 3 (index 2): Studio & Author Credits Page (00:52: 快看! 快看独家 / 出品: 快看漫画 / 原著: 冲冲鱼...)
 * - Pages 4..12: Full-bleed Arabic-translated Manhwa panels with system windows, speech bubbles & action art!
 */
@Composable
fun ChapterPageIllustration(
    work: MangaWork,
    chapterNumber: Int,
    pageIndex: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    when (pageIndex) {
        0 -> TeamXCreditAndWarningPage(work = work, modifier = modifier)
        1 -> WaterSpearHeroSplashPage(work = work, chapterNumber = chapterNumber, modifier = modifier)
        2 -> StudioCreditsAndOpeningPage(work = work, chapterNumber = chapterNumber, modifier = modifier)
        else -> ActionManhwaStoryPanelPage(
            work = work,
            chapterNumber = chapterNumber,
            pageIndex = pageIndex,
            totalPages = totalPages,
            modifier = modifier
        )
    }
}

@Composable
private fun TeamXCreditAndWarningPage(
    work: MangaWork,
    modifier: Modifier = Modifier
) {
    // Exact Page 1 from 00:48 - 00:49 of the reference video!
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF121318))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Team Banner with Cover Card on Left and Straw Hat Team Badges on Right (00:48)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E2430), Color(0xFF14161D))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right side in RTL: Straw Hat Team Credits
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Straw Hat",
                        color = Color(0xFFFFD54F),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeamRoleBadge(role = "ترجمة", member = "SIMPLE")
                        TeamRoleBadge(role = "تحرير", member = "VERA")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeamRoleBadge(role = "تدقيق", member = "VERA")
                        TeamRoleBadge(role = "رفع", member = "EMOO")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFF26324B),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5C6BC0))
                    ) {
                        Text(
                            text = "JOIN OUR DISCORD\nHTTPS://DISCORD.GG/TEAMX",
                            color = Color(0xFFD6E0FF),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Left side in RTL: Mini Framed Cover Card + Title Plaque (00:48)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(135.dp)
                ) {
                    MangaCoverArtwork(
                        work = work,
                        modifier = Modifier
                            .width(125.dp)
                            .height(165.dp)
                            .border(2.dp, Color(0xFFD4AF37), RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFF1F1812),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4AF37))
                    ) {
                        Text(
                            text = work.title,
                            color = TextPrimaryWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Warning Header Section (00:48 - 00:49): "رسالة هامة إلى قرائنا الأعزاء"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF18191E))
                .padding(vertical = 22.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "رسالة هامة",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "إلى قرائنا الأعزاء",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // Arabic Message Paragraph + Official Domain Banner (00:49)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1D1E24))
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مرحباً أعزائي القراء، إن كنتم تقرأون هذا العمل على أي موقع غير تيم إكس فنود إعلامكم أن هذا العمل مهدد بالإيقاف بسبب قلة المشاهدات في الفترة الأخيرة. لذلك، رجاءً إن كنت تحب هذا العمل أو ترغب في استمراره، قم بقراءته من موقعنا فقط ولا تدعم المواقع السارقة. طاقم العمل يبذل جهوداً كبيرة لإصدار الفصول بشكل منتظم، بأفضل جودة وفي أسرع وقت ممكن. دعمكم مهم جداً لاستمرارنا، وكل ما نطلبه هو أن تقرأوا من موقعنا الرسمي.",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(
                    text = "HTTPS://OLYMPUSTAFF.COM",
                    color = Color(0xFF111318),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun TeamRoleBadge(role: String, member: String) {
    Surface(
        color = Color(0xFFD7CCC8),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8D6E63))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(78.dp)
                .padding(vertical = 4.dp, horizontal = 6.dp)
        ) {
            Text(
                text = role,
                color = Color(0xFFB71C1C),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = member,
                color = Color(0xFF212121),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun WaterSpearHeroSplashPage(
    work: MangaWork,
    chapterNumber: Int,
    modifier: Modifier = Modifier
) {
    // Exact Page 2 from 00:50 - 00:51: Full-bleed Martial Artist in Blue Coat with Swirling Water Spear & Glowing Red Eye!
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(580.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF091326),
                        Color(0xFF102A52),
                        Color(0xFF0A1931),
                        Color(0xFF050811)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Futuristic city skyline lights in upper background
            for (i in 0..8) {
                val bx = w * (0.06f + i * 0.11f)
                val bh = h * (0.18f + (i % 3) * 0.07f)
                drawRect(
                    color = Color(0xFF162B4D).copy(alpha = 0.55f),
                    topLeft = Offset(bx, h * 0.12f),
                    size = Size(w * 0.07f, bh)
                )
            }

            // Glowing moon / energy vortex behind hero
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4FC3F7).copy(alpha = 0.55f),
                        Color(0xFF1E88E5).copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.62f
                ),
                center = Offset(w * 0.5f, h * 0.45f),
                radius = w * 0.62f
            )

            // Hero dark blue hooded jacket / shoulders
            val coatPath = Path().apply {
                moveTo(w * 0.05f, h * 0.96f)
                quadraticBezierTo(w * 0.15f, h * 0.52f, w * 0.48f, h * 0.48f)
                quadraticBezierTo(w * 0.85f, h * 0.50f, w * 0.95f, h * 0.96f)
                close()
            }
            drawPath(path = coatPath, color = Color(0xFF10284E))

            // White inner shirt V-neck
            val shirtPath = Path().apply {
                moveTo(w * 0.38f, h * 0.50f)
                lineTo(w * 0.52f, h * 0.72f)
                lineTo(w * 0.64f, h * 0.50f)
                close()
            }
            drawPath(path = shirtPath, color = Color(0xFFDCE6F2))

            // Hero face (sharp jawline)
            val facePath = Path().apply {
                moveTo(w * 0.37f, h * 0.28f)
                lineTo(w * 0.61f, h * 0.28f)
                lineTo(w * 0.58f, h * 0.43f)
                lineTo(w * 0.48f, h * 0.48f)
                lineTo(w * 0.39f, h * 0.43f)
                close()
            }
            drawPath(path = facePath, color = Color(0xFFF2D2B6))

            // Spiky black manhwa hair
            val hairPath = Path().apply {
                moveTo(w * 0.32f, h * 0.33f)
                lineTo(w * 0.35f, h * 0.16f)
                lineTo(w * 0.43f, h * 0.20f)
                lineTo(w * 0.50f, h * 0.13f)
                lineTo(w * 0.58f, h * 0.19f)
                lineTo(w * 0.67f, h * 0.17f)
                lineTo(w * 0.65f, h * 0.34f)
                quadraticBezierTo(w * 0.49f, h * 0.24f, w * 0.32f, h * 0.33f)
                close()
            }
            drawPath(path = hairPath, color = Color(0xFF0E131F))

            // Glowing crimson left eye with laser trail (exact 00:50 detail!)
            drawCircle(
                color = Color(0xFFFF1744),
                radius = w * 0.018f,
                center = Offset(w * 0.54f, h * 0.35f)
            )
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF1744), Color(0xFFFF5252), Color.Transparent)
                ),
                start = Offset(w * 0.53f, h * 0.35f),
                end = Offset(w * 0.72f, h * 0.31f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // Golden-tipped Spear held diagonally across chest (00:50)
            drawLine(
                color = Color(0xFF263238),
                start = Offset(w * 0.46f, h * 0.92f),
                end = Offset(w * 0.78f, h * 0.42f),
                strokeWidth = 18f,
                cap = StrokeCap.Round
            )
            // Golden spearhead
            val spearTip = Path().apply {
                moveTo(w * 0.85f, h * 0.30f)
                lineTo(w * 0.73f, h * 0.42f)
                lineTo(w * 0.78f, h * 0.40f)
                lineTo(w * 0.82f, h * 0.46f)
                close()
            }
            drawPath(path = spearTip, color = Color(0xFFFFCA28))

            // Swirling Water Streams around Spear and Hero (00:50)
            val waterStream1 = Path().apply {
                moveTo(w * 0.08f, h * 0.35f)
                cubicTo(w * 0.02f, h * 0.68f, w * 0.42f, h * 0.88f, w * 0.88f, h * 0.62f)
            }
            drawPath(
                path = waterStream1,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF29B6F6), Color(0xFF81D4FA), Color(0xFF0288D1))
                ),
                style = Stroke(width = 22f, cap = StrokeCap.Round)
            )

            val waterStream2 = Path().apply {
                moveTo(w * 0.16f, h * 0.78f)
                cubicTo(w * 0.38f, h * 0.60f, w * 0.65f, h * 0.75f, w * 0.92f, h * 0.44f)
            }
            drawPath(
                path = waterStream2,
                color = Color(0xFFB3E5FC).copy(alpha = 0.85f),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun StudioCreditsAndOpeningPage(
    work: MangaWork,
    chapterNumber: Int,
    modifier: Modifier = Modifier
) {
    // Exact Page 3 from 00:52: White Studio Credits Strip followed by Opening Dramatic Scene
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Yellow + Black "快看! 快看独家" badge (00:52)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "快看独家",
                color = Color(0xFF111111),
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFFEB3B))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "快看!",
                    color = Color(0xFF111111),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val creditsLines = listOf(
            "出品: 快看漫画",
            "原著: 冲冲鱼",
            "漫画制作: 漫创堂",
            "编剧: 齐昂强",
            "分镜: 蜡笔酱",
            "主笔: BBB ; 886",
            "上色: 阿攸 ; 东灵",
            "场景: 追逐航迹云"
        )
        creditsLines.forEach { line ->
            Text(
                text = line,
                color = Color(0xFF222222),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Transition into Chapter Title Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFF101522), Color(0xFF0B0E17))
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    color = Color(0xFF1E2942),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64B5F6))
                ) {
                    Text(
                        text = "الفصل $chapterNumber",
                        color = Color(0xFF81D4FA),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = work.arabicBannerTitle.replace("\n", " "),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ActionManhwaStoryPanelPage(
    work: MangaWork,
    chapterNumber: Int,
    pageIndex: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    val dialogues = listOf(
        Triple(
            "«تنبيه النظام: تم رصد طاقة قتالية هائلة في محيط الساحة!»",
            "هل يظنون حقاً أن تقنيات الرتبة الثالثة كافية لإيقافي؟",
            "ثانية واحدة للبدائية... ثانيتان للإتقان... وثلاث ثوانٍ للكمال المطلق!"
        ),
        Triple(
            "«مهارة الرمح المائي المتفجر - المستوى الأقصى!»",
            "مستحيل!! كيف استطاع كسر دفاع العشيرة بضربة واحدة فقط؟!",
            "من اليوم فصاعداً، لن يجرؤ أحد على الوقوف في طريقي."
        ),
        Triple(
            "«ارتقاء المستوى! القوة القتالية +15,000»",
            "هذه مجرد البداية... العالم السفلي بأكمله سيركع قريباً!",
            "يتبع في الصفحة التالية..."
        )
    )
    val scene = dialogues[(pageIndex + chapterNumber) % dialogues.size]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0D14))
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // System Notification Box at top of panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0D213A), Color(0xFF16375E), Color(0xFF0D213A))
                    )
                )
                .border(1.5.dp, Color(0xFF29B6F6), RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = scene.first,
                color = Color(0xFF81D4FA),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        // Dramatic Action Panel Illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF263044), RoundedCornerShape(8.dp))
        ) {
            MangaCoverArtwork(
                work = work.copy(coverStyleIndex = work.coverStyleIndex + pageIndex),
                showArabicTitleBanner = false,
                modifier = Modifier.fillMaxSize()
            )

            // Speech Bubble Top-Right
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(220.dp)
            ) {
                Text(
                    text = scene.second,
                    color = Color(0xFF11131A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Dramatic Bottom Speech Bubble
            Surface(
                color = Color(0xFF141926).copy(alpha = 0.94f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFCA28)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = scene.third,
                    color = Color(0xFFFFF8E1),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
