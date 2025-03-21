package com.example.careconnect.logInPage.registrarInsumos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.logInPage.registrarInsumos.InsumoAdapter
import com.example.careconnect.logInPage.registrarInsumos.Insumo

class RegistrarInsumoActivity : AppCompatActivity() {

    private lateinit var adapter: InsumoAdapter
    private lateinit var listaInsumos: List<Insumo>
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_registrar_insumo)

        recyclerView = findViewById(R.id.recyclerViewResultados)
        searchInput = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Lista de insumos de prueba
        listaInsumos = listOf(
            Insumo("022", "Jeringa"),
            Insumo("045", "Gasas"),
            Insumo("078", "Guantes"),
            Insumo("102", "Alcohol"),
            Insumo("203", "Vendas")
        )

        // Configurar adaptador
        adapter = InsumoAdapter(listaInsumos.toMutableList())
        recyclerView.adapter = adapter

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
    }

    private fun filtrarInsumos(query: String) {
        val listaFiltrada = listaInsumos.filter { it.nombre.contains(query, ignoreCase = true) }
        adapter.actualizarLista(listaFiltrada)
    }
}
