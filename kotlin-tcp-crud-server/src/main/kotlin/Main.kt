import com.mongodb.kotlin.client.coroutine.MongoClient
import io.github.cdimascio.dotenv.dotenv
import io.grpc.ServerBuilder

fun main() {
    println("Servidor iniciando...")

    val dotenv = dotenv()

    val dbPassword = dotenv["DB_PASSWORD"]
    val dbUser = dotenv["DB_USER"]

    if (dbPassword == null || dbUser == null) {
        println("ERRO: Variáveis necessárias não encontradas no .env")
        return
    }

    // Conexao com o MongoDB Atlas
    val uri = "mongodb+srv://$dbUser:$dbPassword@cluster0.sh7okth.mongodb.net/?appName=Cluster0"

    val databaseName = "sample_mflix"

    val mongoClient = MongoClient.create(uri)
    val database = mongoClient.getDatabase(databaseName)

    println("Conectado ao banco de dados")

    // Inicializacao dos componentes
    val repository = MovieRepository(database)
    val movieService = MovieService(repository)

    // Configuracao e inicializacao do Servidor gRPC (TCP)
    val port = 5090
    val server = ServerBuilder.forPort(port)
        .addService(movieService)
        .build()

    server.start()
    println("Servidor gRPC TCP operando na porta $port. Aguardando instrucoes...")

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Encerrando")
        server.shutdown()
        mongoClient.close()
        println("Sistemas desligados com sucesso.")
    })


    server.awaitTermination()
}