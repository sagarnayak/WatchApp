package com.sagar.android.watchapp.ui.mainactivity

import android.util.Log
import com.google.android.gms.wearable.*
import com.sagar.android.watchapp.core.KeyWordsAndConstants.LOG_TAG

class DataListener : WearableListenerService() {

    override fun onDataChanged(dataEventBuffer: DataEventBuffer?) {
        super.onDataChanged(dataEventBuffer)

        dataEventBuffer?.forEach { dataEvent ->
            if (dataEvent.type == DataEvent.TYPE_CHANGED) {
                Log.i(
                    LOG_TAG,
                    """
                      received data >>
                      URI ${dataEvent.dataItem.uri}
                      PATH ${dataEvent.dataItem.uri.path}
                      MESSAGE ${DataMapItem.fromDataItem(
                        dataEvent.dataItem
                    ).dataMap.getString("message")}
                  """.trimIndent()
                )

                Wearable.getDataClient(this)
                    .deleteDataItems(dataEvent.dataItem.uri)
                    .addOnSuccessListener {
                        Log.i(
                            LOG_TAG,
                            "data layer clear"
                        )
                    }
            }
        }
    }
}