package com.renaisn.reader.ui.association

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.renaisn.reader.R
import com.renaisn.reader.base.BaseDialogFragment
import com.renaisn.reader.base.adapter.ItemViewHolder
import com.renaisn.reader.base.adapter.RecyclerAdapter
import com.renaisn.reader.databinding.DialogRecyclerViewBinding
import com.renaisn.reader.databinding.ItemBookFileImportBinding
import com.renaisn.reader.lib.dialogs.alert
import com.renaisn.reader.lib.theme.primaryColor
import com.renaisn.reader.ui.widget.dialog.WaitDialog
import com.renaisn.reader.utils.openFileUri
import com.renaisn.reader.utils.setLayout
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding
import com.renaisn.reader.utils.visible


/**
 * 导入在线书籍文件弹出窗口
 */
class ImportOnLineBookFileDialog : BaseDialogFragment(R.layout.dialog_recycler_view) {


    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<ImportOnLineBookFileViewModel>()
    private val adapter by lazy { BookFileAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val bookUrl = arguments?.getString("bookUrl")
        viewModel.initData(bookUrl)
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.download_and_import_file)
        binding.rotateLoading.show()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        viewModel.errorLiveData.observe(this) {
            binding.rotateLoading.hide()
            binding.tvMsg.apply {
                text = it
                visible()
            }
        }
        viewModel.successLiveData.observe(this) {
            binding.rotateLoading.hide()
            if (it > 0) {
                adapter.setItems(viewModel.allBookFiles)
            }
        }
        viewModel.savedFileUriData.observe(this) {
            requireContext().openFileUri(it, "*/*")
        }
    }

    private fun importFileAndUpdate(url: String, fileName: String) {
        val waitDialog = WaitDialog(requireContext())
        waitDialog.show()
        viewModel.importOnLineBookFile(url, fileName) {
           waitDialog.dismiss()
           dismissAllowingStateLoss()
        }
    }

    private fun downloadFile(url: String, fileName: String) {
        val waitDialog = WaitDialog(requireContext())
        waitDialog.show()
        viewModel.downloadUrl(url, fileName) {
            waitDialog.dismiss()
            dismissAllowingStateLoss()
    }
}

    inner class BookFileAdapter(context: Context) :
        RecyclerAdapter<Triple<String, String, Boolean>
, ItemBookFileImportBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemBookFileImportBinding {
            return ItemBookFileImportBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemBookFileImportBinding,
            item: Triple<String, String, Boolean>,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbFileName.text = item.second
            }
        }

        override fun registerListener(
            holder: ItemViewHolder,
            binding: ItemBookFileImportBinding
        ) {
            binding.apply {
                cbFileName.setOnClickListener {
                    val selectFile = viewModel.allBookFiles[holder.layoutPosition]
                    if (selectFile.third) {
                        importFileAndUpdate(selectFile.first, selectFile.second)
                    } else {
                        alert(
                            title = getString(R.string.draw),
                            message = getString(R.string.file_not_supported, selectFile.second)
                        ) {
                            yesButton {
                                importFileAndUpdate(selectFile.first, selectFile.second)
                            }
                            neutralButton(R.string.open_fun) {
                                downloadFile(selectFile.first, selectFile.second)
                            }
                            noButton()
                        }
                    }
                }
            }
        }

    }

}