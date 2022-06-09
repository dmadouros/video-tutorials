package me.dmadouros.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection
import java.sql.DriverManager

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

private fun createDataSource(jdbcUrl: String): HikariDataSource =
    HikariConfig()
        .apply {
            this.driverClassName = "org.postgresql.Driver"
            this.jdbcUrl = jdbcUrl
        }
        .also { config -> config.validate() }
        .let(::HikariDataSource)

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
