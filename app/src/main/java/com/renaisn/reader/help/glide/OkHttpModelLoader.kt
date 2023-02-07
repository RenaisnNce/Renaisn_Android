package com.renaisn.reader.help.glide

import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.renaisn.reader.model.analyzeRule.AnalyzeUrl
import com.renaisn.reader.utils.isAbsUrl

import java.io.InputStream

object OkHttpModelLoader : ModelLoader<GlideUrl?, InputStream?> {

    val loadOnlyWifiOption = Option.memory("loadOnlyWifi", false)
    val sourceOriginOption = Option.memory<String>("sourceOrigin")

    override fun buildLoadData(
        model: GlideUrl,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream?> {
        val cacheKey = model.toString()
        var modelWithHeader = model
        if (cacheKey.isAbsUrl()) {
            modelWithHeader = AnalyzeUrl(cacheKey).getGlideUrl()
        }
        return ModelLoader.LoadData(modelWithHeader, OkHttpStreamFetcher(modelWithHeader, options))
    }

    override fun handles(model: GlideUrl): Boolean {
        return true
    }

}