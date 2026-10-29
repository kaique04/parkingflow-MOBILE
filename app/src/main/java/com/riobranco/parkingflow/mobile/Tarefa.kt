package com.riobranco.parkingflow.mobile

data class Tarefa(
    val id: Long? = null,
    val titulo: String,
    val descricao: String? = null,
    val status: String,
    val prioridade: String,
    val disciplina: String,
    val dataEntrega: String? = null
)