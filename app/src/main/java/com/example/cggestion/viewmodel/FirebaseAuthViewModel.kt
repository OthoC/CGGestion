package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.auth.PerfilUsuario
import com.example.cggestion.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoAutenticacion {
    data object Inicializando : EstadoAutenticacion
    data class SinSesion(
        val procesando: Boolean = false,
        val error: String? = null,
        val mensaje: String? = null,
        val firebaseConfigurado: Boolean = true
    ) : EstadoAutenticacion
    data class Autenticado(val perfil: PerfilUsuario) : EstadoAutenticacion
}

class FirebaseAuthViewModel(private val repository: FirebaseAuthRepository) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoAutenticacion>(EstadoAutenticacion.Inicializando)
    val estado: StateFlow<EstadoAutenticacion> = _estado.asStateFlow()

    init {
        restaurarSesion()
    }

    fun iniciarSesion(email: String, password: String) {
        val actual = _estado.value as? EstadoAutenticacion.SinSesion
        if (actual?.procesando == true) return
        _estado.value = EstadoAutenticacion.SinSesion(procesando = true, firebaseConfigurado = repository.configurado)
        viewModelScope.launch {
            runCatching { repository.iniciarSesion(email, password) }
                .onSuccess { _estado.value = EstadoAutenticacion.Autenticado(it) }
                .onFailure {
                    _estado.value = EstadoAutenticacion.SinSesion(
                        error = repository.mensajeError(it),
                        firebaseConfigurado = repository.configurado
                    )
                }
        }
    }

    fun recuperarClave(email: String) {
        val actual = _estado.value as? EstadoAutenticacion.SinSesion
        if (actual?.procesando == true) return
        _estado.value = EstadoAutenticacion.SinSesion(procesando = true, firebaseConfigurado = repository.configurado)
        viewModelScope.launch {
            runCatching { repository.enviarRestablecimiento(email) }
                .onSuccess {
                    _estado.value = EstadoAutenticacion.SinSesion(
                        mensaje = "Enviamos un enlace para restablecer la contraseña.",
                        firebaseConfigurado = true
                    )
                }
                .onFailure {
                    _estado.value = EstadoAutenticacion.SinSesion(
                        error = repository.mensajeError(it),
                        firebaseConfigurado = repository.configurado
                    )
                }
        }
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
        _estado.value = EstadoAutenticacion.SinSesion(firebaseConfigurado = repository.configurado)
    }

    fun limpiarAvisos() {
        val actual = _estado.value as? EstadoAutenticacion.SinSesion ?: return
        _estado.value = actual.copy(error = null, mensaje = null)
    }

    private fun restaurarSesion() {
        if (!repository.configurado) {
            _estado.value = EstadoAutenticacion.SinSesion(
                error = repository.mensajeError(com.example.cggestion.data.repository.ConfiguracionFirebaseException()),
                firebaseConfigurado = false
            )
            return
        }
        viewModelScope.launch {
            runCatching { repository.restaurarSesion() }
                .onSuccess { perfil ->
                    _estado.value = perfil?.let(EstadoAutenticacion::Autenticado)
                        ?: EstadoAutenticacion.SinSesion(firebaseConfigurado = true)
                }
                .onFailure {
                    _estado.value = EstadoAutenticacion.SinSesion(
                        error = repository.mensajeError(it),
                        firebaseConfigurado = true
                    )
                }
        }
    }

    companion object {
        fun factory(repository: FirebaseAuthRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FirebaseAuthViewModel(repository) as T
        }
    }
}
