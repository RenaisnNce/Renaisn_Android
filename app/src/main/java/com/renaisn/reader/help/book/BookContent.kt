package com.renaisn.reader.help.book

data class BookContent(
    val sameTitleRemoved: Boolean,
    val textList: List<String>
) {

    override fun toString(): String {
        return textList.joinToString("\n")
    }

}
