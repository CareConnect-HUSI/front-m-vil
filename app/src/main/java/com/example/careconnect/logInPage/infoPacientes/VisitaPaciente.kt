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

    private lateinit var listaInsumos: List<Insumo>
    private lateinit var adapter: InsumoAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var botonGuardar: Button
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visita_paciente)

        // Obtener datos del intent
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val horaLlegada = intent.getStringExtra("HORA_LLEGADA") ?: ""
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

        //Insumos
        recyclerView = findViewById(R.id.listainsumos)
        searchInput = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Lista de insumos de prueba
        listaInsumos = listOf(
            Insumo("022", "Insumo Procedimiento #1"),
            Insumo("045", "Insumo Procedimiento #2")
        )

        // Configurar adaptador
        adapter = InsumoAdapter(listaInsumos.toMutableList())
        recyclerView.adapter = adapter

        // Botón de volver atrás
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Volver a la pantalla anterior
        }

        // Botón de cerrar sesión
        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            mostrarDialogoCerrarSesion()
        }

        // Selector de hora para salida
        botonHoraSalida.setOnClickListener {
            mostrarTimePicker(horaSalidaText)
        }

        // Función de búsqueda
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

        // Botón para guardar datos
        val guardarDatosButton = findViewById<Button>(R.id.guardar_datos)
        guardarDatosButton.setOnClickListener {
            mostrarDialogoGuardarDatos()
        }
    }

    private fun filtrarInsumos(query: String) {
        val listaFiltrada = listaInsumos.filter { it.nombre.contains(query, ignoreCase = true) }
        adapter.actualizarLista(listaFiltrada)
    }

    private fun mostrarDialogoGuardarDatos(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Guardar Datos")
        builder.setMessage("¿Desea guardar los datos?")
        builder.setPositiveButton("Si") { _, _ ->
            dialogConfirmacion()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun dialogConfirmacion(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Guardar Datos")
        builder.setMessage("¿Esta seguro de que desea guardar los datos? Recuerde que una vez los datos sean guardados estos no pueden ser editados")
        builder.setPositiveButton("Confirmar") { _, _ ->
            datosGuardados()
        }
        builder.setNegativeButton("No", null)
        builder.show()
    }

    private fun datosGuardados(){
        //Redirigir a pantalla de lista pacientes
        val intent = Intent (this, PacientesActivity::class.java)
        Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
        // Redirigir a la pantalla de login
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