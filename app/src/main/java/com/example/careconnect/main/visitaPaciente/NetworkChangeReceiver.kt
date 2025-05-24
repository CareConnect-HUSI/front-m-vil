package com.example.careconnect.main.visitaPaciente

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && VisitaPaciente.NetworkUtils.hayInternet(context)) {
            VisitaSyncUtils.sincronizarVisitasPendientes(context)

            // Emitir un broadcast personalizado
            val updateIntent = Intent("com.example.careconnect.NETWORK_RESTORED")
            context.sendBroadcast(updateIntent)
        }
    }
}