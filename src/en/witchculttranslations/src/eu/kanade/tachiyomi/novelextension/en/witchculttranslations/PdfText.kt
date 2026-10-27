package eu.kanade.tachiyomi.novelextension.en.witchculttranslations

import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.jsoup.nodes.Element
import java.io.File
import java.io.IOException
import java.io.InputStream

internal object PdfText {
    private val paragraphs = Regex("\\n\\s*\\n")

    fun read(input: InputStream, cacheDirectory: File): String {
        val memory = MemoryUsageSetting.setupTempFileOnly().setTempDir(cacheDirectory)
        return PDDocument.load(input, memory).use { pdf ->
            val text = PDFTextStripper().apply { paragraphEnd = "\n\n" }.getText(pdf)
            if (text.isBlank()) throw IOException("This PDF has no extractable text. Open it in WebView.")
            val body = Element("div")
            paragraphs.split(text.replace("\r\n", "\n")).filter { it.isNotBlank() }.forEach {
                body.appendElement("p").text(it.replace('\n', ' ').trim())
            }
            body.html()
        }
    }
}
