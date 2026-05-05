

import bonfim.jordan.catalog.Movie
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId


class MovieRepository(database: MongoDatabase) {

    // Conectando movies
    private val collection = database.getCollection<Document>("movies")

    //  MÉTODOS DE BUSCA ESPECÍFICOS
    suspend fun findMoviesByActor(actorName: String): List<Movie> {
        // Busca documentos onde o array 'cast' contém o nome do ator
        val results = collection.find(`in`("cast", actorName)).limit(40).toList()
        return results.map { it.toProtoMovie() }
    }

    suspend fun findMoviesByCategory(category: String): List<Movie> {
        // Busca documentos onde o array 'genres' contém a categoria
        val results = collection.find(`in`("genres", category)).limit(40).toList()
        return results.map { it.toProtoMovie() }
    }


    //  MÉTODOS CRUD BÁSICOS
    suspend fun getMovieById(id: String): Movie? {
        println("Movie ID: $id")
        val document = collection.find(eq("_id", ObjectId(id))).firstOrNull()
        return document?.toProtoMovie()
    }

    suspend fun deleteMovie(id: String): Boolean {
        val result = collection.deleteOne(eq("_id", ObjectId(id)))
        return result.deletedCount > 0
    }


    // CREATE
    suspend fun createMovie(movie: Movie): Movie {
        val document = Document()
            .append("title", movie.title)
            .append("year", movie.year)
            .append("plot", movie.plot)
            .append("type", "movie")

        val result = collection.insertOne(document)

        // Recupera o ID gerado pelo BD
        val generatedID = result.insertedId?.asObjectId()?.value?.toHexString() ?: ""
        return movie.toBuilder().setId(generatedID).build()
    }

    // UPDATE
    suspend fun updateMovie(id: String, movie: Movie): Movie? {
        return try {
            val objectId = ObjectId(id)
            val updates = Document()

            // Só modifica os campo se não estiver vazio/zero
            if (movie.title.isNotEmpty()) updates.append("title", movie.title)
            if (movie.year != 0) updates.append("year", movie.year)
            if (movie.plot.isNotEmpty()) updates.append("plot", movie.plot)
            if (movie.fullplot.isNotEmpty()) updates.append("fullplot", movie.fullplot)
            if (movie.poster.isNotEmpty()) updates.append("poster", movie.poster)
            if (movie.type.isNotEmpty()) updates.append("type", movie.type)
            if (movie.runtime != 0) updates.append("runtime", movie.runtime)


            if (movie.genresList.isNotEmpty()) updates.append("genres", movie.genresList)
            if (movie.castList.isNotEmpty()) updates.append("cast", movie.castList)
            if (movie.directorsList.isNotEmpty()) updates.append("directors", movie.directorsList)
            if (movie.writersList.isNotEmpty()) updates.append("writers", movie.writersList)
            if (movie.languagesList.isNotEmpty()) updates.append("languages", movie.languagesList)
            if (movie.countriesList.isNotEmpty()) updates.append("countries", movie.countriesList)

            // Se o pacote estiver vazio cancela a operação
            if (updates.isEmpty()) {
                println("Pacote vazio.")
                return getMovieById(id)
            }

            val updateDoc = Document("\$set", updates)
            collection.updateOne(eq("_id", objectId), updateDoc)

            println("Registro '$id' atualizado com sucesso no banco de dados.")

            // Retorna a matriz atualizada
            getMovieById(id)

        } catch (e: IllegalArgumentException) {
            println("Falha na atualização. Formato de ID inválido.")
            null
        }
    }



    // FUNÇÃO DE MAPEAMENTO BSON -> PROTOBUF
    private fun Document.toProtoMovie(): Movie {
        val builder = Movie.newBuilder()

        // Extração de campos
        this.getObjectId("_id")?.let { builder.id = it.toHexString() }
        this.getString("plot")?.let { builder.plot = it }
        this.getList("genres", String::class.java)?.let { builder.addAllGenres(it) }
        this.getInteger("runtime")?.let { builder.runtime = it }
        this.getList("cast", String::class.java)?.let { builder.addAllCast(it) }
        this.getInteger("num_mflix_comments")?.let { builder.numMflixComments = it }
        this.getString("poster")?.let { builder.poster = it }
        this.getString("title")?.let { builder.title = it }
        this.getString("fullplot")?.let { builder.fullplot = it }
        this.getList("languages", String::class.java)?.let { builder.addAllLanguages(it) }

        // Conversão de Data do MongoDB para Timestamp do Protobuf
        this.getDate("released")?.let { date ->
            val timestamp = com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(date.time / 1000)
                .setNanos(((date.time % 1000) * 1000000).toInt())
                .build()
            builder.released = timestamp
        }

        this.getList("directors", String::class.java)?.let { builder.addAllDirectors(it) }
        this.getList("writers", String::class.java)?.let { builder.addAllWriters(it) }
        this.getString("lastupdated")?.let { builder.lastupdated = it }
        this.getInteger("year")?.let { builder.year = it }
        this.getList("countries", String::class.java)?.let { builder.addAllCountries(it) }
        this.getString("type")?.let { builder.type = it }

        return builder.build()
    }
}