package me.dmadouros.infrastructure.database

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.DriverManager
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
internal class PagesRepositoryTest {
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
    fun `ensures`() {
        val jdbcUrl = "${postgresqlContainer.jdbcUrl}&user=video_tutorials&password=video_tutorials"
        migrateDatabase(jdbcUrl)
        val dataSource = createDataSource(jdbcUrl)
        Database.connect(dataSource)

        val subject = PagesRepository(jacksonObjectMapper())

        subject.ensurePage("home", """{"count": 0}""")

        val actual: Map<String, Any> = subject.findByPageName("home")

        val expected = mapOf("count" to 0)

        assertThat(actual).isEqualTo(expected)
    }
}
