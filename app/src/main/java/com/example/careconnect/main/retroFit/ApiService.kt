package com.example.careconnect.main.retroFit

import com.example.careconnect.main.inicioSesion.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @POST("/login")
    fun login(@Body credenciales: Credenciales): Call<LoginResponse>
}

data class Credenciales(
    val email: String,
    val password: String
)

