package com.example.cggestion.data.repository

import androidx.room.withTransaction
import com.example.cggestion.data.local.dao.ClienteDao
import com.example.cggestion.data.local.dao.CotizacionDao
import com.example.cggestion.data.local.dao.ProductoDao
import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.CotizacionCompleta
import com.example.cggestion.data.local.entity.CotizacionEntity
import com.example.cggestion.data.local.entity.CotizacionResumen
import com.example.cggestion.data.local.entity.ItemCotizacionEntity
import com.example.cggestion.data.local.entity.ProductoEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CotizacionRepository(private val database: CGGestionDatabase) {
    private val clientes: ClienteDao = database.clienteDao()
    private val productos: ProductoDao = database.productoDao()
    private val cotizaciones: CotizacionDao = database.cotizacionDao()

    fun productosActivos(): Flow<List<ProductoEntity>> = productos.activos()
    fun buscarProductos(texto: String): Flow<List<ProductoEntity>> = productos.buscar(texto)
    fun cotizaciones(): Flow<List<CotizacionEntity>> = cotizaciones.todas()
    fun resumenesCotizaciones(): Flow<List<CotizacionResumen>> = cotizaciones.resumenes()
    suspend fun obtenerCompleta(id: Long): CotizacionCompleta? = cotizaciones.completaPorId(id)
    suspend fun siguienteNumero(fecha: Long = System.currentTimeMillis()): String = siguienteNumeroDisponible(fecha)

    suspend fun guardar(datos: DatosGuardarCotizacion): ResultadoGuardar = database.withTransaction {
        val clienteId = guardarCliente(datos.cliente)
        val ahora = System.currentTimeMillis()
        val cotizacionId: Long
        val numero: String
        if (datos.id == 0L) {
            numero = siguienteNumeroDisponible(datos.fechaCreacion)
            cotizacionId = cotizaciones.insertar(datos.cotizacion(numero, clienteId, ahora))
        } else {
            numero = datos.numeroCotizacion
            cotizacionId = datos.id
            cotizaciones.actualizar(datos.cotizacion(numero, clienteId, ahora).copy(id = datos.id, fechaCreacion = datos.fechaCreacion))
            cotizaciones.eliminarItems(datos.id)
        }
        cotizaciones.insertarItems(datos.items.map { it.copy(cotizacionId = cotizacionId, id = 0) })
        ResultadoGuardar(cotizacionId, numero)
    }

    private suspend fun guardarCliente(cliente: ClienteEntity): Long {
        val existente = cliente.rucCedula.takeIf { it.isNotBlank() }?.let { clientes.porRuc(it) }
        return if (existente != null) {
            clientes.actualizar(cliente.copy(id = existente.id)); existente.id
        } else clientes.insertar(cliente)
    }

    private suspend fun siguienteNumeroDisponible(fecha: Long): String {
        val anio = Instant.ofEpochMilli(fecha).atZone(ZoneId.systemDefault()).year
        val prefijo = "CG-$anio-"
        val ultimo = cotizaciones.ultimoNumero(prefijo)?.substringAfterLast('-')?.toIntOrNull() ?: 0
        var consecutivo = ultimo + 1
        var candidato: String
        do { candidato = "$prefijo${consecutivo.toString().padStart(4, '0')}"; consecutivo++ } while (cotizaciones.existeNumero(candidato))
        return candidato
    }
}

data class DatosGuardarCotizacion(
    val id: Long = 0,
    val numeroCotizacion: String = "",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaValidez: Long = System.currentTimeMillis(),
    val cliente: ClienteEntity,
    val condicionPago: String,
    val vendedor: String,
    val observaciones: String,
    val descuentoGlobal: Double,
    val iva: Double,
    val subtotalBruto: Long,
    val descuentoItems: Long,
    val descuentoGlobalValor: Long,
    val baseImponible: Long,
    val valorIva: Long,
    val totalFinal: Long,
    val estado: String,
    val items: List<ItemCotizacionEntity>
) {
    fun cotizacion(numero: String, clienteId: Long, ahora: Long) = CotizacionEntity(numeroCotizacion = numero, clienteId = clienteId, fechaCreacion = fechaCreacion, fechaValidez = fechaValidez, condicionPago = condicionPago, vendedor = vendedor, observaciones = observaciones, descuentoGlobalPorcentaje = descuentoGlobal, ivaPorcentaje = iva, subtotalBrutoCentavos = subtotalBruto, descuentoItemsCentavos = descuentoItems, descuentoGlobalCentavos = descuentoGlobalValor, baseImponibleCentavos = baseImponible, valorIvaCentavos = valorIva, totalFinalCentavos = totalFinal, estado = estado, fechaUltimaModificacion = ahora)
}
data class ResultadoGuardar(val id: Long, val numero: String)
