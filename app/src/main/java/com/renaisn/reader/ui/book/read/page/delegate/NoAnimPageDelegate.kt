package com.renaisn.reader.ui.book.read.page.delegate

import android.graphics.Canvas
import com.renaisn.reader.ui.book.read.page.ReadView

class NoAnimPageDelegate(readView: ReadView) : HorizontalPageDelegate(readView) {

    override fun onAnimStart(animationSpeed: Int) {
        if (!isCancel) {
            readView.fillPage(mDirection)
        }
        stopScroll()
    }

    override fun onDraw(canvas: Canvas) {
        // nothing
    }

    override fun onAnimStop() {
        // nothing
    }


}