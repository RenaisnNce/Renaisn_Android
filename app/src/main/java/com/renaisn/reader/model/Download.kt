package com.renaisn.reader.model

import android.content.Context
import com.renaisn.reader.constant.IntentAction
import com.renaisn.reader.service.DownloadService
import com.renaisn.reader.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}