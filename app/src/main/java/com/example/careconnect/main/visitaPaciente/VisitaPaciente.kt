package com.example.careconnect.main.visitaPaciente

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.example.careconnect.main.registrarInsumos.InsumoConsumidoRequest
import com.example.careconnect.main.registrarProcedimientos.Procedimiento
import com.example.careconnect.main.retroFit.RetrofitClient
import com.example.careconnect.main.retroFit.VisitStatusRequest
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
import android.text.TextWatcher
import android.text.Editable
import androidx.annotation.RequiresPermission

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

    object SyncStorage {
        private const val FILE_NAME = "visitas_pendientes.json"
        fun guardarVisitaPendiente(context: Context, visita: JSONObject) {
            val file = File(context.filesDir, FILE_NAME)
            val array = if (file.exists()) JSONArray(file.readText()) else JSONArray()
            array.put(visita)
            file.writeText(array.toString())
        }
        fun obtenerVisitasPendientes(context: Context): JSONArray {
            val file = File(context.filesDir, FILE_NAME)
            return if (file.exists()) JSONArray(file.readText()) else JSONArray()
        }
        fun limpiar(context: Context) {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
        }
    }

    object NetworkUtils {
        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        fun hayInternet(context: Context): Boolean {
            val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    private lateinit var recyclerProcedimientos: RecyclerView
    private lateinit var procedimientoAdapter: ProcedimientoAdapter
    private lateinit var adapter: InsumoAdapter
    private lateinit var recyclerView: RecyclerView
    private val botonGuardar: Button by lazy { findViewById(R.id.guardar_datos) }
    private var estadoVisita: Int = ESTADO_NO_INICIADA
    private var visitaId: Int = -1
    private lateinit var networkReceiver: BroadcastReceiver

    private val restablecerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.careconnect.NETWORK_RESTORED") {
                Toast.makeText(this@VisitaPaciente, "Conexión restaurada. Actualizando datos...", Toast.LENGTH_SHORT).show()
                obtenerInsumosDesdeBackend()
                obtenerProcedimientosDesdeBackend()
            }
        }
    }

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

        val searchInput = findViewById<EditText>(R.id.search_input)
        searchInput?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                adapter.filtrar(query)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        estadoVisita = intent.getIntExtra("ESTADO_VISITA", ESTADO_NO_INICIADA)
        visitaId = intent.getIntExtra("VISITA_ID", -1)
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        findViewById<TextView>(R.id.detalle_nombre_paciente)?.text = nombrePaciente

        if (intent.getBooleanExtra("NUEVA_VISITA", false)) {
            estadoVisita = ESTADO_EN_PROGRESO
            val horaLlegada = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            findViewById<TextView>(R.id.hora_llegada_text)?.text = horaLlegada
            val jsonData = JsonUtils.loadData(this)
            val pacienteData = JSONObject().apply {
                put("hora_llegada", horaLlegada)
                put("estado_visita", ESTADO_EN_PROGRESO)
            }
            jsonData.put(nombrePaciente, pacienteData)
            JsonUtils.saveData(this, jsonData)
        }

        val comentarios = intent.getStringExtra("COMENTARIOS_VISITA") ?: ""
        findViewById<TextInputLayout>(R.id.comentarios)?.editText?.setText(comentarios)

        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text)
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text)
        val jsonData = JsonUtils.loadData(this)
        if (jsonData.has(nombrePaciente)) {
            val pacienteData = jsonData.getJSONObject(nombrePaciente)
            horaLlegadaText?.text = pacienteData.optString("hora_llegada", "")
            horaSalidaText?.text = pacienteData.optString("hora_salida", "")
            findViewById<TextInputLayout>(R.id.comentarios)?.editText?.setText(
                pacienteData.optString("comentarios", "")
            )
        }

        adapter = InsumoAdapter(emptyList())
        recyclerView = findViewById(R.id.listainsumos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        findViewById<ImageView>(R.id.btnLogout)?.setOnClickListener { mostrarDialogoCerrarSesion() }
        botonGuardar.setOnClickListener { mostrarDialogoGuardarDatos() }

        networkReceiver = NetworkRestoredReceiver {
            VisitaSyncUtils.sincronizarVisitasPendientes(this)
        }
        registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        obtenerInsumosDesdeBackend()
        obtenerProcedimientosDesdeBackend()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(networkReceiver)
    }

    private fun obtenerInsumosDesdeBackend() {
        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE")

        if (token != null && visitaId != -1) {
            val api = RetrofitClient.getInstance(token)
            api.getInsumosPorVisita(visitaId).enqueue(object : Callback<List<Insumo>> {
                override fun onResponse(call: Call<List<Insumo>>, response: Response<List<Insumo>>) {
                    if (response.isSuccessful) {
                        val insumos = response.body() ?: emptyList()

                        if (estadoVisita == ESTADO_COMPLETADA) {
                            api.getInsumosConsumidos(visitaId).enqueue(object : Callback<List<InsumoConsumidoRequest>> {
                                override fun onResponse(call: Call<List<InsumoConsumidoRequest>>, response: Response<List<InsumoConsumidoRequest>>) {
                                    if (response.isSuccessful) {
                                        val consumidos = response.body() ?: emptyList()
                                        val insumosActualizados = insumos.map { insumo ->
                                            consumidos.find { it.codigo == insumo.codigo }?.let {
                                                insumo.cantidad = it.cantidad
                                            }
                                            insumo
                                        }
                                        adapter.actualizarLista(insumosActualizados)
                                        adapter.setEditable(false)
                                    }
                                }
                                override fun onFailure(call: Call<List<InsumoConsumidoRequest>>, t: Throwable) {
                                    Log.e("INSUMOS_API", "Fallo red al obtener insumos consumidos", t)
                                }
                            })
                        } else if (estadoVisita == ESTADO_EN_PROGRESO && nombrePaciente != null) {
                            val jsonData = JsonUtils.loadData(this@VisitaPaciente)
                            val pacienteData = jsonData.optJSONObject(nombrePaciente)
                            val insumosGuardados = pacienteData?.optJSONArray("insumos")
                            val insumosActualizados = if (insumosGuardados != null) {
                                insumos.map { insumo ->
                                    for (i in 0 until insumosGuardados.length()) {
                                        val insumoGuardado = insumosGuardados.getJSONObject(i)
                                        if (insumoGuardado.getInt("codigo") == insumo.codigo) {
                                            insumo.cantidad = insumoGuardado.getInt("cantidad")
                                            break
                                        }
                                    }
                                    insumo
                                }
                            } else insumos
                            adapter.actualizarLista(insumosActualizados)
                            adapter.setEditable(true)
                        } else {
                            adapter.actualizarLista(insumos)
                        }

                        configurarInterfazSegunEstado()
                    }
                }

                override fun onFailure(call: Call<List<Insumo>>, t: Throwable) {
                    Log.e("INSUMOS_API", "Fallo red insumos", t)
                }
            })
        }
    }

    private fun obtenerProcedimientosDesdeBackend() {
        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE")

        if (token != null && visitaId != -1) {
            val api = RetrofitClient.getInstance(token)
            api.getProcedimientosPorVisita(visitaId)
                .enqueue(object : Callback<List<Procedimiento>> {
                    override fun onResponse(call: Call<List<Procedimiento>>, response: Response<List<Procedimiento>>) {
                        if (response.isSuccessful) {
                            val procedimientos = response.body() ?: emptyList()

                            if ((estadoVisita == ESTADO_EN_PROGRESO || estadoVisita == ESTADO_COMPLETADA) && nombrePaciente != null) {
                                val jsonData = JsonUtils.loadData(this@VisitaPaciente)
                                val pacienteData = jsonData.optJSONObject(nombrePaciente)
                                val procedimientosGuardados = pacienteData?.optJSONArray("procedimientos")

                                if (procedimientosGuardados != null) {
                                    procedimientos.forEach { proc ->
                                        for (i in 0 until procedimientosGuardados.length()) {
                                            if (procedimientosGuardados.getString(i) == proc.nombre) {
                                                proc.realizado = true
                                                break
                                            }
                                        }
                                    }
                                }
                            }

                            procedimientoAdapter = ProcedimientoAdapter(procedimientos)
                            recyclerProcedimientos.adapter = procedimientoAdapter
                            procedimientoAdapter.setEditable(estadoVisita != ESTADO_COMPLETADA)
                            configurarInterfazSegunEstado()
                        }
                    }
                    override fun onFailure(call: Call<List<Procedimiento>>, t: Throwable) {
                        Log.e("PROC_API", "Fallo de red al obtener procedimientos", t)
                    }
                })
        }
    }

    private fun enviarHorasAlBackend(horaLlegada: String, horaSalida: String) {
        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)

        if (token != null && visitaId != -1) {
            val api = RetrofitClient.getInstance(token)
            val body = HorasVisitaRequest(horaLlegada, horaSalida)

            api.registrarHoras(visitaId, body).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Log.d("HORAS_API", "Horas registradas correctamente")
                    } else {
                        Log.e("HORAS_API", "Error al registrar horas: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("HORAS_API", "Fallo de red al registrar horas", t)
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
        val procedimientosSeleccionados = JSONArray()
        procedimientoAdapter.obtenerProcedimientosSeleccionados().forEach {
            procedimientosSeleccionados.put(it.nombre)
        }

        val jsonData = JsonUtils.loadData(this)
        val pacienteData = JSONObject().apply {
            put("visita_id", visitaId)
            put("nombre_paciente", nombrePaciente)
            put("hora_llegada", horaLlegadaText)
            put("hora_salida", horaSalidaText)
            put("comentarios", comentariosTexto)
            put("procedimientos", procedimientosSeleccionados)
            put("insumos", JSONArray(insumosUsados.map { JSONObject().apply {
                put("codigo", it.codigo)
                put("cantidad", it.cantidad)
            }}))
            put("estado_visita", ESTADO_COMPLETADA)
        }
        jsonData.put(nombrePaciente, pacienteData)
        JsonUtils.saveData(this, jsonData)

        estadoVisita = ESTADO_COMPLETADA
        configurarInterfazSegunEstado()

        val insumosParaEnviar = insumosUsados.map {
            InsumoConsumidoRequest(it.codigo, it.cantidad)
        }

        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null)

        if (token != null && visitaId != -1) {
            val api = RetrofitClient.getInstance(token)
            api.registrarInsumosConsumidos(visitaId, insumosParaEnviar)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@VisitaPaciente, "Insumos registrados correctamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@VisitaPaciente, "Error al registrar insumos (${response.code()})", Toast.LENGTH_LONG).show()
                            Log.e("INSUMOS_API", "Error al registrar insumos: ${response.errorBody()?.string()}")
                            guardarVisitaPendienteLocal()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Toast.makeText(this@VisitaPaciente, "Fallo de red al enviar insumos", Toast.LENGTH_LONG).show()
                        Log.e("INSUMOS_API", "Fallo de red al registrar insumos", t)
                        guardarVisitaPendienteLocal()
                    }
                })
            val statusRequest = VisitStatusRequest(estadoVisita = "COMPLETADA")
            api.updateVisitStatus(visitaId, statusRequest)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            Log.d("STATUS_API", "Visit status updated to COMPLETADA")
                            Toast.makeText(this@VisitaPaciente, "Estado de visita actualizado", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("STATUS_API", "Error updating status: ${response.code()}, ${response.errorBody()?.string()}")
                            Toast.makeText(this@VisitaPaciente, "Error al actualizar estado (${response.code()})", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("STATUS_API", "Network failure updating status", t)
                        Toast.makeText(this@VisitaPaciente, "Fallo de red al actualizar estado", Toast.LENGTH_LONG).show()
                    }
                })
        }
        enviarHorasAlBackend(horaLlegadaText, horaSalidaText)
    }

    private fun guardarVisitaPendienteLocal() {
        val nombrePaciente = intent.getStringExtra("NOMBRE_PACIENTE") ?: "Desconocido"
        val horaLlegada = findViewById<TextView>(R.id.hora_llegada_text)?.text.toString()
        val horaSalida = findViewById<TextView>(R.id.hora_salida_text)?.text.toString()
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
        val insumosJson = JSONArray(insumosUsados.map {
            JSONObject().apply {
                put("codigo", it.codigo)
                put("cantidad", it.cantidad)
            }
        })
        val visitaJson = JSONObject().apply {
            put("visita_id", visitaId)
            put("hora_llegada", horaLlegada)
            put("hora_salida", horaSalida)
            put("insumos", insumosJson)
        }
        SyncStorage.guardarVisitaPendiente(this, visitaJson)
    }

    private fun guardarDatosTemporales() {
        val horaLlegadaText = findViewById<TextView>(R.id.hora_llegada_text)?.text.toString()
        val horaSalidaText = findViewById<TextView>(R.id.hora_salida_text)?.text.toString()
        val comentariosTexto = findViewById<TextInputLayout>(R.id.comentarios)?.editText?.text?.toString() ?: ""
        val insumosUsados = adapter.obtenerLista().filter { it.cantidad > 0 }
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
            put("insumos", JSONArray(insumosUsados.map { JSONObject().apply {
                put("codigo", it.codigo)
                put("cantidad", it.cantidad)
            }}))
            put("estado_visita", ESTADO_EN_PROGRESO)
        }
        jsonData.put(nombrePaciente, pacienteData)
        JsonUtils.saveData(this, jsonData)
    }

    private fun sincronizarVisitasPendientes(context: Context) {
        val pendientes = SyncStorage.obtenerVisitasPendientes(context)
        if (pendientes.length() == 0) return

        val prefs = getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        val token = prefs.getString("JWT_TOKEN", null) ?: return
        val api = RetrofitClient.getInstance(token)

        for (i in 0 until pendientes.length()) {
            val visita = pendientes.getJSONObject(i)
            val visitaId = visita.getInt("visita_id")
            val llegada = visita.getString("hora_llegada")
            val salida = visita.getString("hora_salida")
            val insumos = visita.getJSONArray("insumos")
            val listaInsumos = mutableListOf<InsumoConsumidoRequest>()
            for (j in 0 until insumos.length()) {
                val insumo = insumos.getJSONObject(j)
                listaInsumos.add(InsumoConsumidoRequest(insumo.getInt("codigo"), insumo.getInt("cantidad")))
            }

            api.registrarHoras(visitaId, HorasVisitaRequest(llegada, salida)).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("SYNC", "Horas sincronizadas para visita $visitaId")
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("SYNC", "Fallo horas: $t")
                }
            })

            api.registrarInsumosConsumidos(visitaId, listaInsumos).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("SYNC", "Insumos sincronizados para visita $visitaId")
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("SYNC", "Fallo insumos: $t")
                }
            })

            api.updateVisitStatus(visitaId, VisitStatusRequest("COMPLETADA"))
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        Log.d("SYNC", "Estado sincronizado visita $visitaId")
                    }
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.e("SYNC", "Fallo estado: $t")
                    }
                })
        }

        SyncStorage.limpiar(context)
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
        comentariosLayout?.isEnabled = true
        comentariosLayout?.editText?.isEnabled = true
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