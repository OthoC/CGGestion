package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.EstadoCotizacion
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.data.repository.ReporteOperativo
import com.example.cggestion.data.repository.ReportesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class PeriodoReporte { HOY, SEMANA, MES, TODO }
data class EstadoReportes(val periodo: PeriodoReporte = PeriodoReporte.MES, val reporte: ReporteOperativo = ReporteOperativo())

class ReportesViewModel(repository: ReportesRepository) : ViewModel() {
    private val periodo = MutableStateFlow(PeriodoReporte.MES)
    val estado: StateFlow<EstadoReportes> = combine(repository.reporte(), periodo) { reporte, seleccionado ->
        val desde = when (seleccionado) { PeriodoReporte.HOY -> System.currentTimeMillis() - 24 * 60 * 60 * 1000L; PeriodoReporte.SEMANA -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L; PeriodoReporte.MES -> System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L; PeriodoReporte.TODO -> 0L }
        EstadoReportes(seleccionado, reporte.copy(cotizaciones = reporte.cotizaciones.filter { it.cotizacion.fechaCreacion >= desde }, hojas = reporte.hojas.filter { it.hoja.fecha >= desde }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoReportes())
    fun seleccionarPeriodo(nuevo: PeriodoReporte) { periodo.value = nuevo }
    companion object { fun factory(repository: ReportesRepository) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ReportesViewModel(repository) as T } }
}
