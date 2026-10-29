package com.riobranco.parkingflow.mobile

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.riobranco.parkingflow.mobile.ui.theme.TaskFlowMobileTheme
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskFlowMobileTheme {
                AppTaskFlow()
            }
        }
    }
}

private sealed class Tela {
    object Lista : Tela()
    data class Detalhe(val tarefa: Tarefa) : Tela()
    data class Formulario(val tarefaParaEditar: Tarefa?) : Tela()
}

private fun tarefaDoMapa(mapa: Map<String, Any?>): Tarefa? {
    val titulo = mapa["titulo"] as? String ?: return null
    return Tarefa(
        id = mapa["id"] as? Long,
        titulo = titulo,
        descricao = mapa["descricao"] as? String,
        status = mapa["status"] as? String ?: "A_FAZER",
        prioridade = mapa["prioridade"] as? String ?: "BAIXA",
        disciplina = mapa["disciplina"] as? String ?: "",
        dataEntrega = mapa["dataEntrega"] as? String
    )
}

private val telaSaver = mapSaver(
    save = { tela: Tela ->
        when (tela) {
            is Tela.Lista -> mapOf("tipo" to "lista")
            is Tela.Detalhe -> mapOf(
                "tipo" to "detalhe",
                "id" to tela.tarefa.id,
                "titulo" to tela.tarefa.titulo,
                "descricao" to tela.tarefa.descricao,
                "status" to tela.tarefa.status,
                "prioridade" to tela.tarefa.prioridade,
                "disciplina" to tela.tarefa.disciplina,
                "dataEntrega" to tela.tarefa.dataEntrega
            )
            is Tela.Formulario -> mapOf(
                "tipo" to "formulario",
                "id" to tela.tarefaParaEditar?.id,
                "titulo" to tela.tarefaParaEditar?.titulo,
                "descricao" to tela.tarefaParaEditar?.descricao,
                "status" to tela.tarefaParaEditar?.status,
                "prioridade" to tela.tarefaParaEditar?.prioridade,
                "disciplina" to tela.tarefaParaEditar?.disciplina,
                "dataEntrega" to tela.tarefaParaEditar?.dataEntrega
            )
        }
    },
    restore = { mapa ->
        when (mapa["tipo"]) {
            "detalhe" -> tarefaDoMapa(mapa)?.let { Tela.Detalhe(it) } ?: Tela.Lista
            "formulario" -> Tela.Formulario(tarefaDoMapa(mapa))
            else -> Tela.Lista
        }
    }
)

@Composable
fun AppTaskFlow() {
    var telaAtual by rememberSaveable(stateSaver = telaSaver) { mutableStateOf<Tela>(Tela.Lista) }
    var versaoLista by rememberSaveable { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val escopoSnackbar = rememberCoroutineScope()

    val mostrarSnackbar: (String) -> Unit = { mensagem ->
        escopoSnackbar.launch { snackbarHostState.showSnackbar(mensagem) }
    }

    when (val tela = telaAtual) {
        is Tela.Lista -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                floatingActionButton = {
                    FloatingActionButton(onClick = { telaAtual = Tela.Formulario(null) }) {
                        Text("+", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            ) { innerPadding ->
                TelaListaTarefas(
                    modifier = Modifier.padding(innerPadding),
                    chaveAtualizacao = versaoLista,
                    onTarefaClick = { tarefa -> telaAtual = Tela.Detalhe(tarefa) }
                )
            }
        }
        is Tela.Detalhe -> {
            TelaDetalheTarefa(
                tarefa = tela.tarefa,
                onVoltar = { telaAtual = Tela.Lista },
                onEditar = { telaAtual = Tela.Formulario(tela.tarefa) },
                onExcluido = {
                    versaoLista++
                    telaAtual = Tela.Lista
                },
                mostrarSnackbar = mostrarSnackbar,
                snackbarHostState = snackbarHostState
            )
        }
        is Tela.Formulario -> {
            TelaFormularioTarefa(
                tarefaParaEditar = tela.tarefaParaEditar,
                onSalvar = {
                    versaoLista++
                    telaAtual = Tela.Lista
                },
                onCancelar = {
                    telaAtual = tela.tarefaParaEditar?.let { Tela.Detalhe(it) } ?: Tela.Lista
                },
                mostrarSnackbar = mostrarSnackbar,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@Composable
fun TelaListaTarefas(
    modifier: Modifier = Modifier,
    chaveAtualizacao: Int = 0,
    onTarefaClick: (Tarefa) -> Unit = {}
) {
    val contexto = LocalContext.current
    var tarefas by remember { mutableStateOf<List<Tarefa>>(emptyList()) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var tentativaManual by remember { mutableStateOf(0) }
    var filtroStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var filtroPrioridade by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(chaveAtualizacao, tentativaManual) {
        carregando = true
        erro = null

        if (!temConexaoInternet(contexto)) {
            erro = "Sem conexão com a internet. Verifique sua rede e tente novamente."
            carregando = false
            return@LaunchedEffect
        }

        try {
            val resposta = try {
                RetrofitClient.api.listarTarefas()
            } catch (e: SocketTimeoutException) {
                Log.w("TaskFlowAPI", "Timeout na primeira tentativa (possível cold start do Azure), tentando de novo...")
                RetrofitClient.api.listarTarefas()
            }
            if (resposta.isSuccessful) {
                tarefas = resposta.body() ?: emptyList()
            } else {
                erro = "Erro ao buscar tarefas (HTTP ${resposta.code()})"
            }
        } catch (e: SocketTimeoutException) {
            erro = "O servidor demorou demais pra responder (pode estar iniciando). Tente novamente em instantes."
        } catch (e: Exception) {
            erro = "Falha na conexão: ${e.message}"
            Log.e("TaskFlowAPI", "Erro ao buscar tarefas", e)
        } finally {
            carregando = false
        }
    }

    val tarefasFiltradas = tarefas.filter { tarefa ->
        (filtroStatus == null || tarefa.status == filtroStatus) &&
                (filtroPrioridade == null || tarefa.prioridade == filtroPrioridade)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (!carregando && erro == null && tarefas.isNotEmpty()) {
            FiltrosDeLista(
                filtroStatus = filtroStatus,
                onFiltroStatusChange = { filtroStatus = it },
                filtroPrioridade = filtroPrioridade,
                onFiltroPrioridadeChange = { filtroPrioridade = it }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                carregando -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                erro != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = erro ?: "Erro desconhecido", textAlign = TextAlign.Center)
                        Button(
                            onClick = { tentativaManual++ },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Tentar novamente")
                        }
                    }
                }
                tarefas.isEmpty() -> {
                    Text(
                        text = "Nenhuma tarefa cadastrada ainda.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                tarefasFiltradas.isEmpty() -> {
                    Text(
                        text = "Nenhuma tarefa encontrada com esses filtros.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(tarefasFiltradas) { tarefa ->
                            TarefaCard(tarefa, onClick = { onTarefaClick(tarefa) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosDeLista(
    filtroStatus: String?,
    onFiltroStatusChange: (String?) -> Unit,
    filtroPrioridade: String?,
    onFiltroPrioridadeChange: (String?) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Status", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filtroStatus == null,
                onClick = { onFiltroStatusChange(null) },
                label = { Text("Todos") }
            )
            listOf("A_FAZER", "FAZENDO", "FEITO").forEach { opcao ->
                FilterChip(
                    selected = filtroStatus == opcao,
                    onClick = { onFiltroStatusChange(if (filtroStatus == opcao) null else opcao) },
                    label = { Text(opcao) }
                )
            }
        }

        Text(
            text = "Prioridade",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filtroPrioridade == null,
                onClick = { onFiltroPrioridadeChange(null) },
                label = { Text("Todas") }
            )
            listOf("BAIXA", "MEDIA", "ALTA").forEach { opcao ->
                FilterChip(
                    selected = filtroPrioridade == opcao,
                    onClick = { onFiltroPrioridadeChange(if (filtroPrioridade == opcao) null else opcao) },
                    label = { Text(opcao) }
                )
            }
        }
    }
}

@Composable
fun TarefaCard(tarefa: Tarefa, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tarefa.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!tarefa.descricao.isNullOrBlank()) {
                Text(
                    text = tarefa.descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Tag(texto = tarefa.status, cor = corStatus(tarefa.status))
                Tag(texto = tarefa.prioridade, cor = corPrioridade(tarefa.prioridade))
            }

            Text(
                text = "${tarefa.disciplina}${tarefa.dataEntrega?.let { " • $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun Tag(texto: String, cor: Color) {
    Surface(
        color = cor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = texto,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

fun corPrioridade(prioridade: String): Color = when (prioridade.uppercase()) {
    "ALTA", "HIGH" -> Color(0xFFD32F2F)
    "MEDIA", "MÉDIA", "MEDIUM" -> Color(0xFFF57C00)
    "BAIXA", "LOW" -> Color(0xFF388E3C)
    else -> Color.Gray
}

fun corStatus(status: String): Color = when (status.uppercase()) {
    "FEITO" -> Color(0xFF388E3C)
    "FAZENDO" -> Color(0xFF1976D2)
    "A_FAZER" -> Color(0xFF757575)
    else -> Color.Gray
}