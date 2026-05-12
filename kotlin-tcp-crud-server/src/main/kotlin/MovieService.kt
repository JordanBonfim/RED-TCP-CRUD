
import bonfim.jordan.catalog.CreateMovieRequest
import bonfim.jordan.catalog.DeleteMovieRequest
import bonfim.jordan.catalog.GetMovieRequest
import bonfim.jordan.catalog.ListByActorRequest
import bonfim.jordan.catalog.ListByCategoryRequest
import bonfim.jordan.catalog.Movie
import bonfim.jordan.catalog.MovieListResponse
import bonfim.jordan.catalog.TcpRequest
import bonfim.jordan.catalog.TcpResponse
import bonfim.jordan.catalog.UpdateMovieRequest
import java.time.Instant
import java.util.logging.Logger

class MovieService(private val repository: MovieRepository) {

    private val logger = Logger.getLogger(MovieService::class.java.name)

    suspend fun processRequest(request: TcpRequest): TcpResponse {

        val responseBuilder = TcpResponse.newBuilder()

        try {
            when (request.operationCase) {

                TcpRequest.OperationCase.CREATE -> {
                    logger.info("TCP Roteamento: Operacao CREATE")
                    val movie = repository.createMovie(request.create.movie)
                    responseBuilder.setMovie(movie)
                }

                TcpRequest.OperationCase.GET -> {
                    logger.info("TCP Roteamento: Operacao GET")
                    var movie = repository.getMovieById(request.get.id)

                    if (movie != null) {
                        responseBuilder.setMovie(movie)
                    } else {
                        responseBuilder.setErrorMessage("Registro nao localizado.")
                    }
                }

                TcpRequest.OperationCase.UPDATE -> {
                    logger.info("TCP Roteamento: Operacao UPDATE")
                    val updatedMovie = repository.updateMovie(request.update.id, request.update.movie)
                    if (updatedMovie != null) {
                        responseBuilder.setMovie(updatedMovie)
                    } else {
                        responseBuilder.setErrorMessage("Falha na atualizacao.")
                    }
                }

                TcpRequest.OperationCase.DELETE -> {
                    logger.info("TCP Roteamento: Operacao DELETE")
                    repository.deleteMovie(request.delete.id)
                    responseBuilder.setEmpty(com.google.protobuf.Empty.getDefaultInstance())
                }

                TcpRequest.OperationCase.LIST_ACTOR -> {
                    logger.info("TCP Roteamento: Operacao LIST BY ACTOR")
                    val movies = repository.findMoviesByActor(request.listActor.actorName)
                    val listResponse = MovieListResponse.newBuilder().addAllMovies(movies).build()
                    responseBuilder.setList(listResponse)
                }

                TcpRequest.OperationCase.LIST_CATEGORY -> {
                    logger.info("TCP Roteamento: Operacao LIST BY CATEGORY")
                    val movies = repository.findMoviesByCategory(request.listCategory.category)
                    val listResponse = MovieListResponse.newBuilder().addAllMovies(movies).build()
                    responseBuilder.setList(listResponse)
                }


                else -> {
                    logger.warning("TCP Roteamento: Envelope vazio ou corrompido.")
                    responseBuilder.setErrorMessage("Operacao invalida.")
                }
            }
        } catch (e: Exception) {
            logger.severe("Erro interno: ${e.message}")
            responseBuilder.setErrorMessage("Erro no servidor: ${e.message}")
        }

        return responseBuilder.build()
    }
}