package com.renaisn.reader.model

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.renaisn.reader.constant.EventBus
import com.renaisn.reader.constant.IntentAction
import com.renaisn.reader.data.appDb
import com.renaisn.reader.data.entities.HttpTTS
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.service.BaseReadAloudService
import com.renaisn.reader.service.HttpReadAloudService
import com.renaisn.reader.service.TTSReadAloudService
import com.renaisn.reader.utils.StringUtils
import com.renaisn.reader.utils.postEvent
import splitties.init.appCtx

object ReadAloud {
    private var aloudClass: Class<*> = getReadAloudClass()
    val ttsEngine get() = ReadBook.book?.getTtsEngine() ?: AppConfig.ttsEngine
    var httpTTS: HttpTTS? = null

    private fun getReadAloudClass(): Class<*> {
        val ttsEngine = ttsEngine
        if (ttsEngine.isNullOrBlank()) {
            return TTSReadAloudService::class.java
        }
        if (StringUtils.isNumeric(ttsEngine)) {
            httpTTS = appDb.httpTTSDao.get(ttsEngine.toLong())
            if (httpTTS != null) {
                return HttpReadAloudService::class.java
            }
        }
        return TTSReadAloudService::class.java
    }

    fun upReadAloudClass() {
        stop(appCtx)
        aloudClass = getReadAloudClass()
    }

    fun play(
        context: Context,
        play: Boolean = true,
        pageIndex: Int = ReadBook.durPageIndex,
        startPos: Int = 0
    ) {
        val intent = Intent(context, aloudClass)
        intent.action = IntentAction.play
        intent.putExtra("play", play)
        intent.putExtra("pageIndex", pageIndex)
        intent.putExtra("startPos", startPos)
        context.startService(intent)
    }

    fun playByEventBus(
        play: Boolean = true,
        pageIndex: Int = ReadBook.durPageIndex,
        startPos: Int = 0
    ) {
        val bundle = Bundle().apply {
            putBoolean("play", play)
            putInt("pageIndex", pageIndex)
            putInt("startPos", startPos)
        }
        postEvent(EventBus.READ_ALOUD_PLAY, bundle)
    }

    fun pause(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.pause
            context.startService(intent)
        }
    }

    fun resume(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.resume
            context.startService(intent)
        }
    }

    fun stop(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.stop
            context.startService(intent)
        }
    }

    fun prevParagraph(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.prevParagraph
            context.startService(intent)
        }
    }

    fun nextParagraph(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.nextParagraph
            context.startService(intent)
        }
    }

    fun upTtsSpeechRate(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.upTtsSpeechRate
            context.startService(intent)
        }
    }

    fun setTimer(context: Context, minute: Int) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.setTimer
            intent.putExtra("minute", minute)
            context.startService(intent)
        }
    }

}