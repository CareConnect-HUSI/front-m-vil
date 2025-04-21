package com.example.careconnect.logInPage.infoPacientes

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
import com.example.careconnect.logInPage.LoginActivity
import com.example.careconnect.logInPage.infoPacientes.VisitaPaciente
import com.example.careconnect.logInPage.listaPacientes.PacientesActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetallePacienteActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var visitaIniciada = false

    override fun onResume() {
        super.onResume()
        actualizarBotonVisita()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_paciente)

        // Establecer tomo de coordenadas
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener datos del intent
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val edadPaciente = intent.getStringExtra("EDAD_PACIENTE") ?: "Edad no disponible"
        val direccionPaciente = intent.getStringExtra("DIRECCION_PACIENTE") ?: ""
        val telefono = intent.getStringExtra("TELEFONO_PACIENTE") ?: "Telefono no disponible"
        val botonVisita = findViewById<Button>(R.id.iniciarVisita)

        // Asignar datos a la interfaz
        findViewById<TextView>(R.id.detalle_nombre_paciente).text = nombrePaciente

        // Botón de volver atrás
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Volver a la pantalla anterior
        }

        // Botón de cerrar sesión
        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        val botonRuta = findViewById<Button>(R.id.ruta_button)
        botonRuta.setOnClickListener {
            abrirMapa(direccionPaciente)
        }

        botonVisita.setOnClickListener {
           verificarUbicacionYRegistrarHora(nombrePaciente, direccionPaciente, botonVisita)
        }
    }

    private fun verificarUbicacionYRegistrarHora(nombrePaciente: String, direccionPaciente: String, botonVisita : Button) {
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

                        if (distancia <= 1.0) {
                            val intent = Intent (this, VisitaPaciente::class.java)
                            intent.putExtra("NOMBRE_PACIENTE", nombrePaciente)

                            val estadoActual = cargarEstadoVisita(nombrePaciente)

                            // Cambiar el estado solo si es una nueva visita
                            if (estadoActual == VisitaPaciente.ESTADO_NO_INICIADA) {
                                // Nueva visita
                                intent.putExtra("NUEVA_VISITA", true)
                                val horaActual = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                intent.putExtra("HORA_LLEGADA", horaActual)
                                guardarEstadoVisita(nombrePaciente, VisitaPaciente.ESTADO_EN_PROGRESO)
                            } else {
                                // Visita existente
                                intent.putExtra("ESTADO_VISITA", estadoActual)
                            }

                            startActivity(intent)

                        } else {
                            Toast.makeText(this, "Estás a más de 1km del paciente", Toast.LENGTH_SHORT).show()
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

    private fun calcularDistancia(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val loc1 = Location("").apply {
            latitude = lat1
            longitude = lon1
        }
        val loc2 = Location("").apply {
            latitude = lat2
            longitude = lon2
        }
        return loc1.distanceTo(loc2) / 1000  // Devuelve la distancia en km
    }

    private fun guardarEstadoVisita(nombrePaciente: String, estado: Int) {
        val prefs = getSharedPreferences("DetallePacientePrefs", MODE_PRIVATE)
        prefs.edit().putInt("estado_visita_$nombrePaciente", estado).apply()
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

    private fun cargarEstadoVisita(nombrePaciente: String): Int {
        val prefs = getSharedPreferences("DetallePacientePrefs", MODE_PRIVATE)
        // Primero intentamos cargar como Int (nuevo sistema)
        if (prefs.contains("estado_visita_$nombrePaciente")) {
            return prefs.getInt("estado_visita_$nombrePaciente", VisitaPaciente.ESTADO_NO_INICIADA)
        }

        // Migración: si existe el antiguo booleano, lo convertimos
        if (prefs.contains("visita_iniciada_$nombrePaciente")) {
            val iniciada = prefs.getBoolean("visita_iniciada_$nombrePaciente", false)
            val estado = if (iniciada) VisitaPaciente.ESTADO_EN_PROGRESO else VisitaPaciente.ESTADO_NO_INICIADA
            guardarEstadoVisita(nombrePaciente, estado)
            return estado
        }
        return VisitaPaciente.ESTADO_NO_INICIADA
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
        // Redirigir a la pantalla de login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun abrirMapa(direccion: String) {
        if (direccion.isNotEmpty()) {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(direccion)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps") // Asegura que se abra en Google Maps
            startActivity(intent)
        } else {
            Toast.makeText(this, "Dirección no disponible", Toast.LENGTH_SHORT).show()
        }
    }
}

