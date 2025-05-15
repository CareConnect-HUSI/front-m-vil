package com.example.careconnect.main.dataUsuarios

data class Enfermera(
    val id: Int,
    val nombre: String,
    val apellidos: String,
    val turno: Int,
    val email: String,
    val password: String,
    val token: String
)
