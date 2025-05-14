package com.example.careconnect.logInPage.usuario.Usuario

import retrofit2.Call
import retrofit2.http.GET

interface ApiService {
    @GET("users/1")
    fun getUser(): Call<Usuario>
}
