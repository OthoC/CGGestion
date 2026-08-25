package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.SesionUsuario
import com.example.cggestion.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EstadoAuth(
    val cargando: Boolean = true,
    val configurado: Boolean = false,
    val sesion: SesionUsuario? = null,
    val error: String? = null
) { val esAdministrador: Boolean get() = sesion?.esAdministrador == true }

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _ui = MutableStateFlow(EstadoAuth())
    val ui: StateFlow<EstadoAuth> = _ui.asStateFlow()
    val usuarios = repository.usuarios()

    init { recargarConfiguracion() }

    fun recargarConfiguracion() = viewModelScope.launch {
        val configurado = withContext(Dispatchers.IO) { repository.hayUsuarios() }
        _ui.value = _ui.value.copy(cargando = false, configurado = configurado)
    }

    fun crearPrimerAdministrador(usuario: String, clave: String, repetir: String) {
        if (clave != repetir) return error("Las contraseñas no coinciden.")
        ejecutar { repository.crearPrimerAdministrador(usuario, clave.toCharArray()) }
    }

    fun ingresar(usuario: String, clave: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        val sesion = runCatching { withContext(Dispatchers.IO) { repository.autenticar(usuario, clave.toCharArray()) } }.getOrNull()
        _ui.value = if (sesion == null) _ui.value.copy(cargando = false, error = "Usuario o contraseña incorrectos.") else _ui.value.copy(cargando = false, sesion = sesion, error = null)
    }
    fun validarAdministrador(usuario: String, clave: String, resultado: (Boolean) -> Unit) = viewModelScope.launch {
        val sesion = runCatching { withContext(Dispatchers.IO) { repository.autenticar(usuario, clave.toCharArray()) } }.getOrNull()
        resultado(sesion?.esAdministrador == true)
    }

    fun bloquear() { _ui.value = _ui.value.copy(sesion = null, error = null) }
    fun limpiarError() { _ui.value = _ui.value.copy(error = null) }
    fun crearUsuario(usuario: String, clave: String, rol: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        runCatching { withContext(Dispatchers.IO) { repository.crearUsuario(usuario, clave.toCharArray(), rol) } }
            .onSuccess { _ui.value = _ui.value.copy(cargando = false) }
            .onFailure { _ui.value = _ui.value.copy(cargando = false, error = it.message ?: "No se pudo crear el usuario.") }
    }
    fun cambiarClave(id: Long, clave: String) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        runCatching { withContext(Dispatchers.IO) { repository.cambiarClave(id, clave.toCharArray()) } }
            .onSuccess { _ui.value = _ui.value.copy(cargando = false) }
            .onFailure { _ui.value = _ui.value.copy(cargando = false, error = it.message ?: "No se pudo cambiar la contraseña.") }
    }
    fun cambiarEstado(id: Long, activo: Boolean) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        runCatching { withContext(Dispatchers.IO) { repository.cambiarEstado(id, activo) } }
            .onSuccess { _ui.value = _ui.value.copy(cargando = false) }
            .onFailure { _ui.value = _ui.value.copy(cargando = false, error = it.message ?: "No se pudo actualizar el usuario.") }
    }
    private fun ejecutar(accion: suspend () -> SesionUsuario) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, error = null)
        runCatching { withContext(Dispatchers.IO) { accion() } }
            .onSuccess { sesion -> _ui.value = _ui.value.copy(cargando = false, configurado = true, sesion = sesion) }
            .onFailure { error -> _ui.value = _ui.value.copy(cargando = false, error = error.message ?: "No se pudo crear el usuario.") }
    }
    private fun error(mensaje: String) { _ui.value = _ui.value.copy(error = mensaje) }
    companion object { fun factory(repository: AuthRepository) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T } }
}
