package com.renaisn.reader.help

import androidx.annotation.Keep
import com.renaisn.reader.constant.AppConst
import com.renaisn.reader.exception.NoStackTraceException
import com.renaisn.reader.help.coroutine.Coroutine
import com.renaisn.reader.help.http.newCallStrResponse
import com.renaisn.reader.help.http.okHttpClient
import com.renaisn.reader.utils.jsonPath
import com.renaisn.reader.utils.readBool
import com.renaisn.reader.utils.readString
import kotlinx.coroutines.CoroutineScope

@Keep
@Suppress("unused")
object AppUpdateGitHub : AppUpdate.AppUpdateInterface {

    override fun check(
        scope: CoroutineScope,
    ): Coroutine<AppUpdate.UpdateInfo> {
        return Coroutine.async(scope) {
            val lastReleaseUrl =
                "https://renaisn.oss-cn-hangzhou.aliyuncs.com/update/update.json?OSSAccessKeyId=LTAI5tKEZCuPrBCQgycqHKbS&Expires=1775309328&Signature=ze1XejCBquOc6T676gal69%2FnXas%3D"
            val body = okHttpClient.newCallStrResponse {
                url(lastReleaseUrl)
            }.body
            if (body.isNullOrBlank()) {
                throw NoStackTraceException("获取新版本出错")
            }
            val rootDoc = jsonPath.parse(body)
            val tagName = rootDoc.readString("$.versionName")
                ?: throw NoStackTraceException("获取新版本出错")
            if (tagName > AppConst.appInfo.versionName) {
                val updateBody = rootDoc.readString("$.body")
                    ?: throw NoStackTraceException("获取新版本出错")
                val downloadUrl = rootDoc.readString("$.downloadUrl")
                    ?: throw NoStackTraceException("获取新版本出错")
                val fileName = rootDoc.readString("$.fileName")
                    ?: throw NoStackTraceException("获取新版本出错")
                var isForce =
                    rootDoc.readBool("$.isForce") ?: throw NoStackTraceException("获取新版本出错")
                return@async AppUpdate.UpdateInfo(tagName, updateBody, downloadUrl, fileName,isForce)
            } else {
                throw NoStackTraceException("已是最新版本")
            }
        }.timeout(10000)
    }


}