package com.example.cggestion.data.repository

import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.local.entity.CotizacionResumen
import com.example.cggestion.data.local.entity.HojaCampoResumen
import com.example.cggestion.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.combine

data class ReporteOperativo(
    val cotizaciones: List<CotizacionResumen> = emptyList(),
    val hojas: List<HojaCampoResumen> = emptyList(),
    val stockBajo: List<ProductoEntity> = emptyList(),
    val productos: List<ProductoEntity> = emptyList()
)

class ReportesRepository(private val database: CGGestionDatabase) {
    fun reporte() = combine(
        database.cotizacionDao().resumenes(),
        database.hojaCampoDao().resumenes(),
        database.productoDao().bajoMinimo(),
        database.productoDao().todos()
    ) { cotizaciones, hojas, stockBajo, productos ->
        ReporteOperativo(cotizaciones, hojas, stockBajo, productos)
    }
}
