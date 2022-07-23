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

// recursively call 3 times for each site
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
                val document = Jsoup.connect(firstUrl).timeout(5000).get()
                val linkElementsList = document.select("a[href]")
                val imageElementsList = document.select("src")
                val titleElementsList = document.select("h1")
                val descElementsList = document.select("p")
                linkElementsList.forEach { it: Element? ->
//                    println("All Links: " + it?.attr("abs:href").toString())
                    if (it?.text()?.contains("https://") == true) {
                        val sanitizedUrl = it.text().toString().substringAfterLast("https://").substringBefore(" ")
                        println("links https: $sanitizedUrl")
                        urlSet1.add(sanitizedUrl)
                    }
                    if (it?.text()?.contains("http://") == true) {
                        val sanitizedUrl = it.text().toString().substringAfterLast("http://").substringBefore(" ")
                        println("links http: $sanitizedUrl")
                        urlSet1.add(sanitizedUrl)
                    }
                }

                // SCRAPE HOME PAGE OF SITE, IF DETAIL PAGE THEN SCRAPE TEXT
                urlSet1.forEach { pageUrl: String ->
                    Jsoup.connect(pageUrl).timeout(5000).get().apply {
                        val linkElementsList = select("a[href]")
                        val imageElementsList = select("src")
                        val titleElementsList = select("h1")
                        val descElementsList = select("p")

                        var title = ""
                        var description = ""
                        titleElementsList.forEach { it: Element? ->
                            title += it?.text().toString()
                        }
                        descElementsList.forEach { it: Element? ->
                            description += it?.text().toString()
                        }

                        imageUrlList.add(imageElementsList.firstOrNull()?.text().toString())
                        titleList.add(title)
                        pageUrlList.add(pageUrl)
                        descUrlList.add(description)
                    }
                }

                // SCRAPE DETAIL PAGE IF NECESSARY

                Result.success(
                    sendResult(
                        imageUrlArray = imageUrlList.toArray() as Array<String>,
                        titleArray = titleList.toArray() as Array<String>,
                        pageUrlArray = pageUrlList.toArray() as Array<String>,
                        descArray = descUrlList.toArray() as Array<String>
                    )
                )
            } catch (e: Exception) {
                if (e is HttpStatusException) println("Error status: ${e.statusCode}")
                Result.failure()
            }
        }
    }

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