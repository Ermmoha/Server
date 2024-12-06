package server

import java.io.*
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class ChatServer : TCPConnectionListener {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ChatServer()
        }
    }

    private val connections: MutableList<TCPConnection> = Collections.synchronizedList(LinkedList())

    init {
        println("Сервер запущен...")

        // Запуск потока для ввода команд администратора
        Thread(AdminMessageHandler(this)).start()

        try {
            ServerSocket(8189).use { serverSocket ->
                while (true) {
                    try {
                        val connection = TCPConnection(this, serverSocket.accept())
                        println("Новое подключение: $connection")
                    } catch (e: IOException) {
                        println("Ошибка подключения: ${e.message}")
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Ошибка запуска сервера: ${e.message}", e)
        }
    }

    // Обработчик событий подключения
    @Synchronized
    override fun onConnectionReady(tcpConnection: TCPConnection) {
        connections.add(tcpConnection)
        broadcastMessage("Клиент подключился: $tcpConnection")
    }

    // Обработка полученного сообщения
    @Synchronized
    override fun onReceiveString(tcpConnection: TCPConnection, value: String) {
        if (value.startsWith("IMAGE:")) {
            // Если сообщение содержит изображение
            saveImage(value)  // Сохраняем изображение на сервере и отправляем ссылку всем клиентам
        } else {
            broadcastMessage(value)  // Обычные сообщения
        }
    }

    // Обработка отключения клиента
    @Synchronized
    override fun onDisconnect(tcpConnection: TCPConnection) {
        connections.remove(tcpConnection)
        broadcastMessage("Клиент отключился: $tcpConnection")
    }

    // Обработка исключений
    override fun onException(tcpConnection: TCPConnection, e: IOException) {
        println("Ошибка TCP-соединения: ${e.message}")
    }

    // Отправка сообщения всем клиентам
    fun broadcastMessage(message: String) {
        println(message)  // Логируем сообщение
        synchronized(connections) {
            connections.forEach { it.sendString(message) }
        }
    }

    // Сохранение изображения на сервере и отправка ссылки всем клиентам
    private fun saveImage(value: String) {
        val parts = value.split(":", limit = 3)
        val imageName = parts[1]
        val base64Data = parts[2]

        // Декодирование изображения из Base64
        val decodedBytes = Base64.getDecoder().decode(base64Data)


        // Создание директории для сохранения изображений (если не существует)
        val uploadDir: Path = Paths.get("uploads")
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir)
        }

        // Путь для сохранения изображения
        val imagePath = uploadDir.resolve(imageName)

        // Сохранение изображения в файл
        try {
            Files.write(imagePath, decodedBytes)

            // После сохранения, отправляем сообщение с путем к файлу всем клиентам
            val imagePathMessage = "IMAGE:$imageName:${imagePath.toUri()}"
            broadcastMessage(imagePathMessage)
        } catch (e: IOException) {
            println("Ошибка при сохранении изображения: ${e.message}")
        }
    }

    // Поток для чтения сообщений от администратора и отправки их всем клиентам
    private class AdminMessageHandler(private val server: ChatServer) : Runnable {

        private val adminInput: BufferedReader = BufferedReader(InputStreamReader(System.`in`))

        override fun run() {
            var adminMessage: String?
            try {
                // Чтение сообщений администратора
                while (true) {
                    adminMessage = adminInput.readLine()  // Чтение сообщения
                    if (adminMessage != null) {
                        // Отправка сообщения всем клиентам через сервер
                        server.broadcastMessage("Администратор: $adminMessage")
                    }
                }
            } catch (ex: IOException) {
                println("Ошибка при вводе сообщения администратора: ${ex.message}")
            }
        }
    }
}

