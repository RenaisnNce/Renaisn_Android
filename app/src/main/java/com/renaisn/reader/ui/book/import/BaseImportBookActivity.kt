package com.renaisn.reader.ui.book.import

import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.renaisn.reader.R
import com.renaisn.reader.base.VMBaseActivity
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.lib.dialogs.alert
import com.renaisn.reader.ui.document.HandleFileContract
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BaseImportBookActivity<VB : ViewBinding, VM : ViewModel> : VMBaseActivity<VB, VM>() {

    private var localBookTreeSelectListener: ((Boolean) -> Unit)? = null

    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
            localBookTreeSelectListener?.invoke(true)
        } ?: localBookTreeSelectListener?.invoke(false)
    }

    /**
     * 设置书籍保存位置
     */
    protected suspend fun setBookStorage() = suspendCoroutine { block ->
        localBookTreeSelectListener = {
            block.resume(it)
        }
        //测试书籍保存位置是否设置
        if (!AppConfig.defaultBookTreeUri.isNullOrBlank()) {
            block.resume(true)
            return@suspendCoroutine
        }
        //测试读写??
        val storageHelp = String(assets.open("storageHelp.md").readBytes())
        val hint = getString(R.string.select_book_folder)
        alert(hint, storageHelp) {
            yesButton {
                localBookTreeSelect.launch {
                    title = hint
                }
            }
            noButton {
                block.resume(false)
            }
            onCancelled {
                block.resume(false)
            }
        }
    }


}