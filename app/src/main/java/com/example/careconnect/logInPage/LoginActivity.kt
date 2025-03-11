package com.example.careconnect.logInPage

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.careconnect.R

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailInput = findViewById<EditText>(R.id.email_input)
        val passwordInput = findViewById<EditText>(R.id.password_input)
        val loginButton = findViewById<Button>(R.id.login_button)
        val forgotPassword = findViewById<TextView>(R.id.forgot_password)

        // Botón de iniciar sesión
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa todos los datos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()
                // Aquí puedes agregar lógica para autenticar al usuario
            }
        }

        // Evento de clic en "Olvidé mi contraseña"
        forgotPassword.setOnClickListener {
            Toast.makeText(this, "Por favor contactar con el administrador", Toast.LENGTH_SHORT).show()
        }
    }
}
