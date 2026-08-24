package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.repository.HojaCampoRepository
import com.example.cggestion.util.pdf.HojaCampoPdfGenerator
import com.example.cggestion.util.pdf.PdfResultado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EstadoHojaPdf(val generando: Boolean = false, val archivo: java.io.File? = null, val mensaje: String? = null, val error: String? = null)
class HojaCampoPdfViewModel(private val repository: HojaCampoRepository, private val generador: HojaCampoPdfGenerator) : ViewModel() {
    private val _estado = MutableStateFlow(EstadoHojaPdf()); val estado: StateFlow<EstadoHojaPdf> = _estado.asStateFlow()
    fun generar(id: Long) { if (_estado.value.generando || id == 0L) return; _estado.value=EstadoHojaPdf(generando=true); viewModelScope.launch { val datos=repository.completa(id); if(datos==null){_estado.value=EstadoHojaPdf(error="No se encontró la hoja.");return@launch}; when(val r=withContext(Dispatchers.IO){generador.generar(datos)}) { is PdfResultado.Exito->_estado.value=EstadoHojaPdf(archivo=r.archivo,mensaje="PDF generado correctamente."); is PdfResultado.Error->_estado.value=EstadoHojaPdf(error=r.mensaje) } } }
    companion object {
        fun factory(repository: HojaCampoRepository, generador: HojaCampoPdfGenerator) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HojaCampoPdfViewModel(repository, generador) as T
            }
        }
    }
}
