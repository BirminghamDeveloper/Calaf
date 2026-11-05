package com.calaf.app

import android.os.Handler
import android.os.Looper
import com.calaf.app.demo.ReplyScheduler

/** Posts scripted demo replies on the main thread, where the rest of the demo store is used. */
class AndroidReplyScheduler : ReplyScheduler {
    private val handler = Handler(Looper.getMainLooper())

    override fun schedule(delayMillis: Long, action: () -> Unit) {
        handler.postDelayed(action, delayMillis)
    }
}
