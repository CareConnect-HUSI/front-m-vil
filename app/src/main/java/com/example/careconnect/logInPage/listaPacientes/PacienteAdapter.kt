package com.example.careconnect.logInPage.listaPacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R

data class Paciente(val nombre: String, val hora: String, val direccion: String)

class PacienteAdapter(private val pacientes: List<Paciente>) :
    RecyclerView.Adapter<PacienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombrePaciente: TextView = view.findViewById(R.id.nombre_paciente)
        val horaAtencion: TextView = view.findViewById(R.id.hora_atencion)
        val direccion: TextView = view.findViewById(R.id.direccion_paciente)
        val rutaButton: ImageView = view.findViewById(R.id.ruta_button)
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

        // Acción al presionar el botón de "Ruta"
        holder.rutaButton.setOnClickListener {
            // Aquí puedes agregar la lógica para abrir Google Maps con la dirección
        }
    }

    override fun getItemCount() = pacientes.size
}
