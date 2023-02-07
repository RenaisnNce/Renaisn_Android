package com.renaisn.reader.help.source

import com.renaisn.reader.data.entities.RssSource
import com.renaisn.reader.utils.ACache
import com.renaisn.reader.utils.MD5Utils
import com.renaisn.reader.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val aCache by lazy { ACache.get("rssSortUrl") }

private fun RssSource.getSortUrlsKey(): String {
    return MD5Utils.md5Encode(sourceUrl + sortUrl)
}

suspend fun RssSource.sortUrls(): List<Pair<String, String>> {
    return arrayListOf<Pair<String, String>>().apply {
        val sortUrlsKey = getSortUrlsKey()
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                var str = sortUrl
                if (sortUrl?.startsWith("<js>", false) == true
                    || sortUrl?.startsWith("@js:", false) == true
                ) {
                    str = aCache.getAsString(sortUrlsKey)
                    if (str.isNullOrBlank()) {
                        val jsStr = if (sortUrl!!.startsWith("@")) {
                            sortUrl!!.substring(4)
                        } else {
                            sortUrl!!.substring(4, sortUrl!!.lastIndexOf("<"))
                        }
                        str = evalJS(jsStr).toString()
                        aCache.put(sortUrlsKey, str)
                    }
                }
                str?.split("(&&|\n)+".toRegex())?.forEach { sort ->
                    val name = sort.substringBefore("::")
                    val url = sort.substringAfter("::", "")
                    if (url.isNotEmpty()) {
                        if (url.startsWith("{{")) {
                            add(Pair(name, url))
                        } else {
                            add(Pair(name, NetworkUtils.getAbsoluteURL(sourceUrl, url)))
                        }
                    }
                }
                if (isEmpty()) {
                    add(Pair("", sourceUrl))
                }
            }
        }
    }
}

suspend fun RssSource.removeSortCache() {
    withContext(Dispatchers.IO) {
        aCache.remove(getSortUrlsKey())
    }
}