package com.example.careconnect.main.inicioSesion

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<TextInputEditText>(R.id.email_edit_text)
        val passwordInput = findViewById<TextInputEditText>(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val forgotPassword = findViewById<TextView>(R.id.forgot_password)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

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
            continuarAlInicio()
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

    private fun continuarAlInicio() {
        val email = findViewById<TextInputEditText>(R.id.email_edit_text).text.toString()
        val password = findViewById<TextInputEditText>(R.id.password_edit_text).text.toString()

        val credenciales = Credenciales(email = email, password = password)

        val call = RetrofitClient.instance.login(credenciales)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null && !user.token.isNullOrBlank()) {
                        // Guardar el token en SharedPreferences
                        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putString("auth_token", user.token)
                            apply()
                        }
                        Log.d("LoginActivity", "Saved token: ${user.token}")

                        Toast.makeText(this@LoginActivity, "Bienvenida ${user.nombre}", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@LoginActivity, PacientesActivity::class.java)
                        intent.putExtra("NOMBRE_ENFERMERA", user.nombre)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Error: Token no recibido", Toast.LENGTH_SHORT).show()
                        Log.e("LoginActivity", "User or token is null: $user")
                    }
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Credenciales incorrectas"
                        500 -> "Error del servidor. Intenta de nuevo más tarde."
                        else -> "Error en el inicio de sesión: ${response.code()}"
                    }
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    Log.e("LoginActivity", "Error response: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("LoginActivity", "Network error: ${t.message}", t)
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
            verificarGPS()
        }
    }
}