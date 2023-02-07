package com.renaisn.reader.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.renaisn.reader.R
import com.renaisn.reader.base.BaseService
import com.renaisn.reader.constant.AppConst
import com.renaisn.reader.constant.EventBus
import com.renaisn.reader.constant.IntentAction
import com.renaisn.reader.constant.PreferKey
import com.renaisn.reader.receiver.NetworkChangedListener
import com.renaisn.reader.utils.*
import com.renaisn.reader.web.HttpServer
import com.renaisn.reader.web.WebSocketServer
import splitties.init.appCtx
import splitties.systemservices.powerManager
import java.io.IOException

class WebService : BaseService() {

    companion object {
        var isRun = false
        var hostAddress = ""

        fun start(context: Context) {
            context.startService<WebService>()
        }

        fun stop(context: Context) {
            context.stopService<WebService>()
        }

        fun serve() {
            appCtx.startService<WebService> {
                action = "serve"
            }
        }
    }

    private val useWakeLock = appCtx.getPrefBoolean(PreferKey.webServiceWakeLock, false)
    private val wakeLock: PowerManager.WakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "legado:webService")
            .apply {
                setReferenceCounted(false)
            }
    }
    private var httpServer: HttpServer? = null
    private var webSocketServer: WebSocketServer? = null
    private var notificationContent = ""
    private val networkChangedListener by lazy {
        NetworkChangedListener(this)
    }

    override fun onCreate() {
        super.onCreate()
        if (useWakeLock) wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        isRun = true
        notificationContent = getString(R.string.service_starting)
        upNotification()
        upTile(true)
        networkChangedListener.register()
        networkChangedListener.onNetworkChanged = {
            val address = NetworkUtils.getLocalIPAddress()
            if (address == null) {
                hostAddress = getString(R.string.network_connection_unavailable)
                notificationContent = hostAddress
                upNotification()
            } else {
                hostAddress = getString(R.string.http_ip, address.hostAddress, getPort())
                notificationContent = hostAddress
                upNotification()
            }
            postEvent(EventBus.WEB_SERVICE, hostAddress)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.stop -> stopSelf()
            "copyHostAddress" -> sendToClip(hostAddress)
            "serve" -> if (useWakeLock) wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
            else -> upWebServer()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (useWakeLock) wakeLock.release()
        networkChangedListener.unRegister()
        isRun = false
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        postEvent(EventBus.WEB_SERVICE, "")
        upTile(false)
    }

    private fun upWebServer() {
        if (httpServer?.isAlive == true) {
            httpServer?.stop()
        }
        if (webSocketServer?.isAlive == true) {
            webSocketServer?.stop()
        }
        val address = NetworkUtils.getLocalIPAddress()
        if (address != null) {
            val port = getPort()
            httpServer = HttpServer(port)
            webSocketServer = WebSocketServer(port + 1)
            try {
                httpServer?.start()
                webSocketServer?.start(1000 * 30) // 通信超时设置
                hostAddress = getString(R.string.http_ip, address.hostAddress, port)
                isRun = true
                postEvent(EventBus.WEB_SERVICE, hostAddress)
                notificationContent = hostAddress
                upNotification()
            } catch (e: IOException) {
                toastOnUi(e.localizedMessage ?: "")
                e.printOnDebug()
                stopSelf()
            }
        } else {
            toastOnUi("web service cant start, no ip address")
            stopSelf()
        }
    }

    private fun getPort(): Int {
        var port = getPrefInt(PreferKey.webPort, 1122)
        if (port > 65530 || port < 1024) {
            port = 1122
        }
        return port
    }

    /**
     * 更新通知
     */
    private fun upNotification() {
        val builder = NotificationCompat.Builder(this, AppConst.channelIdWeb)
            .setSmallIcon(R.drawable.ic_web_service_noti)
            .setOngoing(true)
            .setContentTitle(getString(R.string.web_service))
            .setContentText(notificationContent)
            .setContentIntent(
                servicePendingIntent<WebService>("copyHostAddress")
            )
        builder.addAction(
            R.drawable.ic_stop_black_24dp,
            getString(R.string.cancel),
            servicePendingIntent<WebService>(IntentAction.stop)
        )
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        val notification = builder.build()
        startForeground(AppConst.notificationIdWeb, notification)
    }

    private fun upTile(active: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            kotlin.runCatching {
                startService<WebTileService> {
                    action = if (active) {
                        IntentAction.start
                    } else {
                        IntentAction.stop
                    }
                }
            }

        }
    }
}
