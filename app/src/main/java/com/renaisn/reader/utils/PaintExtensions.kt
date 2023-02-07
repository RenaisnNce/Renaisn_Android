package com.renaisn.reader.utils

import android.text.TextPaint

val TextPaint.textHeight: Float
    get() = fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading