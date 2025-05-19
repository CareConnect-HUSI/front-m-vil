package com.example.careconnect.main.inicioSesion

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.careconnect.R
import com.example.careconnect.main.listaPacientes.PacientesActivity
import com.example.careconnect.main.retroFit.RetrofitClient
import com.example.careconnect.main.retroFit.Credenciales
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private val PERMISO_UBICACION = 1000
    private val SOLICITUD_ENCENDER_GPS = 1001

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText

    private var email = ""
    private var password = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailInput = findViewById(R.id.email_edit_text)
        passwordInput = findViewById(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val forgotPassword = findViewById<TextView>(R.id.forgot_password)

        loginButton.setOnClickListener {
            email = emailInput.text.toString().trim()
            password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa todos los datos", Toast.LENGTH_SHORT).show()
            } else {
                verificarPermisosYGPS()
            }
        }

        forgotPassword.setOnClickListener {
            Toast.makeText(this, "Por favor contactar con el administrador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarPermisosYGPS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISO_UBICACION
            )
        } else {
            verificarGPS()
        }
    }

    private fun verificarGPS() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // GPS ya está encendido
            iniciarSesion()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, SOLICITUD_ENCENDER_GPS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "No se pudo abrir la configuración de GPS", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "GPS no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarSesion() {
        val credenciales = Credenciales(email = email, password = password)

        val call = RetrofitClient.noAuth.login(credenciales)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()

                    // Guardar JWT en SharedPreferences
                    val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
                    prefs.edit().putString("JWT_TOKEN", user?.token).apply()

                    Toast.makeText(this@LoginActivity, "Bienvenida ${user?.nombre}", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@LoginActivity, PacientesActivity::class.java)
                    intent.putExtra("NOMBRE_ENFERMERA", user?.nombre ?: "Enfermera")
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISO_UBICACION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                verificarGPS()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SOLICITUD_ENCENDER_GPS) {
            verificarGPS() // Verificamos de nuevo si el usuario activó el GPS
        }
    }
}
