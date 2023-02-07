package com.renaisn.reader.ui.config

import android.app.Application
import com.renaisn.reader.R
import com.renaisn.reader.base.BaseViewModel
import com.renaisn.reader.help.AppWebDav
import com.renaisn.reader.help.book.BookHelp
import com.renaisn.reader.utils.FileUtils
import com.renaisn.reader.utils.toastOnUi

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }
    }


}