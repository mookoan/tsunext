package eu.kanade.tachiyomi.novelextension.en.asurascans

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsuraSessionInterceptorTest {
    private val site = "https://asurascans.com/".toHttpUrl()
    private val api = "https://api.asurascans.com/api/novel-series".toHttpUrl()
    private var token: String? = "first.token"
    private val jar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = Unit

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            assertEquals(site, url)
            return listOfNotNull(token?.let { Cookie.Builder().name("access_token").value(it).hostOnlyDomain(site.host).build() })
        }
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(AsuraSessionInterceptor(jar, site, api))
        .addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("{}".toResponseBody()).build()
        }
        .build()

    private fun request(url: String = api.toString()): Request =
        client.newCall(Request.Builder().url(url).header("Origin", site.toString()).build())
            .execute().use { it.request }

    @Test
    fun readsCurrentWebViewCookieForEveryRequest() {
        assertEquals("Bearer first.token", request().header("Authorization"))
        token = "new.token"
        assertEquals("Bearer new.token", request().header("Authorization"))
        token = null
        assertNull(request().header("Authorization"))
    }

    @Test
    fun decodesTheCookieWrittenByAsuraLogin() {
        token = "encoded%2Btoken"
        assertEquals("Bearer encoded+token", request().header("Authorization"))
    }

    @Test
    fun preservesHeadersAndPreventsStaleAccountResponses() {
        val request = request()
        assertEquals(site.toString(), request.header("Origin"))
        assertTrue(request.cacheControl.noCache)
        assertTrue(request.cacheControl.noStore)
        assertNull(request.header("Cookie"))
    }

    @Test
    fun doesNotSendTheTokenToOtherOrigins() {
        for (url in listOf("https://cdn.asurascans.com/image.webp", "https://api.asurascans.com.evil.test/", "http://api.asurascans.com/", "https://api.asurascans.com:444/")) {
            assertNull(request(url).header("Authorization"))
        }
    }
}
