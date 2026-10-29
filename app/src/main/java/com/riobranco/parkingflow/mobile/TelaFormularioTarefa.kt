package com.riobranco.parkingflow.mobile

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val OPCOES_STATUS = listOf("A_FAZER", "FAZENDO", "FEITO")
private val OPCOES_PRIORIDADE = listOf("BAIXA", "MEDIA", "ALTA")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFormularioTarefa(
    tarefaParaEditar: Tarefa?,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit,
    mostrarSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var titulo by rememberSaveable { mutableStateOf(tarefaParaEditar?.titulo ?: "") }
    var descricao by rememberSaveable { mutableStateOf(tarefaParaEditar?.descricao ?: "") }
    var disciplina by rememberSaveable { mutableStateOf(tarefaParaEditar?.disciplina ?: "") }
    var dataEntrega by rememberSaveable { mutableStateOf(tarefaParaEditar?.dataEntrega ?: "") }
    var status by rememberSaveable { mutableStateOf(tarefaParaEditar?.status ?: OPCOES_STATUS.first()) }
    var prioridade by rememberSaveable { mutableStateOf(tarefaParaEditar?.prioridade ?: OPCOES_PRIORIDADE.first()) }

    var salvando by remember { mutableStateOf(false) }
    var erro by rememberSaveable { mutableStateOf<String?>(null) }

    val escopo = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (tarefaParaEditar == null) "Nova tarefa" else "Editar tarefa") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descricao,
                onValueChange = { descricao = it },
                label = { Text("Descrição") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = false,
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            OutlinedTextField(
                value = disciplina,
                onValueChange = { disciplina = it },
                label = { Text("Disciplina") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            SeletorData(
                rotulo = "Data de entrega",
                dataSelecionada = dataEntrega,
                onDataSelecionada = { dataEntrega = it },
                modifier = Modifier.padding(top = 12.dp)
            )

            SeletorDropdown(
                rotulo = "Status",
                opcoes = OPCOES_STATUS,
                selecionado = status,
                onSelecionar = { status = it },
                modifier = Modifier.padding(top = 12.dp)
            )

            SeletorDropdown(
                rotulo = "Prioridade",
                opcoes = OPCOES_PRIORIDADE,
                selecionado = prioridade,
                onSelecionar = { prioridade = it },
                modifier = Modifier.padding(top = 12.dp)
            )

            if (erro != null) {
                Text(
                    text = erro ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancelar, enabled = !salvando) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        if (titulo.isBlank() || disciplina.isBlank()) {
                            erro = "Título e disciplina são obrigatórios"
                            return@Button
                        }
                        erro = null
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
                        escopo.launch {
                            try {
                                val resposta = if (tarefaParaEditar?.id != null) {
                                    RetrofitClient.api.atualizarTarefa(tarefaParaEditar.id, tarefa)
                                } else {
                                    RetrofitClient.api.criarTarefa(tarefa)
                                }
                                if (resposta.isSuccessful) {
                                    mostrarSnackbar(
                                        if (tarefaParaEditar == null) "Tarefa criada com sucesso!" else "Tarefa atualizada com sucesso!"
                                    )
                                    onSalvar()
                                } else {
                                    erro = "Erro ao salvar (HTTP ${resposta.code()})"
                                    mostrarSnackbar("Não foi possível salvar a tarefa.")
                                }
                            } catch (e: Exception) {
                                erro = "Falha na conexão: ${e.message}"
                                mostrarSnackbar("Falha na conexão ao salvar.")
                                Log.e("TaskFlowAPI", "Erro ao salvar tarefa", e)
                            } finally {
                                salvando = false
                            }
                        }
                    },
                    enabled = !salvando,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (salvando) "Salvando..." else "Salvar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorData(
    rotulo: String,
    dataSelecionada: String,
    onDataSelecionada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = dataSelecionada,
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            placeholder = { Text("Toque para escolher") },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { mostrarCalendario = true }
        )
    }

    if (mostrarCalendario) {
        val estadoCalendario = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoCalendario.selectedDateMillis?.let { millis ->
                        val formatador = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onDataSelecionada(formatador.format(Date(millis)))
                    }
                    mostrarCalendario = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = estadoCalendario)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorDropdown(
    rotulo: String,
    opcoes: List<String>,
    selecionado: String,
    onSelecionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selecionado,
            onValueChange = {},
            readOnly = true,
            label = { Text(rotulo) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(opcao) },
                    onClick = {
                        onSelecionar(opcao)
                        expandido = false
                    }
                )
            }
        }
    }
}