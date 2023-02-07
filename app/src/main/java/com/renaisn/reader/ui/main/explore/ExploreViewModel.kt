package com.renaisn.reader.ui.main.explore

import android.app.Application
import com.renaisn.reader.base.BaseViewModel
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.BookSource
import com.renaisn.reader.help.config.SourceConfig

class ExploreViewModel(application: Application) : BaseViewModel(application) {

    fun topSource(bookSource: BookSource) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            bookSource.customOrder = minXh - 1
            appDb.bookSourceDao.insert(bookSource)
        }
    }

    fun deleteSource(source: BookSource) {
        execute {
            appDb.bookSourceDao.delete(source)
            SourceConfig.removeSource(source.bookSourceUrl)
        }
    }

}