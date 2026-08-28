package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.EstadoCotizacion
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.data.local.entity.CotizacionResumen
import com.example.cggestion.data.local.entity.HojaCampoResumen
import com.example.cggestion.data.local.entity.ProductoEntity
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
    val metricas: MetricasReportes = MetricasReportes(),
    val error: String? = null
)

data class MetricasReportes(
    val cotizacionesAprobadas: Int = 0,
    val cotizacionesPendientes: Int = 0,
    val valorAprobadoCentavos: Long = 0,
    val hojasCompletadas: Int = 0,
    val hojasBorrador: Int = 0,
    val productosStockBajo: Int = 0,
    val valorInventarioCentavos: Long = 0
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
        val filtrado = reporte.filtrarPorPeriodo(seleccionado)
        EstadoReportes(
            periodo = seleccionado,
            reporte = filtrado,
            metricas = filtrado.calcularMetricas(),
            error = mensajeError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoReportes())
    fun seleccionarPeriodo(nuevo: PeriodoReporte) { periodo.value = nuevo }
    companion object { fun factory(repository: ReportesRepository) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ReportesViewModel(repository) as T } }
}

internal fun ReporteOperativo.filtrarPorPeriodo(
    periodo: PeriodoReporte,
    ahora: Long = System.currentTimeMillis()
): ReporteOperativo {
    val desde = when (periodo) {
        PeriodoReporte.HOY -> ahora - 24 * 60 * 60 * 1000L
        PeriodoReporte.SEMANA -> ahora - 7 * 24 * 60 * 60 * 1000L
        PeriodoReporte.MES -> ahora - 30 * 24 * 60 * 60 * 1000L
        PeriodoReporte.TODO -> 0L
    }
    return copy(
        cotizaciones = cotizaciones.filter { it.cotizacion.fechaCreacion >= desde },
        hojas = hojas.filter { it.hoja.fecha >= desde }
    )
}

internal fun ReporteOperativo.calcularMetricas(): MetricasReportes = MetricasReportes(
    cotizacionesAprobadas = cotizaciones.count { it.cotizacion.estado == EstadoCotizacion.APROBADA.name },
    cotizacionesPendientes = cotizaciones.count {
        it.cotizacion.estado in setOf(EstadoCotizacion.BORRADOR.name, EstadoCotizacion.ENVIADA.name)
    },
    valorAprobadoCentavos = cotizaciones
        .filter { it.cotizacion.estado == EstadoCotizacion.APROBADA.name }
        .sumOf { it.cotizacion.totalFinalCentavos },
    hojasCompletadas = hojas.count { it.hoja.estado == EstadoHoja.COMPLETADA.name },
    hojasBorrador = hojas.count { it.hoja.estado == EstadoHoja.BORRADOR.name },
    productosStockBajo = stockBajo.size,
    valorInventarioCentavos = productos
        .filter { it.activo }
        .sumOf { producto -> (producto.stockActual * producto.precioPredeterminadoCentavos).toLong() }
)
