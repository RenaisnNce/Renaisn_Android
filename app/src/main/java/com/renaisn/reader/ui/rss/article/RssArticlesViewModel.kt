package com.renaisn.reader.ui.rss.article

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.renaisn.reader.base.BaseViewModel
import com.renaisn.reader.constant.AppLog
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.RssArticle
import com.renaisn.reader.data.entities.RssSource
import com.renaisn.reader.model.rss.Rss
import com.renaisn.reader.utils.stackTraceStr
import kotlinx.coroutines.Dispatchers


class RssArticlesViewModel(application: Application) : BaseViewModel(application) {
    val loadFinallyLiveData = MutableLiveData<Boolean>()
    val loadErrorLiveData = MutableLiveData<String>()
    var isLoading = true
    var order = System.currentTimeMillis()
    private var nextPageUrl: String? = null
    var sortName: String = ""
    var sortUrl: String = ""
    var page = 1

    fun init(bundle: Bundle?) {
        bundle?.let {
            sortName = it.getString("sortName") ?: ""
            sortUrl = it.getString("sortUrl") ?: ""
        }
    }

    fun loadContent(rssSource: RssSource) {
        isLoading = true
        page = 1
        order = System.currentTimeMillis()
        Rss.getArticles(viewModelScope, sortName, sortUrl, rssSource, page)
            .onSuccess(Dispatchers.IO) {
                nextPageUrl = it.second
                it.first.let { list ->
                    list.forEach { rssArticle ->
                        rssArticle.order = order--
                    }
                    appDb.rssArticleDao.insert(*list.toTypedArray())
                    if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                        appDb.rssArticleDao.clearOld(rssSource.sourceUrl, sortName, order)
                    }
                    val hasMore = list.isNotEmpty() && !rssSource.ruleNextPage.isNullOrEmpty()
                    loadFinallyLiveData.postValue(hasMore)
                    isLoading = false
                }
            }.onError {
                loadFinallyLiveData.postValue(false)
                AppLog.put("rss获取内容失败", it)
                loadErrorLiveData.postValue(it.stackTraceStr)
            }
    }

    fun loadMore(rssSource: RssSource) {
        isLoading = true
        page++
        val pageUrl = nextPageUrl
        if (!pageUrl.isNullOrEmpty()) {
            Rss.getArticles(viewModelScope, sortName, pageUrl, rssSource, page)
                .onSuccess(Dispatchers.IO) {
                    nextPageUrl = it.second
                    loadMoreSuccess(it.first)
                }
                .onError {
                    loadFinallyLiveData.postValue(false)
                    AppLog.put("rss获取内容失败", it)
                    loadErrorLiveData.postValue(it.stackTraceStr)
                }
        } else {
            loadFinallyLiveData.postValue(false)
        }
    }

    private fun loadMoreSuccess(articles: MutableList<RssArticle>) {
        articles.let { list ->
            if (list.isEmpty()) {
                loadFinallyLiveData.postValue(false)
                return@let
            }
            val firstArticle = list.first()
            val dbArticle = appDb.rssArticleDao
                .get(firstArticle.origin, firstArticle.link)
            if (dbArticle != null) {
                loadFinallyLiveData.postValue(false)
            } else {
                list.forEach { rssArticle ->
                    rssArticle.order = order--
                }
                appDb.rssArticleDao.insert(*list.toTypedArray())
            }
        }
        isLoading = false
    }

}