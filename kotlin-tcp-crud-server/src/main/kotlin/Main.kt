import bonfim.jordan.catalog.TcpRequest
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import java.net.Socket
import kotlin.system.exitProcess
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() = runBlocking{
    println("Servidor iniciando...")

    val dotenv = dotenv()

    val dbPassword = dotenv["DB_PASSWORD"]
    val dbUser = dotenv["DB_USER"]

    if (dbPassword == null || dbUser == null) {
        println("ERRO: Variáveis necessárias não encontradas no .env")
        exitProcess(1)
    }

    // Conexao com o MongoDB Atlas
    val uri = "mongodb+srv://$dbUser:$dbPassword@cluster0.sh7okth.mongodb.net/?appName=Cluster0"

    val databaseName = "sample_mflix"

    val mongoClient = MongoClient.create(uri)
    val database = mongoClient.getDatabase(databaseName)

    println("Conectado ao banco de dados")

    val repository = MovieRepository(database)
    val movieService = MovieService(repository)

    val port = 5090
    val serverSocket = ServerSocket(port)
    println("Servidor Socket TCP operando na porta $port. Aguardando instrucoes...")


    Runtime.getRuntime().addShutdownHook(Thread {
        println("Encerrando servidor...")
        serverSocket.close()
        mongoClient.close()
        println("Sistemas desligados com sucesso.")
    })


    while (true) {
        try {
            val clientSocket = serverSocket.accept()
            println("Nova conexao detectada: ${clientSocket.inetAddress.hostAddress}")
            // coroutine
            launch(Dispatchers.IO) {
                handleClient(clientSocket, movieService)
            }
        } catch (e: Exception) {
            println("Aviso: Falha ao aceitar conexao. ${e.message}")
        }
    }
}


suspend fun handleClient(socket: Socket, movieService: MovieService) {
    try {
        socket.use { client ->
            val input = client.getInputStream()
            val output = client.getOutputStream()

            while (true) {
                val request = TcpRequest.parseDelimitedFrom(input)

                if (request == null) {
                    println("Cliente desconectou")
                    break
                }

                val response = movieService.processRequest(request)
                response.writeDelimitedTo(output)
                output.flush()
            }

        }
    } catch (e: Exception) {
        println("Aviso: O cliente encerrou a conexao.")
    }
}