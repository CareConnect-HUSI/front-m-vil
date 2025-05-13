package com.example.careconnect.logInPage.infoPacientes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.careconnect.R
import com.example.careconnect.logInPage.LoginActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetallePacienteActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

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
        val botonVisita = findViewById<Button>(R.id.iniciarVisita)

        findViewById<TextView>(R.id.detalle_nombre_paciente).text = nombrePaciente

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
            verificarUbicacionYRegistrarHora(nombrePaciente, direccionPaciente, botonVisita)
        }
    }

    private fun verificarUbicacionYRegistrarHora(nombrePaciente: String, direccionPaciente: String, botonVisita: Button) {
        if (hayVisitaEnProgreso()) {
            Toast.makeText(this, "Debe finalizar la visita anterior antes de iniciar una nueva.", Toast.LENGTH_LONG).show()
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

                        if (distancia <= 100.0) {
                            val intent = Intent(this, VisitaPaciente::class.java)
                            intent.putExtra("NOMBRE_PACIENTE", nombrePaciente)

                            val estadoActual = cargarEstadoVisita(nombrePaciente)

                            if (estadoActual == VisitaPaciente.ESTADO_NO_INICIADA) {
                                intent.putExtra("NUEVA_VISITA", true)
                                val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                intent.putExtra("HORA_LLEGADA", horaActual)
                                guardarEstadoVisita(nombrePaciente, VisitaPaciente.ESTADO_EN_PROGRESO)
                            } else {
                                intent.putExtra("ESTADO_VISITA", estadoActual)
                            }

                            startActivity(intent)

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

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val loc1 = Location("").apply {
            latitude = lat1
            longitude = lon1
        }
        val loc2 = Location("").apply {
            latitude = lat2
            longitude = lon2
        }
        return loc1.distanceTo(loc2) / 1000
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