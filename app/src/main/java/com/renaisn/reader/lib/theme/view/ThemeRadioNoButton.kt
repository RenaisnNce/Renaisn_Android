package com.renaisn.reader.lib.theme.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton
import com.renaisn.reader.R
import com.renaisn.reader.lib.theme.Selector
import com.renaisn.reader.lib.theme.accentColor
import com.renaisn.reader.lib.theme.bottomBackground
import com.renaisn.reader.lib.theme.getPrimaryTextColor
import com.renaisn.reader.utils.ColorUtils
import com.renaisn.reader.utils.dpToPx
import com.renaisn.reader.utils.getCompatColor

class ThemeRadioNoButton(context: Context, attrs: AttributeSet) :
    AppCompatRadioButton(context, attrs) {

    private val isBottomBackground: Boolean

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ThemeRadioNoButton)
        isBottomBackground =
            typedArray.getBoolean(R.styleable.ThemeRadioNoButton_isBottomBackground, false)
        typedArray.recycle()
        initTheme()
    }

    private fun initTheme() {
        when {
            isInEditMode -> Unit
            isBottomBackground -> {
                val accentColor = context.accentColor
                val isLight = ColorUtils.isColorLight(context.bottomBackground)
                val textColor = context.getPrimaryTextColor(isLight)
                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dpToPx())
                    .setStrokeWidth(2.dpToPx())
                    .setCheckedBgColor(accentColor)
                    .setCheckedStrokeColor(accentColor)
                    .setDefaultStrokeColor(textColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(textColor)
                        .setCheckedColor(checkedTextColor)
                        .create()
                )
            }
            else -> {
                val accentColor = context.accentColor
                val defaultTextColor = context.getCompatColor(R.color.primaryText)
                val checkedTextColor = if (ColorUtils.isColorLight(accentColor)) {
                    Color.BLACK
                } else {
                    Color.WHITE
                }
                background = Selector.shapeBuild()
                    .setCornerRadius(2.dpToPx())
                    .setStrokeWidth(2.dpToPx())
                    .setCheckedBgColor(accentColor)
                    .setCheckedStrokeColor(accentColor)
                    .setDefaultStrokeColor(defaultTextColor)
                    .create()
                setTextColor(
                    Selector.colorBuild()
                        .setDefaultColor(defaultTextColor)
                        .setCheckedColor(checkedTextColor)
                        .create()
                )
            }
        }

    }

}
