package com.riobranco.parkingflow.mobile

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDetalheTarefa(
    tarefa: Tarefa,
    onVoltar: () -> Unit,
    onEditar: () -> Unit,
    onExcluido: () -> Unit,
    mostrarSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var mostrarConfirmacaoExclusao by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    if (mostrarConfirmacaoExclusao) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacaoExclusao = false },
            title = { Text("Excluir tarefa?") },
            text = { Text("Essa ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacaoExclusao = false
                    tarefa.id?.let { id ->
                        escopo.launch {
                            try {
                                val resposta = RetrofitClient.api.deletarTarefa(id)
                                if (resposta.isSuccessful) {
                                    mostrarSnackbar("Tarefa excluída.")
                                    onExcluido()
                                } else {
                                    mostrarSnackbar("Não foi possível excluir (HTTP ${resposta.code()})")
                                }
                            } catch (e: Exception) {
                                mostrarSnackbar("Falha na conexão ao excluir.")
                                Log.e("TaskFlowAPI", "Erro ao excluir tarefa", e)
                            }
                        }
                    }
                }) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacaoExclusao = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Detalhes da tarefa") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarConfirmacaoExclusao = true }) {
                        Text("🗑")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                text = tarefa.titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            if (!tarefa.descricao.isNullOrBlank()) {
                Text(
                    text = tarefa.descricao,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Tag(texto = tarefa.status, cor = corStatus(tarefa.status))
                Tag(texto = tarefa.prioridade, cor = corPrioridade(tarefa.prioridade))
            }

            InformacaoLinha(rotulo = "Disciplina", valor = tarefa.disciplina)
            InformacaoLinha(rotulo = "Data de entrega", valor = tarefa.dataEntrega ?: "Não definida")

            Button(
                onClick = onEditar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Editar tarefa")
            }

            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Voltar")
            }
        }
    }
}

@Composable
private fun InformacaoLinha(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text(text = "$rotulo: ", fontWeight = FontWeight.Bold)
        Text(text = valor)
    }
}