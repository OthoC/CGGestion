package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.BuildConfig
import com.example.cggestion.data.repository.ActualizacionDisponible
import com.example.cggestion.data.repository.ActualizacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ActualizacionUi(
    val comprobando: Boolean = false,
    val descargando: Boolean = false,
    val disponible: ActualizacionDisponible? = null,
    val archivoListo: File? = null,
    val mensaje: String? = null,
    val error: String? = null,
    val versionActual: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
)

class ActualizacionViewModel(private val repository: ActualizacionRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ActualizacionUi())
    val ui: StateFlow<ActualizacionUi> = _ui.asStateFlow()

    init { comprobar(silencioso = true) }

    fun comprobar(silencioso: Boolean = false) {
        if (_ui.value.comprobando || _ui.value.descargando) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(comprobando = true, error = null, mensaje = null, archivoListo = null)
            runCatching { repository.comprobar() }
                .onSuccess { encontrada -> _ui.value = _ui.value.copy(comprobando = false, disponible = encontrada, mensaje = if (encontrada == null && !silencioso) "Ya tienes la versión más reciente." else null) }
                .onFailure { _ui.value = _ui.value.copy(comprobando = false, error = if (silencioso) null else (it.message ?: "No se pudo comprobar la actualización.")) }
        }
    }

    fun descargar() {
        val info = _ui.value.disponible ?: return
        if (_ui.value.descargando) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(descargando = true, error = null, mensaje = null)
            runCatching { repository.descargar(info) }
                .onSuccess { archivo -> _ui.value = _ui.value.copy(descargando = false, archivoListo = archivo, mensaje = "Actualización lista para instalar.") }
                .onFailure { _ui.value = _ui.value.copy(descargando = false, error = it.message ?: "No se pudo descargar la actualización.") }
        }
    }

    fun consumirInstalacion() { _ui.value = _ui.value.copy(archivoListo = null) }
    companion object {
        fun factory(repository: ActualizacionRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ActualizacionViewModel(repository) as T
        }
    }
}
