package com.example.careconnect.logInPage.infoPacientes

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.careconnect.R
import com.example.careconnect.logInPage.registrarInsumos.RegistrarInsumoActivity
import java.util.*

class DetallePacienteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_paciente)

        // Obtener datos del intent
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val edadPaciente = intent.getStringExtra("EDAD_PACIENTE") ?: "Edad no disponible"
        val diagnosticoPaciente = intent.getStringExtra("DIAGNOSTICO_PACIENTE") ?: "Diagnóstico no disponible"

        // Asignar datos a la interfaz
        findViewById<TextView>(R.id.detalle_nombre_paciente).text = nombrePaciente

        // Hora de llegada y salida
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text)
        val botonHoraLlegada = findViewById<Button>(R.id.boton_hora_llegada)
        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text)
        val botonHoraSalida = findViewById<Button>(R.id.boton_hora_salida)

        // Lista de medicamentos disponibles
        val medicamentos = arrayOf("Paracetamol", "Ibuprofeno", "Amoxicilina", "Omeprazol")

        // Spinner para los medicamentos
        val spinner1 = findViewById<Spinner>(R.id.spinner1)
        val spinner2 = findViewById<Spinner>(R.id.spinner2)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, medicamentos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner1.adapter = adapter
        spinner2.adapter = adapter

        // Selector de hora para llegada
        botonHoraLlegada.setOnClickListener {
            mostrarTimePicker(horaLlegadaText)
        }

        // Selector de hora para salida
        botonHoraSalida.setOnClickListener {
            mostrarTimePicker(horaSalidaText)
        }

        // Botón para registrar insumos
        val registrarInsumosButton = findViewById<Button>(R.id.registrar_insumos_button)
        registrarInsumosButton.setOnClickListener {
            val intent = Intent(this, RegistrarInsumoActivity::class.java)
            startActivity(intent)
        }

        // Botón para guardar datos
        val guardarDatosButton = findViewById<Button>(R.id.guardar_datos_button)
        guardarDatosButton.setOnClickListener {
            Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarTimePicker(textView: TextView) {
        val calendario = Calendar.getInstance()
        val hora = calendario.get(Calendar.HOUR_OF_DAY)
        val minuto = calendario.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(this, { _, h, m ->
            textView.text = String.format("%02d:%02d", h, m)
        }, hora, minuto, true)

        timePicker.show()
    }
}

