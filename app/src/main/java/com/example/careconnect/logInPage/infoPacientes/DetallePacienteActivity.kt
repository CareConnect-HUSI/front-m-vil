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

        val botonVisita = findViewById<Button>(R.id.iniciarVisita)
        botonVisita.setOnClickListener {
            val intent = Intent(this, VisitaPaciente::class.java)
            intent.putExtra("NOMBRE_ENFERMERA", "Enfermera")
            startActivity(intent)
            finish()
        }
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

