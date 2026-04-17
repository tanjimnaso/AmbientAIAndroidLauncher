package com.ambient.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class TodoItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val header: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class SaveStatus {
    SAVED, SAVING, ERROR
}

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val todosFile = File(application.filesDir, "todos.json")

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos

    private val _saveStatus = MutableStateFlow(SaveStatus.SAVED)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus

    private val _selectedIndex = MutableStateFlow<Int?>(null)
    val selectedIndex: StateFlow<Int?> = _selectedIndex

    @Volatile
    private var loaded = false

    init {
        loadTodos()
    }

    private fun loadTodos() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (todosFile.exists()) {
                    val json = todosFile.readText()
                    val items = Json.decodeFromString<List<TodoItem>>(json)
                    _todos.update { existing ->
                        // Merge: keep any items added before load completed
                        val loadedIds = items.map { it.id }.toSet()
                        val addedDuringLoad = existing.filter { it.id !in loadedIds }
                        items + addedDuringLoad
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loaded = true
            }
        }
    }

    fun addTodo(header: String, body: String) {
        if (header.isBlank() && body.isBlank()) return

        val newItem = TodoItem(header = header, body = body)
        _todos.update { it + newItem }
        saveTodos()
    }

    fun removeTodo(index: Int) {
        _todos.update { it.filterIndexed { i, _ -> i != index } }
        _selectedIndex.value = null
        saveTodos()
    }

    fun setSelectedIndex(index: Int?) {
        _selectedIndex.value = index
    }

    private fun saveTodos() {
        viewModelScope.launch(Dispatchers.IO) {
            _saveStatus.value = SaveStatus.SAVING
            try {
                val json = Json.encodeToString(_todos.value)
                todosFile.writeText(json)
                _saveStatus.value = SaveStatus.SAVED
                delay(1000)
                _saveStatus.value = SaveStatus.SAVED
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.ERROR
                e.printStackTrace()
            }
        }
    }
}
