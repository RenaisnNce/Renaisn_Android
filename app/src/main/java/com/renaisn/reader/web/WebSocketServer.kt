package com.renaisn.reader.web

import fi.iki.elonen.NanoWSD
import com.renaisn.reader.service.WebService
import com.renaisn.reader.web.socket.BookSourceDebugWebSocket
import com.renaisn.reader.web.socket.RssSourceDebugWebSocket

class WebSocketServer(port: Int) : NanoWSD(port) {

    override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
        WebService.serve()
        return when (handshake.uri) {
            "/bookSourceDebug" -> {
                BookSourceDebugWebSocket(handshake)
            }
            "/rssSourceDebug" -> {
                RssSourceDebugWebSocket(handshake)
            }
            else -> null
        }
    }
}
