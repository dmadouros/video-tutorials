package me.dmadouros.infrastructure.database

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet

class PagesRepository(private val objectMapper: ObjectMapper) {
    private val logger = LogManager.getLogger()

    fun ensurePage(pageName: String, initialData: String) {
        val query = """
            INSERT INTO pages(page_name, page_data)
            VALUES (?, ?::jsonb)
            ON CONFLICT DO NOTHING
        """.trimIndent()
        val params = listOf(
            Pair(TextColumnType(), pageName),
            Pair(TextColumnType(), initialData),
        )
        executeUpdate(query, params)
    }

    fun incrementVideosWatched(globalPosition: Long) {
        val query = """
            UPDATE pages
            SET page_data = jsonb_set(
                jsonb_set(
                    page_data,
                    '{videosWatched}',
                    ((page_data ->> 'videosWatched')::int + 1)::text::jsonb
                ),
                '{lastViewProcessed}',
                ?::text::jsonb
            ) 
            WHERE page_name = 'home' 
            AND (page_data->>'lastViewProcessed')::int < ?
        """.trimIndent()
        val params = listOf(
            Pair(IntegerColumnType(), globalPosition),
            Pair(IntegerColumnType(), globalPosition)
        )
        executeUpdate(query, params)
    }

    fun findByPageName(pageName: String): Map<String, Any> {
        val query = """
            SELECT *
              FROM pages
             WHERE page_name = ?
             LIMIT 1
        """.trimIndent()
        val params = listOf(Pair(TextColumnType(), pageName))

        return executeQuery(query, params) { rs: ResultSet ->
            objectMapper.readValue(rs.getString("page_data")) as Map<String, Any>
        }.first()
    }

    private fun executeUpdate(query: String, params: List<Pair<ColumnType, Any>>) {
        transaction {
            var stmt: PreparedStatementApi? = null
            try {
                val conn = TransactionManager.current().connection
                stmt = conn.prepareStatement(query, false)
                stmt.fillParameters(params)
                stmt.executeUpdate()
            } catch (e: Exception) {
                logger.error(e)
            } finally {
                stmt?.closeIfPossible()
            }
        }
    }

    private fun <T> executeQuery(
        query: String,
        params: List<Pair<ColumnType, Any>>,
        rowMapper: (ResultSet) -> T,
    ): List<T> {
        return transaction {
            var stmt: PreparedStatementApi? = null
            var rs: ResultSet? = null
            try {
                val conn = TransactionManager.current().connection
                stmt = conn.prepareStatement(query, false)
                stmt.fillParameters(params)
                rs = stmt.executeQuery()
                val results = ArrayList<T>()
                while (rs.next()) {
                    results.add(rowMapper(rs))
                }
                results
            } catch (e: Exception) {
                logger.error(e)
                emptyList()
            } finally {
                rs?.close()
                stmt?.closeIfPossible()
            }
        }
    }
}
