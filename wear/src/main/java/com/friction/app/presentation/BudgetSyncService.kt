package com.friction.app.presentation

import com.friction.shared.BudgetConfig
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class BudgetSyncService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != "/budgets") continue
            val json = DataMapItem.fromDataItem(event.dataItem)
                .dataMap.getString("json") ?: continue
            BudgetStore.update(applicationContext, BudgetConfig.fromJson(json))
        }
    }
}