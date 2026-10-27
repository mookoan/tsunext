package eu.kanade.tachiyomi.novelextension.en.witchculttranslations

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import java.io.IOException

internal object WctParser {
    private val arcHeading = Regex("^Arc\\s+(\\d+)\\b", RegexOption.IGNORE_CASE)
    private val chapterLabel = Regex("^(Chapter|Prologue|Interlude|Final Chapter|Curtain|Special)\\b", RegexOption.IGNORE_CASE)
    private val chapterNumber = Regex("^Chapter\\s+(\\d+)", RegexOption.IGNORE_CASE)
    private val chickenChapter = Regex("rezero-arc-(\\d+)-chapter-(\\d+)-")
    private val partNumber = Regex("Part\\s+(\\d+)\\s*/", RegexOption.IGNORE_CASE)
    private val hosts = setOf("witchculttranslation.com", "eminenttranslations.com", "kagurojp.wordpress.com", "translationchicken.com")

    class ChapterLink(val arc: Int, val url: HttpUrl, private val labels: MutableList<String>) {
        val name: String
            get() {
                if (!isPdf(url) || labels.size == 1) return "Arc $arc, ${labels.first()}${if (isPdf(url)) " (PDF)" else ""}"
                val numbers = labels.mapNotNull { chapterNumber.find(it)?.groupValues?.get(1)?.toIntOrNull() }
                val range = if (numbers.size == labels.size) {
                    "Chapters ${numbers.first()}–${numbers.last()}"
                } else {
                    "${labels.first()} – ${labels.last()}"
                }
                return "Arc $arc, $range (PDF)"
            }

        fun addLabel(label: String) {
            if (label !in labels) labels.add(label)
        }
    }

    fun allowed(url: HttpUrl): Boolean = url.scheme == "https" && url.port == 443 && url.host.removePrefix("www.") in hosts

    fun isPdf(url: HttpUrl): Boolean = url.encodedPath.endsWith(".pdf", ignoreCase = true)

    fun chapters(document: Document): List<ChapterLink> {
        val content = document.selectFirst(".entry-content") ?: throw IOException("WCT table of contents was not found.")
        val chapters = linkedMapOf<String, ChapterLink>()
        var arc = 0
        for (element in content.select("h1, h2, h3, li > a[href]")) {
            val text = element.text().replace('\u00a0', ' ')
            if (element.tagName() != "a") {
                if (text.startsWith("Side Content", ignoreCase = true)) break
                arcHeading.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let { arc = it }
                continue
            }
            if (arc == 0) continue
            val label = if (chapterLabel.containsMatchIn(text)) {
                text
            } else {
                val mainLabel = element.parent()?.selectFirst("a")?.text().orEmpty()
                if (!chapterLabel.containsMatchIn(mainLabel)) continue
                "$mainLabel $text"
            }
            val url = element.absUrl("href").toHttpUrlOrNull()?.newBuilder()?.query(null)?.fragment(null)?.build() ?: continue
            if (!allowed(url)) continue
            val key = "$arc:${url.toString().lowercase()}"
            val existing = chapters[key]
            if (existing != null) {
                if (isPdf(url)) existing.addLabel(label)
            } else {
                chapters[key] = ChapterLink(arc, url, mutableListOf(label))
            }
        }
        if (chapters.isEmpty()) throw IOException("WCT returned no chapter links.")
        return chapters.values.toList()
    }

    fun content(document: Document): Element {
        val selector = when (document.location().toHttpUrlOrNull()?.host?.removePrefix("www.")) {
            "eminenttranslations.com" -> "#reader .reader-paged-content"
            "translationchicken.com" -> ".post-content"
            else -> ".entry-content"
        }
        return document.selectFirst(selector)?.clone() ?: throw IOException("The translator's chapter content was not found.")
    }

    fun chapterHtml(document: Document): String {
        val content = content(document)
        content.select("script, style, iframe, nav, .sharedaddy, .jp-relatedposts, #jp-post-flair, #patreon-snippet, .navigation, .post-navigation").remove()
        content.select("img").forEach { image ->
            val attribute = if (image.hasAttr("data-src")) "data-src" else "src"
            image.attr("src", image.absUrl(attribute))
        }
        content.select("a[href]").forEach { it.attr("href", it.absUrl("href")) }
        if (content.text().isBlank()) throw IOException("The translator returned an empty chapter.")
        val body = Element("div")
        document.selectFirst("h1.entry-title, h2.post-title, h1.post-title")?.let { body.appendElement("h1").text(it.text()) }
        body.appendChild(content)
        return Jsoup.clean(body.html(), document.baseUri(), Safelist.relaxed().addTags("ruby", "rt", "rp"))
    }

    fun linkedParts(document: Document): List<HttpUrl> {
        val original = document.location().toHttpUrlOrNull() ?: return emptyList()
        if (original.host.removePrefix("www.") != "translationchicken.com") return emptyList()
        val chapter = chickenChapter.find(original.encodedPath)?.value ?: return emptyList()
        return content(document).select("a[href]").mapNotNull { anchor ->
            val url = anchor.absUrl("href").toHttpUrlOrNull() ?: return@mapNotNull null
            if (!allowed(url) || url.host != original.host || chickenChapter.find(url.encodedPath)?.value != chapter || !url.encodedPath.contains("-part-")) return@mapNotNull null
            url.newBuilder().query(null).fragment(null).build()
        }.distinctBy { it.toString().lowercase() }
    }

    fun part(document: Document): Int = partNumber.find(document.title().replace('\u00a0', ' '))?.groupValues?.get(1)?.toIntOrNull() ?: 1
}
