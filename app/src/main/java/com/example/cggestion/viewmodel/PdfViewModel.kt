package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.repository.CotizacionRepository
import com.example.cggestion.util.pdf.CotizacionPdfGenerator
import com.example.cggestion.util.pdf.PdfResultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AccionPdf { VER, COMPARTIR }
data class EstadoPdf(val generando: Boolean = false, val mensaje: String? = null, val error: String? = null, val accionPendiente: AccionPendiente? = null)
data class AccionPendiente(val accion: AccionPdf, val archivo: java.io.File, val numero: String, val cliente: String)

class PdfViewModel(private val repository: CotizacionRepository, private val generador: CotizacionPdfGenerator) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoPdf()); val estado: StateFlow<EstadoPdf> = _estado.asStateFlow()
    fun generar(id: Long) = preparar(id, null)
    fun preparar(id: Long, accion: AccionPdf?) { if (_estado.value.generando) return; _estado.value = EstadoPdf(generando = true); viewModelScope.launch { val completa = repository.obtenerCompleta(id); if (completa == null) { _estado.value=EstadoPdf(error="No se encontró la cotización.");return@launch }; when(val resultado=withContext(Dispatchers.IO){generador.generar(completa)}) { is PdfResultado.Exito -> _estado.value=EstadoPdf(mensaje="PDF generado: ${resultado.archivo.name}", accionPendiente=accion?.let{AccionPendiente(it,resultado.archivo,completa.cotizacion.numeroCotizacion,completa.cliente.nombre)}); is PdfResultado.Error -> _estado.value=EstadoPdf(error=resultado.mensaje) } } }
    fun consumirAccion() { _estado.value = _estado.value.copy(accionPendiente = null) }
    fun mostrarError(mensaje: String) { _estado.value = EstadoPdf(error = mensaje) }
    fun limpiarMensaje() { _estado.value = EstadoPdf() }
    companion object { fun factory(repository:CotizacionRepository,generador:CotizacionPdfGenerator)=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=PdfViewModel(repository,generador) as T} }
}
