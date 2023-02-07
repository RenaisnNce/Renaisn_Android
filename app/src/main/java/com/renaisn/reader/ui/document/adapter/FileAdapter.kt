package com.renaisn.reader.ui.document.adapter


import android.content.Context
import android.view.ViewGroup
import com.renaisn.reader.base.adapter.ItemViewHolder
import com.renaisn.reader.base.adapter.RecyclerAdapter
import com.renaisn.reader.databinding.ItemFileFilepickerBinding
import com.renaisn.reader.help.config.AppConfig
import com.renaisn.reader.lib.theme.getPrimaryDisabledTextColor
import com.renaisn.reader.lib.theme.getPrimaryTextColor
import com.renaisn.reader.ui.document.entity.FileItem
import com.renaisn.reader.utils.ConvertUtils
import com.renaisn.reader.utils.FileUtils
import java.io.File


class FileAdapter(context: Context, val callBack: CallBack) :
    RecyclerAdapter<FileItem, ItemFileFilepickerBinding>(context) {
    private var rootPath: String? = null
    var currentPath: String? = null
        private set
    private val homeIcon = ConvertUtils.toDrawable(com.renaisn.reader.ui.document.utils.FilePickerIcon.getHome())
    private val upIcon = ConvertUtils.toDrawable(com.renaisn.reader.ui.document.utils.FilePickerIcon.getUpDir())
    private val folderIcon = ConvertUtils.toDrawable(com.renaisn.reader.ui.document.utils.FilePickerIcon.getFolder())
    private val fileIcon = ConvertUtils.toDrawable(com.renaisn.reader.ui.document.utils.FilePickerIcon.getFile())
    private val primaryTextColor = context.getPrimaryTextColor(!AppConfig.isNightTheme)
    private val disabledTextColor = context.getPrimaryDisabledTextColor(!AppConfig.isNightTheme)

    fun loadData(path: String?) {
        if (path == null) {
            return
        }
        val data = ArrayList<FileItem>()
        if (rootPath == null) {
            rootPath = path
        }
        currentPath = path
        if (callBack.isShowHomeDir) {
            //添加“返回主目录”
            val fileRoot = FileItem()
            fileRoot.isDirectory = true
            fileRoot.icon = homeIcon
            fileRoot.name = DIR_ROOT
            fileRoot.size = 0
            fileRoot.path = rootPath ?: path
            data.add(fileRoot)
        }
        if (callBack.isShowUpDir && path != PathAdapter.sdCardDirectory) {
            //添加“返回上一级目录”
            val fileParent = FileItem()
            fileParent.isDirectory = true
            fileParent.icon = upIcon
            fileParent.name = DIR_PARENT
            fileParent.size = 0
            fileParent.path = File(path).parent ?: ""
            data.add(fileParent)
        }
        currentPath?.let { currentPath ->
            val files: Array<File>? = FileUtils.listDirsAndFiles(currentPath)
            if (files != null) {
                for (file in files) {
                    if (!callBack.isShowHideDir && file.name.startsWith(".")) {
                        continue
                    }
                    val fileItem = FileItem()
                    val isDirectory = file.isDirectory
                    fileItem.isDirectory = isDirectory
                    if (isDirectory) {
                        fileItem.icon = folderIcon
                        fileItem.size = 0
                    } else {
                        fileItem.icon = fileIcon
                        fileItem.size = file.length()
                    }
                    fileItem.name = file.name
                    fileItem.path = file.absolutePath
                    data.add(fileItem)
                }
            }
            setItems(data)
        }

    }

    override fun getViewBinding(parent: ViewGroup): ItemFileFilepickerBinding {
        return ItemFileFilepickerBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFileFilepickerBinding,
        item: FileItem,
        payloads: MutableList<Any>
    ) {
        binding.apply {
            imageView.setImageDrawable(item.icon)
            textView.text = item.name
            if (item.isDirectory) {
                textView.setTextColor(primaryTextColor)
            } else {
                if (callBack.isSelectDir) {
                    textView.setTextColor(disabledTextColor)
                } else {
                    callBack.allowExtensions?.let {
                        if (it.isEmpty() || it.contains(FileUtils.getExtension(item.path))) {
                            textView.setTextColor(primaryTextColor)
                        } else {
                            textView.setTextColor(disabledTextColor)
                        }
                    } ?: textView.setTextColor(primaryTextColor)
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFileFilepickerBinding) {
        holder.itemView.setOnClickListener {
            callBack.onFileClick(holder.layoutPosition)
        }
    }

    interface CallBack {
        fun onFileClick(position: Int)

        //允许的扩展名
        var allowExtensions: Array<String>?

        /**
         * 是否选取目录
         */
        val isSelectDir: Boolean

        /**
         * 是否显示返回主目录
         */
        var isShowHomeDir: Boolean

        /**
         * 是否显示返回上一级
         */
        var isShowUpDir: Boolean

        /**
         * 是否显示隐藏的目录（以“.”开头）
         */
        var isShowHideDir: Boolean
    }

    companion object {
        const val DIR_ROOT = "."
        const val DIR_PARENT = ".."
    }

}

