package com.example.careconnect.logInPage.infoPacientes

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.logInPage.LoginActivity
import com.example.careconnect.logInPage.listaPacientes.PacientesActivity
import com.example.careconnect.logInPage.registrarInsumos.Insumo
import com.example.careconnect.logInPage.registrarInsumos.InsumoAdapter
import com.google.android.material.textfield.TextInputLayout
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.*

class VisitaPaciente : AppCompatActivity() {

    companion object {
        const val ESTADO_NO_INICIADA = 0
        const val ESTADO_EN_PROGRESO = 1
        const val ESTADO_COMPLETADA = 2
    }

    object JsonUtils {

        private const val FILE_NAME = "visitas_pacientes.json"

        // Save data to JSON file
        fun saveData(context: Context, data: JSONObject) {
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(data.toString())
        }

        // Load data from JSON file
        fun loadData(context: Context): JSONObject {
            val file = File(context.filesDir, FILE_NAME)
            return if (file.exists()) {
                JSONObject(file.readText())
            } else {
                JSONObject() // Return an empty JSON object if the file doesn't exist
            }
        }
    }

    private lateinit var listaInsumos: List<Insumo>
    private lateinit var adapter: InsumoAdapter
    private lateinit var recyclerView: RecyclerView
    private val botonGuardar: Button by lazy { findViewById(R.id.guardar_datos) }
    private var estadoVisita: Int = ESTADO_NO_INICIADA

    override fun onPause() {
        super.onPause()
        if (estadoVisita == ESTADO_EN_PROGRESO) {
            guardarDatosTemporales()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visita_paciente)

        // Cargar estado de la visita
        estadoVisita = intent.getIntExtra("ESTADO_VISITA", ESTADO_NO_INICIADA)

        if (intent.getBooleanExtra("NUEVA_VISITA", false)) {
            estadoVisita = ESTADO_EN_PROGRESO
        }

        if (intent.getBooleanExtra("NUEVA_VISITA", false)) {
            val horaLlegada = intent.getStringExtra("HORA_LLEGADA") ?: ""
            val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"

            // Guardar temporalmente la hora de llegada en el JSON
            val jsonData = JsonUtils.loadData(this)
            val pacienteData = JSONObject().apply {
                put("hora_llegada", horaLlegada)
                put("estado_visita", ESTADO_EN_PROGRESO)
            }
            jsonData.put(nombrePaciente, pacienteData)
            JsonUtils.saveData(this, jsonData)

            findViewById<TextView>(R.id.hora_llegada_text).text = horaLlegada
        }

        // Obtener datos del intent
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val horaLlegada = intent.getStringExtra("HORA_LLEGADA") ?: ""
        val comentarios = intent.getStringExtra("COMENTARIOS_VISITA") ?: ""

        // Asignar datos a la interfaz
        findViewById<TextView>(R.id.detalle_nombre_paciente).text = nombrePaciente
        findViewById<TextView>(R.id.hora_llegada_text).text = horaLlegada

        // Hora de llegada y salida
        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text)

        // Obtener EditText desde el TextInputLayout
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosEditText = comentariosLayout.editText
        comentariosEditText?.setText(comentarios)

        val checkBox1 = findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkBox2)

        // Cargar datos guardados si existen
        val jsonData = JsonUtils.loadData(this)
        if (jsonData.has(nombrePaciente)) {
            val pacienteData = jsonData.getJSONObject(nombrePaciente)
            findViewById<TextView>(R.id.hora_llegada_text).text = pacienteData.optString("hora_llegada", "")
            comentariosEditText?.setText(pacienteData.optString("comentarios", ""))
            horaSalidaText.text = pacienteData.optString("hora_salida", "")
            checkBox1.isChecked = pacienteData.optBoolean("check_box_1", false)
            checkBox2.isChecked = pacienteData.optBoolean("check_box_2", false)

            val insumosJson = pacienteData.optString("insumos", "")
            val mapaInsumos = insumosJson.split(";").mapNotNull {
                val partes = it.split(":")
                if (partes.size == 2) {
                    val codigo = partes[0]
                    val cantidad = partes[1].toIntOrNull() ?: 0
                    codigo to cantidad
                } else null
            }.toMap()

            listaInsumos = listOf(
                Insumo("022", "Insumo Procedimiento #1", mapaInsumos["022"] ?: 0),
                Insumo("045", "Insumo Procedimiento #2", mapaInsumos["045"] ?: 0)
            )
        } else {
            listaInsumos = listOf(
                Insumo("022", "Insumo Procedimiento #1", 0),
                Insumo("045", "Insumo Procedimiento #2", 0)
            )
        }

        adapter = InsumoAdapter(listaInsumos.toMutableList())
        recyclerView = findViewById(R.id.listainsumos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        botonGuardar.setOnClickListener {
            mostrarDialogoGuardarDatos()
        }

        configurarInterfazSegunEstado()
    }

    private fun guardarDatosEnJson() {
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text).text.toString()
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosTexto = comentariosLayout?.editText?.text?.toString() ?: ""
        val checkBox1 = findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkBox2)
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
        val insumosJson = insumosUsados.joinToString(";") { "${it.codigo}:${it.cantidad}" }

        val horaSalidaView = findViewById<TextView>(R.id.hora_salida_text)
        var horaSalidaText = horaSalidaView.text.toString()

        if (horaSalidaText.isBlank()) {
            horaSalidaText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            horaSalidaView.text = horaSalidaText
        }

        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"

        val jsonData = JsonUtils.loadData(this)
        val pacienteData = JSONObject().apply {
            put("hora_llegada", horaLlegadaText)
            put("hora_salida", horaSalidaText)
            put("comentarios", comentariosTexto)
            put("check_box_1", checkBox1.isChecked)
            put("check_box_2", checkBox2.isChecked)
            put("insumos", insumosJson)
            put("estado_visita", ESTADO_COMPLETADA)
        }
        jsonData.put(nombrePaciente, pacienteData)
        JsonUtils.saveData(this, jsonData)

        estadoVisita = ESTADO_COMPLETADA
        configurarInterfazSegunEstado()
    }

    private fun guardarDatosTemporales() {
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text).text.toString()
        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text).text.toString()
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosTexto = comentariosLayout?.editText?.text?.toString() ?: ""
        val checkBox1 = findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkBox2)
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
        val insumosJson = insumosUsados.joinToString(";") { "${it.codigo}:${it.cantidad}" }

        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: return

        val jsonData = JsonUtils.loadData(this)
        val pacienteData = JSONObject().apply {
            put("hora_llegada", horaLlegadaText)
            put("hora_salida", horaSalidaText)
            put("comentarios", comentariosTexto)
            put("check_box_1", checkBox1.isChecked)
            put("check_box_2", checkBox2.isChecked)
            put("insumos", insumosJson)
            put("estado_visita", ESTADO_EN_PROGRESO)
        }
        jsonData.put(nombrePaciente, pacienteData)
        JsonUtils.saveData(this, jsonData)
    }

    private fun configurarInterfazSegunEstado() {
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosEditText = comentariosLayout?.editText
        when (estadoVisita) {
            ESTADO_NO_INICIADA -> {
                habilitarControles(true)
                adapter.setEditable(true)
                botonGuardar.text = "Guardar Visita"
            }
            ESTADO_EN_PROGRESO -> {
                findViewById<TextView>(R.id.hora_llegada_text).isEnabled = false
                habilitarControles(true)
                adapter.setEditable(true)
                botonGuardar.text = "Finalizar Visita"
            }
            ESTADO_COMPLETADA -> {
                habilitarControles(false)
                adapter.setEditable(false)
                comentariosEditText?.isEnabled = true
                botonGuardar.text = "Actualizar Comentarios"
            }
        }
    }

    private fun habilitarControles(habilitar: Boolean) {
        findViewById<CheckBox>(R.id.checkBox1).isEnabled = habilitar
        findViewById<CheckBox>(R.id.checkBox2).isEnabled = habilitar
        findViewById<RecyclerView>(R.id.listainsumos).isEnabled = habilitar
        findViewById<EditText>(R.id.search_input).isEnabled = habilitar
        findViewById<ImageView>(R.id.search_button).isEnabled = habilitar

        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        comentariosLayout?.isEnabled = habilitar
        comentariosLayout?.editText?.isEnabled = habilitar
    }

    private fun mostrarDialogoGuardarDatos() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Guardar Datos")
        builder.setMessage("¿Desea guardar los datos?")
        builder.setPositiveButton("Si") { _, _ ->
            dialogoConfirmacion()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun dialogoConfirmacion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmacion Guardar Datos")
        builder.setMessage("¿Esta seguro de que desea guardar los datos?, podra mofificar los comentarios despues")
        builder.setPositiveButton("Confirmar") { _, _ ->
            guardarDatosEnJson()
            datosGuardados()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun datosGuardados() {
        val intent = Intent(this, PacientesActivity::class.java)
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
        startActivity(intent)
        finish()
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
}