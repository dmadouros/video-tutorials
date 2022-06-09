package me.dmadouros

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import me.dmadouros.api.configureVideoTutorials
import me.dmadouros.persistence.MessageStore
import me.dmadouros.plugins.configureSerialization
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.DriverManager
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        val eventStoreDbClient: EventStoreDBClient = createEventstoreDbClient()
        val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        val messageStore = MessageStore(eventStoreDbClient, objectMapper)

        configureCallId()
        configureDatabase()
        configureVideoTutorials(
            messageStore = messageStore,
            objectMapper = objectMapper,
        )
        configureSerialization()
    }.start(wait = true)
}

private fun migrateDatabase(jdbcUrl: String) {
    val jdbcUrlWithSchema = "$jdbcUrl&currentSchema=public"
    val connection = DriverManager.getConnection(jdbcUrlWithSchema)
    val liquibase = Liquibase(
        "db/changelog/db.changelog-master.yml",
        ClassLoaderResourceAccessor(),
        JdbcConnection(connection)
    )
    liquibase.update(Contexts(), LabelExpression())
    connection.close()
}

private fun createDataSource(jdbcUrl: String): HikariDataSource =
    HikariConfig()
        .apply {
            this.driverClassName = "org.postgresql.Driver"
            this.jdbcUrl = jdbcUrl
        }
        .also { config -> config.validate() }
        .let(::HikariDataSource)

private fun createEventstoreDbClient(): EventStoreDBClient =
    "esdb://admin:changeit@video-tutorials.eventstore.db:2113?tls=false"
        .let { EventStoreDBConnectionString.parse(it) }
        .let { EventStoreDBClient.create(it) }

fun Application.configureDatabase() {
    val jdbcUrl = System.getenv("VIDEO_TUTORIALS_DATABASE_URL")
    migrateDatabase(jdbcUrl)
    val dataSource = createDataSource(jdbcUrl)
    val database = Database.connect(dataSource)

    environment.monitor.subscribe(ApplicationStarted) {
        TransactionManager.defaultDatabase = database
    }

    environment.monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
    }
}

fun Application.configureCallId() {
    install(CallId) {
        generate {
            UUID.randomUUID().toString()
        }
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}
