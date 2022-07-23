package com.singularitycoder.audioweb

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

// call 3 times for each site
// If body length more than 150 chars in 3rd iteration then add that to the list
// 100 results in the list - maintain background worker to keep iterating until list size is 100
class WebPageParsingWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(IO) {
            val firstUrl = inputData.getString(FIRST_URL)

            println("WebPageParsingWorker -> Work Started with firstUrl: $firstUrl")

            if (firstUrl.isNullOrBlank()) return@withContext Result.failure()

            try {
                val urlSet1 = ArrayList<String>()
                val urlSet2 = ArrayList<String>()

                val imageUrlList = ArrayList<String>()
                val titleList = ArrayList<String>()
                val pageUrlList = ArrayList<String>()
                val descUrlList = ArrayList<String>()

                // SCRAPE GOOGLE
                Jsoup.connect(firstUrl).timeout(10_000).get().apply {
                    val linkElementsList = select("a[href]")
                    val imageElementsList = select("src")
                    val titleElementsList = select("h1")
                    val descElementsList = select("p")
                    linkElementsList.forEach { it: Element? ->
//                    println("All Links: " + it?.attr("abs:href").toString())
                        if (it?.text()?.contains("https://") == true) {
                            val sanitizedUrl = it.text().toString().substringAfterLast("https://").substringBefore(" ")
                            println("links https: $sanitizedUrl")
                            urlSet1.add("https://$sanitizedUrl")
                        }
                        if (it?.text()?.contains("http://") == true) {
                            val sanitizedUrl = it.text().toString().substringAfterLast("http://").substringBefore(" ")
                            println("links http: $sanitizedUrl")
                            urlSet1.add("http://$sanitizedUrl")
                        }
                    }
                }

                // SCRAPE HOME PAGE OF SITE, IF DETAIL PAGE THEN SCRAPE TEXT
                urlSet1.forEach urlSet1@{ pageUrl: String ->
                    Jsoup.connect(pageUrl).timeout(10_000).get().apply {
                        val linkElementsList2 = select("a[href]")
                        val imageElementsList2 = select("src")
                        val titleElementsList2 = select("h1")
                        val descElementsList2 = select("p")

                        var title = ""
                        var description = ""

                        titleElementsList2.forEach titles@{ it: Element? ->
                            title += it?.text().toString()
                        }

                        if (title.isBlank()) return@urlSet1

                        descElementsList2.forEach { it: Element? ->
                            description += ". " + it?.text().toString()
                        }

                        if (descUrlList.size > 3) return@urlSet1

                        imageUrlList.add(imageElementsList2.firstOrNull()?.text().toString())
                        titleList.add(title)
                        pageUrlList.add(pageUrl)
                        descUrlList.add(description)
                    }
                }

                // SCRAPE DETAIL PAGE IF NECESSARY

                Result.success(
                    sendResult(
                        imageUrlArray = imageUrlList.toTypedArray(),
                        titleArray = titleList.toTypedArray(),
                        pageUrlArray = pageUrlList.toTypedArray(),
                        descArray = descUrlList.toTypedArray()
                    )
                )
            } catch (e: Exception) {
                if (e is HttpStatusException) println("Error status: ${e.statusCode}")
                println("Exception: $e")
                Result.failure()
            }
        }
    }

    // java.lang.IllegalStateException: Data cannot occupy more than 10240 bytes when serialized
    // You can't send data with size more than 10240 bytes.
    // Store results in Room DB. Use flow to get each addition realtime
    private fun sendResult(
        imageUrlArray: Array<String>,
        titleArray: Array<String>,
        pageUrlArray: Array<String>,
        descArray: Array<String>,
    ): Data = Data.Builder()
        .putStringArray(KEY_WEB_PAGE_IMAGE_URL_ARRAY, imageUrlArray)
        .putStringArray(KEY_WEB_PAGE_TITLE_ARRAY, titleArray)
        .putStringArray(KEY_WEB_PAGE_PAGE_URL_ARRAY, pageUrlArray)
        .putStringArray(KEY_WEB_PAGE_DESC_ARRAY, descArray)
        .build()
}