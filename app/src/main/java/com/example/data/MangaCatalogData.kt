package com.example.data

import com.example.data.local.SiteEntity

data class MangaChapter(
    val id: String,
    val number: Int,
    val title: String,
    val dateText: String,
    val pageCount: Int = 12,
    val isDownloaded: Boolean = false,
    val isRead: Boolean = false
)

data class MangaWork(
    val id: String,
    val title: String,
    val arabicBannerTitle: String,
    val siteName: String,
    val siteUrl: String,
    val siteIconKey: String,
    val status: String = "مستمرة",
    val type: String = "مانها\nصيني",
    val rating: String = "0.0",
    val chaptersCount: Int = 77,
    val genres: List<String>,
    val description: String,
    val coverStyleIndex: Int,
    val coverImageUrl: String? = null,
    val chapters: List<MangaChapter> = defaultChaptersForWork(id, 77)
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

    // Default added sites matching 00:02-00:06 of the reference video
    val defaultAddedSites: List<SiteEntity> = listOf(
        SiteEntity(
            id = 101L,
            name = "MangaDar",
            url = "https://mangadar.com",
            domain = "mangadar.com",
            isEnabled = true,
            sortOrder = 0,
            iconKey = "mangadar"
        ),
        SiteEntity(
            id = 102L,
            name = "مانجا ليك Mangalek",
            url = "https://mangalik.net",
            domain = "mangalik.net",
            isEnabled = true,
            sortOrder = 1,
            iconKey = "mangalek"
        ),
        SiteEntity(
            id = 103L,
            name = "ازورا مانجا - Azora Manga",
            url = "https://azoramoon.com",
            domain = "azoramoon.com",
            isEnabled = true,
            sortOrder = 2,
            iconKey = "azora"
        ),
        SiteEntity(
            id = 104L,
            name = "مانجاتك - Mangatek",
            url = "https://mangatek.com",
            domain = "mangatek.com",
            isEnabled = true,
            sortOrder = 3,
            iconKey = "mangatek"
        ),
        SiteEntity(
            id = 105L,
            name = "ProComic",
            url = "https://procomic.net",
            domain = "procomic.net",
            isEnabled = true,
            sortOrder = 4,
            iconKey = "procomic"
        ),
        SiteEntity(
            id = 106L,
            name = "مانجا ستارز Mangastarz",
            url = "https://mangastarz.org",
            domain = "mangastarz.org",
            isEnabled = true,
            sortOrder = 5,
            iconKey = "globe"
        ),
        SiteEntity(
            id = 107L,
            name = "Team-X",
            url = "https://olympustaff.com",
            domain = "olympustaff.com",
            isEnabled = true,
            sortOrder = 6,
            iconKey = "teamx"
        ),
        SiteEntity(
            id = 108L,
            name = "Swat Manga",
            url = "https://swatmanga.net",
            domain = "swatmanga.net",
            isEnabled = true,
            sortOrder = 7,
            iconKey = "swat"
        )
    )

    fun getCatalogForSite(site: SiteEntity): List<MangaWork> {
        val sName = site.name.ifBlank { "Team-X" }
        val sUrl = site.url.ifBlank { "https://olympustaff.com" }
        val sIcon = site.iconKey.ifBlank { "teamx" }

        // Exact works shown in Team-X (00:11-00:15) and Storage (00:01) in the reference video
        return listOf(
            MangaWork(
                id = "high_martial_world",
                title = "High-martial world: with one hand he sweeps aside three thousand...",
                arabicBannerTitle = "بيد واحدة\nيكتسح آلاف الأنماط",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانها\nصيني",
                rating = "0.0",
                chaptersCount = 77,
                genres = listOf("أكشن", "إثارة", "بطل غير إعتيادي", "مغامرات", "نظام"),
                description = "نهضة الفنون القتالية، وطغيان الوحوش الضارية، وزحف آلاف الأعراق لغزو البشرية ومزاحمتهم على المجيد في خضم هذا العالم، يوقظ الفتى سو هونغ \"النظام\"...\nتبدأ رحلته بموهبة أزلية لا يطويها العصور، فنون قتالية من الرتبة (C): ثانية واحدة للبدائية، ثانيتان للإتقان، وثلاث ثوان للكمال المطلق!",
                coverStyleIndex = 0
            ),
            MangaWork(
                id = "damn_necromancer",
                title = "Damn Necromancer",
                arabicBannerTitle = "مستحضر الأرواح\nاللعين",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 64,
                genres = listOf("أكشن", "خيال", "دموي", "نظام"),
                description = "بعد خيانة رفاقه في الطابق الأخير من البرج، يعود بقدرة استحضار الموتى المحرمة ليبني جيش الظلال الخاص به ويسحق كل من وقف في طريقه.",
                coverStyleIndex = 1
            ),
            MangaWork(
                id = "nightwatcher",
                title = "Nightwatcher",
                arabicBannerTitle = "حارس الليل",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانها\nصيني",
                rating = "4.7",
                chaptersCount = 92,
                genres = listOf("أكشن", "إثارة", "خيال", "مغامرات"),
                description = "في عاصمة تعج بالأسرار والشياطين المتخفية، ينضم شاب ذكي إلى حراس الليل ليكشف مؤامرة تهدد توازن الإمبراطورية بأكملها.",
                coverStyleIndex = 2
            ),
            MangaWork(
                id = "green_skin",
                title = "Green Skin",
                arabicBannerTitle = "البشرة الخضراء",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.6",
                chaptersCount = 53,
                genres = listOf("إيسيكاي", "أكشن", "دموي", "بطل غير إعتيادي"),
                description = "يتجسد في عالم آخر لا كبطل بشري بل كأحد الوحوش الخضراء المقهورة، ليقود ثورة تقلب موازين القوى بين الممالك.",
                coverStyleIndex = 3
            ),
            MangaWork(
                id = "dark_magician_66666",
                title = "The Dark Magician Transmigrates After 66666 Years",
                arabicBannerTitle = "عودة الساحر\nالأسود",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 118,
                genres = listOf("سحر", "أكشن", "خيال", "بطل غير إعتيادي"),
                description = "ساحر الظلام الأعظم ديابلو فولبير يستيقظ بعد 66,666 عاماً في جسد طفل نبيل، مستعداً لاستعادة قوته ومواجهة الآلهة من جديد.",
                coverStyleIndex = 4
            ),
            MangaWork(
                id = "regressors_tale",
                title = "A Regressor's Tale of Cultivation",
                arabicBannerTitle = "حكاية العائد\nفي عالم الصقل",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 45,
                genres = listOf("إيسيكاي", "فنون قتال", "خيال", "أكشن"),
                description = "رغم افتقاره للموهبة الروحية، يعود بالزمن في كل مرة يموت فيها، صاقلاً إرادته وفنونه القتالية عبر مئات الحيوات ليبلغ قمة الخلود.",
                coverStyleIndex = 5
            ),
            MangaWork(
                id = "blade_of_demon",
                title = "Blade of the Demon",
                arabicBannerTitle = "نصل الشيطان",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.7",
                chaptersCount = 81,
                genres = listOf("أكشن", "دموي", "فنون قتال", "إثارة"),
                description = "سياف وحيد يحمل نصلاً ملعوناً يتغذى على طاقة الأعداء، ينطلق في رحلة انتقام عبر عالم الموريم المظلم.",
                coverStyleIndex = 6
            ),
            MangaWork(
                id = "limitless_predator",
                title = "Limitless Predator",
                arabicBannerTitle = "المفترس\nبلا حدود",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 69,
                genres = listOf("أكشن", "نظام", "دموي", "بطل غير إعتيادي"),
                description = "يكتسب مهارة الافتراس المطلق التي تمكنه من التهام قدرات الزعماء والوحوش وتطوير جسده بلا سقف للتطور.",
                coverStyleIndex = 7
            ),
            MangaWork(
                id = "return_of_abyss",
                title = "The Return of the Abyss Ruler",
                arabicBannerTitle = "حاكم\nالهاوية",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 104,
                genres = listOf("أكشن", "خيال", "نظام", "إثارة"),
                description = "بعد سقوطه في أعمق طبقات الهاوية لقرون، يعود الحاكم الأعلى للأرض ليجد البوابات قد فتحت والفوضى تعم العالم.",
                coverStyleIndex = 8
            ),
            MangaWork(
                id = "is_it_bad_martial_god",
                title = "Is It Bad That I'm a Martial God?",
                arabicBannerTitle = "إله الفنون\nالقتالية",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.6",
                chaptersCount = 58,
                genres = listOf("فنون قتال", "أكشن", "بطل غير إعتيادي"),
                description = "وصل إلى مرتبة إله القتال في سن مبكرة وأراد فقط حياة هادئة، لكن العشائر الكبرى لا تتوقف عن استفزازه.",
                coverStyleIndex = 9
            ),
            MangaWork(
                id = "reincarnation_battle_god",
                title = "Reincarnation of the Suicidal Battle God",
                arabicBannerTitle = "تناسخ إله\nالمعارك",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 102,
                genres = listOf("أكشن", "خيال", "مغامرات", "إثارة"),
                description = "آخر ناجٍ من البشرية يُرسل إلى الماضي بصفقة مع آلهة الفوضى ليغير مسار التاريخ قبل صحوة تنين التدمير.",
                coverStyleIndex = 10
            ),
            MangaWork(
                id = "master_beek",
                title = "Master Beek's Disciples",
                arabicBannerTitle = "تلاميذ المعلم\nبيك",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.7",
                chaptersCount = 73,
                genres = listOf("فنون قتال", "أكشن", "مغامرات"),
                description = "معلم غامض في جبل ناءٍ يدرب تلاميذ بقدرات أسطورية يهزون أركان تحالف الموريم.",
                coverStyleIndex = 11
            ),
            MangaWork(
                id = "demon_king_return",
                title = "The Demon King's Return",
                arabicBannerTitle = "عودة ملك\nالشياطين",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 88,
                genres = listOf("أكشن", "دموي", "خيال", "نظام"),
                description = "ملك الشياطين يستعيد عرشه المسلوب بعد ألف عام من الختم، مسلحاً بذكريات المستقبل وقوة الظلام.",
                coverStyleIndex = 12
            ),
            MangaWork(
                id = "forgotten_monarch",
                title = "The Forgotten Monarch",
                arabicBannerTitle = "العاهل المنسي",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.7",
                chaptersCount = 61,
                genres = listOf("أكشن", "خيال", "إيسيكاي", "سحر"),
                description = "العاهل الذي محا التاريخ اسمه يعود من العدم ليطالب بحقه في عرش القارة الوسطى.",
                coverStyleIndex = 13
            ),
            MangaWork(
                id = "god_game",
                title = "God game: The Awakening",
                arabicBannerTitle = "لعبة الآلهة",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 95,
                genres = listOf("نظام", "أكشن", "إثارة", "خيال"),
                description = "يتحول العالم فجأة إلى ساحة بقاء تديرها كيانات عليا، ويكتشف البطل ثغرة في قوانين اللعبة تمنحه تفوقاً مطلقاً.",
                coverStyleIndex = 14
            ),
            MangaWork(
                id = "rebel_of_academy",
                title = "The Rebel of the Academy",
                arabicBannerTitle = "متمرد أكاديمية",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 84,
                genres = listOf("أكشن", "سحر", "بطل غير إعتيادي"),
                description = "طالب من عامة الشعب يدخل أرقى أكاديمية للسحر والفرسان ليحطم غطرسة النبلاء بقوته الفريدة.",
                coverStyleIndex = 15
            ),
            MangaWork(
                id = "conquering_dungeons",
                title = "Conquering Dungeons",
                arabicBannerTitle = "غزو الزنزانات",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 112,
                genres = listOf("أكشن", "نظام", "مغامرات"),
                description = "صياد يمتلك خريطة سرية لكل الزنزانات المخفية والزعماء الأسطوريين في العالم.",
                coverStyleIndex = 16
            ),
            MangaWork(
                id = "level_up_with_gods",
                title = "I level up with the Gods",
                arabicBannerTitle = "أرتقي مع الآلهة",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 120,
                genres = listOf("أكشن", "خيال", "نظام", "مغامرات"),
                description = "يعود كيم يوون عبر الزمن إلى بداية البرج ليصعد الطوابق برفقة الآلهة القدامى وينقذ العالم من الدمار.",
                coverStyleIndex = 17
            )
        )
    }

    // Exact saved works grid shown in "التخزين" at 00:01 of the 52-second reference video
    val storageLibraryWorks: List<MangaWork> by lazy {
        val sName = "Team-X"
        val sUrl = "https://olympustaff.com"
        val sIcon = "teamx"
        listOf(
            MangaWork(
                id = "god_game",
                title = "God game...",
                arabicBannerTitle = "لعبة الآلهة",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 95,
                genres = listOf("نظام", "أكشن", "إثارة", "خيال"),
                description = "يتحول العالم فجأة إلى ساحة بقاء تديرها كيانات عليا، ويكتشف البطل ثغرة في قوانين اللعبة تمنحه تفوقاً مطلقاً.",
                coverStyleIndex = 5
            ),
            MangaWork(
                id = "rebel_of_academy",
                title = "The Rebel of t...",
                arabicBannerTitle = "متمرد الأكاديمية",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 84,
                genres = listOf("أكشن", "سحر", "بطل غير إعتيادي"),
                description = "طالب من عامة الشعب يدخل أرقى أكاديمية للسحر والفرسان ليحطم غطرسة النبلاء بقوته الفريدة.",
                coverStyleIndex = 2
            ),
            MangaWork(
                id = "the_mysterious",
                title = "The Mysterious...",
                arabicBannerTitle = "العالم الغامض",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.7",
                chaptersCount = 72,
                genres = listOf("خيال", "إثارة", "أكشن"),
                description = "رحلة في أعماق القارة المفقودة حيث الأسرار القديمة والوحوش الأسطورية.",
                coverStyleIndex = 4
            ),
            MangaWork(
                id = "after_confession",
                title = "After Confessi...",
                arabicBannerTitle = "بعد الاعتراف",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.6",
                chaptersCount = 65,
                genres = listOf("خيال", "مغامرات", "أكشن"),
                description = "تتغير حياته بالكامل بعد مواجهة مصيرية تكشف له حقيقة قواه المختومة.",
                coverStyleIndex = 6
            ),
            MangaWork(
                id = "i_will_grind",
                title = "I Will Grind M...",
                arabicBannerTitle = "سأصقل قوتي",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 88,
                genres = listOf("نظام", "أكشن", "فنون قتال"),
                description = "بطل يعتمد على التدريب المتواصل ومضاعفة نقاط الخبرة ليتجاوز جميع العباقرة.",
                coverStyleIndex = 3
            ),
            MangaWork(
                id = "level_up_with_gods",
                title = "I level up with...",
                arabicBannerTitle = "أرتقي مع الآلهة",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 120,
                genres = listOf("أكشن", "خيال", "نظام", "مغامرات"),
                description = "يعود كيم يوون عبر الزمن إلى بداية البرج ليصعد الطوابق برفقة الآلهة القدامى وينقذ العالم من الدمار.",
                coverStyleIndex = 1
            ),
            MangaWork(
                id = "conquering_dungeons",
                title = "Conquering D...",
                arabicBannerTitle = "غزو الزنزانات",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 112,
                genres = listOf("أكشن", "نظام", "مغامرات"),
                description = "صياد يمتلك خريطة سرية لكل الزنزانات المخفية والزعماء الأسطوريين في العالم.",
                coverStyleIndex = 7
            ),
            MangaWork(
                id = "soul_land_5",
                title = "Soul Land 5",
                arabicBannerTitle = "أرض الروح ٥",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانها\nصيني",
                rating = "4.9",
                chaptersCount = 140,
                genres = listOf("فنون قتال", "خيال", "مغامرات"),
                description = "عودة تانغ سان في عالم جديد لاستعادة ذكرياته وقوته الإلهية.",
                coverStyleIndex = 0
            ),
            MangaWork(
                id = "age_of_terror",
                title = "Age of terror",
                arabicBannerTitle = "عصر الرعب",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانها\nصيني",
                rating = "4.7",
                chaptersCount = 98,
                genres = listOf("إثارة", "أكشن", "دموي"),
                description = "في عالم اجتاحته الأرواح المظلمة، يقف صياد وحيد لحماية البشرية من الانقراض.",
                coverStyleIndex = 8
            ),
            MangaWork(
                id = "necromancer_academy",
                title = "Necromancer ...",
                arabicBannerTitle = "أكاديمية الظلال",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 110,
                genres = listOf("سحر", "أكشن", "خيال"),
                description = "أقوى مستحضر أرواح يلتحق بأكاديمية الصيادين ليقود جيش الظلال.",
                coverStyleIndex = 1
            ),
            MangaWork(
                id = "great_mage_returns",
                title = "The Great Ma...",
                arabicBannerTitle = "عودة الساحر الأعظم",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.9",
                chaptersCount = 155,
                genres = listOf("سحر", "أكشن", "إيسيكاي"),
                description = "عودة الساحر الأسطوري لوكاس ترومان بعد أربعة آلاف عام من الختم.",
                coverStyleIndex = 4
            ),
            MangaWork(
                id = "two_realms",
                title = "Two Realms: L...",
                arabicBannerTitle = "أسطورة العالمين",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانها\nصيني",
                rating = "4.7",
                chaptersCount = 83,
                genres = listOf("فنون قتال", "أكشن", "خيال"),
                description = "محارب يعبر بين عالمين متوازيين ليجمع أعظم تقنيات الصقل القتالي.",
                coverStyleIndex = 5
            ),
            MangaWork(
                id = "genius_assassin",
                title = "The Genius As...",
                arabicBannerTitle = "القاتل العبقري",
                siteName = sName,
                siteUrl = sUrl,
                siteIconKey = sIcon,
                status = "مستمرة",
                type = "مانهوا\nكورية",
                rating = "4.8",
                chaptersCount = 77,
                genres = listOf("أكشن", "إثارة", "بطل غير إعتيادي"),
                description = "قاتل مأجور عبقري يولد من جديد في عائلة سيافين ليصبح سيد النصل الخفي.",
                coverStyleIndex = 6
            ),
            getCatalogForSite(defaultAddedSites[6]).first()
        )
    }
}

fun defaultChaptersForWork(workId: String, totalCount: Int = 77): List<MangaChapter> {
    // Exact chapter titles & dates from 00:25 - 00:38 of the reference video
    val exactFirstEleven = listOf(
        Triple(1, "الفصل 1 - نزال العالم السفلي", "30 ديسمبر 2025"),
        Triple(2, "الفصل 2 - تجلي النظام", "30 ديسمبر 2025"),
        Triple(3, "الفصل 3 - عبقري الفنون القتالية", "30 ديسمبر 2025"),
        Triple(4, "الفصل 4 - إبادة الوحوش الضارية", "30 ديسمبر 2025"),
        Triple(5, "الفصل 5 - أفعى اليشب الأسود", "30 يناير"),
        Triple(6, "الفصل 6 - الهجوم المعاكس", "30 يناير"),
        Triple(7, "الفصل 7 - اختراق المحارب", "30 يناير"),
        Triple(8, "الفصل 8 - ظهور العبقري الجديد", "30 يناير"),
        Triple(9, "الفصل 9 - تقنيات الإرتداد السبعة", "30 يناير"),
        Triple(10, "الفصل 10 - لم الدمج", "28 فبراير"),
        Triple(11, "الفصل 11 - الموجة الحارقة تتألق", "28 فبراير")
    )

    val extraSubtitles = listOf(
        "صحوة القوة الكامنة",
        "معركة وادي الظلال",
        "امتحان النخبة",
        "سيد الرمح المائي",
        "تحطيم الحاجز الروحي",
        "زئير التنين الأزرق",
        "مواجهة زعيم العشيرة",
        "الكنز الإمبراطوري",
        "ارتقاء الرتبة الذهبية",
        "عاصفة الفنون السامية"
    )

    return (1..totalCount).map { num ->
        val preset = exactFirstEleven.find { it.first == num }
        if (preset != null) {
            MangaChapter(
                id = "${workId}_ch_$num",
                number = num,
                title = preset.second,
                dateText = preset.third,
                pageCount = 12
            )
        } else {
            val sub = extraSubtitles[(num - 1) % extraSubtitles.size]
            val month = when {
                num <= 25 -> "مارس"
                num <= 45 -> "أبريل"
                num <= 60 -> "مايو"
                else -> "يونيو"
            }
            MangaChapter(
                id = "${workId}_ch_$num",
                number = num,
                title = "الفصل $num - $sub",
                dateText = "${(num % 28) + 1} $month",
                pageCount = 12
            )
        }
    }
}
