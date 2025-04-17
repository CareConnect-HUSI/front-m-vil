package com.example.careconnect.logInPage.registrarInsumos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.logInPage.registrarInsumos.Insumo

class InsumoAdapter(private var insumos: List<Insumo>) :
    RecyclerView.Adapter<InsumoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val codigo: TextView = view.findViewById(R.id.codigo_insumo)
        val nombre: TextView = view.findViewById(R.id.nombre_insumo)
        val buttonDecrease: ImageView = view.findViewById(R.id.button_decrease)
        val buttonIncrease: ImageView = view.findViewById(R.id.button_increase)
        val cantidadText: TextView = view.findViewById(R.id.text_cantidad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_insumo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val insumo = insumos[position]
        holder.codigo.text = insumo.codigo
        holder.nombre.text = insumo.nombre
        holder.cantidadText.text = "0"

        // Lógica del contador
        holder.buttonIncrease.setOnClickListener {
            var cantidad = holder.cantidadText.text.toString().toInt()
            cantidad++
            holder.cantidadText.text = cantidad.toString()
        }

        holder.buttonDecrease.setOnClickListener {
            var cantidad = holder.cantidadText.text.toString().toInt()
            if (cantidad > 0) {
                cantidad--
                holder.cantidadText.text = cantidad.toString()
            }
        }
    }

    override fun getItemCount(): Int {
        return insumos.size
    }

    fun actualizarLista(nuevaLista: List<Insumo>) {
        insumos = nuevaLista
        notifyDataSetChanged()
    }
}
