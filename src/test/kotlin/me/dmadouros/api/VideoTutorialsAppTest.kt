package me.dmadouros.api

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.testing.testApplication
import java.sql.Connection
import java.sql.DriverManager
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import me.dmadouros.infrastructure.database.PagesRepository
import me.dmadouros.infrastructure.database.UserRegistrationsRepository
import me.dmadouros.infrastructure.message_store.MessageStore
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import io.ktor.server.routing.get
import me.dmadouros.plugins.configureCallId
import me.dmadouros.plugins.configureSerialization

@Testcontainers
internal class VideoTutorialsAppTest {

    @Container
    val eventstoreDbContainer =
        GenericContainer<Nothing>(DockerImageName.parse("eventstore/eventstore:21.10.2-buster-slim")).apply {
            withExposedPorts(2113)
            withEnv("EVENTSTORE_HTTP_PORT", "2113")
            withEnv("EVENTSTORE_INSECURE", "true")
            waitingFor(Wait.forHealthcheck())
            start()
        }

    private val connectionString = buildString {
        val host = eventstoreDbContainer.host
        val port = eventstoreDbContainer.getMappedPort(2113)

        append("esdb://admin:changeit@$host:$port?tls=false")
    }
    private val eventStoreDbClient: EventStoreDBClient = createEventstoreDbClient()
    private fun createEventstoreDbClient(): EventStoreDBClient =
        connectionString
            .let { EventStoreDBConnectionString.parse(it) }
            .let { EventStoreDBClient.create(it) }

    private val messageStore = MessageStore(client = eventStoreDbClient, objectMapper = jacksonObjectMapper())

    @Container
    val postgresqlContainer = PostgreSQLContainer<Nothing>("postgres:12-alpine").apply {
        withDatabaseName("video_tutorials")
        withUsername("video_tutorials")
        withPassword("video_tutorials")
        start()
    }

    private fun migrateDatabase(jdbcUrl: String) {
        var connection: Connection? = null
        try {
            connection = DriverManager.getConnection("$jdbcUrl&currentSchema=public")
            Liquibase(
                "db/changelog/db.changelog-master.yml",
                ClassLoaderResourceAccessor(),
                JdbcConnection(connection)
            ).update(Contexts(), LabelExpression())
        } finally {
            connection?.close()
        }
    }

    private fun createDataSource(jdbcUrl: String): HikariDataSource =
        HikariConfig()
            .apply {
                this.driverClassName = "org.postgresql.Driver"
                this.jdbcUrl = jdbcUrl
            }
            .also { config -> config.validate() }
            .let(::HikariDataSource)

    @Test
    fun `temp`() = testApplication {
        val jdbcUrl = "${postgresqlContainer.jdbcUrl}&user=video_tutorials&password=video_tutorials"
        migrateDatabase(jdbcUrl)
        val dataSource = createDataSource(jdbcUrl)
        Database.connect(dataSource)

        val pagesRepository = PagesRepository(jacksonObjectMapper())
        val userRegistrationsRepository = UserRegistrationsRepository()

        application {
            configureCallId()
            configureSerialization()
            configureVideoTutorials(
                messageStore = messageStore,
                objectMapper = jacksonObjectMapper(),
                pagesRepository = pagesRepository,
                userRegistrationsRepository = userRegistrationsRepository
            )
        }

        val response = client.post("/record-viewing/2021bb89-10e4-4c00-a0c1-81a2fb7fa4fb")
        assertThat(response.status).isEqualTo(HttpStatusCode.NoContent)

        val response2 = client.get("/")
        assertThat(response2.status).isEqualTo(HttpStatusCode.OK)
        val actual: Map<String, Any> = jacksonObjectMapper().readValue(response2.bodyAsText())
        assertThat(actual).isEqualTo(mapOf("videosWatched" to 1, "lastViewProcessed" to 733))
    }
}
