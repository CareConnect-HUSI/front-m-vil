package com.example.careconnect.logInPage.listaPacientes

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R

class PacientesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pacientes)

        // Obtener el nombre de la enfermera desde el Intent
        val nombreEnfermera = intent.getStringExtra("NOMBRE_ENFERMERA") ?: "Enfermera"

        // Configurar el título
        val titleTextView = findViewById<TextView>(R.id.nurse_name)
        titleTextView.text = "$nombreEnfermera Ana"

        // Configurar RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPacientes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Lista de pacientes de prueba
        val pacientes = listOf(
            Paciente("Juan Pérez", "45", "Hipertensión", "08:30", "Calle 123, Bogotá"),
            Paciente("María Gómez", "50", "Diabetes", "09:00", "Carrera 45 #12, Medellín"),
            Paciente("Carlos López", "60", "Asma", "10:15", "Avenida Siempre Viva, Cali")
        )


        // Configurar adaptador
        val adapter = PacienteAdapter(this, pacientes)
        recyclerView.adapter = adapter
    }
}
