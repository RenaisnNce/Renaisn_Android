package com.renaisn.reader.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.StaticLayout
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import com.renaisn.reader.utils.dpToPx

class BatteryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    private val batteryTypeface by lazy {
        Typeface.createFromAsset(context.assets, "font/number.ttf")
    }
    private val batteryPaint = Paint()
    private val outFrame = Rect()
    private val polar = Rect()
    var isBattery = false
        set(value) {
            field = value
            if (value && !isInEditMode) {
                super.setTypeface(batteryTypeface)
                postInvalidate()
            }
        }
    private var battery: Int = 0

    init {
        setPadding(4.dpToPx(), 3.dpToPx(), 6.dpToPx(), 3.dpToPx())
        batteryPaint.strokeWidth = 1f.dpToPx()
        batteryPaint.isAntiAlias = true
        batteryPaint.color = paint.color
    }

    override fun setTypeface(tf: Typeface?) {
        if (!isBattery) {
            super.setTypeface(tf)
        }
    }

    fun setColor(@ColorInt color: Int) {
        setTextColor(color)
        batteryPaint.color = color
        invalidate()
    }

    @SuppressLint("SetTextI18n")
    fun setBattery(battery: Int, text: String? = null) {
        this.battery = battery
        if (text.isNullOrEmpty()) {
            setText(battery.toString())
        } else {
            setText("$text  $battery")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isBattery) return
        layout.getLineBounds(0, outFrame)
        val batteryStart = layout
            .getPrimaryHorizontal(text.length - battery.toString().length)
            .toInt() + 2.dpToPx()
        val batteryEnd = batteryStart +
                StaticLayout.getDesiredWidth(battery.toString(), paint).toInt() + 4.dpToPx()
        outFrame.set(
            batteryStart,
            2.dpToPx(),
            batteryEnd,
            height - 2.dpToPx()
        )
        val dj = (outFrame.bottom - outFrame.top) / 3
        polar.set(
            batteryEnd,
            outFrame.top + dj,
            batteryEnd + 2.dpToPx(),
            outFrame.bottom - dj
        )
        batteryPaint.style = Paint.Style.STROKE
        canvas.drawRect(outFrame, batteryPaint)
        batteryPaint.style = Paint.Style.FILL
        canvas.drawRect(polar, batteryPaint)
    }

}