package com.friction.app

import com.friction.shared.Transaction
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class SyncService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path ?: continue
            if (!path.startsWith("/transaction/")) continue
            val json = DataMapItem.fromDataItem(event.dataItem)
                .dataMap.getString("json") ?: continue
            TransactionRepository.add(applicationContext, Transaction.fromJson(json))
        }
    }
}