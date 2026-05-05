package bonfim.jordan


import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

fun main() {
    val SERVER_PORT = 8080
    val server = ServerSocket(SERVER_PORT)

    println("Servidor TCP online na porta $SERVER_PORT.")
    println("Aguardando conexões de entrada")

    while (true) {
        // Bloqueia até que um cliente estabeleça conexão
        val clientSocket = server.accept()
        println("Nova conexão estabelecida: ${clientSocket.inetAddress.hostAddress}")

        // Inicia uma thread dedicada para não bloquear o loop principal
        thread {
            try {
                val input = BufferedReader(InputStreamReader(clientSocket.inputStream))
                val output = PrintWriter(clientSocket.outputStream, true)

                // Mensagem de boas-vindas ao cliente
                output.println("Conexão estabelecida")

                var message: String?
                // Lê as mensagens do cliente continuamente
                while (input.readLine().also { message = it } != null) {
                    println("[CLIENTE ${clientSocket.inetAddress.hostAddress}] $message")


                    if (message?.trim( )?.lowercase() == "exit") {
                        output.println("Encerrando conexão")
                        break
                    } else {
                        output.println("Servidor recebeu: $message")
                    }
                }
            } catch (e: Exception) {
                println("[ERRO] Falha na comunicação com o cliente: ${e.message}")
            } finally {
                // liberar recursos
                clientSocket.close()
                println("Conexão encerrada")
            }
        }
    }
}