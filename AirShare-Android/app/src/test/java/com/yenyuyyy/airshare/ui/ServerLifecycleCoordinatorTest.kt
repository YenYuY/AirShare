package com.yenyuyyy.airshare.ui

import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerLifecycleCoordinatorTest {
    @Test
    fun `stop before a pending start claims ownership prevents server creation`() = runBlocking {
        val coordinator = ServerLifecycleCoordinator()
        val created = AtomicInteger()
        val startGeneration = coordinator.beginStart()
        val stopGeneration = coordinator.cancelPendingStarts()

        val result = runCatching {
            coordinator.start(
                generation = startGeneration,
                create = {
                    created.incrementAndGet()
                    FakeServer()
                },
                start = { "started" },
            )
        }
        coordinator.stopThrough(stopGeneration)

        assertTrue(result.exceptionOrNull() is ServerStartCancelledException)
        assertEquals(0, created.get())
    }

    @Test
    fun `stop during start closes the server before the start can become active`() = runBlocking {
        supervisorScope {
            val coordinator = ServerLifecycleCoordinator()
            val server = FakeServer()
            val enteredStart = CompletableDeferred<Unit>()
            val releaseStart = CompletableDeferred<Unit>()
            val startGeneration = coordinator.beginStart()

            val starting = async(Dispatchers.Default) {
                coordinator.start(
                    generation = startGeneration,
                    create = { server },
                    start = {
                        enteredStart.complete(Unit)
                        releaseStart.await()
                        "started"
                    },
                )
            }
            enteredStart.await()

            val stopGeneration = coordinator.cancelPendingStarts()
            val stopping = launch(Dispatchers.Default) {
                coordinator.stopThrough(stopGeneration)
            }
            releaseStart.complete(Unit)

            val result = runCatching { starting.await() }
            stopping.join()
            assertTrue(result.exceptionOrNull() is ServerStartCancelledException)
            assertTrue(server.closed)
        }
    }

    @Test
    fun `an older stop command cannot close a newer active server`() = runBlocking {
        val coordinator = ServerLifecycleCoordinator()
        val obsoleteStopGeneration = coordinator.cancelPendingStarts()
        val server = FakeServer()
        val newerStartGeneration = coordinator.beginStart()

        coordinator.start(
            generation = newerStartGeneration,
            create = { server },
            start = { "started" },
        )
        coordinator.stopThrough(obsoleteStopGeneration)

        assertFalse(server.closed)
        coordinator.stopThrough(coordinator.cancelPendingStarts())
        assertTrue(server.closed)
    }

    @Test
    fun `a completed start is not current after a stop invalidates its publication`() = runBlocking {
        val coordinator = ServerLifecycleCoordinator()
        val startGeneration = coordinator.beginStart()

        coordinator.start(
            generation = startGeneration,
            create = { FakeServer() },
            start = { "started" },
        )
        val stopGeneration = coordinator.cancelPendingStarts()

        assertFalse(coordinator.isCurrentStart(startGeneration))
        coordinator.stopThrough(stopGeneration)
    }

    private class FakeServer : Closeable {
        @Volatile var closed = false

        override fun close() {
            closed = true
        }
    }
}
