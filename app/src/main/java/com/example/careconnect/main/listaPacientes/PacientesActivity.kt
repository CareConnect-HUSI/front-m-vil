package com.example.careconnect.main.listaPacientes

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.main.inicioSesion.LoginActivity
import com.example.careconnect.main.infoPacientes.DetallePacienteActivity
import com.example.careconnect.main.retroFit.RetrofitClient
import com.example.careconnect.main.dataUsuarios.Paciente
import com.example.careconnect.main.visitaPaciente.NetworkRestoredReceiver
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class PacientesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var nombreEnfermera: String
    private lateinit var jwtToken: String
    private lateinit var networkRestoredReceiver: BroadcastReceiver
    private lateinit var adapter: PacienteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pacientes)

        findViewById<ImageView>(R.id.btnBack).visibility = View.INVISIBLE
        findViewById<ImageView>(R.id.btnLogout).setOnClickListener { mostrarDialogoCerrarSesion() }

        nombreEnfermera = intent.getStringExtra("NOMBRE_ENFERMERA") ?: "Enfermera"
        findViewById<TextView>(R.id.nurse_name).text = nombreEnfermera

        val fechaActual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        findViewById<TextView>(R.id.fechaHoy).text = fechaActual

        recyclerView = findViewById(R.id.recyclerViewPacientes)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        jwtToken = prefs.getString("JWT_TOKEN", null) ?: ""

        if (jwtToken.isBlank()) {
            Toast.makeText(this, "Token no encontrado. Inicia sesión de nuevo.", Toast.LENGTH_SHORT).show()
            cerrarSesion()
        } else {
            cargarListaPacientes()
        }
    }

    override fun onResume() {
        super.onResume()
        if (jwtToken.isNotBlank()) {
            cargarListaPacientes()
        }

        networkRestoredReceiver = NetworkRestoredReceiver {
            actualizarListaDePacientes()
        }
        registerReceiver(networkRestoredReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(networkRestoredReceiver)
    }

    private fun cargarListaPacientes() {
        lifecycleScope.launch {
            try {
                val api = RetrofitClient.getInstance(jwtToken)
                val pacientesFromApi = api.getPacientesAsignados("Bearer $jwtToken")
                val pacientes = pacientesFromApi.map { paciente ->
                    paciente.copy(estadoVisita = obtenerEstado(paciente.nombre))
                }
                Log.d("DEBUG_PACIENTES", pacientes.joinToString("\n"))
                adapter = PacienteAdapter(this@PacientesActivity, pacientes)
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@PacientesActivity, "Error al cargar pacientes", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun actualizarListaDePacientes() {
        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null) ?: return
        val api = RetrofitClient.getInstance(token)

        lifecycleScope.launch {
            try {
                val pacientes = api.getPacientesAsignados("Bearer $token")
                val pacientesActualizados = pacientes.map { paciente ->
                    paciente.copy(estadoVisita = obtenerEstado(paciente.nombre))
                }
                (recyclerView.adapter as? PacienteAdapter)?.actualizarLista(pacientesActualizados)
            } catch (e: Exception) {
                Log.e("PACIENTES_SYNC", "Error al actualizar pacientes tras reconexión", e)
            }
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("La información no guardada será eliminada. ¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ -> cerrarSesion() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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

    private fun hayInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
