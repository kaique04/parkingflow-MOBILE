package com.riobranco.parkingflow.mobile

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @GET("tarefas")
    suspend fun listarTarefas(): Response<List<Tarefa>>

    @POST("tarefas")
    suspend fun criarTarefa(@Body tarefa: Tarefa): Response<Tarefa>

    @PUT("tarefas/{id}")
    suspend fun atualizarTarefa(@Path("id") id: Long, @Body tarefa: Tarefa): Response<Tarefa>

    @DELETE("tarefas/{id}")
    suspend fun deletarTarefa(@Path("id") id: Long): Response<Unit>
}