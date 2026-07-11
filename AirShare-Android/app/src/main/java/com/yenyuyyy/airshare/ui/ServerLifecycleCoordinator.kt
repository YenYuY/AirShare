package com.yenyuyyy.airshare.ui

import java.io.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ServerStartCancelledException(cause: Throwable? = null) :
    Exception("Sharing start was cancelled", cause)

/** Serializes server ownership while allowing UI events to invalidate pending work immediately. */
internal class ServerLifecycleCoordinator {
    private val stateLock = Any()
    private val operations = Mutex()
    private var generation = 0L
    private var active: OwnedServer? = null

    fun beginStart(): Long = synchronized(stateLock) {
        nextGeneration()
    }

    fun cancelPendingStarts(): Long = synchronized(stateLock) {
        nextGeneration()
    }

    fun isCurrentStart(expectedGeneration: Long): Boolean = isCurrent(expectedGeneration)

    suspend fun <T : Closeable, R> start(
        generation: Long,
        create: () -> T,
        start: suspend (T) -> R,
    ): R = operations.withLock {
        ensureCurrent(generation)
        val previous = synchronized(stateLock) {
            active?.server.also { active = null }
        }
        if (previous != null) {
            try {
                previous.close()
            } catch (error: Throwable) {
                throwForGeneration(generation, error)
            }
        }
        ensureCurrent(generation)

        val server = try {
            create()
        } catch (error: Throwable) {
            throwForGeneration(generation, error)
        }
        val claimed = synchronized(stateLock) {
            if (this.generation == generation) {
                active = OwnedServer(generation, server)
                true
            } else {
                false
            }
        }
        if (!claimed) {
            runCatching(server::close)
            throw ServerStartCancelledException()
        }

        try {
            val result = start(server)
            if (!isCurrentOwner(generation, server)) {
                throw ServerStartCancelledException()
            }
            result
        } catch (error: Throwable) {
            val wasCurrentOwner = synchronized(stateLock) {
                val current = this.generation == generation && active?.server === server
                if (active?.server === server) active = null
                current
            }
            runCatching(server::close).exceptionOrNull()?.let(error::addSuppressed)
            if (error is CancellationException || error is ServerStartCancelledException) throw error
            if (!wasCurrentOwner) throw ServerStartCancelledException(error)
            throw error
        }
    }

    suspend fun stopThrough(stopGeneration: Long) {
        operations.withLock {
            val server = synchronized(stateLock) {
                active
                    ?.takeIf { it.generation < stopGeneration }
                    ?.server
                    ?.also { active = null }
            }
            server?.close()
        }
    }

    fun closeNow() {
        val stopGeneration = cancelPendingStarts()
        val server = synchronized(stateLock) {
            active
                ?.takeIf { it.generation < stopGeneration }
                ?.server
                ?.also { active = null }
        }
        runCatching { server?.close() }
    }

    private fun ensureCurrent(expectedGeneration: Long) {
        if (!isCurrent(expectedGeneration)) throw ServerStartCancelledException()
    }

    private fun isCurrent(expectedGeneration: Long): Boolean =
        synchronized(stateLock) { generation == expectedGeneration }

    private fun isCurrentOwner(expectedGeneration: Long, server: Closeable): Boolean =
        synchronized(stateLock) {
            generation == expectedGeneration && active?.server === server
        }

    private fun throwForGeneration(expectedGeneration: Long, error: Throwable): Nothing {
        if (error is CancellationException) throw error
        if (!isCurrent(expectedGeneration)) throw ServerStartCancelledException(error)
        throw error
    }

    private fun nextGeneration(): Long {
        check(generation < Long.MAX_VALUE) { "Server lifecycle generation exhausted" }
        generation += 1L
        return generation
    }

    private data class OwnedServer(
        val generation: Long,
        val server: Closeable,
    )
}
