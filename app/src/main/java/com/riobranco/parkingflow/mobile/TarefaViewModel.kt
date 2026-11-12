package com.riobranco.parkingflow.mobile

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

// AndroidViewModel (em vez de ViewModel puro) só pra ter acesso ao Context
// e poder chamar temConexaoInternet() sem precisar passar Context por fora.
class TarefaViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TarefaRepository()

    // Estado da lista. Só o ViewModel muda; a tela só observa
    var tarefas by mutableStateOf<List<Tarefa>>(emptyList())
        private set
    var carregandoLista by mutableStateOf(true)
        private set
    var erroLista by mutableStateOf<String?>(null)
        private set

    // Estado do formulário
    var salvando by mutableStateOf(false)
        private set
    var erroFormulario by mutableStateOf<String?>(null)
        private set

    // Roda uma vez, quando o ViewModel nasce. Girar o celular NÃO repete isto
    init { carregarTarefas() }

    fun carregarTarefas() {
        viewModelScope.launch {
            carregandoLista = true
            erroLista = null

            if (!temConexaoInternet(getApplication())) {
                erroLista = "Sem conexão com a internet. Verifique sua rede e tente novamente."
                carregandoLista = false
                return@launch
            }

            try {
                tarefas = try {
                    repo.listar()
                } catch (e: SocketTimeoutException) {
                    Log.w("TaskFlowAPI", "Timeout na 1ª tentativa (possível cold start do Azure), tentando de novo...")
                    repo.listar()
                }
            } catch (e: SocketTimeoutException) {
                erroLista = "O servidor demorou demais pra responder (pode estar iniciando). Tente novamente em instantes."
            } catch (e: Exception) {
                erroLista = "Falha na conexão: ${e.message}"
                Log.e("TaskFlowAPI", "Erro ao buscar tarefas", e)
            } finally {
                carregandoLista = false
            }
        }
    }

    // id == null: cadastrando. id com valor: editando
    fun salvar(
        tarefaParaEditar: Tarefa?,
        titulo: String, descricao: String, status: String,
        prioridade: String, disciplina: String, dataEntrega: String,
        aoConcluir: (sucesso: Boolean, mensagem: String) -> Unit
    ) {
        // Regra da tela: validar antes de mandar para a API
        if (titulo.isBlank() || disciplina.isBlank()) {
            erroFormulario = "Título e disciplina são obrigatórios"
            return
        }
        erroFormulario = null
        salvando = true
        val tarefa = Tarefa(
            id = tarefaParaEditar?.id,
            titulo = titulo,
            descricao = descricao.ifBlank { null },
            status = status,
            prioridade = prioridade,
            disciplina = disciplina,
            dataEntrega = dataEntrega.ifBlank { null }
        )
        viewModelScope.launch {
            try {
                repo.salvar(tarefa)
                carregarTarefas() // depois de gravar com sucesso, pede a lista de novo
                aoConcluir(true, if (tarefaParaEditar == null) "Tarefa criada com sucesso!" else "Tarefa atualizada com sucesso!")
            } catch (e: Exception) {
                erroFormulario = "Erro ao salvar: ${e.message}"
                Log.e("TaskFlowAPI", "Erro ao salvar tarefa", e)
                aoConcluir(false, "Não foi possível salvar a tarefa.")
            } finally {
                salvando = false
            }
        }
    }

    fun excluir(id: Long, aoConcluir: (sucesso: Boolean, mensagem: String) -> Unit) {
        viewModelScope.launch {
            try {
                repo.excluir(id)
                carregarTarefas()
                aoConcluir(true, "Tarefa excluída.")
            } catch (e: Exception) {
                Log.e("TaskFlowAPI", "Erro ao excluir tarefa", e)
                aoConcluir(false, "Não foi possível excluir: ${e.message}")
            }
        }
    }

    fun limparErroFormulario() { erroFormulario = null }
}
