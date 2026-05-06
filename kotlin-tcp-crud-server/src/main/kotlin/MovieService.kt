
import bonfim.jordan.catalog.CreateMovieRequest
import bonfim.jordan.catalog.DeleteMovieRequest
import bonfim.jordan.catalog.GetMovieRequest
import bonfim.jordan.catalog.ListByActorRequest
import bonfim.jordan.catalog.ListByCategoryRequest
import bonfim.jordan.catalog.Movie
import bonfim.jordan.catalog.MovieListResponse
import bonfim.jordan.catalog.MovieServiceGrpcKt
import bonfim.jordan.catalog.UpdateMovieRequest
import java.time.Instant

class MovieService(private val repository: MovieRepository) : MovieServiceGrpcKt.MovieServiceCoroutineImplBase() {


    override suspend fun createMovie(request: CreateMovieRequest): Movie {
        println("Requisicao recebida: Criar novo filme '${request.movie.title}'")
        return repository.createMovie(request.movie)
    }

    override suspend fun getMovie(request: GetMovieRequest): Movie {
        println("Requisicao recebida: Buscar filme ID '${request.id}'")
        val movie = repository.getMovieById(request.id)
        return movie ?: Movie.getDefaultInstance()
    }
    override suspend fun updateMovie(request: UpdateMovieRequest): Movie {
        val tempoAtual = Instant.now().toString()
        val filmeComTempoAtualizado = request.movie.toBuilder()
            .setLastupdated(tempoAtual)
            .build()
        val updatedMovie = repository.updateMovie(request.id, filmeComTempoAtualizado)


        return updatedMovie ?: Movie.getDefaultInstance()
    }



    override suspend fun deleteMovie(request: DeleteMovieRequest): com.google.protobuf.Empty {
        println("Requisicao recebida: deletar filme ID '${request.id}'")
        repository.deleteMovie(request.id)
        return com.google.protobuf.Empty.getDefaultInstance()
    }


    override suspend fun listMoviesByActor(request: ListByActorRequest): MovieListResponse {
        val actorName = request.actorName

        val moviesFromDb = repository.findMoviesByActor(actorName)

        return MovieListResponse.newBuilder()
            .addAllMovies(moviesFromDb)
            .build()
    }

    override suspend fun listMoviesByCategory(request: ListByCategoryRequest): MovieListResponse {
        val category = request.category

        val moviesFromDb = repository.findMoviesByCategory(category)

        return MovieListResponse.newBuilder()
            .addAllMovies(moviesFromDb)
            .build()
    }

}