package com.renaisn.reader.ui.book.changecover

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.renaisn.reader.base.adapter.DiffRecyclerAdapter
import com.renaisn.reader.base.adapter.ItemViewHolder
import com.renaisn.reader.data.entities.SearchBook
import com.renaisn.reader.databinding.ItemCoverBinding


class CoverAdapter(context: Context, val callBack: CallBack) :
    DiffRecyclerAdapter<SearchBook, ItemCoverBinding>(context) {

    override val diffItemCallback: DiffUtil.ItemCallback<SearchBook>
        get() = object : DiffUtil.ItemCallback<SearchBook>() {
            override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

            override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem.originName == newItem.originName
                        && oldItem.coverUrl == newItem.coverUrl
            }

        }

    override fun getViewBinding(parent: ViewGroup): ItemCoverBinding {
        return ItemCoverBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemCoverBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) = binding.run {
        ivCover.load(item.coverUrl, item.name, item.author, false, item.origin)
        tvSource.text = item.originName
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemCoverBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.changeTo(it.coverUrl ?: "")
                }
            }
        }
    }

    interface CallBack {
        fun changeTo(coverUrl: String)
    }
}