package server

import java.io.*
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Класс представляет TCP-соединение
 */
class TCPConnection(
    private val eventListener: TCPConnectionListener,
    private val socket: Socket
) : Runnable {

    private val inStream: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
    private val outStream: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))
    private val thread: Thread = Thread(this)

    init {
        thread.start()
    }

    constructor(eventListener: TCPConnectionListener, ipAddress: String, port: Int) :
            this(eventListener, Socket(ipAddress, port))

    override fun run() {
        try {
            eventListener.onConnectionReady(this)
            while (!Thread.currentThread().isInterrupted) {
                val message = inStream.readLine()
                if (message != null) {
                    eventListener.onReceiveString(this, message)
                } else {
                    break
                }
            }
        } catch (e: IOException) {
            eventListener.onException(this, e)
        } finally {
            eventListener.onDisconnect(this)
        }
    }

    @Synchronized
    fun sendString(value: String) {
        try {
            outStream.write("$value\r\n")
            outStream.flush()
        } catch (e: IOException) {
            eventListener.onException(this, e)
            disconnect()
        }
    }

    @Synchronized
    fun disconnect() {
        thread.interrupt()
        try {
            socket.close()
        } catch (e: IOException) {
            eventListener.onException(this, e)
        }
    }

    override fun toString(): String {
        return "TCPConnection: ${socket.inetAddress}:${socket.port}"
    }
}
