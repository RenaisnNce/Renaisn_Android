package com.renaisn.reader.help.http

import okhttp3.*
import okhttp3.Response.Builder

/**
 * An HTTP response.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class StrResponse {
    var raw: Response
        private set
    var body: String? = null
        private set
    var errorBody: ResponseBody? = null
        private set

    constructor(rawResponse: Response, body: String?) {
        this.raw = rawResponse
        this.body = body
    }

    constructor(url: String, body: String?) {
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: Exception) {
            Request.Builder().url("http://localhost/").build()
        }
        raw = Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(request)
            .build()
        this.body = body
    }

    constructor(rawResponse: Response, errorBody: ResponseBody?) {
        this.raw = rawResponse
        this.errorBody = errorBody
    }

    fun raw() = raw

    fun url(): String {
        raw.networkResponse?.let {
            return it.request.url.toString()
        }
        return raw.request.url.toString()
    }

    val url: String get() = url()

    fun body() = body

    fun code(): Int {
        return raw.code
    }

    fun message(): String {
        return raw.message
    }

    fun headers(): Headers {
        return raw.headers
    }

    fun isSuccessful(): Boolean = raw.isSuccessful

    fun errorBody(): ResponseBody? {
        return errorBody
    }

    override fun toString(): String {
        return raw.toString()
    }

}