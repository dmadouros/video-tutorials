val exposedVersion: String by project
val kotlinVersion: String by project
val ktorVersion: String by project
val log4jVersion: String by project
val testContainersVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("com.google.cloud.tools.jib") version "3.2.1"
    id("org.liquibase.gradle") version "2.1.1"
    jacoco
}

group = "me.dmadouros"
version = "0.0.1"
application {
    mainClass.set("me.dmadouros.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation(platform("org.apache.logging.log4j:log4j-bom:$log4jVersion"))
    implementation(platform("org.jetbrains.exposed:exposed-bom:$exposedVersion"))
    implementation(platform("org.testcontainers:testcontainers-bom:$testContainersVersion"))

    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.16")

    implementation("com.eventstore:db-client-java:3.0.0")

    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl")
    implementation("com.mattbertolini:liquibase-slf4j:4.1.0")

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-dao")
    implementation("org.jetbrains.exposed:exposed-jdbc")
    implementation("org.liquibase:liquibase-core:4.10.0")
    implementation("org.postgresql:postgresql:42.3.4")
    implementation("org.yaml:snakeyaml:1.30")

    implementation("io.konform:konform-jvm:0.4.0")

    implementation("org.springframework.security:spring-security-crypto:5.7.1")

    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.mockk:mockk:1.12.5")
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.awaitility:awaitility-kotlin:4.2.0")

    liquibaseRuntime("info.picocli:picocli:4.6.3")
    liquibaseRuntime("org.liquibase:liquibase-core:4.8.0")
    liquibaseRuntime("org.postgresql:postgresql:42.3.3")
    liquibaseRuntime("org.yaml:snakeyaml:1.30")
}

jib.to.image = "dmadouros/video-tutorials"

liquibase {
    activities.register("master") {
        arguments = mapOf(
            "classpath" to "src/main/resources",
            "changeLogFile" to "db/changelog/db.changelog-master.yml",
            "username" to "provider",
            "password" to "provider",
            "url" to "jdbc:postgresql://localhost:5432/provider"
        )
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}
