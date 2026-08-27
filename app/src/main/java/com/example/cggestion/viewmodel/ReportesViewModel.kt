package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.EstadoCotizacion
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.data.repository.ReporteOperativo
import com.example.cggestion.data.repository.ReportesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class PeriodoReporte { HOY, SEMANA, MES, TODO }
data class EstadoReportes(
    val periodo: PeriodoReporte = PeriodoReporte.MES,
    val reporte: ReporteOperativo = ReporteOperativo(),
    val error: String? = null
)

class ReportesViewModel(repository: ReportesRepository) : ViewModel() {
    private val periodo = MutableStateFlow(PeriodoReporte.MES)
    private val error = MutableStateFlow<String?>(null)
    val estado: StateFlow<EstadoReportes> = combine(
        repository.reporte().catch { causa ->
            if (causa is CancellationException) throw causa
            error.value = "No se pudieron cargar los reportes. Intenta nuevamente."
            emit(ReporteOperativo())
        },
        periodo,
        error
    ) { reporte, seleccionado, mensajeError ->
        val desde = when (seleccionado) { PeriodoReporte.HOY -> System.currentTimeMillis() - 24 * 60 * 60 * 1000L; PeriodoReporte.SEMANA -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L; PeriodoReporte.MES -> System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L; PeriodoReporte.TODO -> 0L }
        EstadoReportes(
            periodo = seleccionado,
            reporte = reporte.copy(
                cotizaciones = reporte.cotizaciones.filter { it.cotizacion.fechaCreacion >= desde },
                hojas = reporte.hojas.filter { it.hoja.fecha >= desde }
            ),
            error = mensajeError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoReportes())
    fun seleccionarPeriodo(nuevo: PeriodoReporte) { periodo.value = nuevo }
    companion object { fun factory(repository: ReportesRepository) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ReportesViewModel(repository) as T } }
}
