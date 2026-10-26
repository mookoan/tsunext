package eu.kanade.tachiyomi.novelextension.en.asurascans

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import keiyoushi.utils.tryParse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import kotlin.time.Instant

@Serializable
internal class SeriesList(val data: List<Series>)

@Serializable
internal class SeriesDetail(val series: Series, val chapters: List<Chapter>)

@Serializable
internal class Series(
    val slug: String,
    private val title: String,
    @SerialName("cover_url") private val coverUrl: String? = null,
    private val description: String? = null,
    private val author: String? = null,
    private val genres: List<String>? = null,
    private val status: String? = null,
) {
    fun toManga() = SManga.create().apply {
        url = "/novels/$slug"
        title = this@Series.title
        thumbnail_url = coverUrl
        description = this@Series.description?.let { Jsoup.parseBodyFragment(it).wholeText().trim() }
        author = this@Series.author
        genre = genres?.joinToString()
        status = when (this@Series.status?.lowercase()) {
            "ongoing" -> SManga.ONGOING
            "complete", "completed" -> SManga.COMPLETED
            "hiatus" -> SManga.ON_HIATUS
            "dropped" -> SManga.CANCELLED
            else -> SManga.UNKNOWN
        }
    }
}

@Serializable
internal class Chapter(
    val number: Float,
    private val title: String? = null,
    @SerialName("published_at") private val publishedAt: String? = null,
    @SerialName("is_locked") private val isLocked: Boolean = false,
) {
    fun toChapter(slug: String) = SChapter.create().apply {
        val numberText = number.toString().removeSuffix(".0")
        url = "/novels/$slug/chapter/$numberText"
        name = buildString {
            if (isLocked) append("🔒 ")
            append("Chapter $numberText")
            title?.takeIf { it.isNotBlank() }?.let { append(": ${it.trim()}") }
        }
        chapter_number = number
        date_upload = Instant.tryParse(publishedAt)
    }
}

@Serializable
internal class ChapterContent(
    @SerialName("content_html") val contentHtml: String? = null,
    @SerialName("is_locked") val isLocked: Boolean = false,
)
