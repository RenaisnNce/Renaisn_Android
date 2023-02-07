package com.renaisn.reader.ui.book.changesource

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import com.renaisn.reader.R
import com.renaisn.reader.base.adapter.DiffRecyclerAdapter
import com.renaisn.reader.base.adapter.ItemViewHolder
import com.renaisn.reader.data.entities.SearchBook
import com.renaisn.reader.databinding.ItemChangeSourceBinding
import com.renaisn.reader.utils.getCompatColor
import com.renaisn.reader.utils.gone
import com.renaisn.reader.utils.invisible
import com.renaisn.reader.utils.visible
import splitties.init.appCtx
import splitties.views.onLongClick


class ChangeBookSourceAdapter(
    context: Context,
    val viewModel: ChangeBookSourceViewModel,
    val callBack: CallBack
) : DiffRecyclerAdapter<SearchBook, ItemChangeSourceBinding>(context) {

    override val diffItemCallback = object : DiffUtil.ItemCallback<SearchBook>() {
        override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
            return oldItem.bookUrl == newItem.bookUrl
        }

        override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
            return oldItem.originName == newItem.originName
                    && oldItem.getDisplayLastChapterTitle() == newItem.getDisplayLastChapterTitle()
        }

    }

    override fun getViewBinding(parent: ViewGroup): ItemChangeSourceBinding {
        return ItemChangeSourceBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemChangeSourceBinding,
        item: SearchBook,
        payloads: MutableList<Any>
    ) {
        val bundle = payloads.getOrNull(0) as? Bundle
        binding.apply {
            if (bundle == null) {
                tvOrigin.text = item.originName
                tvAuthor.text = item.author
                tvLast.text = item.getDisplayLastChapterTitle()
                if (callBack.bookUrl == item.bookUrl) {
                    ivChecked.visible()
                } else {
                    ivChecked.invisible()
                }
            } else {
                bundle.keySet().map {
                    when (it) {
                        "name" -> tvOrigin.text = item.originName
                        "latest" -> tvLast.text = item.getDisplayLastChapterTitle()
                        "upCurSource" -> if (callBack.bookUrl == item.bookUrl) {
                            ivChecked.visible()
                        } else {
                            ivChecked.invisible()
                        }
                    }
                }
            }
            val score = callBack.getBookScore(item)
            if (score > 0) {
                binding.ivBad.gone()
                binding.ivGood.visible()
                DrawableCompat.setTint(binding.ivGood.drawable, appCtx.getCompatColor(R.color.md_red_A200))
                DrawableCompat.setTint(binding.ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_100))
            } else if (score < 0) {
                binding.ivGood.gone()
                binding.ivBad.visible()
                DrawableCompat.setTint(binding.ivGood.drawable, appCtx.getCompatColor(R.color.md_red_100))
                DrawableCompat.setTint(binding.ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_A200))
            } else {
                binding.ivGood.visible()
                binding.ivBad.visible()
                DrawableCompat.setTint(binding.ivGood.drawable, appCtx.getCompatColor(R.color.md_red_100))
                DrawableCompat.setTint(binding.ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_100))
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemChangeSourceBinding) {
        binding.ivGood.setOnClickListener {
            if (binding.ivBad.isVisible) {
                DrawableCompat.setTint(binding.ivGood.drawable, appCtx.getCompatColor(R.color.md_red_A200))
                binding.ivBad.gone()
                getItem(holder.layoutPosition)?.let {
                    callBack.setBookScore(it, 1)
                }
            } else {
                DrawableCompat.setTint(binding.ivGood.drawable, appCtx.getCompatColor(R.color.md_red_100))
                binding.ivBad.visible()
                getItem(holder.layoutPosition)?.let {
                    callBack.setBookScore(it, 0)
                }
            }
        }
        binding.ivBad.setOnClickListener {
            if (binding.ivGood.isVisible) {
                DrawableCompat.setTint(binding.ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_A200))
                binding.ivGood.gone()
                getItem(holder.layoutPosition)?.let {
                    callBack.setBookScore(it, -1)
                }
            } else {
                DrawableCompat.setTint(binding.ivBad.drawable, appCtx.getCompatColor(R.color.md_blue_100))
                binding.ivGood.visible()
                getItem(holder.layoutPosition)?.let {
                    callBack.setBookScore(it, 0)
                }
            }
        }
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                if (it.bookUrl != callBack.bookUrl) {
                    callBack.changeTo(it)
                }
            }
        }
        holder.itemView.onLongClick {
            showMenu(holder.itemView, getItem(holder.layoutPosition))
        }
    }

    private fun showMenu(view: View, searchBook: SearchBook?) {
        searchBook ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.change_source_item)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_top_source -> {
                    callBack.topSource(searchBook)
                }
                R.id.menu_bottom_source -> {
                    callBack.bottomSource(searchBook)
                }
                R.id.menu_edit_source -> {
                    callBack.editSource(searchBook)
                }
                R.id.menu_disable_source -> {
                    callBack.disableSource(searchBook)
                }
                R.id.menu_delete_source -> {
                    callBack.deleteSource(searchBook)
                    updateItems(0, itemCount, listOf<Int>())
                }
            }
            true
        }
        popupMenu.show()
    }

    interface CallBack {
        val bookUrl: String?
        fun changeTo(searchBook: SearchBook)
        fun topSource(searchBook: SearchBook)
        fun bottomSource(searchBook: SearchBook)
        fun editSource(searchBook: SearchBook)
        fun disableSource(searchBook: SearchBook)
        fun deleteSource(searchBook: SearchBook)
        fun setBookScore(searchBook: SearchBook, score: Int)
        fun getBookScore(searchBook: SearchBook): Int
    }
}