package com.friction.app.presentation

import android.content.Context
import com.friction.shared.Transaction
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

object PhoneSync {
    fun sendTransaction(context: Context, tx: Transaction) {
        val request = PutDataMapRequest.create("/transaction/${tx.id}").apply {
            dataMap.putString("json", tx.toJson())
            dataMap.putLong("ts", tx.timestamp)
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request)
    }
}