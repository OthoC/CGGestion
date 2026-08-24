package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.EquipoEntity
import com.example.cggestion.data.repository.ClienteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientesViewModel(private val repository: ClienteRepository) : ViewModel() {
    val clientes = repository.clientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun guardar(cliente: ClienteEntity, alError: (String) -> Unit, alExito: () -> Unit) = viewModelScope.launch {
        runCatching { repository.guardar(cliente) }
            .onSuccess { alExito() }
            .onFailure { alError(it.message ?: "No se pudo guardar el cliente.") }
    }
    fun equipos(clienteId: Long) = repository.equipos(clienteId)
    fun hojasEquipo(equipoId: Long) = repository.hojasEquipo(equipoId)
    fun guardarEquipo(equipo: EquipoEntity, alError: (String) -> Unit, alExito: () -> Unit) = viewModelScope.launch {
        runCatching { repository.guardarEquipo(equipo) }
            .onSuccess { alExito() }
            .onFailure { alError(it.message ?: "No se pudo guardar el equipo.") }
    }

    companion object {
        fun factory(repository: ClienteRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ClientesViewModel(repository) as T
        }
    }
}
