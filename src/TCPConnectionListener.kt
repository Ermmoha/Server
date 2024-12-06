package server

import java.io.IOException

interface TCPConnectionListener {

    fun onConnectionReady(tcpConnection: TCPConnection)

    fun onReceiveString(tcpConnection: TCPConnection, message: String)

    fun onDisconnect(tcpConnection: TCPConnection)

    fun onException(tcpConnection: TCPConnection, exception: IOException)
}