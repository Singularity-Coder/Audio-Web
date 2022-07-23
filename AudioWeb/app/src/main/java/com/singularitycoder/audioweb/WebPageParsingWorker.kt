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

class WebPageParsingWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(IO) {
            val firstUrl = inputData.getString(FIRST_URL)

            println("WebPageParsingWorker -> Work Started with firstUrl: $firstUrl")

            if (firstUrl.isNullOrBlank()) return@withContext Result.failure()

            try {
                // recursively call 3 times for each site
                // If body length more than 150 chars in 3rd iteration then add that to the list
                // 100 results in the list - maintain background worker to keep iterating until list size is 100
                val document = Jsoup.connect(firstUrl).timeout(5000).get()
                val linkList = document.select("a[href]")
                val imageList = document.select("src")
                val titleList = document.select("h1")
                val descList = document.select("p")
                linkList.forEach { it: Element? ->
//                    println("All Links: " + it?.attr("abs:href").toString())
                    if (it?.text()?.contains("https://") == true) {
                        println("links https: " + it.text().toString().substringAfterLast("https://").substringBefore(" "))
                    }
                    if (it?.text()?.contains("http://") == true) {
                        println("links http: " + it.text().toString().substringAfterLast("http://").substringBefore(" "))
                    }
                }
                Result.success(
                    sendResult(
                        imageUrlArray = arrayOf(),
                        titleArray = arrayOf(),
                        pageUrlArray = arrayOf(),
                        descArray = arrayOf()
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