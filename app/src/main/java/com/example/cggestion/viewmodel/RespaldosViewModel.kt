package com.example.cggestion.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.util.backup.BackupManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EstadoRespaldos(
    val trabajando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null,
    val archivo: File? = null,
    val restaurado: Boolean = false,
    val carpetaNubeConfigurada: Boolean = false
)

class RespaldosViewModel(private val manager: BackupManager) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoRespaldos(archivo = manager.ultimoRespaldo(), carpetaNubeConfigurada = manager.carpetaNube() != null))
    val estado: StateFlow<EstadoRespaldos> = _estado.asStateFlow()

    fun crear() { if (_estado.value.trabajando) return; ejecutar("Respaldo creado correctamente.") { manager.crear().archivo } }
    fun configurarCarpetaNube(uri: Uri) {
        manager.guardarCarpetaNube(uri)
        _estado.value = _estado.value.copy(carpetaNubeConfigurada = true, mensaje = "Carpeta de nube configurada.", error = null)
    }
    fun subirANube() {
        if (_estado.value.trabajando) return
        _estado.value = _estado.value.copy(trabajando = true, mensaje = null, error = null)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { manager.subirANube() } }
                .onSuccess { _estado.value = EstadoRespaldos(mensaje = "Respaldo subido a la carpeta seleccionada.", archivo = manager.ultimoRespaldo(), carpetaNubeConfigurada = true) }
                .onFailure { _estado.value = _estado.value.copy(trabajando = false, error = it.message ?: "No se pudo subir el respaldo.") }
        }
    }
    fun restaurar(uri: Uri) {
        if (_estado.value.trabajando) return
        _estado.value = EstadoRespaldos(trabajando = true)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { manager.restaurar(uri) } }
                .onSuccess { _estado.value = EstadoRespaldos(mensaje = "Respaldo restaurado. Reiniciando la aplicación…", restaurado = true, carpetaNubeConfigurada = manager.carpetaNube() != null) }
                .onFailure { _estado.value = EstadoRespaldos(error = it.message ?: "No se pudo restaurar el respaldo.") }
        }
    }
    fun limpiarMensaje() { _estado.value = _estado.value.copy(mensaje = null, error = null) }
    private fun ejecutar(exito: String, accion: () -> File) {
        _estado.value = EstadoRespaldos(trabajando = true)
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { accion() } }
                .onSuccess { _estado.value = EstadoRespaldos(mensaje = exito, archivo = it, carpetaNubeConfigurada = manager.carpetaNube() != null) }
                .onFailure { _estado.value = EstadoRespaldos(error = it.message ?: "No se pudo crear el respaldo.") }
        }
    }
    companion object { fun factory(manager: BackupManager) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = RespaldosViewModel(manager) as T } }
}
