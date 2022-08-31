package com.singularitycoder.audioweb

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException

const val WORKER_TAG_WEB_PAGE_PARSER = "WORKER_TAG_WEB_PAGE_PARSER"

const val FIRST_URL = "FIRST_URL"

const val DB_AUDIO_WEB = "db_audio_web"
const val TABLE_WEB_PAGE = "table_web_page"

const val KEY_IS_WORK_COMPLETE = "KEY_IS_WORK_COMPLETE"

fun View.showSnackBar(
    message: String,
    anchorView: View? = null,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionBtnText: String = "NA",
    action: () -> Unit = {},
) {
    Snackbar.make(this, message, duration).apply {
        this.animationMode = BaseTransientBottomBar.ANIMATION_MODE_SLIDE
        if (null != anchorView) this.anchorView = anchorView
        if ("NA" != actionBtnText) setAction(actionBtnText) { action.invoke() }
        this.show()
    }
}

fun getDeviceSize(): Point = try {
    Point(deviceWidth(), deviceHeight())
} catch (e: Exception) {
    e.printStackTrace()
    Point(0, 0)
}

fun deviceWidth() = Resources.getSystem().displayMetrics.widthPixels

fun deviceHeight() = Resources.getSystem().displayMetrics.heightPixels

fun Context.color(@ColorRes colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun Context.drawable(@DrawableRes drawableRes: Int): Drawable? =
    ContextCompat.getDrawable(this, drawableRes)


fun File?.customPath(directory: String?, fileName: String?): String {
    var path = this?.absolutePath

    if (directory != null) {
        path += File.separator + directory
    }

    if (fileName != null) {
        path += File.separator + fileName
    }

    return path ?: ""
}

/** /data/user/0/com.singularitycoder.audioweb/files */
fun Context.internalFilesDir(
    directory: String? = null,
    fileName: String? = null,
): File = File(filesDir.customPath(directory, fileName))

/** /storage/emulated/0/Android/data/com.singularitycoder.audioweb/files */
fun Context.externalFilesDir(
    rootDir: String = "",
    subDir: String? = null,
    fileName: String? = null,
): File = File(getExternalFilesDir(rootDir).customPath(subDir, fileName))

inline fun deleteAllFilesFrom(
    directory: File?,
    withName: String,
    crossinline onDone: () -> Unit = {},
) {
    CoroutineScope(Default).launch {
        directory?.listFiles()?.forEach files@{ it: File? ->
            it ?: return@files
            if (it.name.contains(withName)) {
                if (it.exists()) it.delete()
            }
        }

        withContext(Main) { onDone.invoke() }
    }
}

suspend fun parseHtml(): List<WebPage> = withContext(IO) {
    return@withContext try {
        val webPageList = ArrayList<WebPage>()
        val url = "https://www.example.com/"
        val doc = Jsoup.connect(url).get()
        val data = doc.select("class_name")
        println("data: $data")
        for (i in 0 until data.size) {
            val imgUrl = data.select("class_name")
                .select("img")
                .eq(i)
                .attr("src")
            val title = data.select("h4.class_name")
                .select("span")
                .eq(i)
                .text()
            val detailUrl = data.select("h4.class_name")
                .select("a")
                .eq(i)
                .attr("href")
            println("""
                imgUrl: $imgUrl
                title: $title
            """.trimIndent())
            webPageList.add(
                WebPage(
                    imageUrl = imgUrl,
                    title = title,
                    pageUrl = "",
                    description = ""
                )
            )
        }
        return@withContext webPageList
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    }
}
