
import com.exemplo.catalog.MovieRepository
import io.grpc.Server
import io.grpc.ServerBuilder

// O serviço implementa a base gerada pelo Protobuf
class MovieService(private val repository: MovieRepository) : MovieServiceGrpcKt.MovieServiceCoroutineImplBase() {
    override suspend fun readMovie(request: Catalog.ReadMovieRequest): Catalog.Movie {
        println("Requisicao recebida: Buscar filme ID '${request.id}'")
        val movie = repository.getMovieById(request.id)

        return movie ?: Catalog.Movie.getDefaultInstance()
    }
    
    override suspend fun listMoviesByActor(request: Catalog.ListByActorRequest): Catalog.MovieListResponse {
        val actorName = request.actorName

        val moviesFromDb = repository.findMoviesByActor(actorName)

        // Construindo a resposta no formato Protobuf
        return Catalog.MovieListResponse.newBuilder()
            .addAllMovies(moviesFromDb)
            .build()
    }

}