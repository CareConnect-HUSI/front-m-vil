package com.example.careconnect.logInPage.infoPacientes

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

    private lateinit var listaInsumos: List<Insumo>
    private lateinit var adapter: InsumoAdapter
    private lateinit var recyclerView: RecyclerView
    private val botonGuardar: Button by lazy { findViewById(R.id.guardar_datos) }
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageView
    private lateinit var sharedPreferences: SharedPreferences
    private var estadoVisita: Int = ESTADO_NO_INICIADA

    override fun onPause() {
        super.onPause()
        if (estadoVisita == ESTADO_EN_PROGRESO) {
            // Guardar automáticamente los datos actuales (sin marcar como completada)
            guardarDatosTemporales()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visita_paciente)

        // Cargar SharedPreferences
        sharedPreferences = getSharedPreferences("VisitaPacientePrefs", Context.MODE_PRIVATE)

        // Cargar estado de la visita
        estadoVisita = intent.getIntExtra("ESTADO_VISITA", ESTADO_NO_INICIADA)

        if (intent.getBooleanExtra("NUEVA_VISITA", false)) {
            estadoVisita = ESTADO_EN_PROGRESO
            sharedPreferences.edit().putInt("estado_visita", estadoVisita).apply()
        }

        // Obtener datos del intent
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val horaLlegada = intent.getStringExtra("HORA_LLEGADA")
            ?: sharedPreferences.getString("hora_llegada", "") ?: ""
        val comentarios = intent.getStringExtra("COMENTARIOS_VISITA") ?: ""

        // Asignar datos a la interfaz
        findViewById<TextView>(R.id.detalle_nombre_paciente).text = nombrePaciente
        findViewById<TextView>(R.id.hora_llegada_text).text = horaLlegada

        // Hora de llegada y salida
        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text)
        val botonHoraSalida = findViewById<ImageView>(R.id.boton_hora_salida)

        // Obtener EditText desde el TextInputLayout
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosEditText = comentariosLayout.editText
        comentariosEditText?.setText(comentarios)

        val checkBox1 = findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkBox2)

        // Cargar datos guardados si existen
        val comentariosGuardados = sharedPreferences.getString("comentarios", "")
        val horaSalidaGuardada = sharedPreferences.getString("hora_salida", "")
        checkBox1.isChecked = sharedPreferences.getBoolean("check_box_1", false)
        checkBox2.isChecked = sharedPreferences.getBoolean("check_box_2", false)
        val insumosGuardados = sharedPreferences.getString("insumos", "")

        if (!comentariosGuardados.isNullOrEmpty()) {
            comentariosEditText?.setText(comentariosGuardados)
        }
        if (!horaSalidaGuardada.isNullOrEmpty()) {
            horaSalidaText.text = horaSalidaGuardada
        }

        // Insumos
        recyclerView = findViewById(R.id.listainsumos)
        searchInput = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)

        val mapaInsumos = insumosGuardados?.split(",")?.mapNotNull {
            val partes = it.split(":")
            if (partes.size == 2) {
                val codigo = partes[0]
                val cantidad = partes[1].toIntOrNull() ?: 0
                codigo to cantidad
            } else null
        }?.toMap() ?: emptyMap()

        listaInsumos = listOf(
            Insumo("022", "Insumo Procedimiento #1", mapaInsumos["022"] ?: 0),
            Insumo("045", "Insumo Procedimiento #2", mapaInsumos["045"] ?: 0)
        )

        adapter = InsumoAdapter(listaInsumos.toMutableList())
        recyclerView.adapter = adapter
        adapter.notifyDataSetChanged()

        recyclerView.layoutManager = LinearLayoutManager(this)

        val insumosUsados = sharedPreferences.getString("insumos_usados", "")
        if (!insumosUsados.isNullOrEmpty()) {
            val mapaCantidades = insumosUsados.split(";").associate {
                val (codigo, cantidad) = it.split(",")
                codigo to cantidad.toInt()
            }

            listaInsumos = listaInsumos.map { insumo ->
                adapter.actualizarLista(listaInsumos)
                insumo.copy(cantidad = mapaCantidades[insumo.codigo] ?: 0)
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        botonHoraSalida.setOnClickListener {
            mostrarTimePicker(horaSalidaText)
        }

        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            filtrarInsumos(query)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarInsumos(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        botonGuardar.setOnClickListener {
            mostrarDialogoGuardarDatos()
        }
    }

    private fun filtrarInsumos(query: String) {
        val listaFiltrada = listaInsumos.filter { it.nombre.contains(query, ignoreCase = true) }
        adapter.actualizarLista(listaFiltrada)
    }

    private fun mostrarDialogoGuardarDatos() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Guardar Datos")
        builder.setMessage("¿Desea guardar los datos?")
        builder.setPositiveButton("Si") { _, _ ->
            dialogConfirmacion()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun dialogConfirmacion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Guardar Datos")
        builder.setMessage("¿Está seguro de que desea guardar los datos? Los datos podrán ser editados más tarde.")
        builder.setPositiveButton("Confirmar") { _, _ ->
            guardarDatosEnSharedPreferences()
            datosGuardados()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun configurarInterfazSegunEstado() {
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosEditText = comentariosLayout?.editText
        when (estadoVisita) {
            ESTADO_NO_INICIADA -> {
                // Todos los campos editables
                habilitarControles(true)
                botonGuardar.text = "Guardar Visita"
            }
            ESTADO_EN_PROGRESO -> {
                // Campos editables excepto hora de llegada
                findViewById<TextView>(R.id.hora_llegada_text).isEnabled = false
                habilitarControles(true)
                botonGuardar.text = "Finalizar Visita"
            }
            ESTADO_COMPLETADA -> {
                // Solo comentarios editables
                habilitarControles(false)
                comentariosEditText?.isEnabled = true
                botonGuardar.text = "Actualizar Comentarios"
            }
        }
    }

    private fun habilitarControles(habilitar: Boolean) {
        findViewById<ImageView>(R.id.boton_hora_salida).isEnabled = habilitar
        findViewById<CheckBox>(R.id.checkBox1).isEnabled = habilitar
        findViewById<CheckBox>(R.id.checkBox2).isEnabled = habilitar
        findViewById<RecyclerView>(R.id.listainsumos).isEnabled = habilitar
        findViewById<EditText>(R.id.search_input).isEnabled = habilitar
        findViewById<ImageView>(R.id.search_button).isEnabled = habilitar

        // Manejar el TextInputLayout de comentarios
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        comentariosLayout?.isEnabled = habilitar
        comentariosLayout?.editText?.isEnabled = habilitar
    }

    private fun guardarDatosEnSharedPreferences() {
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text).text.toString()
        val checkBox1 = findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkBox2)
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
        val insumosJson = insumosUsados.joinToString(";") {"${it.codigo}:${it.cantidad}"}
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosTexto = comentariosLayout?.editText?.text?.toString() ?: ""

        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text).text.toString()
        // Determinar el nuevo estado (si se establece hora de salida, es completada)
        val nuevoEstado = if (horaSalidaText.isNotEmpty() && estadoVisita != ESTADO_COMPLETADA) {
            ESTADO_COMPLETADA
        } else {
            estadoVisita
        }
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val detallePrefs = getSharedPreferences("DetallePacientePrefs", Context.MODE_PRIVATE)

        with(detallePrefs.edit()) {
            putInt("estado_visita_$nombrePaciente", nuevoEstado)
            apply()
        }

        val editor = sharedPreferences.edit()
        editor.putString("comentarios", comentariosTexto)
        editor.putString("hora_salida", horaSalidaText)
        editor.putString("hora_llegada", horaLlegadaText)
        editor.putBoolean("check_box_1", checkBox1.isChecked)
        editor.putBoolean("check_box_2", checkBox2.isChecked)
        editor.putString("insumos", insumosJson)
        editor.putInt("estado_visita", nuevoEstado)
        editor.apply()

        estadoVisita = nuevoEstado
        configurarInterfazSegunEstado()
    }

    private fun datosGuardados() {
        val intent = Intent(this, PacientesActivity::class.java)
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
        startActivity(intent)
        finish()
    }

    private fun guardarDatosTemporales() {
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text).text.toString()
        val checkBox1 = findViewById<CheckBox>(R.id.checkBox1)
        val checkBox2 = findViewById<CheckBox>(R.id.checkBox2)
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
        val insumosJson = insumosUsados.joinToString(";") {"${it.codigo}:${it.cantidad}"}
        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        val comentariosEditText = comentariosLayout.editText
        val comentariosTexto = comentariosEditText?.text.toString()

        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text).text.toString()
        // Determinar el nuevo estado (si se establece hora de salida, es completada)
        val nuevoEstado = if (horaSalidaText.isNotEmpty()) ESTADO_COMPLETADA else ESTADO_EN_PROGRESO

        val editor = sharedPreferences.edit()
        editor.putString("comentarios", comentariosTexto)
        editor.putString("hora_salida", horaSalidaText)
        editor.putString("hora_llegada", horaLlegadaText)
        editor.putBoolean("check_box_1", checkBox1.isChecked)
        editor.putBoolean("check_box_2", checkBox2.isChecked)
        editor.putString("insumos", insumosJson)
        editor.putInt("estado_visita", nuevoEstado)
        editor.apply()
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
