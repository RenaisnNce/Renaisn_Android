package com.renaisn.reader.help.http

import okhttp3.CookieJar
import okhttp3.Interceptor

object Cronet {

    val loader: LoaderInterface? by lazy {
        kotlin.runCatching {
            Class.forName("com.renaisn.reader.lib.cronet.CronetLoader")
                .kotlin.objectInstance as LoaderInterface
        }.getOrNull()
    }

    fun preDownload() {
        loader?.preDownload()
    }

    val interceptor: Interceptor? by lazy {
        kotlin.runCatching {
            val iClass = Class.forName("com.renaisn.reader.lib.cronet.CronetInterceptor")
            iClass.getDeclaredConstructor(CookieJar::class.java)
                .newInstance(cookieJar) as Interceptor
        }.getOrNull()
    }

    interface LoaderInterface {

        fun install(): Boolean

        fun preDownload()

    }

}