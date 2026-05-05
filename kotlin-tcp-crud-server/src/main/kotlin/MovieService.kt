
import bonfim.jordan.catalog.CreateMovieRequest
import bonfim.jordan.catalog.GetMovieRequest
import bonfim.jordan.catalog.ListByActorRequest
import bonfim.jordan.catalog.Movie
import bonfim.jordan.catalog.MovieListResponse
import bonfim.jordan.catalog.MovieServiceGrpcKt
import bonfim.jordan.catalog.UpdateMovieRequest

class MovieService(private val repository: MovieRepository) : MovieServiceGrpcKt.MovieServiceCoroutineImplBase() {
    override suspend fun getMovie(request: GetMovieRequest): Movie {
        println("Requisicao recebida: Buscar filme ID '${request.id}'")
        val movie = repository.getMovieById(request.id)
        return movie ?: Movie.getDefaultInstance()
    }

    override suspend fun createMovie(request: CreateMovieRequest): Movie {
        println("Requisicao recebida: Criar novo filme '${request.movie.title}'")
        return repository.createMovie(request.movie)
    }

    override suspend fun updateMovie(request: UpdateMovieRequest): Movie {
        println("Requisicao recebida: Atualizar filme ID '${request.id}'")
        val updatedMovie = repository.updateMovie(request.id, request.movie)
        return updatedMovie ?: Movie.getDefaultInstance()
    }


    override suspend fun listMoviesByActor(request: ListByActorRequest): MovieListResponse {
        val actorName = request.actorName

        val moviesFromDb = repository.findMoviesByActor(actorName)

        return MovieListResponse.newBuilder()
            .addAllMovies(moviesFromDb)
            .build()
    }

}