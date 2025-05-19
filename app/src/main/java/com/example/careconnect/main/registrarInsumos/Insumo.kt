package com.example.careconnect.main.registrarInsumos

data class Insumo(
    val codigo: Int,
    val insumo: String,
    var cantidad: Int = 0
)