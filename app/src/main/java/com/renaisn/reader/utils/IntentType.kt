package com.renaisn.reader.utils

import android.net.Uri
import androidx.annotation.Keep
import java.io.File

@Keep
object IntentType {

    fun from(uri: Uri): String? {
        return from(uri.toString())
    }

    fun from(file: File): String? {
        return from(file.absolutePath)
    }

    fun from(path: String?): String? {
        return when (path?.substringAfterLast(".")?.lowercase()) {
            "m4a", "mp3", "mid", "xmf", "ogg", "wav" -> "video/*"
            "3gp", "mp4" -> "audio/*"
            "jpg", "gif", "png", "jpeg", "bmp" -> "image/*"
            "txt", "json" -> "text/plain"
            else -> appIntentType?.from(path)
        }
    }

    private val appIntentType: TypeInterface? by lazy {
        Class.forName("com.renaisn.reader.help.AppIntentType")
            .kotlin.objectInstance as? TypeInterface
    }

    interface TypeInterface {

        fun from(path: String?): String?

    }

}