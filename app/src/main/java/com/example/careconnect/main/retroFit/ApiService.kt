package com.example.careconnect.main.retroFit

import com.example.careconnect.main.inicioSesion.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/login")
    fun login(@Body credenciales: Credenciales): Call<LoginResponse>

    @GET("/pacientes")
    fun getPatients(@Header("Authorization") token: String): Call<PatientsListResponse>
}

data class Credenciales(
    val email: String,
    val password: String
)

data class PatientsListResponse(
    val patients: List<PatientResponse>
)

data class PatientResponse(
    val nombre: String,
    val diagnostico: String,
    val hora: String,
    val direccion: String,
    val estadoVisita: String // PROGRAMADO, EN_PROCESO, FINALIZADO
)

