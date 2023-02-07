package com.renaisn.reader.ui.rss.article

import android.content.Context
import androidx.viewbinding.ViewBinding
import com.renaisn.reader.base.adapter.RecyclerAdapter
import com.renaisn.reader.data.entities.RssArticle


abstract class BaseRssArticlesAdapter<VB : ViewBinding>(context: Context, val callBack: CallBack) :
    RecyclerAdapter<RssArticle, VB>(context) {

    interface CallBack {
        val isGridLayout: Boolean
        fun readRss(rssArticle: RssArticle)
    }
}