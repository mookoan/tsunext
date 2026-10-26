package eu.kanade.tachiyomi.novelextension.en.asurascans

import eu.kanade.tachiyomi.source.NovelSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.get
import keiyoushi.network.rateLimit
import keiyoushi.source.KeiSource
import keiyoushi.utils.parseAs
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import java.io.IOException

@Source
abstract class AsuraScans :
    KeiSource(),
    NovelSource {
    private val apiUrl = "https://api.asurascans.com/api/novel-series".toHttpUrl()

    override fun OkHttpClient.Builder.configureClient(): OkHttpClient.Builder = rateLimit(2)
        .addInterceptor(AsuraSessionInterceptor(network.client.cookieJar, baseUrl.toHttpUrl(), apiUrl))

    override suspend fun getPopularManga(page: Int) = browse(page, "popular")

    override suspend fun getLatestUpdates(page: Int) = browse(page, "update")

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList) = browse(page, "popular", query)

    private suspend fun browse(page: Int, sort: String, query: String = ""): MangasPage {
        val url = apiUrl.newBuilder()
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .addQueryParameter("offset", ((page - 1) * PAGE_SIZE).toString())
            .addQueryParameter("sort", sort)
            .apply { if (query.isNotBlank()) addQueryParameter("search", query) }
            .build()
        val series = fetchJson<SeriesList>(url).data
        return MangasPage(series.map { it.toManga() }, series.size == PAGE_SIZE)
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.scheme != "https" || url.host != baseUrl.toHttpUrl().host) return null
        val segments = url.pathSegments
        if (segments.firstOrNull() != "novels") return null
        val slug = segments.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
        return detail(slug).series.toManga()
    }

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val detail = detail(manga.url.substringAfterLast('/'))
        return SMangaUpdate(
            manga = detail.series.toManga(),
            chapters = detail.chapters.sortedByDescending { it.number }.map { it.toChapter(detail.series.slug) },
        )
    }

    private suspend fun detail(slug: String): SeriesDetail = fetchJson(apiUrl.newBuilder().addPathSegment(slug).build())

    override suspend fun getPageList(chapter: SChapter) = listOf(Page(0, getChapterUrl(chapter)))

    override suspend fun fetchPageText(page: Page): String {
        val url = page.url.toHttpUrl()
        require(url.scheme == "https" && url.host == baseUrl.toHttpUrl().host)
        val segments = url.pathSegments
        require(segments.size == 4 && segments[0] == "novels" && segments[2] == "chapter")
        val chapterUrl = apiUrl.newBuilder()
            .addPathSegment(segments[1])
            .addPathSegment("chapter")
            .addPathSegment(segments[3])
            .build()
        val chapter = fetchJson<ChapterContent>(chapterUrl)
        val content = chapter.contentHtml?.takeIf { it.isNotBlank() }
            ?: throw IOException(
                if (chapter.isLocked) {
                    "Chapter locked. Log in to your premium account in this source's WebView, then retry."
                } else {
                    "Asura returned an empty chapter."
                },
            )
        return cleanChapter(content, page.url)
    }

    private suspend inline fun <reified T> fetchJson(url: HttpUrl): T = client.get(url, ensureSuccess = false).use { response ->
        when (response.code) {
            401 -> throw IOException("Asura session expired. Log in again in this source's WebView.")
            403 -> throw IOException("Asura denied access. Check your account and chapter access in WebView.")
        }
        if (!response.isSuccessful) throw IOException("Asura request failed: HTTP ${response.code}")
        response.parseAs<T>()
    }

    companion object {
        private const val PAGE_SIZE = 20

        internal fun cleanChapter(html: String, url: String): String {
            val document = Jsoup.parseBodyFragment(html, url)
            document.select("script, style, iframe").remove()
            var last = document.select("p").lastOrNull()
            while (last != null && last.text().contains("Back to homepage") && last.text().contains("Reporting chapter:")) {
                last.remove()
                last = document.select("p").lastOrNull()
            }
            return document.body().html()
        }
    }
}
