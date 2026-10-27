package eu.kanade.tachiyomi.novelextension.en.witchculttranslations

import eu.kanade.tachiyomi.source.NovelSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.annotation.Source
import keiyoushi.network.get
import keiyoushi.network.rateLimit
import keiyoushi.source.KeiSource
import keiyoushi.utils.applicationContext
import keiyoushi.utils.tryParseDate
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Source
abstract class WitchCultTranslations :
    KeiSource(),
    NovelSource {
    override val supportsLatest = false

    override fun OkHttpClient.Builder.configureClient(): OkHttpClient.Builder = rateLimit(2)

    override suspend fun getPopularManga(page: Int): MangasPage {
        if (page != 1) return MangasPage(emptyList(), false)
        return MangasPage(listOf(book(client.get(baseUrl).asJsoup())), false)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage = getPopularManga(page)

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage {
        val normalized = query.lowercase().filter { it.isLetterOrDigit() }
        return if ("rezerokarahajimeruisekaiseikatsuwebnovelwitchculttranslations".contains(normalized)) {
            getPopularManga(page)
        } else {
            MangasPage(emptyList(), false)
        }
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? = if (url.host == baseUrl.toHttpUrl().host) book(client.get("$baseUrl/table-of-content/").asJsoup()) else null

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val document = client.get("$baseUrl/table-of-content/").asJsoup()
        val parsed = WctParser.chapters(document).mapIndexed { index, link ->
            SChapter.create().apply {
                url = link.url.toString()
                name = link.name
                chapter_number = (index + 1).toFloat()
                date_upload = datePrefix.find(link.url.encodedPath)?.groupValues?.get(1)?.let { dateFormat.tryParseDate(it, ZoneOffset.UTC) } ?: 0L
            }
        }.reversed()
        return SMangaUpdate(book(document), parsed)
    }

    private fun book(document: Document) = SManga.create().apply {
        url = "/table-of-content/"
        title = "Re:Zero kara Hajimeru Isekai Seikatsu (Web Novel)"
        author = "Tappei Nagatsuki"
        status = SManga.ONGOING
        genre = "Fantasy, Adventure"
        thumbnail_url = document.select(".entry-content img").lastOrNull()?.absUrl("src")
        description = "Re:Zero web novel translations indexed by Witch Cult Translations, from Arc 1 through the latest available arc. Includes the translation partners linked by WCT. PDF collections appear once per collection. Early arcs contain translation gaps; this source cannot supply unpublished chapters."
    }

    override fun getChapterUrl(chapter: SChapter): String = chapter.url

    override suspend fun getPageList(chapter: SChapter): List<Page> = listOf(Page(0, chapter.url))

    override suspend fun fetchPageText(page: Page): String {
        val url = page.url.toHttpUrl()
        require(WctParser.allowed(url)) { "Unsupported translation host." }
        if (WctParser.isPdf(url)) {
            return client.get(url).use { response -> PdfText.read(response.body.byteStream(), applicationContext.cacheDir) }
        }
        val documents = linkedMapOf<String, Document>()
        val queue = ArrayDeque<HttpUrl>()
        queue.add(url)
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            val key = next.toString().lowercase()
            if (key in documents) continue
            val document = client.get(next).asJsoup()
            documents[key] = document
            queue.addAll(WctParser.linkedParts(document))
        }
        return documents.values.sortedBy { WctParser.part(it) }.joinToString("\n") { WctParser.chapterHtml(it) }
    }

    companion object {
        private val datePrefix = Regex("^/(\\d{4}/\\d{2}/\\d{2})/")
        private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    }
}
