package com.renaisn.reader.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import com.renaisn.reader.lib.theme.accentColor
import com.renaisn.reader.utils.applyTint

class ThemeCheckBox(context: Context, attrs: AttributeSet) : AppCompatCheckBox(context, attrs) {
    init {
        if (!isInEditMode) {
            applyTint(context.accentColor)
        }
    }
}
