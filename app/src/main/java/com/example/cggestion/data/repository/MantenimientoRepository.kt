package com.example.cggestion.data.repository

import androidx.room.withTransaction
import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.local.entity.EquipoEntity
import com.example.cggestion.data.local.entity.EstadoMantenimiento
import com.example.cggestion.data.local.entity.MantenimientoEntity
import com.example.cggestion.data.local.entity.MantenimientoResumen
import com.example.cggestion.data.local.entity.ClienteEntity
import kotlinx.coroutines.flow.Flow

class MantenimientoRepository(private val database: CGGestionDatabase) {
    private val dao = database.mantenimientoDao()

    fun resumenes(): Flow<List<MantenimientoResumen>> = dao.resumenes()
    fun clientes() = database.clienteDao().todos()
    fun equipos(clienteId: Long) = database.equipoDao().porCliente(clienteId)

    suspend fun guardar(mantenimiento: MantenimientoEntity): Long = database.withTransaction {
        require(mantenimiento.clienteId > 0) { "Selecciona un cliente." }
        require(mantenimiento.equipoId > 0) { "Selecciona un equipo." }
        require(mantenimiento.descripcion.trim().isNotBlank()) { "Describe el mantenimiento." }
        require((mantenimiento.periodicidadDias ?: 0) >= 0) { "La periodicidad no es válida." }
        val equipo = database.equipoDao().porId(mantenimiento.equipoId)
            ?: error("El equipo seleccionado ya no existe.")
        require(equipo.clienteId == mantenimiento.clienteId) { "El equipo no pertenece al cliente seleccionado." }
        val limpio = mantenimiento.copy(descripcion = mantenimiento.descripcion.trim(), fechaModificacion = System.currentTimeMillis())
        if (limpio.id == 0L) dao.insertar(limpio) else { dao.actualizar(limpio); limpio.id }
    }

    suspend fun actualizarEstado(id: Long, estado: String) = database.withTransaction {
        val actual = dao.porId(id) ?: error("No se encontró el mantenimiento.")
        dao.actualizar(actual.copy(estado = estado, fechaModificacion = System.currentTimeMillis()))
    }

    suspend fun mantenimiento(id: Long): MantenimientoEntity? = dao.porId(id)
    suspend fun equipo(id: Long): EquipoEntity? = database.equipoDao().porId(id)
    suspend fun cliente(id: Long): ClienteEntity? = database.clienteDao().porId(id)
}
