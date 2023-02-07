package com.renaisn.reader.ui.book.read.page.entities.column

import androidx.annotation.Keep

/**
 * 图片列
 */
@Keep
data class ImageColumn(
    override var start: Float,
    override var end: Float,
    var src: String
) : BaseColumn