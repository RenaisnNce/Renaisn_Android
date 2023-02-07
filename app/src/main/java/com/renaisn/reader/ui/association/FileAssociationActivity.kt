package com.renaisn.reader.ui.association

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.documentfile.provider.DocumentFile
import com.renaisn.reader.R
import com.renaisn.reader.base.VMBaseActivity
import com.renaisn.reader.constant.AppConst
import com.renaisn.reader.constant.AppLog
import com.renaisn.reader.databinding.ActivityTranslucenceBinding
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.lib.dialogs.alert
import com.renaisn.reader.lib.permission.Permissions
import com.renaisn.reader.lib.permission.PermissionsCompat
import com.renaisn.reader.ui.book.read.ReadBookActivity
import com.renaisn.reader.ui.document.HandleFileContract
import com.renaisn.reader.utils.*
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx

import java.io.File
import java.io.FileOutputStream

class FileAssociationActivity :
    VMBaseActivity<ActivityTranslucenceBinding, FileAssociationViewModel>() {

    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        intent.data?.let { uri ->
            it.uri?.let { treeUri ->
                AppConfig.defaultBookTreeUri = treeUri.toString()
                importBook(treeUri, uri)
            } ?: let {
                val storageHelp = String(assets.open("storageHelp.md").readBytes())
                toastOnUi(storageHelp)
                viewModel.importBook(uri)
            }
        }
    }

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override val viewModel by viewModels<FileAssociationViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.rotateLoading.show()
        viewModel.importBookLiveData.observe(this) { uri ->
            importBook(uri)
        }
        viewModel.onLineImportLive.observe(this) {
            startActivity<OnLineImportActivity> {
                data = it
            }
            finish()
        }
        viewModel.successLive.observe(this) {
            when (it.first) {
                "bookSource" -> showDialogFragment(
                    ImportBookSourceDialog(it.second, true)
                )
                "rssSource" -> showDialogFragment(
                    ImportRssSourceDialog(it.second, true)
                )
                "replaceRule" -> showDialogFragment(
                    ImportReplaceRuleDialog(it.second, true)
                )
                "httpTts" -> showDialogFragment(
                    ImportHttpTtsDialog(it.second, true)
                )
                "theme" -> showDialogFragment(
                    ImportThemeDialog(it.second, true)
                )
                "txtRule" -> showDialogFragment(
                    ImportTxtTocRuleDialog(it.second, true)
                )
            }
        }
        viewModel.errorLive.observe(this) {
            binding.rotateLoading.hide()
            toastOnUi(it)
            finish()
        }
        viewModel.openBookLiveData.observe(this) {
            binding.rotateLoading.hide()
            startActivity<ReadBookActivity> {
                putExtra("bookUrl", it)
            }
            finish()
        }
        viewModel.notSupportedLiveData.observe(this) { data ->
            binding.rotateLoading.hide()
            alert(
                title = appCtx.getString(R.string.draw),
                message = appCtx.getString(R.string.file_not_supported, data.second)
            ) {
                yesButton {
                    importBook(data.first)
                }
                noButton {
                    finish()
                }
            }
        }
        intent.data?.let { data ->
            if (data.isContentScheme()) {
                viewModel.dispatchIndent(data)
            } else if (!AppConst.isPlayChannel || Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                PermissionsCompat.Builder(this)
                    .addPermissions(*Permissions.Group.STORAGE)
                    .rationale(R.string.tip_perm_request_storage)
                    .onGranted {
                        viewModel.dispatchIndent(data)
                    }.onDenied {
                        toastOnUi("请求存储权限失败。")
                    }.request()
            } else {
                toastOnUi("由于安卓系统限制，请使用系统文件管理重新打开。")
            }
        }
    }

    private fun importBook(uri: Uri) {
        if (uri.isContentScheme()) {
            val treeUriStr = AppConfig.defaultBookTreeUri
            if (treeUriStr.isNullOrEmpty()) {
                localBookTreeSelect.launch {
                    title = getString(R.string.select_book_folder)
                    mode = HandleFileContract.DIR_SYS
                }
            } else {
                importBook(Uri.parse(treeUriStr), uri)
            }
        } else {
            viewModel.importBook(uri)
        }
    }

    private fun importBook(treeUri: Uri, uri: Uri) {
        launch {
            runCatching {
                withContext(IO) {
                    if (treeUri.isContentScheme()) {
                        val treeDoc =
                            DocumentFile.fromTreeUri(this@FileAssociationActivity, treeUri)
                        readUri(uri) { fileDoc, inputStream ->
                            val name = fileDoc.name
                            var doc = treeDoc!!.findFile(name)
                            if (doc == null || fileDoc.lastModified > doc.lastModified()) {
                                if (doc == null) {
                                    doc = treeDoc.createFile(FileUtils.getMimeType(name), name)
                                        ?: throw SecurityException("请重新设置书籍保存位置\nPermission Denial")
                                }
                                contentResolver.openOutputStream(doc.uri)!!.use { oStream ->
                                    inputStream.copyTo(oStream)
                                    oStream.flush()
                                }
                            }
                            viewModel.importBook(doc.uri)
                        }
                    } else {
                        val treeFile = File(treeUri.path ?: treeUri.toString())
                        readUri(uri) { fileDoc, inputStream ->
                            val name = fileDoc.name
                            val file = treeFile.getFile(name)
                            if (!file.exists() || fileDoc.lastModified > file.lastModified()) {
                                FileOutputStream(file).use { oStream ->
                                    inputStream.copyTo(oStream)
                                    oStream.flush()
                                }
                            }
                            viewModel.importBook(Uri.fromFile(file))
                        }
                    }
                }
            }.onFailure {
                when (it) {
                    is SecurityException -> localBookTreeSelect.launch {
                        title = getString(R.string.select_book_folder)
                        mode = HandleFileContract.DIR_SYS
                    }
                    else -> {
                        AppLog.put("导入书籍失败", it)
                        toastOnUi(it.localizedMessage)
                        finish()
                    }
                }
            }
        }
    }

}
