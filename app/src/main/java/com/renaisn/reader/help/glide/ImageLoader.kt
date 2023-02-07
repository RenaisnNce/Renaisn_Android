package com.renaisn.reader.help.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import com.bumptech.glide.RequestBuilder
import com.renaisn.reader.utils.isAbsUrl
import com.renaisn.reader.utils.isContentScheme
import com.renaisn.reader.utils.isDataUrl
import java.io.File

@Suppress("unused")
object ImageLoader {

    /**
     * 自动判断path类型
     */
    fun load(context: Context, path: String?): RequestBuilder<Drawable> {
        return when {
            path.isNullOrEmpty() -> com.renaisn.reader.help.glide.GlideApp.with(context).load(path)
            path.isDataUrl() -> com.renaisn.reader.help.glide.GlideApp.with(context).load(path)
            path.isAbsUrl() -> com.renaisn.reader.help.glide.GlideApp.with(context).load(path)
            path.isContentScheme() -> com.renaisn.reader.help.glide.GlideApp.with(context).load(Uri.parse(path))
            else -> kotlin.runCatching {
                com.renaisn.reader.help.glide.GlideApp.with(context).load(File(path))
            }.getOrElse {
                com.renaisn.reader.help.glide.GlideApp.with(context).load(path)
            }
        }
    }

    fun loadBitmap(context: Context, path: String?): RequestBuilder<Bitmap> {
        return when {
            path.isNullOrEmpty() -> com.renaisn.reader.help.glide.GlideApp.with(context).asBitmap().load(path)
            path.isDataUrl() -> com.renaisn.reader.help.glide.GlideApp.with(context).asBitmap().load(path)
            path.isAbsUrl() -> com.renaisn.reader.help.glide.GlideApp.with(context).asBitmap().load(path)
            path.isContentScheme() -> com.renaisn.reader.help.glide.GlideApp.with(context).asBitmap().load(Uri.parse(path))
            else -> kotlin.runCatching {
                com.renaisn.reader.help.glide.GlideApp.with(context).asBitmap().load(File(path))
            }.getOrElse {
                com.renaisn.reader.help.glide.GlideApp.with(context).asBitmap().load(path)
            }
        }
    }

    fun loadFile(context: Context, path: String?): RequestBuilder<File> {
        return when {
            path.isNullOrEmpty() -> com.renaisn.reader.help.glide.GlideApp.with(context).asFile().load(path)
            path.isAbsUrl() -> com.renaisn.reader.help.glide.GlideApp.with(context).asFile().load(path)
            path.isContentScheme() -> com.renaisn.reader.help.glide.GlideApp.with(context).asFile().load(Uri.parse(path))
            else -> kotlin.runCatching {
                com.renaisn.reader.help.glide.GlideApp.with(context).asFile().load(File(path))
            }.getOrElse {
                com.renaisn.reader.help.glide.GlideApp.with(context).asFile().load(path)
            }
        }
    }

    fun load(context: Context, @DrawableRes resId: Int?): RequestBuilder<Drawable> {
        return com.renaisn.reader.help.glide.GlideApp.with(context).load(resId)
    }

    fun load(context: Context, file: File?): RequestBuilder<Drawable> {
        return com.renaisn.reader.help.glide.GlideApp.with(context).load(file)
    }

    fun load(context: Context, uri: Uri?): RequestBuilder<Drawable> {
        return com.renaisn.reader.help.glide.GlideApp.with(context).load(uri)
    }

    fun load(context: Context, drawable: Drawable?): RequestBuilder<Drawable> {
        return com.renaisn.reader.help.glide.GlideApp.with(context).load(drawable)
    }

    fun load(context: Context, bitmap: Bitmap?): RequestBuilder<Drawable> {
        return com.renaisn.reader.help.glide.GlideApp.with(context).load(bitmap)
    }

    fun load(context: Context, bytes: ByteArray?): RequestBuilder<Drawable> {
        return com.renaisn.reader.help.glide.GlideApp.with(context).load(bytes)
    }

}
