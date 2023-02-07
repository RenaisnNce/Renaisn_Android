package com.renaisn.reader.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class SearchRule(
    var checkKeyWord: String? = null,               // 校验关键字
    override var bookList: String? = null,
    override var name: String? = null,
    override var author: String? = null,
    override var intro: String? = null,
    override var kind: String? = null,
    override var lastChapter: String? = null,
    override var updateTime: String? = null,
    override var bookUrl: String? = null,
    override var coverUrl: String? = null,
    override var wordCount: String? = null
) : BookListRule, Parcelable