package com.renaisn.reader.ui.book.read.config

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.renaisn.reader.R
import com.renaisn.reader.base.BaseDialogFragment
import com.renaisn.reader.constant.EventBus
import com.renaisn.reader.databinding.DialogReadPaddingBinding
import com.renaisn.reader.help.config.ReadBookConfig
import com.renaisn.reader.utils.postEvent
import com.renaisn.reader.utils.setLayout
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding

class PaddingConfigDialog : BaseDialogFragment(R.layout.dialog_read_padding) {

    private val binding by viewBinding(DialogReadPaddingBinding::bind)

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val attr = it.attributes
            attr.dimAmount = 0.0f
            it.attributes = attr
        }
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initData()
        initView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        ReadBookConfig.save()
    }

    private fun initData() = binding.run {
        //正文
        dsbPaddingTop.progress = ReadBookConfig.paddingTop
        dsbPaddingBottom.progress = ReadBookConfig.paddingBottom
        dsbPaddingLeft.progress = ReadBookConfig.paddingLeft
        dsbPaddingRight.progress = ReadBookConfig.paddingRight
        //页眉
        dsbHeaderPaddingTop.progress = ReadBookConfig.headerPaddingTop
        dsbHeaderPaddingBottom.progress = ReadBookConfig.headerPaddingBottom
        dsbHeaderPaddingLeft.progress = ReadBookConfig.headerPaddingLeft
        dsbHeaderPaddingRight.progress = ReadBookConfig.headerPaddingRight
        //页脚
        dsbFooterPaddingTop.progress = ReadBookConfig.footerPaddingTop
        dsbFooterPaddingBottom.progress = ReadBookConfig.footerPaddingBottom
        dsbFooterPaddingLeft.progress = ReadBookConfig.footerPaddingLeft
        dsbFooterPaddingRight.progress = ReadBookConfig.footerPaddingRight
        cbShowTopLine.isChecked = ReadBookConfig.showHeaderLine
        cbShowBottomLine.isChecked = ReadBookConfig.showFooterLine
    }

    private fun initView() = binding.run {
        //正文
        dsbPaddingTop.onChanged = {
            ReadBookConfig.paddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbPaddingBottom.onChanged = {
            ReadBookConfig.paddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbPaddingLeft.onChanged = {
            ReadBookConfig.paddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbPaddingRight.onChanged = {
            ReadBookConfig.paddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        //页眉
        dsbHeaderPaddingTop.onChanged = {
            ReadBookConfig.headerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbHeaderPaddingBottom.onChanged = {
            ReadBookConfig.headerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbHeaderPaddingLeft.onChanged = {
            ReadBookConfig.headerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbHeaderPaddingRight.onChanged = {
            ReadBookConfig.headerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        //页脚
        dsbFooterPaddingTop.onChanged = {
            ReadBookConfig.footerPaddingTop = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbFooterPaddingBottom.onChanged = {
            ReadBookConfig.footerPaddingBottom = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbFooterPaddingLeft.onChanged = {
            ReadBookConfig.footerPaddingLeft = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        dsbFooterPaddingRight.onChanged = {
            ReadBookConfig.footerPaddingRight = it
            postEvent(EventBus.UP_CONFIG, true)
        }
        cbShowTopLine.onCheckedChangeListener = { _, isChecked ->
            ReadBookConfig.showHeaderLine = isChecked
            postEvent(EventBus.UP_CONFIG, true)
        }
        cbShowBottomLine.onCheckedChangeListener = { _, isChecked ->
            ReadBookConfig.showFooterLine = isChecked
            postEvent(EventBus.UP_CONFIG, true)
        }
    }

}
