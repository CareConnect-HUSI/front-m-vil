package com.example.careconnect.logInPage.listaPacientes

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.logInPage.LoginActivity
import com.example.careconnect.logInPage.infoPacientes.DetallePacienteActivity

class PacientesActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        cargarListaPacientes()
    }

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
        val sharedPrefs = getSharedPreferences("DetallePacientePrefs", MODE_PRIVATE)

        // Configurar el título
        val titleTextView = findViewById<TextView>(R.id.nurse_name)
        titleTextView.text = "$nombreEnfermera Ana"

        // Configurar RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPacientes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Lista de pacientes de prueba
        val pacientes = listOf(
            Paciente("Juan Pérez", "45", "Hipertensión", "08:30", "Cra. 68b #24-39, Bogotá", obtenerEstado("Juan Pérez")),
            Paciente("María Gómez", "50", "Diabetes", "09:00", "Ak 7 #40 - 62, Bogotá", obtenerEstado("María Gómez")),
            Paciente("Carlos López", "60", "Asma", "10:15", "Cra. 3 #2-49, El Colegio, Mesitas del Colegio, Cundinamarca", obtenerEstado("Carlos López"))
        )

        // Configurar adaptador
        val adapter = PacienteAdapter(this, pacientes)
        recyclerView.adapter = adapter
    }

    private fun obtenerEstado(nombre: String): String {
        val jsonData = DetallePacienteActivity.JsonUtils.loadData(this)
        val estado = jsonData.optJSONObject(nombre)?.optInt("estado_visita", 0) ?: 0
        return when (estado) {
            0 -> "NO_INICIADA"
            1 -> "EN_PROCESO"
            2 -> "FINALIZADA"
            else -> "NO_INICIADA"
        }
    }

    private fun cargarListaPacientes() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPacientes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("DetallePacientePrefs", MODE_PRIVATE)

        val pacientes = listOf(
            Paciente("Juan Pérez", "45", "Hipertensión", "08:30", "Ak 7 #40 - 62, Bogotá", obtenerEstado("Juan Pérez")),
            Paciente("María Gómez", "50", "Diabetes", "09:00", "Ak 7 #40 - 62, Bogotá", obtenerEstado("María Gómez")),
            Paciente("Carlos López", "60", "Asma", "10:15", "Ak 7 #40 - 62, Bogotá", obtenerEstado("Carlos López"))
        )

        recyclerView.adapter = PacienteAdapter(this, pacientes)
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
