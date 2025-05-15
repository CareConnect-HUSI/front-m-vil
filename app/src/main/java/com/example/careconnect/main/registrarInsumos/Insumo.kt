package com.example.careconnect.main.registrarInsumos

data class Insumo(
    val codigo: String,
    val nombre: String,
    var cantidad: Int = 0
)