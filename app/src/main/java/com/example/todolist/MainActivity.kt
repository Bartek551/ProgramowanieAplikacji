package com.example.todolist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay

data class Task(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val isDone: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoApp()
        }
    }
}

@Composable
fun ToDoApp() {
    val context = LocalContext.current
    var isDarkTheme by remember { mutableStateOf(loadTheme(context)) }
    var tasks by remember { mutableStateOf(loadTasks(context)) }

    fun toggleTheme(isDark: Boolean) {
        isDarkTheme = isDark
        saveTheme(context, isDark)
    }

    fun addTask(name: String) {
        tasks = tasks + Task(name = name)
        saveTasks(context, tasks)
    }

    fun removeTask(task: Task) {
        tasks = tasks - task
        saveTasks(context, tasks)
    }

    fun toggleTaskDone(task: Task) {
        tasks = tasks.map { if (it.id == task.id) it.copy(isDone = !it.isDone) else it }
        saveTasks(context, tasks)
    }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") { SplashScreen(navController) }
            composable("home") { HomeScreen(tasks, navController, { removeTask(it) }, { toggleTaskDone(it) }) }
            composable("add") { AddScreen(navController) { addTask(it) } }
            composable("settings") { SettingsScreen(navController, isDarkTheme) { toggleTheme(it) } }
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(key1 = true) {
        scale.animateTo(targetValue = 1.2f, animationSpec = tween(durationMillis = 800))
        delay(1500)
        navController.navigate("home") { popUpTo("splash") { inclusive = true } }
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp).scale(scale.value),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(tasks: List<Task>, navController: NavController, onDelete: (Task) -> Unit, onToggle: (Task) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Moje Zadania") }, actions = {
                IconButton(onClick = { navController.navigate("settings") }) { Icon(Icons.Default.Settings, null) }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add") }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { Text("Brak zadań!", color = Color.Gray) }
        } else {
            LazyColumn(contentPadding = padding) {
                items(tasks) { task ->
                    Card(Modifier.fillMaxWidth().padding(8.dp).clickable { onToggle(task) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                task.name,
                                Modifier.weight(1f),
                                fontSize = 18.sp,
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                color = if (task.isDone) Color.Gray else Color.Unspecified
                            )
                            IconButton(onClick = { onDelete(task) }) { Icon(Icons.Default.Delete, null) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(navController: NavController, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Dodaj zadanie") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(text, { text = it }, label = { Text("Co masz do zrobienia?") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button({ if (text.isNotBlank()) { onSave(text); navController.popBackStack() } }, Modifier.fillMaxWidth()) { Text("ZAPISZ") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Ustawienia") }, navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Tryb ciemny")
                Switch(isDarkTheme, onThemeChange)
            }
            Spacer(Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Icon(Icons.Default.Info, null, Modifier.size(48.dp), tint = Color.Gray)
            Text("O aplikacji", style = MaterialTheme.typography.titleMedium)
            Text("Wersja: 1.0", color = Color.Gray)
            Text("Autor: Bartek D", color = Color.Gray)
        }
    }
}

fun saveTasks(c: Context, t: List<Task>) = c.getSharedPreferences("prefs", 0).edit().putString("tasks", Gson().toJson(t)).apply()
fun loadTasks(c: Context): List<Task> {
    val json = c.getSharedPreferences("prefs", 0).getString("tasks", null) ?: return emptyList()
    return Gson().fromJson(json, object : TypeToken<List<Task>>() {}.type)
}
fun saveTheme(c: Context, d: Boolean) = c.getSharedPreferences("prefs", 0).edit().putBoolean("dark", d).apply()
fun loadTheme(c: Context) = c.getSharedPreferences("prefs", 0).getBoolean("dark", false)