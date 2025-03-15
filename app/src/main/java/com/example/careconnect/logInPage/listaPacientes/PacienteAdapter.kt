package com.example.careconnect.logInPage.listaPacientes

import android.content.Intent
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.logInPage.infoPacientes.DetallePacienteActivity

data class Paciente(val nombre: String, val edad: String, val diagnostico: String, val hora: String, val direccion: String)

class PacienteAdapter(private val context: Context, private val pacientes: List<Paciente>) :
    RecyclerView.Adapter<PacienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarPaciente: ImageView = view.findViewById(R.id.avatar_paciente)
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

        holder.avatarPaciente.setOnClickListener {
            val intent = Intent(context, DetallePacienteActivity::class.java)
            intent.putExtra("NOMBRE_PACIENTE", paciente.nombre)
            intent.putExtra("EDAD_PACIENTE", paciente.edad)
            intent.putExtra("DIAGNOSTICO_PACIENTE", paciente.diagnostico)
            context.startActivity(intent)
        }

        // Acción al presionar el botón de "Ruta"
        holder.rutaButton.setOnClickListener {
            // Aquí puedes agregar la lógica para abrir Google Maps con la dirección
        }
    }

    override fun getItemCount() = pacientes.size
}
