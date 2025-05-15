package com.example.careconnect.main.listaPacientes

import android.content.Intent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.main.infoPacientes.DetallePacienteActivity

data class Paciente(
    val nombre: String,
    val edad: String,
    val diagnostico: String,
    val hora: String,
    val direccion: String,
    val estadoVisita: String
)

class PacienteAdapter(private val context: Context, private val pacientes: List<Paciente>) :
    RecyclerView.Adapter<PacienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombrePaciente: TextView = view.findViewById(R.id.nombre_paciente)
        val horaAtencion: TextView = view.findViewById(R.id.hora_atencion)
        val direccion: TextView = view.findViewById(R.id.direccion_paciente)
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

        holder.botonDetallePaciente.setOnClickListener {
            val intent = Intent(context, DetallePacienteActivity::class.java)
            intent.putExtra("NOMBRE_PACIENTE", paciente.nombre)
            intent.putExtra("EDAD_PACIENTE", paciente.edad)
            intent.putExtra("DIAGNOSTICO_PACIENTE", paciente.diagnostico)
            intent.putExtra("DIRECCION_PACIENTE", paciente.direccion)
            context.startActivity(intent)
        }

        mostrarEstado(holder, paciente.estadoVisita)
    }

    private fun mostrarEstado(holder: ViewHolder, estado: String) {
        holder.iconoCompletado.visibility = View.GONE
        holder.iconoEspera.visibility = View.GONE
        holder.iconoNoIniciada.visibility = View.GONE

        when (estado) {
            "NO_INICIADA" -> holder.iconoNoIniciada.visibility = View.VISIBLE
            "EN_PROCESO" -> holder.iconoEspera.visibility = View.VISIBLE
            "FINALIZADA" -> holder.iconoCompletado.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = pacientes.size
}
