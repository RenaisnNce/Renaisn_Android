package com.renaisn.reader.help

import androidx.annotation.Keep
import com.renaisn.reader.utils.IntentType

@Keep
@Suppress("unused")
object AppIntentType : IntentType.TypeInterface {

    override fun from(path: String?): String? {
        return when (path?.substringAfterLast(".")?.lowercase()) {
            "apk" -> "application/vnd.android.package-archive"
            else -> null
        }
    }

}