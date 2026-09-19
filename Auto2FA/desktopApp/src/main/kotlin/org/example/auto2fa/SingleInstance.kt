package org.example.auto2fa

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException

/**
 * Keeps the app to a single running instance per user. Needed once autostart can launch the app
 * hidden in the tray (see `AUTOSTART_ARG`/`main.kt`) -- without this, opening the app again later
 * via the Start Menu/taskbar search would start a second process that immediately fails to bind
 * `AUTO2FA_SERVER_PORT` (already held by the first instance) instead of just showing the window
 * that's already running.
 *
 * Uses a plain file lock rather than the OTP server's own port, so this works even before the TLS
 * keystore/server exist yet, and a tiny loopback-only socket as the "show yourself" signal --
 * deliberately a different port than `AUTO2FA_SERVER_PORT` (that one accepts real OTP pushes from
 * the phone; this one only ever accepts a connection from this same machine).
 */
object SingleInstance {
    private val appDir = File(System.getProperty("user.home"), ".auto2fa")
    private val lockFile = File(appDir, "instance.lock")
    private const val SIGNAL_PORT = 48432

    // Held for the process's lifetime -- an unreferenced FileChannel is eligible for GC, and a
    // GC'd/closed channel releases the lock out from under a still-running app.
    private var lockChannel: FileChannel? = null

    /**
     * Returns true if this process should proceed to start normally. Returns false if another
     * instance is already running -- in which case *that* instance has just been asked (via its
     * own [onShowRequested], not this call's) to show its window, and this process should exit
     * immediately without starting a server, generating keys, or opening anything.
     */
    fun tryAcquireOrNotifyExisting(onShowRequested: () -> Unit): Boolean {
        appDir.mkdirs()
        val channel = RandomAccessFile(lockFile, "rw").channel
        val lock = try {
            channel.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        }

        if (lock == null) {
            channel.close()
            notifyExistingInstance()
            return false
        }

        lockChannel = channel
        listenForShowRequests(onShowRequested)
        return true
    }

    private fun notifyExistingInstance() {
        try {
            Socket(InetAddress.getLoopbackAddress(), SIGNAL_PORT).close()
        } catch (_: Exception) {
            // Best-effort -- if the running instance's listener isn't up for some reason, there's
            // nothing more a second launch can usefully do beyond exiting quietly.
        }
    }

    private fun listenForShowRequests(onShowRequested: () -> Unit) {
        val server = ServerSocket(SIGNAL_PORT, 50, InetAddress.getLoopbackAddress())
        Thread({
            while (true) {
                try {
                    server.accept().close()
                    onShowRequested()
                } catch (_: Exception) {
                    // Keep serving later requests even if a single one misbehaves.
                }
            }
        }, "SingleInstance-Listener").apply { isDaemon = true }.start()
    }
}
