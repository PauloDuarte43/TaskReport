package br.tec.pauloduarte.taskreport

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskDistributorApp()
                }
            }
        }
    }
}

data class Task(
    val id: Int,
    val project: String,
    val description: String,
    val days: Int
)

class TaskViewModel : ViewModel() {
    var totalHours by mutableStateOf("")
    var project by mutableStateOf("")
    var description by mutableStateOf("")
    var days by mutableStateOf("")
    var tasks by mutableStateOf<List<Task>>(emptyList())
    var summary by mutableStateOf("")
    var showingSummary by mutableStateOf(false)

    private var nextId = 1

    fun addTask() {
        val daysInt = days.toIntOrNull() ?: 1
        if (project.isNotBlank() && description.isNotBlank() && daysInt > 0) {
            tasks = tasks + Task(nextId++, project, description, daysInt)
            // Reset fields
            project = ""
            description = ""
            days = ""
        }
    }

    fun removeTask(task: Task) {
        tasks = tasks.filter { it.id != task.id }
    }

    fun generateSummary() {
        summary = calculateSummary()
        showingSummary = true
    }

    fun returnToEditing() {
        showingSummary = false
    }

    private fun calculateSummary(): String {
        val hoursMinutes = parseHoursMinutes(totalHours)
        if (hoursMinutes == null || tasks.isEmpty()) {
            return "Dados inválidos ou nenhuma tarefa cadastrada."
        }

        val totalMinutes = hoursMinutes.first * 60 + hoursMinutes.second
        val totalDays = tasks.sumOf { it.days }
        val minutesPerDay = totalMinutes.toFloat() / totalDays

        val sb = StringBuilder()
        sb.append("Total de horas: ${formatHoursMinutes(hoursMinutes.first, hoursMinutes.second)}\n")
        sb.append("Total de dias: $totalDays\n")
        sb.append("Horas por dia: ${formatHoursMinutes((minutesPerDay / 60).toInt(), (minutesPerDay % 60).toInt())}\n\n")

        tasks.forEach { task ->
            val taskMinutes = minutesPerDay * task.days
            val taskHours = (taskMinutes / 60).toInt()
            val taskMins = (taskMinutes % 60).toInt()

            sb.append("${task.project} - ${task.description}\n")
            sb.append("Tempo: ${formatHoursMinutes(taskHours, taskMins)}\n\n")
        }

        return sb.toString()
    }

    private fun parseHoursMinutes(input: String): Pair<Int, Int>? {
        val parts = input.split(":")
        if (parts.size != 2) return null

        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null

        if (hours < 0 || minutes < 0 || minutes >= 60) return null

        return Pair(hours, minutes)
    }

    private fun formatHoursMinutes(hours: Int, minutes: Int): String {
        return String.format("%02d:%02d", hours, minutes)
    }
}

@Composable
fun TaskDistributorApp(viewModel: TaskViewModel = viewModel()) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Alocação de Horas",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (viewModel.showingSummary) {
            SummaryScreen(viewModel, context)
        } else {
            EditScreen(viewModel)
        }
    }
}

@Composable
fun EditScreen(viewModel: TaskViewModel) {
    // Total hours input
    OutlinedTextField(
        value = viewModel.totalHours,
        onValueChange = { viewModel.totalHours = it },
        label = { Text("Total de Horas (HH:MM)") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Task inputs
    Text(
        text = "Adicionar Tarefa",
        style = MaterialTheme.typography.titleMedium
    )

    OutlinedTextField(
        value = viewModel.project,
        onValueChange = { viewModel.project = it },
        label = { Text("Projeto") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = viewModel.description,
        onValueChange = { viewModel.description = it },
        label = { Text("Descrição") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = viewModel.days,
        onValueChange = { viewModel.days = it },
        label = { Text("Dias") },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = { viewModel.addTask() }
        ) {
            Text("Adicionar Tarefa")
        }

        Button(
            onClick = { viewModel.generateSummary() },
            enabled = viewModel.tasks.isNotEmpty() && viewModel.totalHours.isNotBlank()
        ) {
            Text("Gerar Resumo")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Task list
    Text(
        text = "Tarefas",
        style = MaterialTheme.typography.titleMedium
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Outros elementos da UI aqui
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(viewModel.tasks) { task ->
                TaskItem(task = task, onRemove = { viewModel.removeTask(task) })
            }
        }
        // Outros elementos da UI aqui
    }
}

@Composable
fun SummaryScreen(viewModel: TaskViewModel, context: Context) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Resumo",
            style = MaterialTheme.typography.titleMedium
        )

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(text = viewModel.summary)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.returnToEditing() }
            ) {
                Text("Voltar para Edição")
            }

            Button(
                onClick = {
                    copyToClipboard(context, viewModel.summary)
                    Toast.makeText(context, "Copiado para a área de transferência", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Copiar")
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.project,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(text = task.description)
                Text(text = "Dias: ${task.days}")
            }

            IconButton(onClick = onRemove) {
                Text("X")
            }
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Resumo de Horas", text)
    clipboard.setPrimaryClip(clip)
}