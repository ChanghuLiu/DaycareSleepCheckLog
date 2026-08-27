package com.daycare.sleepcheck.log.domain

/**
 * Guards one visible completion action from being submitted more than once.
 * A new session-screen entry gets a fresh gate key after a successful save.
 */
class CompletionSubmissionGate {
    private val inFlight = mutableSetOf<String>()

    @Synchronized
    fun tryStart(sessionId: String): Boolean = inFlight.add(sessionId)

    @Synchronized
    fun finish(sessionId: String) {
        inFlight.remove(sessionId)
    }
}
