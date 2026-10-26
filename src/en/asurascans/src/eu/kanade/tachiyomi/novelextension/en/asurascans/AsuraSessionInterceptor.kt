package eu.kanade.tachiyomi.novelextension.en.asurascans

import okhttp3.CacheControl
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLDecoder

internal class AsuraSessionInterceptor(
    private val cookieJar: CookieJar,
    private val siteUrl: HttpUrl,
    private val apiUrl: HttpUrl,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.scheme != apiUrl.scheme || request.url.host != apiUrl.host || request.url.port != apiUrl.port) {
            return chain.proceed(request)
        }

        // Asura's host-only login cookie must become a Bearer header on the API subdomain.
        val token = cookieJar.loadForRequest(siteUrl).firstOrNull { it.name == "access_token" }?.value
        val authenticated = request.newBuilder()
            .cacheControl(CacheControl.Builder().noCache().noStore().build())
            .apply {
                if (!token.isNullOrBlank()) header("Authorization", "Bearer ${URLDecoder.decode(token, "UTF-8")}")
            }
            .build()
        return chain.proceed(authenticated)
    }
}
