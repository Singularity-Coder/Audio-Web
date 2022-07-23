package com.singularitycoder.audioweb

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.view.View
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val WORKER_TAG_WEB_PAGE_PARSER = "WORKER_TAG_WEB_PAGE_PARSER"

const val FIRST_URL = "FIRST_URL"

const val DB_AUDIO_WEB = "db_audio_web"
const val TABLE_WEB_PAGE = "table_web_page"

const val KEY_WEB_PAGE_IMAGE_URL_ARRAY = "KEY_WEB_PAGE_IMAGE_URL_ARRAY"
const val KEY_WEB_PAGE_TITLE_ARRAY = "KEY_WEB_PAGE_TITLE_ARRAY"
const val KEY_WEB_PAGE_PAGE_URL_ARRAY = "KEY_WEB_PAGE_PAGE_URL_ARRAY"
const val KEY_WEB_PAGE_DESC_ARRAY = "KEY_WEB_PAGE_DESC_ARRAY"

fun View.showSnackBar(
    message: String,
    anchorView: View? = null,
    duration: Int = Snackbar.LENGTH_SHORT,
    actionBtnText: String = "NA",
    action: () -> Unit = {}
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
    crossinline onDone: () -> Unit = {}
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
