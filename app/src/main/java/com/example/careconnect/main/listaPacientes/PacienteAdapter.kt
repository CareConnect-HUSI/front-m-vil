package com.example.careconnect.main.listaPacientes

import android.content.Intent
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.main.dataUsuarios.Paciente
import com.example.careconnect.main.infoPacientes.DetallePacienteActivity
import com.example.careconnect.main.retroFit.RetrofitClient
import com.example.careconnect.main.visitaPaciente.EstadoVisitaResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PacienteAdapter(private val context: Context, private val pacientes: List<Paciente>) :
    RecyclerView.Adapter<PacienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombrePaciente: TextView = view.findViewById(R.id.nombre_paciente)
        val horaAtencion: TextView = view.findViewById(R.id.hora_atencion)
        val direccion: TextView = view.findViewById(R.id.direccion_paciente)
        val telefono: TextView = view.findViewById(R.id.telefono_paciente)
        val botonDetallePaciente: LinearLayout = view.findViewById(R.id.boton_detalle_paciente)
        val iconoCompletado: ImageView = view.findViewById(R.id.icono_estado_visita_completada)
        val iconoEspera: ImageView = view.findViewById(R.id.icono_estado_visita_espera)
        val iconoNoIniciada: ImageView = view.findViewById(R.id.icono_estado_visita_noIniciada)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val paciente = pacientes[position]
        holder.nombrePaciente.text = paciente.nombre
        holder.horaAtencion.text = "Hora: ${paciente.hora}"
        holder.direccion.text = "Dirección: ${paciente.direccion}"
        holder.telefono.text = "Teléfono: ${paciente.telefono}"

        // Obtener estado real desde backend
        val prefs = context.getSharedPreferences("SessionPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)
        if (token != null) {
            val api = RetrofitClient.getInstance(token)
            api.obtenerEstadoVisita(paciente.visitaId).enqueue(object : Callback<EstadoVisitaResponse> {
                override fun onResponse(
                    call: Call<EstadoVisitaResponse>,
                    response: Response<EstadoVisitaResponse>
                ) {
                    if (response.isSuccessful) {
                        val estadoReal = response.body()?.estado ?: "PROGRAMADA"
                        mostrarEstado(holder, estadoReal)

                        holder.botonDetallePaciente.setOnClickListener {
                            val intent = Intent(context, DetallePacienteActivity::class.java)
                            intent.putExtra("NOMBRE_PACIENTE", paciente.nombre)
                            intent.putExtra("DIRECCION_PACIENTE", paciente.direccion)
                            intent.putExtra("TELEFONO_PACIENTE", paciente.telefono)
                            intent.putExtra("VISITA_ID", paciente.visitaId)
                            intent.putExtra("ESTADO_VISITA", when (estadoReal) {
                                "PROGRAMADA" -> 0
                                "EN_PROCESO" -> 1
                                "FINALIZADA" -> 2
                                else -> 0
                            })
                            intent.putExtra("NUEVA_VISITA", estadoReal == "PROGRAMADA")
                            context.startActivity(intent)
                        }
                    } else {
                        Log.e("ESTADO_API", "Error al obtener estado visita: ${response.code()}")
                        mostrarEstado(holder, paciente.estadoVisita)
                    }
                }

                override fun onFailure(call: Call<EstadoVisitaResponse>, t: Throwable) {
                    Log.e("ESTADO_API", "Fallo red al obtener estado", t)
                    mostrarEstado(holder, paciente.estadoVisita)
                }
            })
        } else {
            mostrarEstado(holder, paciente.estadoVisita)
        }
    }

    private fun mostrarEstado(holder: ViewHolder, estado: String) {
        holder.iconoCompletado.visibility = View.GONE
        holder.iconoEspera.visibility = View.GONE
        holder.iconoNoIniciada.visibility = View.GONE

        when (estado) {
            "PROGRAMADA" -> holder.iconoNoIniciada.visibility = View.VISIBLE
            "EN_PROGRESO" -> holder.iconoEspera.visibility = View.VISIBLE
            "COMPLETADA" -> holder.iconoCompletado.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = pacientes.size
}
