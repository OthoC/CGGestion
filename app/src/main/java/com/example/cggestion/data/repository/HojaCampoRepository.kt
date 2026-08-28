package com.example.cggestion.data.repository

import androidx.room.withTransaction
import com.example.cggestion.data.local.dao.ClienteDao
import com.example.cggestion.data.local.dao.HojaCampoDao
import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.HojaCampoCompleta
import com.example.cggestion.data.local.entity.HojaCampoEntity
import com.example.cggestion.data.local.entity.HojaCampoResumen
import com.example.cggestion.data.local.entity.JornadaTrabajoEntity
import com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity
import com.example.cggestion.data.local.entity.ProductoEntity
import com.example.cggestion.data.local.entity.RepuestoUsadoEntity
import com.example.cggestion.data.local.entity.EvidenciaEntity
import com.example.cggestion.data.local.entity.ConsumoHojaInventarioEntity
import com.example.cggestion.data.local.entity.MovimientoInventarioEntity
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.util.evidencias.ArchivoEvidencia
import com.example.cggestion.util.evidencias.EvidenciaStorage
import com.example.cggestion.util.firma.FirmaClienteStorage
import com.example.cggestion.util.firma.TrazoFirma
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ResultadoGuardarHoja(val id: Long, val numero: String)
sealed interface PrepararHojaDesdeCotizacion {
    data class Existente(val hojaId: Long) : PrepararHojaDesdeCotizacion
    data class Nueva(val hoja: HojaCampoEntity, val cliente: ClienteEntity) : PrepararHojaDesdeCotizacion
    data class Error(val mensaje: String) : PrepararHojaDesdeCotizacion
}

class HojaCampoRepository(
    private val database: CGGestionDatabase,
    private val storage: EvidenciaStorage,
    private val firmaStorage: FirmaClienteStorage
) {
    private val hojas: HojaCampoDao = database.hojaCampoDao()
    private val clientes: ClienteDao = database.clienteDao()

    fun resumenes(): Flow<List<HojaCampoResumen>> = hojas.resumenes()
    fun clientes(): Flow<List<ClienteEntity>> = clientes.buscarPorNombre("")
    fun productosActivos(): Flow<List<ProductoEntity>> = database.productoDao().activos()
    fun equipos(clienteId: Long): Flow<List<com.example.cggestion.data.local.entity.EquipoEntity>> = database.equipoDao().porCliente(clienteId)
    suspend fun completa(id: Long): HojaCampoCompleta? = hojas.completa(id)
    fun evidencias(hojaCampoId: Long): Flow<List<EvidenciaEntity>> = database.evidenciaDao().porHoja(hojaCampoId)
    fun crearArchivoTemporal() = storage.crearTemporal()
    fun eliminarTemporal(ruta: String) = storage.eliminarTemporal(ruta)
    fun existeFirmaCliente(ruta: String?): Boolean = firmaStorage.existe(ruta)

    suspend fun guardarFirmaCliente(hojaCampoId: Long, trazos: List<TrazoFirma>): String {
        val hoja = hojas.porId(hojaCampoId) ?: error("No se encontró la hoja de campo.")
        val ruta = withContext(Dispatchers.IO) { firmaStorage.guardar(hoja.numeroHoja, trazos) }
        database.withTransaction {
            check(hojas.actualizarFirmaCliente(hojaCampoId, ruta, "FIRMADA") == 1) {
                "No se pudo asociar la firma a la hoja."
            }
        }
        return ruta
    }

    suspend fun eliminarFirmaCliente(hojaCampoId: Long) {
        val hoja = hojas.porId(hojaCampoId) ?: error("No se encontró la hoja de campo.")
        database.withTransaction {
            check(hojas.actualizarFirmaCliente(hojaCampoId, null, "PENDIENTE") == 1) {
                "No se pudo actualizar la hoja."
            }
        }
        withContext(Dispatchers.IO) { firmaStorage.eliminar(hoja.firmaClienteRuta) }
    }
    suspend fun actualizarEvidencia(evidencia: EvidenciaEntity) = database.evidenciaDao().actualizar(evidencia)
    suspend fun eliminarEvidencia(evidencia: EvidenciaEntity) = database.withTransaction {
        check(storage.eliminar(evidencia.rutaInterna)) { "No se pudo eliminar el archivo de la evidencia." }
        database.evidenciaDao().eliminar(evidencia)
    }

    suspend fun moverEvidencia(evidencia: EvidenciaEntity, desplazamiento: Int) = database.withTransaction {
        val lista = database.evidenciaDao().listaPorHoja(evidencia.hojaCampoId)
        val indice = lista.indexOfFirst { it.id == evidencia.id }
        val destino = indice + desplazamiento
        if (indice !in lista.indices || destino !in lista.indices) return@withTransaction
        val otra = lista[destino]
        database.evidenciaDao().actualizarOrden(evidencia.id, otra.orden)
        database.evidenciaDao().actualizarOrden(otra.id, evidencia.orden)
    }

    suspend fun guardarEvidencia(hojaCampoId: Long, hojaNumero: String, rutaTemporal: String, descripcion: String, tipo: String): EvidenciaEntity {
        val archivo: ArchivoEvidencia = storage.moverTemporal(hojaNumero, rutaTemporal)
        return try {
            val evidencia = EvidenciaEntity(hojaCampoId = hojaCampoId, rutaInterna = archivo.rutaInterna, nombreArchivo = archivo.nombre, descripcion = descripcion, tipoEvidencia = tipo, orden = database.evidenciaDao().siguienteOrden(hojaCampoId), tamanoBytes = archivo.tamano, ancho = archivo.ancho, alto = archivo.alto)
            database.evidenciaDao().insertar(evidencia)
            evidencia
        } catch (error: Throwable) {
            storage.eliminar(archivo.rutaInterna)
            throw error
        }
    }

    suspend fun guardarEvidenciaGaleria(hojaCampoId: Long, hojaNumero: String, uri: Uri): EvidenciaEntity =
        guardarEvidenciaGaleria(hojaCampoId, hojaNumero, uri, "", "OTRO")

    suspend fun guardarEvidenciaGaleria(hojaCampoId: Long, hojaNumero: String, uri: Uri, descripcion: String, tipo: String): EvidenciaEntity {
        val archivo = storage.importarUri(hojaNumero, uri)
        return try {
            val evidencia = EvidenciaEntity(hojaCampoId = hojaCampoId, rutaInterna = archivo.rutaInterna, nombreArchivo = archivo.nombre, descripcion = descripcion, tipoEvidencia = tipo, orden = database.evidenciaDao().siguienteOrden(hojaCampoId), tamanoBytes = archivo.tamano, ancho = archivo.ancho, alto = archivo.alto)
            database.evidenciaDao().insertar(evidencia)
            evidencia
        } catch (error: Throwable) { storage.eliminar(archivo.rutaInterna); throw error }
    }

    suspend fun prepararDesdeCotizacion(cotizacionId: Long): PrepararHojaDesdeCotizacion {
        val existente = hojas.porCotizacion(cotizacionId)
        if (existente != null) return PrepararHojaDesdeCotizacion.Existente(existente)
        val cotizacion = database.cotizacionDao().completaPorId(cotizacionId)
            ?: return PrepararHojaDesdeCotizacion.Error("No se encontró la cotización.")
        if (cotizacion.cotizacion.estado != "APROBADA") {
            return PrepararHojaDesdeCotizacion.Error("Solo se pueden crear hojas desde cotizaciones aprobadas.")
        }
        val ahora = System.currentTimeMillis()
        return PrepararHojaDesdeCotizacion.Nueva(
            hoja = HojaCampoEntity(
                numeroHoja = siguienteNumero(),
                fecha = ahora,
                clienteId = cotizacion.cliente.id,
                cotizacionId = cotizacion.cotizacion.id,
                fechaCreacion = ahora,
                fechaModificacion = ahora,
                direccion = cotizacion.cliente.direccion,
                telefono = cotizacion.cliente.telefono,
                observaciones = cotizacion.cotizacion.observaciones
            ),
            cliente = cotizacion.cliente
        )
    }

    suspend fun siguienteNumero(): String {
        var consecutivo = siguienteConsecutivoHoja(hojas.ultimoNumero())
        while (true) {
            val candidato = "Q ${consecutivo.toString().padStart(7, '0')}"
            if (!hojas.existeNumero(candidato)) return candidato
            consecutivo++
        }
    }

    suspend fun vincularMantenimiento(maintenanceId: Long, hojaId: Long, hojaCompletada: Boolean) = database.withTransaction {
        if (!hojaCompletada) return@withTransaction
        val dao = database.mantenimientoDao()
        val actual = dao.porId(maintenanceId) ?: return@withTransaction
        if (actual.hojaCampoId != null) return@withTransaction
        dao.vincularHoja(maintenanceId, hojaId, "COMPLETADO")
        if ((actual.periodicidadDias ?: 0) > 0) {
            dao.insertar(actual.copy(
                id = 0,
                fechaProgramada = actual.fechaProgramada + (actual.periodicidadDias ?: 0) * 86_400_000L,
                estado = "PENDIENTE",
                hojaCampoId = null,
                fechaCreacion = System.currentTimeMillis(),
                fechaModificacion = System.currentTimeMillis()
            ))
        }
    }

    suspend fun guardar(
        hoja: HojaCampoEntity,
        cliente: ClienteEntity,
        mediciones: MedicionesHojaCampoEntity,
        repuestos: List<RepuestoUsadoEntity>,
        jornadas: List<JornadaTrabajoEntity>
    ): ResultadoGuardarHoja = database.withTransaction {
        val clienteGuardado = guardarCliente(cliente, hoja.clienteId)
        val ahora = System.currentTimeMillis()
        val id: Long
        val numero: String

        if (hoja.id == 0L) {
            numero = siguienteNumero()
            id = hojas.insertar(
                hoja.copy(
                    numeroHoja = numero,
                    clienteId = clienteGuardado,
                    fechaCreacion = ahora,
                    fechaModificacion = ahora
                )
            )
        } else {
            numero = hoja.numeroHoja
            id = hoja.id
            hojas.actualizar(
                hoja.copy(
                    clienteId = clienteGuardado,
                    fechaModificacion = ahora
                )
            )
            hojas.eliminarRepuestos(id)
            hojas.eliminarJornadas(id)
        }

        hojas.guardarMediciones(mediciones.copy(hojaCampoId = id))
        if (repuestos.isNotEmpty()) {
            hojas.insertarRepuestos(repuestos.map { it.copy(id = 0, hojaCampoId = id) })
        }
        if (jornadas.isNotEmpty()) {
            hojas.insertarJornadas(jornadas.map { it.copy(id = 0, hojaCampoId = id) })
        }
        sincronizarInventario(id, numero, hoja.estado, repuestos)
        ResultadoGuardarHoja(id, numero)
    }

    private suspend fun sincronizarInventario(hojaId: Long, numero: String, estado: String, repuestos: List<RepuestoUsadoEntity>) {
        val consumosDao = database.consumoHojaInventarioDao()
        val anteriores = consumosDao.porHoja(hojaId).associate { it.productoId to it.cantidad }
        val deseados = if (estado == EstadoHoja.COMPLETADA.name) repuestos
            .filter { it.productoId != null && it.cantidad > 0.0 }
            .groupBy { it.productoId!! }
            .mapValues { (_, items) -> items.sumOf { it.cantidad } }
        else emptyMap()
        val ids = anteriores.keys + deseados.keys
        ids.forEach { productoId ->
            val diferencia = (deseados[productoId] ?: 0.0) - (anteriores[productoId] ?: 0.0)
            if (diferencia == 0.0) return@forEach
            val producto = database.productoDao().porId(productoId)
                ?: throw IllegalArgumentException("Un repuesto seleccionado ya no existe en Inventario.")
            val saldo = producto.stockActual - diferencia
            val actualizado = if (diferencia > 0) database.productoDao().descontarStockSiDisponible(productoId, diferencia)
                else database.productoDao().ajustarStock(productoId, -diferencia)
            if (actualizado != 1) throw IllegalArgumentException("Stock insuficiente para ${producto.nombre}. Disponible: ${producto.stockActual} ${producto.unidad}.")
            database.movimientoInventarioDao().insertar(MovimientoInventarioEntity(
                productoId = productoId,
                tipo = if (diferencia > 0) "SALIDA" else "ENTRADA",
                cantidad = -diferencia,
                observacion = "Hoja de campo $numero",
                stockResultante = saldo
            ))
        }
        consumosDao.eliminarPorHoja(hojaId)
        if (deseados.isNotEmpty()) consumosDao.insertarTodos(deseados.map { (productoId, cantidad) -> ConsumoHojaInventarioEntity(hojaCampoId = hojaId, productoId = productoId, cantidad = cantidad) })
    }

    private suspend fun guardarCliente(cliente: ClienteEntity, clienteIdActual: Long): Long {
        val limpio = cliente.copy(nombre = cliente.nombre.trim().ifBlank { "Cliente pendiente" })
        val documento = limpio.rucCedula.takeIf { it.isNotBlank() }
        val existentePorDocumento = if (documento == null) null else clientes.porRuc(documento)
        return when {
            existentePorDocumento != null -> {
                clientes.actualizar(limpio.copy(id = existentePorDocumento.id))
                existentePorDocumento.id
            }
            clienteIdActual != 0L -> {
                clientes.actualizar(limpio.copy(id = clienteIdActual))
                clienteIdActual
            }
            else -> clientes.insertar(limpio.copy(id = 0))
        }
    }
}

internal fun siguienteConsecutivoHoja(ultimoNumero: String?): Int =
    ((ultimoNumero?.filter(Char::isDigit)?.toIntOrNull() ?: 99) + 1).coerceAtLeast(100)
