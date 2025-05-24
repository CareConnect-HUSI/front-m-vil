package com.example.careconnect.main.visitaPaciente

import android.content.Context
import android.util.Log
import com.example.careconnect.main.registrarInsumos.InsumoConsumidoRequest
import com.example.careconnect.main.retroFit.RetrofitClient
import com.example.careconnect.main.retroFit.VisitStatusRequest
import com.example.careconnect.main.visitaPaciente.VisitaPaciente.SyncStorage
import com.example.careconnect.main.visitaPaciente.HorasVisitaRequest
import okhttp3.ResponseBody
import org.json.JSONArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object VisitaSyncUtils {
    fun sincronizarVisitasPendientes(context: Context) {
        val pendientes = SyncStorage.obtenerVisitasPendientes(context)
        if (pendientes.length() == 0) return

        val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null) ?: return
        val api = RetrofitClient.getInstance(token)

        for (i in 0 until pendientes.length()) {
            val visita = pendientes.getJSONObject(i)
            val visitaId = visita.getInt("visita_id")
            val llegada = visita.getString("hora_llegada")
            val salida = visita.getString("hora_salida")
            val insumos = visita.getJSONArray("insumos")
            val listaInsumos = mutableListOf<InsumoConsumidoRequest>()
            for (j in 0 until insumos.length()) {
                val insumo = insumos.getJSONObject(j)
                listaInsumos.add(InsumoConsumidoRequest(insumo.getInt("codigo"), insumo.getInt("cantidad")))
            }

            api.registrarHoras(visitaId, HorasVisitaRequest(llegada, salida)).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("SYNC", "Horas sincronizadas para visita $visitaId")
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("SYNC", "Fallo horas: $t")
                }
            })

            api.registrarInsumosConsumidos(visitaId, listaInsumos).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("SYNC", "Insumos sincronizados para visita $visitaId")
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("SYNC", "Fallo insumos: $t")
                }
            })

            api.updateVisitStatus(visitaId, VisitStatusRequest("COMPLETADA"))
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("SYNC", "Estado sincronizado visita $visitaId")
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("SYNC", "Fallo estado: $t")
                    }
                })
        }

        SyncStorage.limpiar(context)
    }
}