package com.lagradost.cloudstream3.animeproviders

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.lagradost.cloudstream3.utils.getQualityFromName

class KamyrollDEProvider: MainAPI() {
    companion object {
        var latestHeader: Map<String, String> = emptyMap()
        var latestKrunchyHeader: Map<String, String> = emptyMap()
        var latestKrunchySession: Map<String, String> = emptyMap()
        var latestcountryID = ""
        private const val krunchyapi = "https://beta-api.crunchyroll.com"
    }
    override var name = "Kamyroll DE"
    override var mainUrl = "https://api.kamyroll.tech" //apirurl
    override val instantLinkLoading = false
    override val hasMainPage = true
    override var lang = "de"
    override val supportedTypes = setOf(
        TvType.AnimeMovie,
        TvType.Anime,
    )

    data class KamyToken (
        @JsonProperty("access_token" ) val accessToken : String,
        @JsonProperty("token_type"   ) val tokenType   : String,
        @JsonProperty("expires_in"   ) val expiresIn   : Int?=null
    )

    data class KrunchyToken (
        @JsonProperty("access_token" ) val accessToken : String? = null,
        @JsonProperty("expires_in"   ) val expiresIn   : Int?    = null,
        @JsonProperty("token_type"   ) val tokenType   : String? = null,
        @JsonProperty("scope"        ) val scope       : String? = null,
        @JsonProperty("country"      ) val country     : String? = null
    )

    private suspend fun getToken(): Map<String, String> {
        //Thanks to https://github.com/saikou-app/saikou/blob/main/app/src/main/java/ani/saikou/parsers/anime/Kamyroll.kt
        val tokenrequest = app.get("$mainUrl/auth/v1/token",
            params = mapOf(
                "device_id" to "com.service.data",
                "device_type" to "cloudstream",
                "access_token" to "HMbQeThWmZq4t7w",
            )
        ).parsed<KamyToken>()
        val header = mapOf(
            "Authorization" to "${tokenrequest.tokenType} ${tokenrequest.accessToken}"
        )
        latestHeader = header
        return latestHeader
    }

    private suspend fun getKrunchyToken(): Map<String, String> {
        val testingasa = app.post("$krunchyapi/auth/v1/token",
            headers = mapOf(
                "User-Agent"  to "Crunchyroll/3.26.1 Android/11 okhttp/4.9.2",
                "Content-Type" to "application/x-www-form-urlencoded",
                "Authorization" to "Basic aHJobzlxM2F3dnNrMjJ1LXRzNWE6cHROOURteXRBU2Z6QjZvbXVsSzh6cUxzYTczVE1TY1k="
            ),
            data = mapOf("grant_type" to "client_id")
        ).parsed<KrunchyToken>()
        val header = mapOf(
            "Authorization" to "${testingasa.tokenType} ${testingasa.accessToken}"
        )
        val countryID = testingasa.country!!
        latestKrunchyHeader = header
        latestcountryID = countryID
        return latestKrunchyHeader
    }

    data class PosterTall (
        @JsonProperty("height" ) var height : Int?    = null,
        @JsonProperty("source" ) var source : String? = null,
        @JsonProperty("type"   ) var type   : String? = null,
        @JsonProperty("width"  ) var width  : Int?    = null
    )


    data class KrunchyHome (
        @JsonProperty("total"            ) val total         : Int?             = null,
        @JsonProperty("items"            ) val items         : ArrayList<KrunchyItems> = arrayListOf(),
        @JsonProperty("__class__"        ) val _class_       : String?          = null,
        @JsonProperty("__href__"         ) val _href_        : String?          = null,
        @JsonProperty("__resource_key__" ) val _resourceKey_ : String?          = null,
    )

    data class KrunchyItems (
        @JsonProperty("__class__"           ) var _class_           : String?         = null,
        @JsonProperty("new_content"         ) var newContent        : Boolean?        = null,
        @JsonProperty("description"         ) var description       : String?         = null,
        @JsonProperty("__href__"            ) var _href_            : String?         = null,
        @JsonProperty("title"               ) var title             : String?         = null,
        @JsonProperty("promo_description"   ) var promoDescription  : String?         = null,
        @JsonProperty("slug"                ) var slug              : String?         = null,
        @JsonProperty("channel_id"          ) var channelId         : String?         = null,
        @JsonProperty("images"              ) var images            : KrunchyImages?         = KrunchyImages(),
        @JsonProperty("linked_resource_key" ) var linkedResourceKey : String?         = null,
        @JsonProperty("last_public"         ) var lastPublic        : String?         = null,
        @JsonProperty("slug_title"          ) var slugTitle         : String?         = null,
        @JsonProperty("external_id"         ) var externalId        : String?         = null,
        @JsonProperty("series_metadata"     ) var seriesMetadata    : KrunchySeriesMetadata? = KrunchySeriesMetadata(),
        @JsonProperty("type"                ) var type              : String?         = null,
        @JsonProperty("id"                  ) var id                : String?         = null,
        @JsonProperty("promo_title"         ) var promoTitle        : String?         = null,
        @JsonProperty("new"                 ) var new               : Boolean?        = null
    )

    data class KrunchyImages (
        @JsonProperty("poster_tall" ) var posterTall : ArrayList<ArrayList<PosterTall>> = arrayListOf(),
        @JsonProperty("poster_wide" ) var posterWide : ArrayList<ArrayList<PosterTall>> = arrayListOf(),
    )
    data class KrunchySeriesMetadata (
        @JsonProperty("audio_locales"            ) var audioLocales           : ArrayList<String>       = arrayListOf(),
        @JsonProperty("availability_notes"       ) var availabilityNotes      : String?                 = null,
        @JsonProperty("episode_count"            ) var episodeCount           : Int?                    = null,
        @JsonProperty("extended_description"     ) var extendedDescription    : String?                 = null,
        @JsonProperty("is_dubbed"                ) var isDubbed               : Boolean?                = null,
        @JsonProperty("is_mature"                ) var isMature               : Boolean?                = null,
        @JsonProperty("is_simulcast"             ) var isSimulcast            : Boolean?                = null,
        @JsonProperty("is_subbed"                ) var isSubbed               : Boolean?                = null,
        @JsonProperty("mature_blocked"           ) var matureBlocked          : Boolean?                = null,
        @JsonProperty("maturity_ratings"         ) var maturityRatings        : ArrayList<String>       = arrayListOf(),
        @JsonProperty("season_count"             ) var seasonCount            : Int?                    = null,
        @JsonProperty("series_launch_year"       ) var seriesLaunchYear       : Int?                    = null,
        @JsonProperty("subtitle_locales"         ) var subtitleLocales        : ArrayList<String>       = arrayListOf()
    )
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val items = ArrayList<HomePageList>()
        val urls = listOf(
            Pair("$krunchyapi/content/v1/browse?locale=de-DE&n=20&sort_by=popularity", "Popular"),
            Pair("$krunchyapi/content/v1/browse?locale=de-DE&n=20&sort_by=newly_added", "Recientes")
        )

        urls.apmap {(url, name) ->
            getKrunchyToken()
            val res = app.get(url,
                headers = latestKrunchyHeader).parsed<KrunchyHome>()
            val home = res.items.map {
                val title = it.title
                val posterstring = it.images?.posterTall.toString()
                val posterregex = Regex("height=2340.*source=(.*),.*type=poster_tall")
                val poster = posterregex.find(posterstring)?.destructured?.component1() ?: ""
                val cmsId = it.linkedResourceKey
                val seriesID = it.id
                newAnimeSearchResponse(title!!, seriesID!!){
                    this.posterUrl = poster
                }
            }
            items.add(HomePageList(name, home))
        }
        if (items.size <= 0) throw ErrorLoadingException()
        return HomePageResponse(items)
    }

    data class KrunchySearch (
        @JsonProperty("items"            ) var items         : ArrayList<KrunchySearchItems> = arrayListOf(),
    )

    data class KrunchySearchItems (
        @JsonProperty("items"            ) var items         : ArrayList<KrunchySearchItemsItems> = arrayListOf(),
    )

    data class KrunchySearchItemsItems (
        @JsonProperty("slug_title"          ) var slugTitle         : String?         = null,
        @JsonProperty("new_content"         ) var newContent        : Boolean?        = null,
        @JsonProperty("description"         ) var description       : String?         = null,
        @JsonProperty("channel_id"          ) var channelId         : String?         = null,
        @JsonProperty("external_id"         ) var externalId        : String?         = null,
        @JsonProperty("linked_resource_key" ) var linkedResourceKey : String?         = null,
        @JsonProperty("images"              ) var images            : KrunchyImages?         = KrunchyImages(),
        @JsonProperty("new"                 ) var new               : Boolean?        = null,
        @JsonProperty("promo_title"         ) var promoTitle        : String?         = null,
        @JsonProperty("title"               ) var title             : String?         = null,
        @JsonProperty("slug"                ) var slug              : String?         = null,
        @JsonProperty("__class__"           ) var _class_           : String?         = null,
        @JsonProperty("id"                  ) var id                : String?         = null,
        @JsonProperty("__href__"            ) var _href_            : String?         = null,
        @JsonProperty("promo_description"   ) var promoDescription  : String?         = null,
        @JsonProperty("type"                ) var type              : String?         = null
    )


    private data class KamySearchResponse(
        @JsonProperty("total") val total: Long? = null,
        @JsonProperty("items") val items: List<ResponseItem>? = null
    )
    data class ResponseItem(
        @JsonProperty("items") val items: List<ItemItem>,
        @JsonProperty("type") val type              : String?         = null

    )
    data class Images (
        @JsonProperty("poster_tall" ) var posterTall : ArrayList<PosterTall> = arrayListOf(),
    )

    data class ItemItem(
        @JsonProperty("id") val id: String,
        @JsonProperty("media_type") val type: String,
        @JsonProperty("title") val title: String,
        @JsonProperty("images") val images: Images? = null,
        @JsonProperty("description") val desc: String?,
    )
    override suspend fun search(query: String): List<SearchResponse> {
        getToken()
        val main = app.get("$mainUrl/content/v1/search",
            headers = latestHeader,
            params = mapOf(
                "query" to query,
                "channel_id" to "crunchyroll",
                "limit" to "20",
            )
        ).parsed<KamySearchResponse>()
        val type = main.items?.first()?.type
        val search = ArrayList<SearchResponse>()
        val jsonsearch = main.items?.map {
            val aaa = it.items.map {
                val title = it.title
                val id = it.id
                val posterstring = it.images?.posterTall.toString()
                val posterregex = Regex("height=2340.*source=(.*),.*type=poster_tall")
                val poster = posterregex.find(posterstring)?.destructured?.component1() ?: ""
                search.add(newAnimeSearchResponse(title, id){
                    this.posterUrl = poster
                })
            }
        }
        return search
    }


    data class KamySeasons (
        @JsonProperty("items"     ) var items   : ArrayList<ItemsSeason> = arrayListOf()
    )
    data class ItemsSeason (
        @JsonProperty("id"            ) var id           : String?             = null,
        @JsonProperty("channel_id"    ) var channelId    : String?             = null,
        @JsonProperty("title"         ) var title        : String?             = null,
        @JsonProperty("slug_title"    ) var slugTitle    : String?             = null,
        @JsonProperty("series_id"     ) var seriesId     : String?             = null,
        @JsonProperty("season_number" ) var seasonNumber : Int?                = null,
        @JsonProperty("description"   ) var description  : String?             = null,
        @JsonProperty("episodes"      ) var episodes     : ArrayList<Episodes> = arrayListOf(),
        @JsonProperty("episode_count" ) var episodeCount : Int?                = null
    )

    data class Episodes (
        @JsonProperty("id"                ) var id              : String?  = null,
        @JsonProperty("channel_id"        ) var channelId       : String?  = null,
        @JsonProperty("series_id"         ) var seriesId        : String?  = null,
        @JsonProperty("series_title"      ) var seriesTitle     : String?  = null,
        @JsonProperty("series_slug_title" ) var seriesSlugTitle : String?  = null,
        @JsonProperty("season_id"         ) var seasonId        : String?  = null,
        @JsonProperty("season_title"      ) var seasonTitle     : String?  = null,
        @JsonProperty("season_slug_title" ) var seasonSlugTitle : String?  = null,
        @JsonProperty("season_number"     ) var seasonNumber    : Int?     = null,
        @JsonProperty("episode"           ) var episode         : String?  = null,
        @JsonProperty("episode_number"    ) var episodeNumber   : Int?     = null,
        @JsonProperty("sequence_number"   ) var sequenceNumber  : Int?     = null,
        @JsonProperty("title"             ) var title           : String?  = null,
        @JsonProperty("slug_title"        ) var slugTitle       : String?  = null,
        @JsonProperty("description"       ) var description     : String?  = null,
        @JsonProperty("hd_flag"           ) var hdFlag          : Boolean? = null,
        @JsonProperty("is_mature"         ) var isMature        : Boolean? = null,
        @JsonProperty("episode_air_date"  ) var episodeAirDate  : String?  = null,
        @JsonProperty("is_subbed"         ) var isSubbed        : Boolean? = null,
        @JsonProperty("is_dubbed"         ) var isDubbed        : Boolean? = null,
        @JsonProperty("is_clip"           ) var isClip          : Boolean? = null,
        @JsonProperty("type"              ) var type            : String?  = null,
        @JsonProperty("images"            ) var images          : ImagesEps?  = ImagesEps(),
        @JsonProperty("duration_ms"       ) var durationMs      : Int?     = null,
        @JsonProperty("is_premium_only"   ) var isPremiumOnly   : Boolean? = null
    )

    data class ImagesEps (
        @JsonProperty("thumbnail" ) var thumbnail : ArrayList<Thumbnail> = arrayListOf()
    )
    data class Thumbnail (
        @JsonProperty("width"  ) var width  : Int?    = null,
        @JsonProperty("height" ) var height : Int?    = null,
        @JsonProperty("type"   ) var type   : String? = null,
        @JsonProperty("source" ) var source : String? = null
    )


    data class KrunchyLoadMain (
        @JsonProperty("total" ) var total : Int?            = null,
        @JsonProperty("data"  ) var data  : ArrayList<KrunchyMetadata> = arrayListOf(),
    )

    data class KrunchyMetadata (
        @JsonProperty("series_launch_year"       ) var seriesLaunchYear       : Int?                    = null,
        @JsonProperty("title"                    ) var title                  : String?                 = null,
        @JsonProperty("keywords"                 ) var keywords               : ArrayList<String>       = arrayListOf(),
        @JsonProperty("content_provider"         ) var contentProvider        : String?                 = null,
        @JsonProperty("subtitle_locales"         ) var subtitleLocales        : ArrayList<String>       = arrayListOf(),
        @JsonProperty("is_dubbed"                ) var isDubbed               : Boolean?                = null,
        @JsonProperty("audio_locales"            ) var audioLocales           : ArrayList<String>       = arrayListOf(),
        @JsonProperty("season_tags"              ) var seasonTags             : ArrayList<String>       = arrayListOf(),
        @JsonProperty("episode_count"            ) var episodeCount           : Int?                    = null,
        @JsonProperty("season_count"             ) var seasonCount            : Int?                    = null,
        @JsonProperty("is_subbed"                ) var isSubbed               : Boolean?                = null,
        @JsonProperty("channel_id"               ) var channelId              : String?                 = null,
        @JsonProperty("extended_description"     ) var extendedDescription    : String?                 = null,
        @JsonProperty("seo_description"          ) var seoDescription         : String?                 = null,
        @JsonProperty("is_simulcast"             ) var isSimulcast            : Boolean?                = null,
        @JsonProperty("availability_notes"       ) var availabilityNotes      : String?                 = null,
        @JsonProperty("slug"                     ) var slug                   : String?                 = null,
        @JsonProperty("maturity_ratings"         ) var maturityRatings        : ArrayList<String>       = arrayListOf(),
        @JsonProperty("mature_blocked"           ) var matureBlocked          : Boolean?                = null,
        @JsonProperty("images"                   ) var images                 : KrunchyImages?                 = KrunchyImages(),
        @JsonProperty("media_count"              ) var mediaCount             : Int?                    = null,
        @JsonProperty("id"                       ) var id                     : String?                 = null,
        @JsonProperty("slug_title"               ) var slugTitle              : String?                 = null,
        @JsonProperty("description"              ) var description            : String?                 = null,
        @JsonProperty("is_mature"                ) var isMature               : Boolean?                = null,
        @JsonProperty("seo_title"                ) var seoTitle               : String?                 = null
    )

    override suspend fun load(url: String): LoadResponse {
        val fixID = url.replace("https://api.kamyroll.tech/","")
        getToken()
        getKrunchyToken()
        val metadataUrl = "$krunchyapi/content/v2/cms/series/$fixID?&locale=de-DE"
        val metadainfo = app.get(metadataUrl, headers = latestKrunchyHeader).parsed<KrunchyLoadMain>()
        val title = metadainfo.data.first().title
        val tags = metadainfo.data.first().keywords
        val year = metadainfo.data.first().seriesLaunchYear
        val posterstring = metadainfo.data.first().images?.posterWide?.toString()
        val posterstring2 = metadainfo.data.first().images?.posterTall.toString()
        val posterregex = Regex("height=900.*source=(.*),.*type=poster_wide.*PosterTall")
        val posterRegex2 = Regex("height=1800.*source=(.*),.*type=poster_tall.*PosterTall")
        val backgroundposter = posterregex.find(posterstring!!)?.destructured?.component1() ?: ""
        val poster = posterRegex2.find(posterstring2)?.destructured?.component1() ?: ""
        val description = metadainfo.data.first().description
        val seasonsresponse = app.get("$mainUrl/content/v1/seasons",
            headers = latestHeader,
            params = mapOf(
                "channel_id" to "crunchyroll",
                "id" to fixID,
            )
        ).parsed<KamySeasons>()
        val eps = ArrayList<Episode>()
        val dubeps = ArrayList<Episode>()
        val test = seasonsresponse.items.forEach {
            val dubTitle = it.title
            it.episodes.forEach {
                val isclip = it.isClip == true
                val seasonTitle = it.seasonTitle
                val dub = it.isDubbed
                val eptitle = it.title
                val realeptitle = if (eptitle.isNullOrEmpty()) seasonTitle else eptitle
                val epthumb = it.images?.thumbnail?.getOrNull(6)?.source
                // val epdesc = if (dub == true) "$dubTitle \n ${it.description}" else "${it.description}"
                /* Removed cause crunchy it's confusing
                val epnum = it.episodeNumber
                val seasonID = it.seasonNumber
                */
                val epID = it.id
                val ep = Episode(
                    data = epID!!,
                    name = realeptitle,
                    posterUrl = epthumb,
                    // description = epdesc
                )
                if (seasonTitle!!.contains(Regex("Piece: East Blue|Piece: Alabasta|Piece: Sky Island")) || isclip) {
                    //nothing to filter out non HD eps and clips
                } else if ((dubTitle!!.contains("German") && dub == true)) {
                    dubeps.add(ep)
                } else if (!seasonTitle.contains(Regex("Dub\\)"))) {
                    eps.add(ep)
                }
            }
        }
        return newAnimeLoadResponse(title!!, fixID, TvType.Anime){
            addEpisodes(DubStatus.Subbed,eps)
            addEpisodes(DubStatus.Dubbed,dubeps)
            this.plot = description
            this.tags = tags
            this.year = year
            this.posterUrl = poster
            this.backgroundPosterUrl = backgroundposter
        }
    }

    data class KamyStreams (
        @JsonProperty("channel_id" ) var channelId : String?              = null,
        @JsonProperty("media_id"   ) var mediaId   : String?              = null,
        @JsonProperty("subtitles"  ) var subtitles : ArrayList<Subtitles> = arrayListOf(),
        @JsonProperty("streams"    ) var streams   : ArrayList<Streams>   = arrayListOf()
    )

    data class Subtitles (
        @JsonProperty("locale" ) var locale : String? = null,
        @JsonProperty("url"    ) var url    : String? = null,
        @JsonProperty("format" ) var format : String? = null
    )
    data class Streams (
        @JsonProperty("type"           ) var type          : String? = null,
        @JsonProperty("audio_locale"   ) var audioLocale   : String? = null,
        @JsonProperty("hardsub_locale" ) var hardsubLocale : String? = null,
        @JsonProperty("url"            ) var url           : String? = null
    )

    suspend fun getKamyStream(
        streamLink: String,
        name: String,
        callback: (ExtractorLink) -> Unit
    )  {
        return generateM3u8(
            this.name,
            streamLink,
            "https://static.crunchyroll.com/"
        ).forEach { sub ->
            callback(
                ExtractorLink(
                    this.name,
                    name,
                    sub.url,
                    "https://static.crunchyroll.com/",
                    getQualityFromName(sub.quality.toString()),
                    true
                )
            )
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val streamsrequest = app.get("$mainUrl/videos/v1/streams",
            headers = latestHeader,
            params = mapOf(
                "id" to data,
                "channel_id" to "crunchyroll",
                "type" to "adaptive_hls",
            )).parsed<KamyStreams>()
        println("DATA-ID $data")
        streamsrequest.streams.forEach {
            val urlstream = it.url!!
            if ((it.audioLocale!!.contains(Regex("ja-JP|zh-CN")) && it.hardsubLocale.isNullOrEmpty()))
                getKamyStream(urlstream, "Kamyroll RAW", callback)
            if ((it.hardsubLocale!!.contains(Regex("de-DE")) || it.audioLocale!!.contains(Regex("de-DE")))) {
                val name = if (it.audioLocale!!.contains(Regex("de-DE"))) "Kamyroll German" else "Kamyroll Hardsub German"
                getKamyStream(urlstream, name, callback)
            }
        }
        streamsrequest.subtitles.forEach {
            val suburl = it.url!!
            //Thanks to https://github.com/saikou-app/saikou/blob/e5db3e00dc32ed1c2d14de43de0c18bf79f5da43/app/src/main/java/ani/saikou/parsers/anime/Kamyroll.kt#L137
            val lang= when (it.locale){
                "ja-JP" -> "[ja-JP] Japanese"
                "en-US" -> "[en-US] English"
                "de-DE" -> "[de-DE] German"
                "es-ES" -> "[es-ES] Spanish"
                "es-419" -> "[es-419] Spanish"
                "fr-FR" -> "[fr-FR] French"
                "it-IT" -> "[it-IT] Italian"
                "pt-BR" -> "[pt-BR] Portuguese (Brazil)"
                "pt-PT" -> "[pt-PT] Portuguese (Portugal)"
                "ru-RU" -> "[ru-RU] Russian"
                "zh-CN" -> "[zh-CN] Chinese (Simplified)"
                "tr-TR" -> "[tr-TR] Turkish"
                "ar-ME" -> "[ar-ME] Arabic"
                "ar-SA" -> "[ar-SA] Arabic (Saudi Arabia)"
                "uk-UK" -> "[uk-UK] Ukrainian"
                "he-IL" -> "[he-IL] Hebrew"
                "pl-PL" -> "[pl-PL] Polish"
                "ro-RO" -> "[ro-RO] Romanian"
                "sv-SE" -> "[sv-SE] Swedish"
                ""      -> ""
                else -> "[${it.locale}] "
            }
            subtitleCallback(
                SubtitleFile(lang, suburl)
            )
        }
        return true
    }
}