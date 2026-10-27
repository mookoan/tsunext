package eu.kanade.tachiyomi.novelextension.en.witchculttranslations

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class WctParserTest {
    private val base = "https://witchculttranslation.com/"

    @Test
    fun includesAllTenArcsAndKeepsTheirReadingOrder() {
        val html = buildString {
            append("<div class='entry-content'>")
            for (arc in 1..10) {
                append("<h1>Arc $arc – Title</h1><h1>Phase 1</h1>")
                append("<ul><li><a href='/2026/01/01/arc-$arc-chapter-1/'>Chapter 1: Start</a></li></ul>")
                append("<h2>Phase 2</h2><ul><li><a href='/2026/01/01/arc-$arc-interlude/'>Interlude: End</a></li></ul>")
            }
            append("<h1>Side Content</h1><ul><li><a href='/extra/'>Chapter 1: Extra</a></li></ul></div>")
        }
        val chapters = WctParser.chapters(Jsoup.parse(html, base))
        assertEquals(20, chapters.size)
        assertEquals((1..10).flatMap { listOf(it, it) }, chapters.map { it.arc })
        assertTrue(chapters.first().name.startsWith("Arc 1,"))
        assertTrue(chapters.last().name.startsWith("Arc 10,"))
    }

    @Test
    fun includesTranslationPartnersAndSecondaryPartLinks() {
        val document = Jsoup.parse(
            """
            <div class='entry-content'><h1>Arc 6 – Memories</h1><ul>
            <li><a href='https://translationchicken.com/2017/chapter-part-12/'>Chapter 35: Ice</a> and <a href='/2018/08/22/arc-6-chapter-35-part-3/'>[P3]</a></li>
            <li><a href='https://eminenttranslations.com/reader/chapter-89'>Chapter 89: End</a></li>
            <li><a href='https://kagurojp.wordpress.com/2016/chapter/'>Chapter 90: Next</a></li>
            <li><a href='https://example.com/'>Chapter 91: Not a translator</a></li>
            <li><a href='https://twitter.com/artist'>Artist</a></li></ul></div>
            """.trimIndent(),
            base,
        )
        val chapters = WctParser.chapters(document)
        assertEquals(4, chapters.size)
        assertTrue(chapters[1].name.contains("Chapter 35: Ice [P3]"))
        assertEquals("eminenttranslations.com", chapters[2].url.host)
    }

    @Test
    fun groupsPdfCollectionsInsteadOfRepeatingTheWholeBookForEveryChapter() {
        val document = Jsoup.parse(
            """
            <div class='entry-content'><h1>Arc 4 – Contract</h1><ul>
            <li><a href='/first.pdf?x123'>Chapter 1: First</a></li>
            <li><a href='/first.pdf?x456'>Chapter 2: Second</a></li>
            <li><a href='/second.pdf'>Chapter 3: Third</a></li>
            </ul></div>
            """.trimIndent(),
            base,
        )
        val chapters = WctParser.chapters(document)
        assertEquals(2, chapters.size)
        assertEquals("Arc 4, Chapters 1–2 (PDF)", chapters.first().name)
        assertEquals("/first.pdf", chapters.first().url.encodedPath)
    }

    @Test
    fun extractsWordpressAndEminentContentWithoutCommentsOrSiteNavigation() {
        val wordpress = Jsoup.parse(
            "<h1 class='entry-title'>Title</h1><div class='entry-content'><p>Story.</p><div id='patreon-snippet'>Donate</div><div class='sharedaddy'>Share</div><script>bad()</script></div><div id='comments'>A comment</div>",
            base,
        )
        val html = WctParser.chapterHtml(wordpress)
        assertTrue(html.contains("Story."))
        for (unwanted in listOf("Donate", "Share", "bad()", "A comment")) assertFalse(html.contains(unwanted))
        val eminent = Jsoup.parse("<div id='reader'><div class='reader-paged-content'><p>Partner story.</p></div><button>Next</button></div>", "https://eminenttranslations.com/reader/book/chapter-1")
        assertTrue(WctParser.chapterHtml(eminent).contains("Partner story."))
        assertFalse(WctParser.chapterHtml(eminent).contains("Next"))
    }

    @Test
    fun followsOnlyOtherPartsOfTheSameChapter() {
        val document = Jsoup.parse(
            """
            <title>Chapter 33 (Part 1/2)</title><div class='post-content'>
            <p>Story.</p>
            <a href='/2017/05/25/rezero-arc-6-chapter-33-title-part-22/'>Part two</a>
            <a href='/2017/05/25/rezero-arc-6-chapter-33-title-part-22/#comments'>Duplicate</a>
            <a href='/2017/06/07/rezero-arc-6-chapter-34-title-part-14/'>Next chapter</a>
            </div><div id='comments'><a href='/2017/05/25/rezero-arc-6-chapter-33-other-part-32/'>Comment link</a></div>
            """.trimIndent(),
            "https://translationchicken.com/2017/05/23/rezero-arc-6-chapter-33-title-part-12/",
        )
        assertEquals(1, WctParser.linkedParts(document).size)
        assertTrue(WctParser.linkedParts(document).single().encodedPath.contains("part-22"))
        assertEquals(1, WctParser.part(document))
    }

    @Test
    fun rejectsUnrelatedOrInsecureHosts() {
        assertFalse(WctParser.allowed("https://witchculttranslation.com.evil.test/".toHttpUrl()))
        assertFalse(WctParser.allowed("http://witchculttranslation.com/".toHttpUrl()))
        assertFalse(WctParser.allowed("https://witchculttranslation.com:444/".toHttpUrl()))
        assertTrue(WctParser.allowed("https://www.witchculttranslation.com/".toHttpUrl()))
    }

    @Test
    fun extractsPdfTextAndEscapesHtmlUsingPackagedResources() {
        val directory = Files.createTempDirectory("wct-pdf-test").toFile()
        try {
            val input = javaClass.getResourceAsStream("/sample.pdf")!!
            val html = input.use { PdfText.read(it, directory) }
            assertTrue(html.contains("Fixture PDF chapter text"))
            assertTrue(html.contains("&lt;markup&gt;"))
            assertTrue(html.contains("&amp; symbols"))
        } finally {
            directory.deleteRecursively()
        }
    }
}
