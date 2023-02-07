package com.renaisn.reader.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.renaisn.reader.constant.IntentAction
import com.renaisn.reader.utils.printOnDebug


/**
 * web服务快捷开关
 */
@RequiresApi(Build.VERSION_CODES.N)
class WebTileService : TileService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                IntentAction.start -> qsTile?.run {
                    state = Tile.STATE_ACTIVE
                    updateTile()
                }
                IntentAction.stop -> qsTile?.run {
                    state = Tile.STATE_INACTIVE
                    updateTile()
                }
            }
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onStartListening() {
        super.onStartListening()
        if (WebService.isRun) {
            qsTile.state = Tile.STATE_ACTIVE
            qsTile.updateTile()
        } else {
            qsTile.state = Tile.STATE_INACTIVE
            qsTile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (WebService.isRun) {
            WebService.stop(this)
        } else {
            WebService.start(this)
        }
    }

}