package com.example.newsreader

import android.content.Context
import android.text.Html
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class NewsRepository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun loadSources(): List<NewsSource> {
        val json = context.assets.open("news_sources.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            val cats = item.getJSONArray("categories")
            NewsSource(item.getString("name"), item.getString("url"),
                (0 until cats.length()).map { cats.getString(it) }, item.optString("region", ""))
        }
    }

    suspend fun fetch(category: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val sources = loadSources().filter { category in it.categories }
        coroutineScope {
            sources.map { source ->
                async {
                    runCatching { fetchFeed(source) }.getOrDefault(emptyList())
                }
            }.awaitAll().flatten()
                .distinctBy { it.link }
                .sortedByDescending { parseDate(it.published) }
                .take(80)
        }
    }

    private fun fetchFeed(source: NewsSource): List<NewsArticle> {
        val request = Request.Builder().url(source.url)
            .header("User-Agent", "DailyBriefAndroid/1.0 (RSS reader)")
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.byteStream() ?: error("Empty response")
            return parseXml(body, source.name)
        }
    }

    private fun parseXml(input: java.io.InputStream, source: String): List<NewsArticle> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(input, null)
        val results = mutableListOf<NewsArticle>()
        var event = parser.eventType
        var insideItem = false
        var title = ""; var link = ""; var date = ""; var description = ""
        var currentTag = ""
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == "item" || tag == "entry") {
                        insideItem = true
                        title = ""; link = ""; date = ""; description = ""
                    } else if (insideItem) {
                        currentTag = tag
                        if (tag == "link") {
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrBlank()) link = href
                        }
                    }
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (insideItem) {
                    when (currentTag) {
                        "title" -> title += parser.text
                        "link" -> link += parser.text
                        "pubdate", "published", "updated", "dc:date" -> date += parser.text
                        "description", "summary", "content:encoded", "content" -> description += parser.text
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == "item" || tag == "entry") {
                        val cleanTitle = clean(title)
                        val cleanLink = link.trim()
                        if (cleanTitle.isNotBlank() && cleanLink.startsWith("http")) {
                            results += NewsArticle(cleanTitle, cleanLink, source, date.trim(), clean(description).take(260))
                        }
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            event = parser.nextToken()
        }
        return results
    }

    private fun clean(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_COMPACT).toString()
            .replace(Regex("\\s+"), " ").trim()

    private fun parseDate(value: String): Long {
        if (value.isBlank()) return 0L
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )
        for (pattern in patterns) try {
            val format = SimpleDateFormat(pattern, Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            return format.parse(value)?.time ?: 0L
        } catch (_: Exception) { }
        return 0L
    }
}
