package com.renaisn.reader.lib.cronet

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.text.TextUtils
import androidx.annotation.Keep
import com.renaisn.reader.BuildConfig
import com.renaisn.reader.constant.AppConst
import com.renaisn.reader.help.coroutine.Coroutine
import com.renaisn.reader.help.http.Cronet
import com.renaisn.reader.utils.DebugLog
import com.renaisn.reader.utils.printOnDebug

import org.chromium.net.CronetEngine
import org.json.JSONObject
import splitties.init.appCtx

import java.io.*
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.*

@Keep
object CronetLoader : CronetEngine.Builder.LibraryLoader(), Cronet.LoaderInterface {
    //https://storage.googleapis.com/chromium-cronet/android/92.0.4515.159/Release/cronet/libs/arm64-v8a/libcronet.92.0.4515.159.so

    private const val soVersion = com.renaisn.reader.BuildConfig.Cronet_Version
    private const val soName = "libcronet.$soVersion.so"
    private val soUrl: String
    private val soFile: File
    private val downloadFile: File
    private var cpuAbi: String? = null
    private var md5: String
    var download = false

    @Volatile
    private var cacheInstall = false

    init {
        soUrl = ("https://storage.googleapis.com/chromium-cronet/android/"
                + soVersion + "/Release/cronet/libs/"
                + getCpuAbi(appCtx) + "/" + soName)
        md5 = getMd5(appCtx)
        val dir = appCtx.getDir("cronet", Context.MODE_PRIVATE)
        soFile = File(dir.toString() + "/" + getCpuAbi(appCtx), soName)
        downloadFile = File(appCtx.cacheDir.toString() + "/so_download", soName)
        DebugLog.d(javaClass.simpleName, "soName+:$soName")
        DebugLog.d(javaClass.simpleName, "destSuccessFile:$soFile")
        DebugLog.d(javaClass.simpleName, "tempFile:$downloadFile")
        DebugLog.d(javaClass.simpleName, "soUrl:$soUrl")
    }

    /**
     * ??????Cronet??????????????????
     */
    override fun install(): Boolean {
        synchronized(this) {
            if (cacheInstall) {
                return true
            }
        }

        if (AppConst.isPlayChannel) {
            return false
        }
        if (md5.length != 32 || !soFile.exists() || md5 != getFileMD5(soFile)) {
            cacheInstall = false
            return cacheInstall
        }
        cacheInstall = soFile.exists()
        return cacheInstall
    }


    /**
     * ?????????Cronet
     */
    override fun preDownload() {
        if (AppConst.isPlayChannel) {
            return
        }
        Coroutine.async {
            //md5 = getUrlMd5(md5Url)
            if (soFile.exists() && md5 == getFileMD5(soFile)) {
                DebugLog.d(javaClass.simpleName, "So ????????????")
            } else {
                download(soUrl, md5, downloadFile, soFile)
            }
            DebugLog.d(javaClass.simpleName, soName)
        }
    }

    private fun getMd5(context: Context): String {
        val stringBuilder = StringBuilder()
        return try {
            //??????assets???????????????
            val assetManager = context.assets
            //????????????????????????????????????
            val bf = BufferedReader(
                InputStreamReader(
                    assetManager.open("cronet.json")
                )
            )
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            JSONObject(stringBuilder.toString()).optString(getCpuAbi(context), "")
        } catch (e: java.lang.Exception) {
            return ""
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    override fun loadLibrary(libName: String) {
        DebugLog.d(javaClass.simpleName, "libName:$libName")
        val start = System.currentTimeMillis()
        @Suppress("SameParameterValue")
        try {
            //???cronet???so????????????????????????
            if (!libName.contains("cronet")) {
                System.loadLibrary(libName)
                return
            }
            //???????????????cronet???????????????????????????????????????????????????
            //????????????????????????????????????
            System.loadLibrary(libName)
            DebugLog.d(javaClass.simpleName, "load from system")
        } catch (e: Throwable) {
            //????????????????????????????????????
            //??????????????????
            deleteHistoryFile(Objects.requireNonNull(soFile.parentFile), soFile)
            //md5 = getUrlMd5(md5Url)
            DebugLog.d(javaClass.simpleName, "soMD5:$md5")
            if (md5.length != 32 || soUrl.isEmpty()) {
                //??????md5????????????url??????????????????????????????????????????
                System.loadLibrary(libName)
                return
            }
            if (!soFile.exists() || !soFile.isFile) {
                soFile.delete()
                download(soUrl, md5, downloadFile, soFile)
                //????????????????????????????????????????????????????????????????????????
                System.loadLibrary(libName)
                return
            }
            if (soFile.exists()) {
                //??????????????????????????????md5???
                val fileMD5 = getFileMD5(soFile)
                if (fileMD5 != null && fileMD5.equals(md5, ignoreCase = true)) {
                    //md5?????????????????????
                    System.load(soFile.absolutePath)
                    DebugLog.d(javaClass.simpleName, "load from:$soFile")
                    return
                }
                //md5??????????????????
                soFile.delete()
            }
            //??????????????????
            download(soUrl, md5, downloadFile, soFile)
            //????????????????????????
            System.loadLibrary(libName)
        } finally {
            DebugLog.d(javaClass.simpleName, "time:" + (System.currentTimeMillis() - start))
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun getCpuAbi(context: Context): String? {
        if (cpuAbi != null) {
            return cpuAbi
        }
        // 5.0??????Application??????primaryCpuAbi??????
        try {
            val appInfo = context.applicationInfo
            val abiField = ApplicationInfo::class.java.getDeclaredField("primaryCpuAbi")
            abiField.isAccessible = true
            cpuAbi = abiField.get(appInfo) as String?
        } catch (e: Exception) {
            e.printOnDebug()
        }
        if (TextUtils.isEmpty(cpuAbi)) {
            cpuAbi = Build.SUPPORTED_ABIS[0]
        }
        return cpuAbi
    }


    /**
     * ??????????????????
     */
    private fun deleteHistoryFile(dir: File, currentFile: File?) {
        val files = dir.listFiles()
        @Suppress("SameParameterValue")
        if (files != null && files.isNotEmpty()) {
            for (f in files) {
                if (f.exists() && (currentFile == null || f.absolutePath != currentFile.absolutePath)) {
                    val delete = f.delete()
                    DebugLog.d(javaClass.simpleName, "delete file: $f result: $delete")
                    if (!delete) {
                        f.deleteOnExit()
                    }
                }
            }
        }
    }

    /**
     * ????????????
     */
    private fun downloadFileIfNotExist(url: String, destFile: File): Boolean {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            inputStream = connection.inputStream
            if (destFile.exists()) {
                return true
            }
            destFile.parentFile!!.mkdirs()
            destFile.createNewFile()
            outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(32768)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                outputStream.flush()
            }
            return true
        } catch (e: Throwable) {
            e.printOnDebug()
            if (destFile.exists() && !destFile.delete()) {
                destFile.deleteOnExit()
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printOnDebug()
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close()
                } catch (e: IOException) {
                    e.printOnDebug()
                }
            }
        }
        return false
    }

    /**
     * ?????????????????????
     */
    @Suppress("SameParameterValue")
    @Synchronized
    private fun download(
        url: String,
        md5: String?,
        downloadTempFile: File,
        destSuccessFile: File
    ) {
        if (download || AppConst.isPlayChannel) {
            return
        }
        download = true

        Coroutine.async {
            val result = downloadFileIfNotExist(url, downloadTempFile)
            DebugLog.d(javaClass.simpleName, "download result:$result")
            //??????md5????????????
            val fileMD5 = getFileMD5(downloadTempFile)
            if (md5 != null && !md5.equals(fileMD5, ignoreCase = true)) {
                val delete = downloadTempFile.delete()
                if (!delete) {
                    downloadTempFile.deleteOnExit()
                }
                download = false
                return@async
            }
            DebugLog.d(javaClass.simpleName, "download success, copy to $destSuccessFile")
            //????????????????????????
            copyFile(downloadTempFile, destSuccessFile)
            cacheInstall = false
            val parentFile = downloadTempFile.parentFile
            @Suppress("SameParameterValue")
            (deleteHistoryFile(parentFile!!, null))
        }
    }

    /**
     * ????????????
     */
    private fun copyFile(source: File?, dest: File?): Boolean {
        if (source == null || !source.exists() || !source.isFile || dest == null) {
            return false
        }
        if (source.absolutePath == dest.absolutePath) {
            return true
        }
        var fileInputStream: FileInputStream? = null
        var os: FileOutputStream? = null
        val parent = dest.parentFile
        if (parent != null && !parent.exists()) {
            val mkdirs = parent.mkdirs()
            if (!mkdirs) {
                parent.mkdirs()
            }
        }
        try {
            fileInputStream = FileInputStream(source)
            os = FileOutputStream(dest, false)
            val buffer = ByteArray(1024 * 512)
            var length: Int
            while (fileInputStream.read(buffer).also { length = it } > 0) {
                os.write(buffer, 0, length)
            }
            return true
        } catch (e: Exception) {
            e.printOnDebug()
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: Exception) {
                    e.printOnDebug()
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (e: Exception) {
                    e.printOnDebug()
                }
            }
        }
        return false
    }

    /**
     * ????????????md5
     */
    private fun getFileMD5(file: File): String? {
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(file)
            val md5 = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(1024)
            var numRead: Int
            while (fileInputStream.read(buffer).also { numRead = it } > 0) {
                md5.update(buffer, 0, numRead)
            }
            return String.format("%032x", BigInteger(1, md5.digest())).lowercase()
        } catch (e: Exception) {
            e.printOnDebug()
        } catch (e: OutOfMemoryError) {
            e.printOnDebug()
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: Exception) {
                    e.printOnDebug()
                }
            }
        }
        return null
    }

}