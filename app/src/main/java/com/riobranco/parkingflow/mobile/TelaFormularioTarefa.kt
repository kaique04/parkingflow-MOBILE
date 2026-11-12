package com.riobranco.parkingflow.mobile

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val OPCOES_STATUS = listOf("A_FAZER", "FAZENDO", "FEITO")
private val OPCOES_PRIORIDADE = listOf("BAIXA", "MEDIA", "ALTA")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFormularioTarefa(
    viewModel: TarefaViewModel,
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

    // limpa erro de uma tentativa anterior ao abrir a tela de novo
    LaunchedEffect(Unit) { viewModel.limparErroFormulario() }

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

            if (viewModel.erroFormulario != null) {
                Text(
                    text = viewModel.erroFormulario ?: "",
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
                TextButton(onClick = onCancelar, enabled = !viewModel.salvando) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        viewModel.salvar(
                            tarefaParaEditar = tarefaParaEditar,
                            titulo = titulo, descricao = descricao, status = status,
                            prioridade = prioridade, disciplina = disciplina, dataEntrega = dataEntrega
                        ) { sucesso, mensagem ->
                            mostrarSnackbar(mensagem)
                            if (sucesso) onSalvar()
                        }
                    },
                    enabled = !viewModel.salvando,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(if (viewModel.salvando) "Salvando..." else "Salvar")
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )
        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
            modifier = Modifier.exposedDropdownSize()
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
