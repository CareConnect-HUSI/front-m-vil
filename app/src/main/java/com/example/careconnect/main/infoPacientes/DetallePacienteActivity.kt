package com.example.careconnect.main.infoPacientes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.careconnect.R
import com.example.careconnect.main.inicioSesion.LoginActivity
import com.example.careconnect.main.retroFit.VisitStatusRequest
import com.example.careconnect.main.visitaPaciente.VisitaPaciente
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetallePacienteActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var visitaId: Int = -1


    object JsonUtils {
        private const val FILE_NAME = "visitas_pacientes.json"

        fun saveData(context: AppCompatActivity, data: JSONObject) {
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(data.toString())
        }

        fun loadData(context: AppCompatActivity): JSONObject {
            val file = File(context.filesDir, FILE_NAME)
            return if (file.exists()) {
                JSONObject(file.readText())
            } else {
                JSONObject()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarBotonVisita()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_paciente)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val direccionPaciente = intent.getStringExtra("DIRECCION_PACIENTE") ?: ""
        val telefonoPaciente = intent.getStringExtra("TELEFONO_PACIENTE") ?: ""
         visitaId = intent.getIntExtra("VISITA_ID", -1)
        if (visitaId == -1){
            Log.e("DEBUG_VISITA", "visitaId invalido recibido")
        }
        val botonVisita = findViewById<Button>(R.id.iniciarVisita)

        findViewById<TextView>(R.id.detalle_nombre_paciente).text = nombrePaciente

        val direccionTextView = findViewById<TextView>(R.id.direccion_paciente)
        val telefonoTextView = findViewById<TextView>(R.id.telefono_paciente)
        direccionTextView.text = direccionPaciente
        telefonoTextView.text = telefonoPaciente

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        findViewById<Button>(R.id.ruta_button).setOnClickListener {
            abrirMapa(direccionPaciente)
        }

        botonVisita.setOnClickListener {
            verificarUbicacionYRegistrarHora(visitaId, nombrePaciente, direccionPaciente, telefonoPaciente, botonVisita)
        }
    }

    private fun verificarUbicacionYRegistrarHora(visitaId: Int, nombrePaciente: String, direccionPaciente: String, telefonoPaciente: String, botonVisita: Button) {
        val hayOtraVisitaEnProgreso = verificarOtraVisitaEnProgreso(nombrePaciente)
        val estadoActual = cargarEstadoVisita(nombrePaciente)

        if (estadoActual == VisitaPaciente.ESTADO_NO_INICIADA && hayOtraVisitaEnProgreso) {
            Toast.makeText(this, "No puede iniciar una nueva visita hasta finalizar la anterior.", Toast.LENGTH_LONG).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latUsuario = location.latitude
                val lonUsuario = location.longitude

                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val resultados = geocoder.getFromLocationName(direccionPaciente, 1)

                    if (resultados != null && resultados.isNotEmpty()) {
                        val direccionLocalizada = resultados[0]
                        val latPaciente = direccionLocalizada.latitude
                        val lonPaciente = direccionLocalizada.longitude

                        val distancia = calcularDistancia(latUsuario, lonUsuario, latPaciente, lonPaciente)

                        if (distancia <= 500.0) {
                            if (estadoActual == VisitaPaciente.ESTADO_NO_INICIADA) {
                                updateVisitStatusToEnProgreso(visitaId, nombrePaciente)
                                guardarEstadoVisita(nombrePaciente, VisitaPaciente.ESTADO_EN_PROGRESO)
                            } else {
                                proceedToVisitaPaciente(visitaId, nombrePaciente, estadoActual)
                            }
                        } else {
                            Toast.makeText(this, "No te encuentras en el lugar de la visita", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "No se pudo localizar la dirección del paciente", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al buscar dirección", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateVisitStatusToEnProgreso(visitaId: Int, nombrePaciente: String) {
        if (visitaId == -1) {
            Log.e("STATUS_API", "Invalid visitaId: $visitaId")
            Toast.makeText(this, "ID de visita inválido", Toast.LENGTH_LONG).show()
            return
        }

        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)
        if (token == null) {
            Log.e("STATUS_API", "No token found")
            Toast.makeText(this, "No se encontró el token de autenticación", Toast.LENGTH_LONG).show()
            return
        }

        val api = com.example.careconnect.main.retroFit.RetrofitClient.getInstance(token)
        val statusRequest = VisitStatusRequest(estadoVisita = "EN_PROGRESO")
        api.updateVisitStatus(visitaId, statusRequest).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("STATUS_API", "Visit status updated to EN_PROGRESO")
                    Toast.makeText(this@DetallePacienteActivity, "Estado de visita actualizado a En Progreso", Toast.LENGTH_SHORT).show()
                    // Update local JSON state
                    guardarEstadoVisita(nombrePaciente, VisitaPaciente.ESTADO_EN_PROGRESO)
                    // Proceed to VisitaPaciente activity
                    proceedToVisitaPaciente(visitaId, nombrePaciente, VisitaPaciente.ESTADO_EN_PROGRESO)
                } else {
                    Log.e("STATUS_API", "Error updating status: ${response.code()}, ${response.errorBody()?.string()}")
                    Toast.makeText(this@DetallePacienteActivity, "Error al actualizar estado (${response.code()})", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("STATUS_API", "Network failure updating status", t)
                Toast.makeText(this@DetallePacienteActivity, "Fallo de red al actualizar estado", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun proceedToVisitaPaciente(visitaId: Int, nombrePaciente: String, estadoActual: Int) {
        val intent = Intent(this, VisitaPaciente::class.java)
        intent.putExtra("NOMBRE_PACIENTE", nombrePaciente)
        intent.putExtra("VISITA_ID", visitaId)

        if (estadoActual == VisitaPaciente.ESTADO_NO_INICIADA) {
            intent.putExtra("NUEVA_VISITA", true)
            val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            intent.putExtra("HORA_LLEGADA", horaActual)
        } else {
            intent.putExtra("ESTADO_VISITA", estadoActual)
        }

        startActivity(intent)
    }

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val loc1 = Location("").apply {
            latitude = lat1
            longitude = lon1

        }
        val loc2 = Location("").apply {
            latitude = lat2
            longitude = lon2
        }
        return loc1.distanceTo(loc2)
    }

    private fun guardarEstadoVisita(nombrePaciente: String, estado: Int) {
        val jsonData = JsonUtils.loadData(this)
        val pacienteData = jsonData.optJSONObject(nombrePaciente) ?: JSONObject()
        pacienteData.put("estado_visita", estado)
        jsonData.put(nombrePaciente, pacienteData)
        JsonUtils.saveData(this, jsonData)
    }

    private fun cargarEstadoVisita(nombrePaciente: String): Int {
        val jsonData = JsonUtils.loadData(this)
        return jsonData.optJSONObject(nombrePaciente)?.optInt("estado_visita", VisitaPaciente.ESTADO_NO_INICIADA)
            ?: VisitaPaciente.ESTADO_NO_INICIADA
    }

    private fun actualizarBotonVisita() {
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val estado = cargarEstadoVisita(nombrePaciente)

        findViewById<Button>(R.id.iniciarVisita).text = when (estado) {
            VisitaPaciente.ESTADO_NO_INICIADA -> "Iniciar Visita"
            VisitaPaciente.ESTADO_EN_PROGRESO -> "Continuar Visita"
            VisitaPaciente.ESTADO_COMPLETADA -> "Ver Visita"
            else -> "Iniciar Visita"
        }
    }

    private fun hayVisitaEnProgreso(): Boolean {
        val jsonData = VisitaPaciente.JsonUtils.loadData(this)
        for (key in jsonData.keys()) {
            val visita = jsonData.getJSONObject(key)
            if (visita.optInt("estado_visita", VisitaPaciente.ESTADO_NO_INICIADA) == VisitaPaciente.ESTADO_EN_PROGRESO) {
                return true
            }
        }
        return false
    }

    fun verificarOtraVisitaEnProgreso(nombreActual: String): Boolean {
        val jsonData = JsonUtils.loadData(this)
        for (key in jsonData.keys()) {
            if (key != nombreActual) {
                val visita = jsonData.getJSONObject(key)
                val estado = visita.optInt("estado_visita", VisitaPaciente.ESTADO_NO_INICIADA)
                if (estado == VisitaPaciente.ESTADO_EN_PROGRESO) {
                    return true
                }
            }
        }
        return false
    }

    private fun mostrarDialogoCerrarSesion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cerrar sesión")
        builder.setMessage("¿Estás seguro de que quieres cerrar sesión?")
        builder.setPositiveButton("Sí") { _, _ ->
            cerrarSesion()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun cerrarSesion() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun abrirMapa(direccion: String) {
        if (direccion.isNotEmpty()) {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(direccion)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } else {
            Toast.makeText(this, "Dirección no disponible", Toast.LENGTH_SHORT).show()
        }
    }
}