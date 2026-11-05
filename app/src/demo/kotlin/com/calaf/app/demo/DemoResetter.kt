package com.calaf.app.demo

/** Starts the demo over: wipes the store, signs the member out and runs [onReset] (for example, to show the tour again). */
class DemoResetter(
    private val store: DemoStore,
    private val session: DemoSession,
    private val onReset: () -> Unit = {},
) {
    fun reset() {
        store.reset()
        session.signOut()
        onReset()
    }
}
