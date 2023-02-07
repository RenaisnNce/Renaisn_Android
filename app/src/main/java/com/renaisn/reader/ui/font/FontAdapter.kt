package com.renaisn.reader.ui.font

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.view.ViewGroup
import com.renaisn.reader.base.adapter.ItemViewHolder
import com.renaisn.reader.base.adapter.RecyclerAdapter
import com.renaisn.reader.databinding.ItemFontBinding
import com.renaisn.reader.utils.*
import java.io.File
import java.net.URLDecoder

class FontAdapter(context: Context, curFilePath: String, val callBack: CallBack) :
    RecyclerAdapter<FileDoc, ItemFontBinding>(context) {

    private val curName = kotlin.runCatching {
        URLDecoder.decode(curFilePath, "utf-8")
    }.getOrNull()?.substringAfterLast(File.separator)

    override fun getViewBinding(parent: ViewGroup): ItemFontBinding {
        return ItemFontBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemFontBinding,
        item: FileDoc,
        payloads: MutableList<Any>
    ) {
        binding.run {
            kotlin.runCatching {
                val typeface: Typeface? = if (item.isContentScheme) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.contentResolver
                            .openFileDescriptor(item.uri, "r")?.use {
                                Typeface.Builder(it.fileDescriptor).build()
                            }
                    } else {
                        Typeface.createFromFile(RealPathUtil.getPath(context, item.uri))
                    }
                } else {
                    Typeface.createFromFile(item.uri.path!!)
                }
                tvFont.typeface = typeface
            }.onFailure {
                it.printOnDebug()
                context.toastOnUi("Read ${item.name} Error: ${it.localizedMessage}")
            }
            tvFont.text = item.name
            root.setOnClickListener { callBack.onFontSelect(item) }
            if (item.name == curName) {
                ivChecked.visible()
            } else {
                ivChecked.invisible()
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemFontBinding) {
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.onFontSelect(it)
            }
        }
    }

    interface CallBack {
        fun onFontSelect(docItem: FileDoc)
    }
}