package me.dmadouros.infrastructure.message_store

import com.eventstore.dbclient.EventStoreDBClient
import com.eventstore.dbclient.EventStoreDBConnectionString
import com.eventstore.dbclient.RecordedEvent
import com.eventstore.dbclient.ResolvedEvent
import com.eventstore.dbclient.Subscription
import com.eventstore.dbclient.SubscriptionListener
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.concurrent.TimeUnit.SECONDS
import me.dmadouros.infrastructure.message_store.dtos.DomainEventDto
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

data class TestEvent(override val traceId: String, override val data: Data) :
    DomainEventDto<TestEvent.Data>(type = "EventTested", data = data, traceId = traceId) {
    data class Data(val foo: String)
}

data class TestDto(val type: String = "", val actorId: String = "", val traceId: String = "", val foo: String = "")

class TestProjection : Projection<TestDto> {
    override val init: TestDto = TestDto()
    override val handlers: Map<String, (TestDto, RecordedEvent) -> TestDto> =
        mapOf(
            "EventTested" to { memo: TestDto, event: RecordedEvent ->
                val testEvent: TestEvent = jacksonObjectMapper().readValue(event.eventData)
                memo.copy(
                    traceId = testEvent.traceId,
                    type = testEvent.type,
                    actorId = testEvent.actorId ?: "",
                    foo = testEvent.data.foo
                )
            }
        )
}

@Testcontainers
internal class MessageStoreTest {
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
    val eventStoreDbClient: EventStoreDBClient = createEventstoreDbClient()
    private fun createEventstoreDbClient(): EventStoreDBClient =
        connectionString
            .let { EventStoreDBConnectionString.parse(it) }
            .let { EventStoreDBClient.create(it) }

    private val subject = MessageStore(client = eventStoreDbClient, objectMapper = jacksonObjectMapper())

    @Test
    fun `can read same information as written`() {
        val streamName = "testStream-12345"

        val traceId = "traceId"
        val event = TestEvent(
            traceId = traceId,
            data = TestEvent.Data(
                foo = "bar"
            )
        )

        subject.write(streamName, event)

        val actual = subject.fetch(streamName, TestProjection())

        assertThat(actual.traceId).isEqualTo(traceId)
        assertThat(actual.type).isEqualTo("EventTested")
        assertThat(actual.actorId).isEqualTo("")
        assertThat(actual.foo).isEqualTo("bar")
    }

    @Test
    fun `creates a subscription`() {
        val streamName = "testStream-12345"
        val event = TestEvent(
            traceId = "traceId",
            data = TestEvent.Data(
                foo = "bar"
            )
        )
        subject.write(streamName, event)

        var actual: TestEvent? = null
        val listener = object : SubscriptionListener() {
            override fun onEvent(subscription: Subscription, event: ResolvedEvent) {
                actual = jacksonObjectMapper().readValue(event.originalEvent.eventData)
            }
        }


        subject.createSubscription("testStream", "test", listener)

        await.atMost(5, SECONDS).untilAsserted { assertThat(actual).isEqualTo(event) }
    }

    @Test
    fun `creates a subscription (legacy)`() {
        val streamName = "testStream-12345"
        val event = TestEvent(
            traceId = "traceId",
            data = TestEvent.Data(
                foo = "bar"
            )
        )
        subject.write(streamName, event)

        var actual: TestEvent? = null


        val eventHandlers = mapOf("EventTested" to { event: RecordedEvent ->
            actual = jacksonObjectMapper().readValue(event.eventData)
        })


        subject.createSubscription("testStream", "test", eventHandlers)

        await.atMost(5, SECONDS).untilAsserted { assertThat(actual).isEqualTo(event) }
    }
}
