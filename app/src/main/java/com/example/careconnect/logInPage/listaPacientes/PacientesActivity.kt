package com.example.careconnect.logInPage.listaPacientes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.logInPage.LoginActivity

class PacientesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pacientes)

        // Botón de volver atrás
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        // Botón de cerrar sesión
        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

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
            Paciente("Juan Pérez", "45", "Hipertensión", "08:30", "Cra. 68b #24-39, Bogotá"),
            Paciente("María Gómez", "50", "Diabetes", "09:00", "Ak 7 #40 - 62, Bogotá"),
            Paciente("Carlos López", "60", "Asma", "10:15", "Cra. 3 #2-49, El Colegio, Mesitas del Colegio, Cundinamarca")
        )

        // Configurar adaptador
        val adapter = PacienteAdapter(this, pacientes)
        recyclerView.adapter = adapter
    }

    private fun mostrarDialogoCerrarSesion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cerrar sesión")
        builder.setMessage("La informacion no guardada sera eliminada, ¿Estás seguro de que quieres cerrar sesión?")
        builder.setPositiveButton("Sí") { _, _ ->
            cerrarSesion()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun cerrarSesion() {
        // Redirigir a la pantalla de login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
