package com.sagar.android.watchapp.core

import android.content.Context
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.wearable.Wearable
import com.sagar.android.watchapp.core.KeyWordsAndConstants.HANDSHAKE

class MessageHandlerThread(
    private val context: Context,
    private val host: String,
    private val onCompleteListener: OnCompleteListener<Int>
) :
    Thread() {

    override fun run() {
        super.run()

        Wearable.getMessageClient(context)
            .sendMessage(
                host,
                HANDSHAKE,
                "Hi wear device.".toByteArray()
            )
            .addOnCompleteListener(onCompleteListener)
    }
}