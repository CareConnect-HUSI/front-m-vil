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
            Paciente("Juan Pérez", "10:00 AM", "Calle 123"),
            Paciente("María López", "11:30 AM", "Av. Central 456"),
            Paciente("Carlos Gómez", "02:00 PM", "Carrera 789"),
            Paciente("Ana Torres", "04:15 PM", "Diagonal 101"),
        )

        // Configurar adaptador
        val adapter = PacienteAdapter(pacientes)
        recyclerView.adapter = adapter
    }
}
