package com.example.careconnect.main.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.example.careconnect.R
import com.example.careconnect.main.registrarProcedimientos.Procedimiento

class ProcedimientoAdapter(private val procedimientos: List<Procedimiento>):
    RecyclerView.Adapter<ProcedimientoAdapter.ProcedimientoViewHolder>() {

    private var editable: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcedimientoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_procedimiento, parent, false)
        return ProcedimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProcedimientoViewHolder, position: Int) {
        val procedimiento = procedimientos[position]
        holder.checkBox.text = procedimiento.nombre
        holder.checkBox.isChecked = procedimiento.realizado
        holder.checkBox.isEnabled = editable

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            procedimiento.realizado = isChecked
        }
    }

    override fun getItemCount() = procedimientos.size

    fun obtenerProcedimientosSeleccionados(): List<Procedimiento> {
        return procedimientos.filter { it.realizado }
    }

    fun setEditable(valor: Boolean) {
        editable = valor
        notifyDataSetChanged()
    }

    inner class ProcedimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox_procedimiento)
    }
}
