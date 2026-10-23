package com.vintagecam.core.gl.egl

import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Dedicated single-threaded executor for all OpenGL ES & EGL commands.
 *
 * Runs on a dedicated [HandlerThread] prioritized for display rendering
 * ([Process.THREAD_PRIORITY_DISPLAY]) to guarantee jitter-free 60 FPS viewfinder
 * throughput and serial EGL context affinity.
 */
class GlThreadExecutor(threadName: String = "GlRenderThread") : Executor {

    private val handlerThread: HandlerThread = HandlerThread(threadName, Process.THREAD_PRIORITY_DISPLAY).apply {
        start()
    }

    val handler: Handler = Handler(handlerThread.looper)

    override fun execute(command: Runnable) {
        handler.post(command)
    }

    /**
     * Dispatches an asynchronous block to the GL thread.
     */
    fun post(action: () -> Unit) {
        handler.post(action)
    }

    /**
     * Dispatches a block to the GL thread and blocks the calling thread until completion
     * or timeout.
     */
    fun postAndWait(timeoutMs: Long = 5000L, action: () -> Unit): Boolean {
        if (Thread.currentThread().id == handlerThread.id) {
            action()
            return true
        }

        val latch = CountDownLatch(1)
        var error: Throwable? = null

        val posted = handler.post {
            try {
                action()
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        if (!posted) {
            return false
        }

        val completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        if (!completed) {
            throw TimeoutException("GlThreadExecutor task timed out after ${timeoutMs}ms")
        }
        error?.let { throw RuntimeException("Exception during GL task execution", it) }
        return true
    }

    /**
     * Executes a callable on the GL thread and returns the result synchronously.
     */
    fun <T> postWithResult(timeoutMs: Long = 5000L, action: () -> T): T {
        if (Thread.currentThread().id == handlerThread.id) {
            return action()
        }

        val latch = CountDownLatch(1)
        var result: T? = null
        var error: Throwable? = null

        val posted = handler.post {
            try {
                result = action()
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        if (!posted) {
            throw IllegalStateException("GlThreadExecutor handler thread is not active or has exited")
        }

        val completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        if (!completed) {
            throw TimeoutException("GlThreadExecutor task timed out after ${timeoutMs}ms")
        }
        error?.let { throw RuntimeException("Exception during GL task execution", it) }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * Safely terminates the underlying render thread.
     */
    fun quitSafely() {
        handlerThread.quitSafely()
    }
}
