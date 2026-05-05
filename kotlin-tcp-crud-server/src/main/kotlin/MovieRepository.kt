package com.exemplo.catalog

import Catalog
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
    suspend fun findMoviesByActor(actorName: String): List<Catalog.Movie> {
        // Busca documentos onde o array 'cast' contém o nome do ator
        val results = collection.find(`in`("cast", actorName)).toList()
        return results.map { it.toProtoMovie() }
    }

    suspend fun findMoviesByCategory(category: String): List<Catalog.Movie> {
        // Busca documentos onde o array 'genres' contém a categoria
        val results = collection.find(`in`("genres", category)).toList()
        return results.map { it.toProtoMovie() }
    }

    //  MÉTODOS CRUD BÁSICOS

    suspend fun getMovieById(id: String): Catalog.Movie? {
        println("Movie ID: $id")
        val document = collection.find(eq("_id", ObjectId(id))).firstOrNull()
        println(document)
        return document?.toProtoMovie()
    }

    suspend fun deleteMovie(id: String): Boolean {
        val result = collection.deleteOne(eq("_id", ObjectId(id)))
        return result.deletedCount > 0
    }

    // FUNÇÃO DE MAPEAMENTO BSON -> PROTOBUF

    private fun Document.toProtoMovie(): Catalog.Movie {
        val builder = Catalog.Movie.newBuilder()

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