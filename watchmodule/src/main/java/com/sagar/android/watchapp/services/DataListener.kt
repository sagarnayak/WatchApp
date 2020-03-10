package com.sagar.android.watchapp.services

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.wearable.*
import com.sagar.android.watchapp.core.KeyWordsAndConstants
import com.sagar.android.watchapp.core.MessageHandlerThread

class DataListener : WearableListenerService() {

    override fun onDataChanged(dataEventBuffer: DataEventBuffer?) {
        super.onDataChanged(dataEventBuffer)

        dataEventBuffer?.forEach { dataEvent ->
            if (dataEvent.type == DataEvent.TYPE_CHANGED) {
                Log.i(
                    KeyWordsAndConstants.LOG_TAG,
                    """
                      received data >>
                      URI ${dataEvent.dataItem.uri}
                      PATH ${dataEvent.dataItem.uri.path}
                      MESSAGE ${DataMapItem.fromDataItem(
                        dataEvent.dataItem
                    ).dataMap.getString("actionHandshakeRequest")}
                  """.trimIndent()
                )

                DataMapItem.fromDataItem(
                    dataEvent.dataItem
                )
                    .dataMap.getString("newKeyword")?.let {
                    MessageHandlerThread(
                        this,
                        dataEvent.dataItem.uri.host!!,
                        OnCompleteListener {
                        }
                    ).start()
                }

                Wearable.getDataClient(this)
                    .deleteDataItems(dataEvent.dataItem.uri)
                    .addOnSuccessListener {
                        Log.i(
                            KeyWordsAndConstants.LOG_TAG,
                            "data layer clear"
                        )
                    }


            }
        }
    }
}