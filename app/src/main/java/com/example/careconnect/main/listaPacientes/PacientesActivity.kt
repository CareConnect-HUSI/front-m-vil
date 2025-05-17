package com.example.careconnect.main.listaPacientes

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
import com.example.careconnect.main.inicioSesion.LoginActivity
import com.example.careconnect.main.infoPacientes.DetallePacienteActivity
import com.example.careconnect.main.retroFit.PatientsListResponse
import com.example.careconnect.main.retroFit.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        titleTextView.text = "Enfermera $nombreEnfermera"

        // Configurar RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPacientes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Lista de pacientes de prueba
        val pacientes = listOf(
            Paciente("Juan Pérez", "Hipertensión", "08:30", "Cra. 68b #24-39, Bogotá", obtenerEstado("Juan Pérez")),
            Paciente("María Gómez", "Diabetes", "09:00", "Ak 7 #40 - 62, Bogotá", obtenerEstado("María Gómez")),
            Paciente("Carlos López", "Asma", "10:15", "Cra. 3 #2-49, El Colegio, Mesitas del Colegio, Cundinamarca", obtenerEstado("Carlos López"))
        )

        // Configurar adaptador
        val adapter = PacienteAdapter(this, pacientes)
        recyclerView.adapter = adapter
    }

    private fun obtenerEstado(nombre: String): String {
        val jsonData = DetallePacienteActivity.JsonUtils.loadData(this)
        val estado = jsonData.optJSONObject(nombre)?.optInt("estado_visita", 0) ?: 0
        return when (estado) {
            0 -> "PROGRAMADA"
            1 -> "EN_PROCESO"
            2 -> "FINALIZADA"
            else -> "NO_INICIADA"
        }
    }

    private fun cargarListaPacientes() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewPacientes)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val token = getSharedPreferences("auth_prefs", MODE_PRIVATE).getString("auth_token", null)

        val call = RetrofitClient.instance.getPatients("Bearer $token")
        call.enqueue(object : Callback<PatientsListResponse> {
            override fun onResponse(call: Call<PatientsListResponse>, response: Response<PatientsListResponse>) {
                if (response.isSuccessful) {
                    val patientsResponse = response.body()?.patients ?: emptyList()
                    val pacientes = patientsResponse.map { patient ->
                        Paciente(
                            nombre = patient.nombre,
                            diagnostico = patient.diagnostico,
                            hora = patient.hora,
                            direccion = patient.direccion,
                            estadoVisita = patient.estadoVisita,
                        )}
                    recyclerView.adapter = PacienteAdapter(this@PacientesActivity, pacientes)
                } else {
                    // Manejar error (por ejemplo, token inválido o endpoint no disponible)
                    Toast.makeText(this@PacientesActivity, "Error al cargar pacientes", Toast.LENGTH_SHORT).show()
                    if (response.code() == 401) {
                        cerrarSesion() // Redirigir a login si el token es inválido
                    }
                }
            }

            override fun onFailure(call: Call<PatientsListResponse>, t: Throwable) {
                Toast.makeText(this@PacientesActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
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
