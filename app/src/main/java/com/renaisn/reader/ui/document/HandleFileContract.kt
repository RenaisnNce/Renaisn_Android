package com.renaisn.reader.ui.document

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.renaisn.reader.help.IntentData
import com.renaisn.reader.lib.dialogs.SelectItem
import com.renaisn.reader.utils.putJson

@Suppress("unused")
class HandleFileContract :
    ActivityResultContract<(HandleFileContract.HandleFileParam.() -> Unit)?, HandleFileContract.Result>() {

    private var requestCode: Int = 0

    override fun createIntent(context: Context, input: (HandleFileParam.() -> Unit)?): Intent {
        val intent = Intent(context, HandleFileActivity::class.java)
        val handleFileParam = HandleFileParam()
        input?.let {
            handleFileParam.apply(input)
        }
        handleFileParam.let {
            requestCode = it.requestCode
            intent.putExtra("mode", it.mode)
            intent.putExtra("title", it.title)
            intent.putExtra("allowExtensions", it.allowExtensions)
            intent.putJson("otherActions", it.otherActions)
            it.fileData?.let { fileData ->
                intent.putExtra("fileName", fileData.first)
                intent.putExtra("fileKey", IntentData.put(fileData.second))
                intent.putExtra("contentType", fileData.third)
            }
            intent.putExtra("value", it.value)
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Result {
        if (resultCode == RESULT_OK) {
            return Result(intent?.data, requestCode, intent?.getStringExtra("value"))
        }
        return Result(null, requestCode, intent?.getStringExtra("value"))
    }

    companion object {
        const val DIR = 0
        const val FILE = 1
        const val DIR_SYS = 2
        const val EXPORT = 3
    }

    @Suppress("ArrayInDataClass")
    data class HandleFileParam(
        var mode: Int = DIR,
        var title: String? = null,
        var allowExtensions: Array<String> = arrayOf(),
        var otherActions: ArrayList<SelectItem<Int>>? = null,
        var fileData: Triple<String, Any, String>? = null,
        var requestCode: Int = 0,
        var value: String? = null
    )

    data class Result(
        val uri: Uri?,
        val requestCode: Int,
        val value: String?
    )

}