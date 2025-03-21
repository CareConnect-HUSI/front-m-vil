package com.example.careconnect.logInPage.infoPacientes

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.logInPage.registrarInsumos.InsumoAdapter
import com.example.careconnect.logInPage.registrarInsumos.Insumo
import java.util.Calendar

class DetallePacienteActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var insumoAdapter: InsumoAdapter
    private lateinit var listaInsumos: List<Insumo>

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

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, medicamentos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Selector de hora para llegada
        botonHoraLlegada.setOnClickListener {
            mostrarTimePicker(horaLlegadaText)
        }

        // Selector de hora para salida
        botonHoraSalida.setOnClickListener {
            mostrarTimePicker(horaSalidaText)
        }

        // Botón para registrar insumos (Abre el pop-up)
        val registrarInsumosButton = findViewById<Button>(R.id.registrar_insumos_button)
        registrarInsumosButton.setOnClickListener {
            // Mostrar el pop-up con los insumos
            abrirDialogoInsumos()
        }

        // Botón para guardar datos
        val guardarDatosButton = findViewById<Button>(R.id.guardar_datos_button)
        guardarDatosButton.setOnClickListener {
            Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
        }

        // Lista de insumos de prueba
        listaInsumos = listOf(
            Insumo("022", "Jeringa"),
            Insumo("045", "Gasas"),
            Insumo("078", "Guantes"),
            Insumo("102", "Alcohol"),
            Insumo("203", "Vendas")
        )
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

    private fun abrirDialogoInsumos() {
        // Inflar el layout del Dialog
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.dialog_registrar_insumo, null)

        // Configurar RecyclerView en el pop-up
        val recyclerViewDialog = view.findViewById<RecyclerView>(R.id.recyclerViewResultados)
        recyclerViewDialog.layoutManager = LinearLayoutManager(this)
        val insumoAdapter = InsumoAdapter(listaInsumos) // Usamos los insumos de prueba
        recyclerViewDialog.adapter = insumoAdapter
        // Referencia al campo de búsqueda dentro del diálogo
        val searchInputDialog = view.findViewById<EditText>(R.id.search_input)
        val searchButtonDialog = view.findViewById<ImageView>(R.id.search_button)

// Agregar funcionalidad de búsqueda
        searchInputDialog.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val listaFiltrada = listaInsumos.filter { it.nombre.contains(query, ignoreCase = true) }
                insumoAdapter.actualizarLista(listaFiltrada)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

// Botón de búsqueda opcional (si lo usas)
        searchButtonDialog.setOnClickListener {
            val query = searchInputDialog.text.toString().trim()
            val listaFiltrada = listaInsumos.filter { it.nombre.contains(query, ignoreCase = true) }
            insumoAdapter.actualizarLista(listaFiltrada)
        }

        // Crear el Dialog
        val builder = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .setTitle("Seleccionar Insumos")
            .setPositiveButton("Guardar") { dialogInterface: DialogInterface, _: Int ->
                // Aquí agregar la lógica para guardar los insumos
                Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cerrar") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }

        // Mostrar el Dialog
        val dialog = builder.create()
        dialog.show()
    }
}
