package com.singularitycoder.audioweb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = TABLE_WEB_PAGE)
data class WebPage(
    @ColumnInfo(name = "image_url") val imageUrl: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "page_url") val pageUrl: String,
    @PrimaryKey @ColumnInfo(name = "description") val description: String
) {
    constructor() : this("", "", "", "")
}
