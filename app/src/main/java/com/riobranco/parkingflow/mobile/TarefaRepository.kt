package com.riobranco.parkingflow.mobile

// A única classe do app que fala com o Retrofit
class TarefaRepository(private val api: ApiService = RetrofitClient.api) {

    suspend fun listar(): List<Tarefa> {
        val resposta = api.listarTarefas()
        if (resposta.isSuccessful) return resposta.body() ?: emptyList()
        throw Exception("Erro ao buscar tarefas (HTTP ${resposta.code()})")
    }

    // Sem id: cadastra (POST). Com id: edita (PUT)
    suspend fun salvar(tarefa: Tarefa): Tarefa {
        val resposta = if (tarefa.id == null) api.criarTarefa(tarefa)
                       else api.atualizarTarefa(tarefa.id, tarefa)
        if (resposta.isSuccessful) return resposta.body() ?: tarefa
        throw Exception("Erro ao salvar (HTTP ${resposta.code()})")
    }

    suspend fun excluir(id: Long) {
        val resposta = api.deletarTarefa(id)
        if (!resposta.isSuccessful) throw Exception("Não foi possível excluir (HTTP ${resposta.code()})")
    }
}
