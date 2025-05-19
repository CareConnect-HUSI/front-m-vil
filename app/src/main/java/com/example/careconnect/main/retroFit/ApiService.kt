package com.example.careconnect.main.retroFit

import com.example.careconnect.main.dataUsuarios.Paciente
import com.example.careconnect.main.registrarProcedimientos.Procedimiento
import com.example.careconnect.main.registrarInsumos.Insumo
import com.example.careconnect.main.inicioSesion.LoginResponse
import com.example.careconnect.main.registrarInsumos.InsumoConsumidoRequest
import com.example.careconnect.main.visitaPaciente.HorasVisitaRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response

interface ApiService {
    @POST("/login")
    fun login(
        @Body credenciales: Credenciales
    ): Call<LoginResponse>

    @GET("pacientes-asignados")
    suspend fun getPacientesAsignados(@Header("Authorization") token: String): List<Paciente>

    @POST("visita/{id}/hora")
    fun registrarHoras(
        @Path("id") visitaId: Int,
        @Body horas: HorasVisitaRequest
    ): Call<ResponseBody>

    @GET("visita/{visita_id}/procedimientos")
    fun getProcedimientosPorVisita(
        @Path("visita_id") visitaId: Int
    ): Call<List<Procedimiento>>

    @GET("visita/{visitaId}/insumos")
    fun getInsumosPorVisita(@Path("visitaId") visitaId: Int): Call<List<Insumo>>

    @POST("visita/{visita_id}/insumos/consumidos")
    fun registrarInsumosConsumidos(
        @Path("visita_id") visitaId: Int,
        @Body insumos: List<InsumoConsumidoRequest>
    ): Call<ResponseBody>
}

data class Credenciales(
    val email: String,
    val password: String
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)

