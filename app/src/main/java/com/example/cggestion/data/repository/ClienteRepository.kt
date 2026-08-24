package com.example.cggestion.data.repository

import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.EquipoEntity

class ClienteRepository(database: CGGestionDatabase) {
    private val dao = database.clienteDao()
    private val equipos = database.equipoDao()
    private val hojas = database.hojaCampoDao()
    fun clientes() = dao.todos()
    suspend fun guardar(cliente: ClienteEntity): Long {
        val limpio = cliente.copy(nombre = cliente.nombre.trim(), rucCedula = cliente.rucCedula.trim())
        require(limpio.nombre.isNotBlank()) { "Ingresa el nombre del cliente." }
        val existente = limpio.rucCedula.takeIf { it.isNotBlank() }?.let { dao.porRuc(it) }
        return when {
            existente != null -> { dao.actualizar(limpio.copy(id = existente.id)); existente.id }
            limpio.id != 0L -> { dao.actualizar(limpio); limpio.id }
            else -> dao.insertar(limpio)
        }
    }
    fun equipos(clienteId: Long) = equipos.porCliente(clienteId)
    fun hojasEquipo(equipoId: Long) = hojas.porEquipo(equipoId)
    suspend fun guardarEquipo(equipo: EquipoEntity): Long {
        require(equipo.clienteId > 0) { "Selecciona un cliente." }
        val limpio = equipo.copy(serie = equipo.serie?.trim()?.ifBlank { null }, actualizadoEn = System.currentTimeMillis())
        return if (limpio.id == 0L) equipos.insertar(limpio) else { equipos.actualizar(limpio); limpio.id }
    }
}
