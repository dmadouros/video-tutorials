val kotlinVersion: String by project
val ktorVersion: String by project
val log4jVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.21"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("com.google.cloud.tools.jib") version "3.2.1"
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

    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.16")

    implementation("com.eventstore:db-client-java:3.0.0")

    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl")
    implementation("com.mattbertolini:liquibase-slf4j:4.1.0")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}

jib.to.image = "dmadouros/video-tutorials"
