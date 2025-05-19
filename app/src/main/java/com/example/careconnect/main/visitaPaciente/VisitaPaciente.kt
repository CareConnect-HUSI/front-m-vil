package com.example.careconnect.main.visitaPaciente

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.main.adapters.ProcedimientoAdapter
import com.example.careconnect.main.inicioSesion.LoginActivity
import com.example.careconnect.main.registrarInsumos.Insumo
import com.example.careconnect.main.registrarInsumos.InsumoAdapter
import com.example.careconnect.main.registrarProcedimientos.Procedimiento
import com.example.careconnect.main.retroFit.RetrofitClient
import com.google.android.material.textfield.TextInputLayout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray
import org.json.JSONObject

class VisitaPaciente : AppCompatActivity() {

    companion object {
        const val ESTADO_NO_INICIADA = 0
        const val ESTADO_EN_PROGRESO = 1
        const val ESTADO_COMPLETADA = 2
    }

    object JsonUtils {
        private const val FILE_NAME = "visitas_pacientes.json"

        fun saveData(context: Context, data: JSONObject) {
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(data.toString())
        }

        fun loadData(context: Context): JSONObject {
            val file = File(context.filesDir, FILE_NAME)
            return if (file.exists()) JSONObject(file.readText()) else JSONObject()
        }
    }

    private lateinit var recyclerProcedimientos: RecyclerView
    private lateinit var procedimientoAdapter: ProcedimientoAdapter
    private lateinit var adapter: InsumoAdapter
    private lateinit var recyclerView: RecyclerView
    private val botonGuardar: Button by lazy { findViewById(R.id.guardar_datos) }
    private var estadoVisita: Int = ESTADO_NO_INICIADA
    private var visitaId: Int = -1

    override fun onPause() {
        super.onPause()
        if (estadoVisita == ESTADO_EN_PROGRESO) {
            guardarDatosTemporales()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visita_paciente)

        recyclerProcedimientos = findViewById(R.id.recycler_procedimientos)
        recyclerProcedimientos.layoutManager = LinearLayoutManager(this)

        procedimientoAdapter = ProcedimientoAdapter(emptyList())
        recyclerProcedimientos.adapter = procedimientoAdapter

        estadoVisita = intent.getIntExtra("ESTADO_VISITA", ESTADO_NO_INICIADA)
        visitaId = intent.getIntExtra("VISITA_ID", -1)

        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        findViewById<TextView>(R.id.detalle_nombre_paciente)?.text = nombrePaciente

        if (intent.getBooleanExtra("NUEVA_VISITA", false)) {
            estadoVisita = ESTADO_EN_PROGRESO
            val horaLlegada = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val jsonData = JsonUtils.loadData(this)
            val pacienteData = JSONObject().apply {
                put("hora_llegada", horaLlegada)
                put("estado_visita", ESTADO_EN_PROGRESO)
            }
            jsonData.put(nombrePaciente, pacienteData)
            JsonUtils.saveData(this, jsonData)
            findViewById<TextView>(R.id.hora_llegada_text)?.text = horaLlegada
        }

        val comentarios = intent.getStringExtra("COMENTARIOS_VISITA") ?: ""
        findViewById<TextInputLayout>(R.id.comentarios)?.editText?.setText(comentarios)

        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text)

        val jsonData = JsonUtils.loadData(this)

        adapter = InsumoAdapter(emptyList())
        recyclerView = findViewById(R.id.listainsumos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Carga los insumos reales
        obtenerInsumosDesdeBackend()

        recyclerView = findViewById(R.id.listainsumos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<ImageView>(R.id.btnLogout)?.setOnClickListener { mostrarDialogoCerrarSesion() }
        botonGuardar.setOnClickListener { mostrarDialogoGuardarDatos() }

        obtenerProcedimientosDesdeBackend()
    }

    private fun obtenerProcedimientosDesdeBackend() {
        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)

        if (token != null && visitaId != -1) {
            val api = RetrofitClient.getInstance(token)
            Log.d("PROC_API", "Visita ID enviado: $visitaId")
            api.getProcedimientosPorVisita(visitaId)
                .enqueue(object : Callback<List<Procedimiento>> {
                    override fun onResponse(call: Call<List<Procedimiento>>, response: Response<List<Procedimiento>>) {
                        if (response.isSuccessful) {
                            val lista = response.body() ?: emptyList()
                            procedimientoAdapter = ProcedimientoAdapter(lista)
                            recyclerProcedimientos.adapter = procedimientoAdapter

                            val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: return
                            val jsonData = JsonUtils.loadData(this@VisitaPaciente)
                            val pacienteData = jsonData.optJSONObject(nombrePaciente)
                            val procedimientosGuardados = pacienteData?.optJSONArray("procedimientos")

                            lista.forEach { proc ->
                                if (procedimientosGuardados != null) {
                                    for (i in 0 until procedimientosGuardados.length()) {
                                        if (procedimientosGuardados.getString(i) == proc.nombre) {
                                            proc.realizado = true
                                            break
                                        }
                                    }
                                }
                            }

                            procedimientoAdapter.setEditable(estadoVisita != ESTADO_COMPLETADA)
                            configurarInterfazSegunEstado()
                        } else {
                            Log.e("PROC_API", "Error al obtener procedimientos: ${response.code()}")
                        }
                    }
                    override fun onFailure(call: Call<List<Procedimiento>>, t: Throwable) {
                        Log.e("PROC_API", "Fallo de red al obtener procedimientos", t)
                    }
                })
        }
    }

    private fun obtenerInsumosDesdeBackend() {
        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)

        if (token != null && visitaId != -1) {
            val api = RetrofitClient.getInstance(token)
            api.getInsumosPorVisita(visitaId).enqueue(object : Callback<List<Insumo>> {
                override fun onResponse(call: Call<List<Insumo>>, response: Response<List<Insumo>>) {
                    if (response.isSuccessful) {
                        val lista = response.body()?.map {
                            Insumo(it.codigo, it.insumo, 0)  // Cantidad inicial en 0
                        } ?: emptyList()

                        adapter.actualizarLista(lista)
                        adapter.setEditable(estadoVisita != ESTADO_COMPLETADA)
                    } else {
                        Log.e("INSUMOS_API", "Error al obtener insumos: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<List<Insumo>>, t: Throwable) {
                    Log.e("INSUMOS_API", "Fallo de red al obtener insumos", t)
                }
            })
        }
    }

    private fun guardarDatosEnJson() {
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text)?.text.toString()
        val horaSalidaView = findViewById<TextView>(R.id.hora_salida_text)
        var horaSalidaText = horaSalidaView?.text.toString()

        if (horaSalidaText.isBlank()) {
            horaSalidaText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            horaSalidaView?.text = horaSalidaText
        }

        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val comentariosTexto = findViewById<TextInputLayout>(R.id.comentarios)?.editText?.text?.toString() ?: ""
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
        val insumosJson = insumosUsados.joinToString(";") { "${it.codigo}:${it.cantidad}" }

        val procedimientosSeleccionados = JSONArray()
        procedimientoAdapter.obtenerProcedimientosSeleccionados().forEach {
            procedimientosSeleccionados.put(it.nombre)
        }

        val jsonData = JsonUtils.loadData(this)
        val pacienteData = JSONObject().apply {
            put("hora_llegada", horaLlegadaText)
            put("hora_salida", horaSalidaText)
            put("comentarios", comentariosTexto)
            put("procedimientos", procedimientosSeleccionados)
            put("insumos", insumosJson)
            put("estado_visita", ESTADO_COMPLETADA)
        }
        jsonData.put(nombrePaciente, pacienteData)
        JsonUtils.saveData(this, jsonData)

        estadoVisita = ESTADO_COMPLETADA
        configurarInterfazSegunEstado()

        val insumosParaEnviar = insumosUsados.map {
            com.example.careconnect.main.registrarInsumos.InsumoConsumidoRequest(it.codigo, it.cantidad)
        }

        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)

        if (token != null && visitaId != -1) {
            val api = com.example.careconnect.main.retroFit.RetrofitClient.getInstance(token)
            api.registrarInsumosConsumidos(visitaId, insumosParaEnviar)
                .enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
                    override fun onResponse(call: retrofit2.Call<okhttp3.ResponseBody>, response: retrofit2.Response<okhttp3.ResponseBody>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@VisitaPaciente, "Insumos registrados correctamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@VisitaPaciente, "Error al registrar insumos (${response.code()})", Toast.LENGTH_LONG).show()
                            android.util.Log.e("INSUMOS_API", "Error al registrar insumos: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                        Toast.makeText(this@VisitaPaciente, "Fallo de red al enviar insumos", Toast.LENGTH_LONG).show()
                        android.util.Log.e("INSUMOS_API", "Fallo de red al registrar insumos", t)
                    }
                })
        }
    }

    private fun guardarDatosTemporales() {
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text)?.text.toString()
        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text)?.text.toString()
        val comentariosTexto = findViewById<TextInputLayout>(R.id.comentarios)?.editText?.text?.toString() ?: ""
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
        val insumosJson = insumosUsados.joinToString(";") { "${it.codigo}:${it.cantidad}" }
        val procedimientosSeleccionados = JSONArray()
        procedimientoAdapter.obtenerProcedimientosSeleccionados().forEach {
            procedimientosSeleccionados.put(it.nombre)
        }

        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: return

        val jsonData = JsonUtils.loadData(this)
        val pacienteData = JSONObject().apply {
            put("hora_llegada", horaLlegadaText)
            put("hora_salida", horaSalidaText)
            put("comentarios", comentariosTexto)
            put("procedimientos", procedimientosSeleccionados)
            put("insumos", insumosJson)
            put("estado_visita", ESTADO_EN_PROGRESO)
        }
        jsonData.put(nombrePaciente, pacienteData)
        JsonUtils.saveData(this, jsonData)
    }

    private fun configurarInterfazSegunEstado() {
        val habilitar = estadoVisita != ESTADO_COMPLETADA
        habilitarControles(habilitar)
        adapter.setEditable(habilitar)
        if (::procedimientoAdapter.isInitialized) {
            procedimientoAdapter.setEditable(habilitar)
        }
        botonGuardar.text = when (estadoVisita) {
            ESTADO_NO_INICIADA -> "Guardar Visita"
            ESTADO_EN_PROGRESO -> "Finalizar Visita"
            ESTADO_COMPLETADA -> "Actualizar Comentarios"
            else -> "Guardar"
        }
    }

    private fun habilitarControles(habilitar: Boolean) {
        findViewById<EditText>(R.id.search_input)?.isEnabled = habilitar
        findViewById<ImageView>(R.id.search_button)?.isEnabled = habilitar

        val comentariosLayout = findViewById<TextInputLayout>(R.id.comentarios)
        comentariosLayout?.isEnabled = habilitar
        comentariosLayout?.editText?.isEnabled = habilitar
    }

    private fun mostrarDialogoGuardarDatos() {
        AlertDialog.Builder(this)
            .setTitle("Guardar Datos")
            .setMessage("¿Desea guardar los datos?")
            .setPositiveButton("Si") { _, _ -> dialogoConfirmacion() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun dialogoConfirmacion() {
        AlertDialog.Builder(this)
            .setTitle("Confirmación Guardar Datos")
            .setMessage("¿Está seguro de que desea guardar los datos? Podrá modificar los comentarios después")
            .setPositiveButton("Confirmar") { _, _ ->
                guardarDatosEnJson()
                val intent = Intent(this, com.example.careconnect.main.listaPacientes.PacientesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _, _ -> cerrarSesion() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cerrarSesion() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
