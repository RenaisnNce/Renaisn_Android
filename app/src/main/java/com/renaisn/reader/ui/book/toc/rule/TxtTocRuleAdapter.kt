package com.renaisn.reader.ui.book.toc.rule

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.renaisn.reader.R
import com.renaisn.reader.base.adapter.ItemViewHolder
import com.renaisn.reader.base.adapter.RecyclerAdapter
import com.renaisn.reader.data.entities.TxtTocRule
import com.renaisn.reader.databinding.ItemTxtTocRuleBinding
import com.renaisn.reader.lib.theme.backgroundColor
import com.renaisn.reader.ui.widget.recycler.DragSelectTouchHelper
import com.renaisn.reader.ui.widget.recycler.ItemTouchCallback
import com.renaisn.reader.utils.ColorUtils

class TxtTocRuleAdapter(context: Context, private val callBack: CallBack) :
    RecyclerAdapter<TxtTocRule, ItemTxtTocRuleBinding>(context),
    ItemTouchCallback.Callback {

    private val selected = linkedSetOf<TxtTocRule>()

    val selection: List<TxtTocRule>
        get() = getItems().filter {
            selected.contains(it)
        }

    override fun getViewBinding(parent: ViewGroup): ItemTxtTocRuleBinding {
        return ItemTxtTocRuleBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemTxtTocRuleBinding,
        item: TxtTocRule,
        payloads: MutableList<Any>
    ) {
        binding.run {
            val bundle = payloads.getOrNull(0) as? Bundle
            if (bundle == null) {
                root.setBackgroundColor(ColorUtils.withAlpha(context.backgroundColor, 0.5f))
                cbSource.text = item.name
                swtEnabled.isChecked = item.enable
                cbSource.isChecked = selected.contains(item)
                titleExample.text = item.example
            } else {
                bundle.keySet().map {
                    when (it) {
                        "selected" -> cbSource.isChecked = selected.contains(item)
                    }
                }
            }
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemTxtTocRuleBinding) {
        binding.cbSource.setOnCheckedChangeListener { buttonView, isChecked ->
            getItem(holder.layoutPosition)?.let {
                if (buttonView.isPressed) {
                    if (isChecked) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                    callBack.upCountView()
                }
            }
        }
        binding.swtEnabled.setOnCheckedChangeListener { buttonView, isChecked ->
            getItem(holder.layoutPosition)?.let {
                if (buttonView.isPressed) {
                    it.enable = isChecked
                    callBack.update(it)
                }
            }
        }
        binding.ivEdit.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.edit(it)
            }
        }
        binding.ivMenuMore.setOnClickListener {
            showMenu(it, holder.layoutPosition)
        }
    }

    private fun showMenu(view: View, position: Int) {
        val source = getItem(position) ?: return
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.txt_toc_rule_item)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_top -> callBack.toTop(source)
                R.id.menu_bottom -> callBack.toBottom(source)
                R.id.menu_del -> callBack.del(source)
            }
            true
        }
        popupMenu.show()
    }

    fun selectAll() {
        getItems().forEach {
            selected.add(it)
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    fun revertSelection() {
        getItems().forEach {
            if (selected.contains(it)) {
                selected.remove(it)
            } else {
                selected.add(it)
            }
        }
        notifyItemRangeChanged(0, itemCount, bundleOf(Pair("selected", null)))
        callBack.upCountView()
    }

    override fun swap(srcPosition: Int, targetPosition: Int): Boolean {
        val srcItem = getItem(srcPosition)
        val targetItem = getItem(targetPosition)
        if (srcItem != null && targetItem != null) {
            if (srcItem.serialNumber == targetItem.serialNumber) {
                callBack.upOrder()
            } else {
                val srcOrder = srcItem.serialNumber
                srcItem.serialNumber = targetItem.serialNumber
                targetItem.serialNumber = srcOrder
                movedItems.add(srcItem)
                movedItems.add(targetItem)
            }
        }
        swapItem(srcPosition, targetPosition)
        return true
    }

    private val movedItems = hashSetOf<TxtTocRule>()

    override fun onClearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        if (movedItems.isNotEmpty()) {
            callBack.update(*movedItems.toTypedArray())
            movedItems.clear()
        }
    }

    val dragSelectCallback: DragSelectTouchHelper.Callback =
        object : DragSelectTouchHelper.AdvanceCallback<TxtTocRule>(Mode.ToggleAndReverse) {
            override fun currentSelectedId(): MutableSet<TxtTocRule> {
                return selected
            }

            override fun getItemId(position: Int): TxtTocRule {
                return getItem(position)!!
            }

            override fun updateSelectState(position: Int, isSelected: Boolean): Boolean {
                getItem(position)?.let {
                    if (isSelected) {
                        selected.add(it)
                    } else {
                        selected.remove(it)
                    }
                    notifyItemChanged(position, bundleOf(Pair("selected", null)))
                    callBack.upCountView()
                    return true
                }
                return false
            }
        }

    interface CallBack {
        fun del(source: TxtTocRule)
        fun edit(source: TxtTocRule)
        fun update(vararg source: TxtTocRule)
        fun toTop(source: TxtTocRule)
        fun toBottom(source: TxtTocRule)
        fun upOrder()
        fun upCountView()
    }

}