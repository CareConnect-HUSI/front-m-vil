package com.example.careconnect.main.dataUsuarios

import com.google.gson.annotations.SerializedName

data class Paciente(
    val nombre: String,
    val hora: String,
    val direccion: String,
    val estadoVisita: String,
    val telefono: String,
    @SerializedName("visitaId") val visitaId: Int
)
