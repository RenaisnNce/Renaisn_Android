package com.renaisn.reader.model.remote

import android.net.Uri
import com.renaisn.reader.constant.AppPattern.bookFileRegex
import com.renaisn.reader.constant.BookType
import com.renaisn.reader.data.entities.Book
import com.renaisn.reader.exception.NoStackTraceException
import com.renaisn.reader.help.AppWebDav
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.lib.webdav.WebDav
import com.renaisn.reader.lib.webdav.WebDavFile
import com.renaisn.reader.model.localBook.LocalBook
import com.renaisn.reader.utils.NetworkUtils
import com.renaisn.reader.utils.isContentScheme
import com.renaisn.reader.utils.readBytes
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File

object RemoteBookWebDav : RemoteBookManager() {

    val rootBookUrl get() = "${AppWebDav.rootWebDavUrl}${remoteBookFolder}"

    init {
        runBlocking {
            initRemoteContext()
        }
    }

    suspend fun initRemoteContext() {
        AppWebDav.authorization?.let {
            WebDav(rootBookUrl, it).makeAsDir()
        }
    }

    @Throws(Exception::class)
    override suspend fun getRemoteBookList(path: String): MutableList<RemoteBook> {
        val remoteBooks = mutableListOf<RemoteBook>()
        AppWebDav.authorization?.let {
            //读取文件列表
            val remoteWebDavFileList: List<WebDavFile> = WebDav(path, it).listFiles()
            //转化远程文件信息到本地对象
            remoteWebDavFileList.forEach { webDavFile ->
                if (webDavFile.isDir || bookFileRegex.matches(webDavFile.displayName)) {
                    //扩展名符合阅读的格式则认为是书籍
                    remoteBooks.add(RemoteBook(webDavFile))
                }
            }
        } ?: throw NoStackTraceException("webDav没有配置")
        return remoteBooks
    }

    override suspend fun getRemoteBook(path: String): RemoteBook? {
        AppWebDav.authorization?.let {
            val webDavFile = WebDav(path, it).getWebDavFile()
                ?: return null
            return RemoteBook(webDavFile)
        } ?: throw NoStackTraceException("webDav没有配置")
    }

    override suspend fun downloadRemoteBook(remoteBook: RemoteBook): Uri {
        AppConfig.defaultBookTreeUri
            ?: throw NoStackTraceException("没有设置书籍保存位置!")
        return AppWebDav.authorization?.let {
            val webdav = WebDav(remoteBook.path, it)
            webdav.downloadInputStream().let { inputStream ->
                LocalBook.saveBookFile(inputStream, remoteBook.filename)
            }
        } ?: throw NoStackTraceException("webDav没有配置")
    }

    override suspend fun upload(book: Book) {
        if (!NetworkUtils.isAvailable()) throw NoStackTraceException("网络不可用")
        val localBookUri = Uri.parse(book.bookUrl)
        val putUrl = "$rootBookUrl${File.separator}${book.originName}"
        AppWebDav.authorization?.let {
            if (localBookUri.isContentScheme()) {
                WebDav(putUrl, it).upload(
                    byteArray = localBookUri.readBytes(appCtx),
                    contentType = "application/octet-stream"
                )
            } else {
                WebDav(putUrl, it).upload(localBookUri.path!!)
            }
        } ?: throw NoStackTraceException("webDav没有配置")
        book.origin = BookType.webDavTag + putUrl
        book.save()
    }

    override suspend fun delete(remoteBookUrl: String) {
        AppWebDav.authorization?.let {
            WebDav(remoteBookUrl, it).delete()
        } ?: throw NoStackTraceException("webDav没有配置")
    }

}
