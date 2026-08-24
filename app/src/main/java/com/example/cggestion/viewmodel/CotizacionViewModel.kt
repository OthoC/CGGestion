package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.ItemCotizacion
import com.example.cggestion.data.Producto
import com.example.cggestion.data.aCentavos
import com.example.cggestion.data.aDolares
import com.example.cggestion.data.calcularTotales
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.EstadoCotizacion
import com.example.cggestion.data.local.entity.ItemCotizacionEntity
import com.example.cggestion.data.repository.CotizacionRepository
import com.example.cggestion.data.repository.DatosGuardarCotizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class EditorCotizacionState(
    val id: Long = 0,
    val numero: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val cliente: String = "",
    val ruc: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val correo: String = "",
    val condicionPago: String = "Contado",
    val vendedor: String = "",
    val observaciones: String = "",
    val estado: EstadoCotizacion = EstadoCotizacion.BORRADOR,
    val descuentoGlobalTexto: String = "0",
    val ivaTexto: String = "15",
    val items: List<ItemCotizacion> = emptyList(),
    val catalogo: List<Producto> = emptyList(),
    val guardando: Boolean = false,
    val mensaje: String? = null,
    val error: String? = null
)

class CotizacionViewModel(private val repository: CotizacionRepository) : ViewModel() {
    private val _state = MutableStateFlow(EditorCotizacionState())
    val state: StateFlow<EditorCotizacionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.productosActivos().collectLatest { entidades ->
                actualizar { copy(catalogo = entidades.map { Producto(it.id, it.nombre, it.categoria, it.precioPredeterminadoCentavos.aDolares()) }) }
            }
        }
        nuevaCotizacion()
    }

    fun nuevaCotizacion() = viewModelScope.launch {
        val ahora = System.currentTimeMillis()
        val numero = repository.siguienteNumero(ahora)
        _state.value = EditorCotizacionState(numero = numero, fechaCreacion = ahora, catalogo = _state.value.catalogo)
    }

    fun cargar(id: Long) = viewModelScope.launch {
        val completa = repository.obtenerCompleta(id) ?: run { mostrarError("No se encontró la cotización."); return@launch }
        val productos = _state.value.catalogo.associateBy { it.id }
        val items = completa.items.map { item ->
            val producto = productos[item.productoId] ?: Producto(item.productoId, item.nombreProducto, item.categoriaProducto, item.precioUnitarioCentavos.aDolares())
            ItemCotizacion(producto, item.cantidad, item.precioUnitarioCentavos.aDolares().toString(), item.descuentoPorcentaje.toString())
        }
        _state.value = EditorCotizacionState(id = completa.cotizacion.id, numero = completa.cotizacion.numeroCotizacion, fechaCreacion = completa.cotizacion.fechaCreacion, cliente = completa.cliente.nombre, ruc = completa.cliente.rucCedula, telefono = completa.cliente.telefono, direccion = completa.cliente.direccion, correo = completa.cliente.correoElectronico.orEmpty(), condicionPago = completa.cotizacion.condicionPago, vendedor = completa.cotizacion.vendedor, observaciones = completa.cotizacion.observaciones, estado = runCatching { EstadoCotizacion.valueOf(completa.cotizacion.estado) }.getOrDefault(EstadoCotizacion.BORRADOR), descuentoGlobalTexto = completa.cotizacion.descuentoGlobalPorcentaje.toString(), ivaTexto = completa.cotizacion.ivaPorcentaje.toString(), items = items, catalogo = _state.value.catalogo)
    }

    fun actualizarCliente(nombre: String? = null, ruc: String? = null, telefono: String? = null, direccion: String? = null, correo: String? = null) = actualizar { copy(cliente = nombre ?: cliente, ruc = ruc ?: this.ruc, telefono = telefono ?: this.telefono, direccion = direccion ?: this.direccion, correo = correo ?: this.correo) }
    fun actualizarExtras(condicionPago: String? = null, vendedor: String? = null, observaciones: String? = null, estado: EstadoCotizacion? = null, descuento: String? = null, iva: String? = null) = actualizar { copy(condicionPago = condicionPago ?: this.condicionPago, vendedor = vendedor ?: this.vendedor, observaciones = observaciones ?: this.observaciones, estado = estado ?: this.estado, descuentoGlobalTexto = descuento ?: this.descuentoGlobalTexto, ivaTexto = iva ?: this.ivaTexto) }
    fun agregarProducto(producto: Producto) = actualizar { val indice = items.indexOfFirst { it.producto.id == producto.id }; copy(items = if (indice >= 0) items.mapIndexed { i, item -> if (i == indice) item.copy(cantidad = item.cantidad + 1) else item } else items + ItemCotizacion(producto)) }
    fun actualizarItem(indice: Int, item: ItemCotizacion) = actualizar { copy(items = items.mapIndexed { i, actual -> if (i == indice) item else actual }) }
    fun eliminarItem(indice: Int) = actualizar { copy(items = items.filterIndexed { i, _ -> i != indice }) }
    fun limpiarMensaje() = actualizar { copy(mensaje = null, error = null) }

    fun guardar() {
        val actual = _state.value
        if (actual.guardando) return
        when {
            actual.cliente.trim().isEmpty() -> return mostrarError("Ingresa el nombre del cliente.")
            actual.items.isEmpty() -> return mostrarError("Agrega al menos un producto.")
        }
        viewModelScope.launch {
            actualizar { copy(guardando = true, mensaje = null, error = null) }
            try {
                val estado = _state.value
                val descuento = estado.descuentoGlobalTexto.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                val iva = estado.ivaTexto.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                if (estado.items.any { it.cantidad < 1 || it.precioUnitario < 0.0 || it.descuento !in 0.0..100.0 }) throw IllegalArgumentException("Revisa las cantidades, precios y descuentos.")
                val totales = calcularTotales(estado.items, descuento, iva)
                val items = estado.items.map { item ->
                    val subtotal = item.subtotalLinea.aCentavos(); val descuentoItem = item.descuentoLinea.aCentavos()
                    ItemCotizacionEntity(productoId = item.producto.id, nombreProducto = item.producto.nombre, categoriaProducto = item.producto.categoria, cantidad = item.cantidad, precioUnitarioCentavos = item.precioUnitario.aCentavos(), descuentoPorcentaje = item.descuento, subtotalCentavos = subtotal, descuentoCentavos = descuentoItem, totalLineaCentavos = subtotal - descuentoItem, cotizacionId = 0)
                }
                val resultado = repository.guardar(DatosGuardarCotizacion(id = estado.id, numeroCotizacion = estado.numero, fechaCreacion = estado.fechaCreacion, fechaValidez = estado.fechaCreacion + 30L * 24 * 60 * 60 * 1000, cliente = ClienteEntity(nombre = estado.cliente.trim(), rucCedula = estado.ruc.trim(), telefono = estado.telefono.trim(), direccion = estado.direccion.trim(), correoElectronico = estado.correo.trim().ifBlank { null }), condicionPago = estado.condicionPago, vendedor = estado.vendedor, observaciones = estado.observaciones, descuentoGlobal = descuento, iva = iva, subtotalBruto = totales.subtotalBruto, descuentoItems = totales.descuentoItems, descuentoGlobalValor = totales.descuentoGlobal, baseImponible = totales.baseImponible, valorIva = totales.valorIva, totalFinal = totales.totalFinal, estado = estado.estado.name, items = items))
                actualizar { copy(id = resultado.id, numero = resultado.numero, guardando = false, mensaje = "Cotización ${resultado.numero} guardada correctamente.") }
            } catch (e: Exception) { actualizar { copy(guardando = false, error = e.message ?: "No se pudo guardar la cotización.") } }
        }
    }

    private fun mostrarError(texto: String) = actualizar { copy(error = texto) }
    private fun actualizar(cambio: EditorCotizacionState.() -> EditorCotizacionState) { _state.value = _state.value.cambio() }
    companion object { fun factory(repository: CotizacionRepository) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = CotizacionViewModel(repository) as T } }
}
