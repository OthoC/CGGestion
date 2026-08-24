package com.example.cggestion

import android.app.Application
import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.repository.CotizacionRepository
import com.example.cggestion.util.pdf.CotizacionPdfGenerator
import com.example.cggestion.data.repository.HojaCampoRepository
import com.example.cggestion.data.repository.InventarioRepository
import com.example.cggestion.data.repository.ClienteRepository
import com.example.cggestion.util.evidencias.EvidenciaStorage
import com.example.cggestion.util.backup.BackupManager
import com.example.cggestion.data.repository.ReportesRepository
import com.example.cggestion.util.pdf.HojaCampoPdfGenerator
import com.example.cggestion.data.repository.MantenimientoRepository
import com.example.cggestion.data.repository.ActualizacionRepository

class CGGestionApplication : Application() {
    private var databaseActual: CGGestionDatabase? = null
    @Synchronized fun database(): CGGestionDatabase {
        return databaseActual ?: CGGestionDatabase.crear(this).also { databaseActual = it }
    }
    @Synchronized fun cerrarBaseDeDatos() {
        databaseActual?.close()
        databaseActual = null
    }
    val repository get() = CotizacionRepository(database())
    val pdfGenerator by lazy { CotizacionPdfGenerator(this) }
    private val evidenciaStorage by lazy { EvidenciaStorage(applicationContext) }
    val hojaCampoRepository get() = HojaCampoRepository(database(), evidenciaStorage)
    val inventarioRepository get() = InventarioRepository(database())
    val clienteRepository get() = ClienteRepository(database())
    val backupManager by lazy { BackupManager(applicationContext, ::database, ::cerrarBaseDeDatos) }
    val reportesRepository get() = ReportesRepository(database())
    val hojaCampoPdfGenerator by lazy { HojaCampoPdfGenerator(applicationContext) }
    val mantenimientoRepository get() = MantenimientoRepository(database())
    val actualizacionRepository by lazy { ActualizacionRepository(applicationContext) }
}
