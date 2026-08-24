package com.example.cggestion.data.repository

import androidx.room.withTransaction
import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.local.entity.MovimientoInventarioEntity
import com.example.cggestion.data.local.entity.ProductoEntity

class InventarioRepository(private val database: CGGestionDatabase) {
    private val productos = database.productoDao()

    fun productos() = productos.todos()
    fun productosBajoMinimo() = productos.bajoMinimo()
    fun movimientos(productoId: Long) = database.movimientoInventarioDao().porProducto(productoId)

    suspend fun guardar(producto: ProductoEntity, stockInicial: Double? = null): Long = database.withTransaction {
        val limpio = producto.copy(
            codigo = producto.codigo?.trim()?.ifBlank { null },
            nombre = producto.nombre.trim(),
            categoria = producto.categoria.trim(),
            unidad = producto.unidad.trim().ifBlank { "Unidad" },
            stockMinimo = producto.stockMinimo.coerceAtLeast(0.0),
            actualizadoEn = System.currentTimeMillis()
        )
        if (limpio.id == 0L) {
            val id = productos.insertar(limpio.copy(stockActual = 0.0))
            if (stockInicial != null && stockInicial > 0.0) {
                productos.ajustarStock(id, stockInicial)
                database.movimientoInventarioDao().insertar(
                    MovimientoInventarioEntity(productoId = id, tipo = "ENTRADA", cantidad = stockInicial, observacion = "Stock inicial", stockResultante = stockInicial)
                )
            }
            id
        } else {
            productos.actualizar(limpio)
            limpio.id
        }
    }

    suspend fun movimiento(
        producto: ProductoEntity,
        cantidad: Double,
        tipo: String,
        observacion: String = ""
    ) = database.withTransaction {
        require(producto.id > 0L) { "El producto debe estar guardado antes de registrar movimientos." }
        require(cantidad.isFinite() && (if (tipo == "AJUSTE") cantidad >= 0 else cantidad > 0)) { "Ingresa una cantidad válida." }
        require(tipo == "ENTRADA" || tipo == "SALIDA" || tipo == "AJUSTE") { "Tipo de movimiento inválido." }
        val actual = productos.porId(producto.id) ?: throw IllegalArgumentException("El producto ya no está disponible.")

        val (variacion, stockResultante, filasActualizadas) = when (tipo) {
            "ENTRADA" -> Triple(cantidad, actual.stockActual + cantidad, productos.ajustarStock(producto.id, cantidad))
            "SALIDA" -> Triple(-cantidad, actual.stockActual - cantidad, productos.descontarStockSiDisponible(producto.id, cantidad))
            "AJUSTE" -> Triple(cantidad - actual.stockActual, cantidad, productos.ajustarStock(producto.id, cantidad - actual.stockActual))
            else -> Triple(0.0, actual.stockActual, 0)
        }
        if (filasActualizadas != 1) {
            throw IllegalArgumentException("Stock insuficiente o producto no disponible.")
        }

        database.movimientoInventarioDao().insertar(
            MovimientoInventarioEntity(
                productoId = producto.id,
                tipo = tipo,
                cantidad = variacion,
                observacion = observacion.trim().ifBlank { if (tipo == "AJUSTE") "Ajuste de inventario" else "" },
                stockResultante = stockResultante
            )
        )
    }
}
