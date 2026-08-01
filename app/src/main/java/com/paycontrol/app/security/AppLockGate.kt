package com.paycontrol.app.security

/**
 * Evita que el PIN se active al abrir SAF / share / biometría
 * (actividades externas que disparan ON_STOP en MainActivity).
 */
object AppLockGate {
    @Volatile
    private var suppressUntilMs: Long = 0L

    fun suppressFor(durationMs: Long = 180_000L) {
        suppressUntilMs = System.currentTimeMillis() + durationMs.coerceAtLeast(5_000L)
    }

    fun shouldSuppressLock(): Boolean =
        System.currentTimeMillis() < suppressUntilMs

    fun clear() {
        suppressUntilMs = 0L
    }
}
