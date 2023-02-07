package com.renaisn.reader.ui.book.changesource

import android.app.Application
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.renaisn.reader.base.BaseViewModel
import com.renaisn.reader.constant.AppConst
import com.renaisn.reader.constant.AppPattern
import com.renaisn.reader.constant.PreferKey
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.Book
import com.renaisn.reader.data.entities.BookChapter
import com.renaisn.reader.data.entities.BookSource
import com.renaisn.reader.data.entities.SearchBook
import com.renaisn.reader.exception.NoStackTraceException
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.help.config.SourceConfig
import com.renaisn.reader.help.coroutine.CompositeCoroutine
import com.renaisn.reader.help.coroutine.Coroutine
import com.renaisn.reader.model.webBook.WebBook
import com.renaisn.reader.utils.getPrefBoolean
import com.renaisn.reader.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.min

@Suppress("MemberVisibilityCanBePrivate")
open class ChangeBookSourceViewModel(application: Application) : BaseViewModel(application) {
    private val threadCount = AppConfig.threadCount
    private var searchPool: ExecutorCoroutineDispatcher? = null
    val searchStateData = MutableLiveData<Boolean>()
    var searchFinishCallback: ((isEmpty: Boolean) -> Unit)? = null
    var name: String = ""
    var author: String = ""
    private var tasks = CompositeCoroutine()
    private var screenKey: String = ""
    private var bookSourceList = arrayListOf<BookSource>()
    private var searchBookList = arrayListOf<SearchBook>()
    private val searchBooks = Collections.synchronizedList(arrayListOf<SearchBook>())
    private val tocMap = ConcurrentHashMap<String, List<BookChapter>>()
    private var searchCallback: SourceCallback? = null
    val searchDataFlow = callbackFlow {

        searchCallback = object : SourceCallback {

            override fun searchSuccess(searchBook: SearchBook) {
                appDb.searchBookDao.insert(searchBook)
                when {
                    screenKey.isEmpty() -> searchBooks.add(searchBook)
                    searchBook.name.contains(screenKey) -> searchBooks.add(searchBook)
                    else -> return
                }
                trySend(arrayOf(searchBooks))
            }

            override fun upAdapter() {
                trySend(arrayOf(searchBooks))
            }

        }

        getDbSearchBooks().let {
            searchBooks.clear()
            searchBooks.addAll(it)
            trySend(arrayOf(searchBooks))
        }

        if (searchBooks.isEmpty()) {
            startSearch()
        }

        awaitClose {
            searchCallback = null
        }
    }.map {
        searchBooks.sortedWith { o1, o2 ->
            val o1bs = SourceConfig.getBookScore(o1.origin, o1.name, o1.author)
            val o2bs = SourceConfig.getBookScore(o2.origin, o2.name, o2.author)
            when {
                o1bs - o2bs > 0 -> -1
                o1bs - o2bs < 0 -> 1
                else -> {
                    val o1ss = SourceConfig.getSourceScore(o1.origin)
                    val o2ss = SourceConfig.getSourceScore(o2.origin)
                    when {
                        o1ss - o2ss > 0 -> -1
                        o1ss - o2ss < 0 -> 1
                        else -> o1.originOrder - o2.originOrder
                    }
                }
            }
        }
    }.flowOn(IO)

    @Volatile
    private var searchIndex = -1

    override fun onCleared() {
        super.onCleared()
        searchPool?.close()
    }

    @CallSuper
    open fun initData(arguments: Bundle?) {
        arguments?.let { bundle ->
            bundle.getString("name")?.let {
                name = it
            }
            bundle.getString("author")?.let {
                author = it.replace(AppPattern.authorRegex, "")
            }
        }
    }

    private fun initSearchPool() {
        searchPool = Executors
            .newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
        searchIndex = -1
    }

    fun refresh() {
        getDbSearchBooks().let {
            searchBooks.clear()
            searchBooks.addAll(it)
            searchCallback?.upAdapter()
        }
    }

    /**
     * 搜索书籍
     */
    fun startSearch() {
        execute {
            stopSearch()
            appDb.searchBookDao.clear(name, author)
            searchBooks.clear()
            bookSourceList.clear()
            val searchGroup = AppConfig.searchGroup
            if (searchGroup.isBlank()) {
                bookSourceList.addAll(appDb.bookSourceDao.allEnabled)
            } else {
                val sources = appDb.bookSourceDao.getEnabledByGroup(searchGroup)
                if (sources.isEmpty()) {
                    AppConfig.searchGroup = ""
                    bookSourceList.addAll(appDb.bookSourceDao.allEnabled)
                } else {
                    bookSourceList.addAll(sources)
                }
            }
            searchStateData.postValue(true)
            initSearchPool()
            for (i in 0 until threadCount) {
                search()
            }
        }
    }

    private fun search() {
        synchronized(this) {
            if (searchIndex >= bookSourceList.lastIndex) {
                return
            }
            searchIndex++
        }
        val source = bookSourceList[searchIndex]
        val task = Coroutine.async(scope = viewModelScope, context = searchPool!!) {
            val resultBooks = WebBook.searchBookAwait(source, name)
            resultBooks.forEach { searchBook ->
                if (searchBook.name == name) {
                    if ((AppConfig.changeSourceCheckAuthor && searchBook.author.contains(author))
                        || !AppConfig.changeSourceCheckAuthor
                    ) {
                        if (searchBook.latestChapterTitle.isNullOrEmpty()) {
                            if (AppConfig.changeSourceLoadInfo || AppConfig.changeSourceLoadToc) {
                                loadBookInfo(source, searchBook.toBook())
                            } else {
                                searchCallback?.searchSuccess(searchBook)
                            }
                        } else {
                            searchCallback?.searchSuccess(searchBook)
                        }
                    }
                }
            }
        }.timeout(60000L)
            .onError {
                nextSearch()
            }
            .onSuccess {
                nextSearch()
            }
        tasks.add(task)
    }

    private suspend fun loadBookInfo(source: BookSource, book: Book) {
        WebBook.getBookInfoAwait(source, book)
        if (context.getPrefBoolean(PreferKey.changeSourceLoadToc)) {
            loadBookToc(source, book)
        } else {
            //从详情页里获取最新章节
            val searchBook = book.toSearchBook()
            searchCallback?.searchSuccess(searchBook)
        }
    }

    private suspend fun loadBookToc(source: BookSource, book: Book) {
        val chapters = WebBook.getChapterListAwait(source, book).getOrThrow()
        tocMap[book.bookUrl] = chapters
        book.latestChapterTitle = chapters.last().title
        val searchBook: SearchBook = book.toSearchBook()
        searchCallback?.searchSuccess(searchBook)
    }

    private fun nextSearch() {
        synchronized(this) {
            if (searchIndex < bookSourceList.lastIndex) {
                search()
            } else {
                searchIndex++
            }
            if (searchIndex >= bookSourceList.lastIndex + bookSourceList.size
                || searchIndex >= bookSourceList.lastIndex + threadCount
            ) {
                searchStateData.postValue(false)
                tasks.clear()
                searchFinishCallback?.invoke(searchBooks.isEmpty())
            }
        }
    }

    /**
     * 刷新列表
     */
    fun startRefreshList() {
        execute {
            stopSearch()
            searchBookList.clear()
            searchBookList.addAll(searchBooks)
            searchBooks.clear()
            searchStateData.postValue(true)
            initSearchPool()
            for (i in 0 until threadCount) {
                refreshList()
            }
        }
    }

    private fun refreshList() {
        synchronized(this) {
            if (searchIndex >= searchBookList.lastIndex) {
                return
            }
            searchIndex++
        }
        val searchBook = searchBookList[searchIndex]
        val task = Coroutine.async(scope = viewModelScope, context = searchPool!!) {
            val source = appDb.bookSourceDao.getBookSource(searchBook.origin) ?: return@async
            loadBookInfo(source, searchBook.toBook())
        }.timeout(60000L)
            .onError {
                nextRefreshList()
            }
            .onSuccess {
                nextRefreshList()
            }
        tasks.add(task)
    }

    private fun nextRefreshList() {
        synchronized(this) {
            if (searchIndex < searchBookList.lastIndex) {
                refreshList()
            } else {
                searchIndex++
            }
            if (searchIndex >= searchBookList.lastIndex + min(searchBookList.size, threadCount)) {
                searchStateData.postValue(false)
                tasks.clear()
            }
        }
    }

    private fun getDbSearchBooks(): List<SearchBook> {
        return if (screenKey.isEmpty()) {
            if (AppConfig.changeSourceCheckAuthor) {
                appDb.searchBookDao.changeSourceByGroup(
                    name, author, AppConfig.searchGroup
                )
            } else {
                appDb.searchBookDao.changeSourceByGroup(
                    name, "", AppConfig.searchGroup
                )
            }
        } else {
            if (AppConfig.changeSourceCheckAuthor) {
                appDb.searchBookDao.changeSourceSearch(
                    name, author, screenKey, AppConfig.searchGroup
                )
            } else {
                appDb.searchBookDao.changeSourceSearch(
                    name, "", screenKey, AppConfig.searchGroup
                )
            }
        }
    }

    /**
     * 筛选
     */
    fun screen(key: String?) {
        screenKey = key?.trim() ?: ""
        execute {
            getDbSearchBooks().let {
                searchBooks.clear()
                searchBooks.addAll(it)
                searchCallback?.upAdapter()
            }
        }
    }

    fun startOrStopSearch() {
        if (tasks.isEmpty) {
            startSearch()
        } else {
            stopSearch()
        }
    }

    fun stopSearch() {
        tasks.clear()
        searchPool?.close()
        searchStateData.postValue(false)
    }

    fun getToc(
        book: Book,
        onError: (msg: String) -> Unit,
        onSuccess: (toc: List<BookChapter>, source: BookSource) -> Unit
    ): Coroutine<Pair<List<BookChapter>, BookSource>> {
        return execute {
            val toc = tocMap[book.bookUrl]
            if (toc != null) {
                val source = appDb.bookSourceDao.getBookSource(book.origin)
                return@execute Pair(toc, source!!)
            }
            val result = getToc(book).getOrThrow()
            tocMap[book.bookUrl] = result.first
            return@execute result
        }.onSuccess {
            onSuccess.invoke(it.first, it.second)
        }.onError {
            onError.invoke(it.localizedMessage ?: "获取目录出错")
        }
    }

    suspend fun getToc(book: Book): Result<Pair<List<BookChapter>, BookSource>> {
        return kotlin.runCatching {
            withContext(IO) {
                val source = appDb.bookSourceDao.getBookSource(book.origin)
                    ?: throw NoStackTraceException("书源不存在")
                if (book.tocUrl.isEmpty()) {
                    WebBook.getBookInfoAwait(source, book)
                }
                val toc = WebBook.getChapterListAwait(source, book).getOrThrow()
                Pair(toc, source)
            }
        }
    }

    fun disableSource(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                source.enabled = false
                appDb.bookSourceDao.update(source)
            }
            searchBooks.remove(searchBook)
            searchCallback?.upAdapter()
        }
    }

    fun topSource(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                val minOrder = appDb.bookSourceDao.minOrder - 1
                source.customOrder = minOrder
                searchBook.originOrder = source.customOrder
                appDb.bookSourceDao.update(source)
                updateSource(searchBook)
            }
            searchCallback?.upAdapter()
        }
    }

    fun bottomSource(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                val maxOrder = appDb.bookSourceDao.maxOrder + 1
                source.customOrder = maxOrder
                searchBook.originOrder = source.customOrder
                appDb.bookSourceDao.update(source)
                updateSource(searchBook)
            }
            searchCallback?.upAdapter()
        }
    }

    fun updateSource(searchBook: SearchBook) {
        appDb.searchBookDao.update(searchBook)
    }

    fun del(searchBook: SearchBook) {
        execute {
            appDb.bookSourceDao.getBookSource(searchBook.origin)?.let { source ->
                appDb.bookSourceDao.delete(source)
                appDb.searchBookDao.delete(searchBook)
                SourceConfig.removeSource(source.bookSourceUrl)
            }
        }
        searchBooks.remove(searchBook)
        searchCallback?.upAdapter()
    }

    fun autoChangeSource(
        bookType: Int?,
        onSuccess: (book: Book, toc: List<BookChapter>, source: BookSource) -> Unit
    ) {
        execute {
            searchBooks.forEach {
                if (it.type == bookType) {
                    val book = it.toBook()
                    val result = getToc(book).getOrNull()
                    if (result != null) {
                        return@execute Triple(book, result.first, result.second)
                    }
                }
            }
            throw NoStackTraceException("没有有效源")
        }.onSuccess {
            onSuccess.invoke(it.first, it.second, it.third)
        }.onError {
            context.toastOnUi("自动换源失败\n${it.localizedMessage}")
        }
    }

    fun setBookScore(searchBook: SearchBook, score: Int) {
        execute {
            SourceConfig.setBookScore(searchBook.origin, searchBook.name, searchBook.author, score)
            searchCallback?.upAdapter()
        }
    }

    fun getBookScore(searchBook: SearchBook): Int {
        return SourceConfig.getBookScore(searchBook.origin, searchBook.name, searchBook.author)
    }

    interface SourceCallback {

        fun searchSuccess(searchBook: SearchBook)

        fun upAdapter()

    }

}