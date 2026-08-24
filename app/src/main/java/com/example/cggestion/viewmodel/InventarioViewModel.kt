package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.ProductoEntity
import com.example.cggestion.data.repository.InventarioRepository
import com.example.cggestion.data.repository.InventarioRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventarioViewModel(private val repository: InventarioRepository) : ViewModel() {
    val productos = repository.productos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val productosBajoMinimo = repository.productosBajoMinimo()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val productoConMovimientos = MutableStateFlow<Long?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val movimientos = productoConMovimientos
        .filterNotNull()
        .flatMapLatest(repository::movimientos)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cargarMovimientos(productoId: Long) {
        productoConMovimientos.value = productoId
    }

    fun limpiarMovimientos() {
        productoConMovimientos.value = null
    }

    fun guardar(producto: ProductoEntity, stockInicialTexto: String, alError: (String) -> Unit, alExito: () -> Unit = {}) = viewModelScope.launch {
        if (producto.nombre.isBlank()) {
            alError("Ingresa el nombre del producto.")
            return@launch
        }
        if (producto.precioPredeterminadoCentavos < 0) {
            alError("El precio no puede ser negativo.")
            return@launch
        }
        if (producto.stockMinimo < 0) {
            alError("El stock mínimo no puede ser negativo.")
            return@launch
        }
        val stockInicial = if (producto.id == 0L) {
            InventarioRules.cantidadNoNegativaDesdeTexto(stockInicialTexto).also {
                if (it == null) alError("Ingresa un stock inicial válido.")
            }
        } else null
        if (producto.id == 0L && stockInicial == null) return@launch
        runCatching { repository.guardar(producto, stockInicial) }
            .onSuccess { alExito() }
            .onFailure { alError(it.message ?: "No se pudo guardar el producto.") }
    }

    fun alternar(producto: ProductoEntity, alError: (String) -> Unit = {}) = viewModelScope.launch {
        runCatching { repository.guardar(producto.copy(activo = !producto.activo)) }
            .onFailure { alError(it.message ?: "No se pudo actualizar el producto.") }
    }

    fun registrarMovimiento(
        producto: ProductoEntity,
        cantidad: String,
        tipo: String,
        observacion: String,
        alError: (String) -> Unit,
        alExito: () -> Unit
    ) = viewModelScope.launch {
        val valor = if (tipo == "AJUSTE") InventarioRules.cantidadNoNegativaDesdeTexto(cantidad) else InventarioRules.cantidadDesdeTexto(cantidad)
        if (valor == null) {
            alError("Ingresa una cantidad válida.")
            return@launch
        }
        runCatching { repository.movimiento(producto, valor, tipo, observacion) }
            .onSuccess { alExito() }
            .onFailure { alError(it.message ?: "No se pudo registrar el movimiento.") }
    }

    companion object {
        fun factory(repository: InventarioRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                InventarioViewModel(repository) as T
        }
    }
}
