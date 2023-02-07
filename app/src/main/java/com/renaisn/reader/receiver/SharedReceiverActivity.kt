package com.renaisn.reader.receiver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.renaisn.reader.ui.book.search.SearchActivity
import com.renaisn.reader.ui.main.MainActivity
import com.renaisn.reader.utils.startActivity
import splitties.init.appCtx

class SharedReceiverActivity : AppCompatActivity() {

    private val receivingType = "text/plain"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initIntent()
        finish()
    }

    private fun initIntent() {
        when {
            intent.action == Intent.ACTION_SEND && intent.type == receivingType -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    dispose(it)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                    && intent.action == Intent.ACTION_PROCESS_TEXT
                    && intent.type == receivingType -> {
                intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)?.let {
                    dispose(it)
                }
            }
            intent.getStringExtra("action") == "readAloud" -> {
                MediaButtonReceiver.readAloud(appCtx, false)
            }
        }
    }

    private fun dispose(text: String) {
        if (text.isBlank()) {
            return
        }
        val urls = text.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val result = StringBuilder()
        for (url in urls) {
            if (url.matches("http.+".toRegex()))
                result.append("\n").append(url.trim { it <= ' ' })
        }
        if (result.length > 1) {
            startActivity<MainActivity>()
        } else {
            SearchActivity.start(this, text)
        }
    }
}