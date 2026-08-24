package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.EquipoEntity
import com.example.cggestion.data.local.entity.EstadoMantenimiento
import com.example.cggestion.data.local.entity.MantenimientoEntity
import com.example.cggestion.data.repository.MantenimientoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContextoMantenimientoHoja(val mantenimiento: MantenimientoEntity, val cliente: ClienteEntity, val equipo: EquipoEntity)
data class MantenimientoUi(
    val mantenimiento: MantenimientoEntity = MantenimientoEntity(clienteId = 0, equipoId = 0, fechaProgramada = System.currentTimeMillis()),
    val guardando: Boolean = false,
    val mensaje: String? = null
)

class MantenimientoViewModel(private val repository: MantenimientoRepository) : ViewModel() {
    private val _ui = MutableStateFlow(MantenimientoUi())
    val ui: StateFlow<MantenimientoUi> = _ui.asStateFlow()
    private val _eventoHoja = MutableStateFlow<ContextoMantenimientoHoja?>(null)
    val eventoHoja: StateFlow<ContextoMantenimientoHoja?> = _eventoHoja.asStateFlow()
    val mantenimientos = repository.resumenes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val clientes = repository.clientes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val equipos = ui.map { it.mantenimiento.clienteId }.distinctUntilChanged().flatMapLatest { id ->
        if (id == 0L) flowOf(emptyList()) else repository.equipos(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun nuevo() { _ui.value = MantenimientoUi() }
    fun editar(mantenimiento: MantenimientoEntity) { _ui.value = MantenimientoUi(mantenimiento = mantenimiento) }
    fun actualizar(cambio: (MantenimientoEntity) -> MantenimientoEntity) { _ui.value = _ui.value.copy(mantenimiento = cambio(_ui.value.mantenimiento), mensaje = null) }
    fun seleccionarCliente(cliente: ClienteEntity) { actualizar { it.copy(clienteId = cliente.id, equipoId = 0) } }
    fun seleccionarEquipo(equipo: EquipoEntity) { actualizar { it.copy(equipoId = equipo.id) } }

    fun guardar(alExito: () -> Unit) {
        val actual = _ui.value
        if (actual.guardando) return
        viewModelScope.launch {
            _ui.value = actual.copy(guardando = true, mensaje = null)
            runCatching { repository.guardar(actual.mantenimiento) }
                .onSuccess { id -> _ui.value = _ui.value.copy(mantenimiento = _ui.value.mantenimiento.copy(id = id), guardando = false, mensaje = "Mantenimiento guardado."); alExito() }
                .onFailure { _ui.value = _ui.value.copy(guardando = false, mensaje = it.message ?: "No se pudo guardar el mantenimiento.") }
        }
    }

    fun cambiarEstado(id: Long, estado: EstadoMantenimiento) = viewModelScope.launch {
        runCatching { repository.actualizarEstado(id, estado.name) }
            .onFailure { _ui.value = _ui.value.copy(mensaje = it.message ?: "No se pudo actualizar el estado.") }
    }

    fun iniciarHoja(id: Long) = viewModelScope.launch {
        runCatching {
            val mantenimiento = repository.mantenimiento(id) ?: error("No se encontró el mantenimiento.")
            if (mantenimiento.hojaCampoId != null) error("Este mantenimiento ya tiene una hoja vinculada.")
            val cliente = repository.cliente(mantenimiento.clienteId) ?: error("No se encontró el cliente.")
            val equipo = repository.equipo(mantenimiento.equipoId) ?: error("No se encontró el equipo.")
            repository.actualizarEstado(id, EstadoMantenimiento.EN_PROCESO.name)
            ContextoMantenimientoHoja(mantenimiento.copy(estado = EstadoMantenimiento.EN_PROCESO.name), cliente, equipo)
        }.onSuccess { _eventoHoja.value = it }
            .onFailure { _ui.value = _ui.value.copy(mensaje = it.message ?: "No se pudo iniciar la hoja.") }
    }

    fun consumirEventoHoja() { _eventoHoja.value = null }
    companion object {
        fun factory(repository: MantenimientoRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = MantenimientoViewModel(repository) as T
        }
    }
}
