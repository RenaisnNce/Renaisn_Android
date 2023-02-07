package com.renaisn.reader.utils

import android.text.TextUtils
import com.renaisn.reader.lib.icu4j.CharsetDetector
import org.jsoup.Jsoup
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * 自动获取文件的编码
 * */
@Suppress("MemberVisibilityCanBePrivate", "unused")
object EncodingDetect {

    fun getHtmlEncode(bytes: ByteArray): String? {
        try {
            val doc = Jsoup.parse(String(bytes, StandardCharsets.UTF_8))
            val metaTags = doc.getElementsByTag("meta")
            var charsetStr: String
            for (metaTag in metaTags) {
                charsetStr = metaTag.attr("charset")
                if (!TextUtils.isEmpty(charsetStr)) {
                    return charsetStr
                }
                val content = metaTag.attr("content")
                val httpEquiv = metaTag.attr("http-equiv")
                if (httpEquiv.lowercase(Locale.getDefault()) == "content-type") {
                    charsetStr = if (content.lowercase(Locale.getDefault()).contains("charset")) {
                        content.substring(
                            content.lowercase(Locale.getDefault())
                                .indexOf("charset") + "charset=".length
                        )
                    } else {
                        content.substring(content.lowercase(Locale.getDefault()).indexOf(";") + 1)
                    }
                    if (!TextUtils.isEmpty(charsetStr)) {
                        return charsetStr
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return getEncode(bytes)
    }

    fun getEncode(bytes: ByteArray): String {
        val match = com.renaisn.reader.lib.icu4j.CharsetDetector().setText(bytes).detect()
        return match?.name ?: "UTF-8"
    }

    /**
     * 得到文件的编码
     */
    fun getEncode(filePath: String): String {
        return getEncode(File(filePath))
    }

    /**
     * 得到文件的编码
     */
    fun getEncode(file: File): String {
        val tempByte = getFileBytes(file)
        return getEncode(tempByte)
    }

    private fun getFileBytes(file: File?): ByteArray {
        val byteArray = ByteArray(8000)
        try {
            FileInputStream(file).use {
                it.read(byteArray)
            }
        } catch (e: Exception) {
            System.err.println("Error: $e")
        }
        return byteArray
    }
}