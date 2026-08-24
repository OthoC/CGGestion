package com.example.cggestion.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clientes", indices = [Index(value = ["rucCedula"])])
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val rucCedula: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val correoElectronico: String? = null
)

@Entity(tableName = "productos", indices = [Index(value = ["nombre"]), Index(value = ["codigo"], unique = true)])
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigo: String? = null,
    val nombre: String,
    val categoria: String,
    val precioPredeterminadoCentavos: Long = 0,
    val unidad: String = "Unidad",
    val stockActual: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val stockMinimo: Double = 0.0,
    val activo: Boolean = true,
    val actualizadoEn: Long = System.currentTimeMillis()
)
@Entity(tableName="movimientos_inventario",foreignKeys=[ForeignKey(entity=ProductoEntity::class,parentColumns=["id"],childColumns=["productoId"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["productoId"])])
data class MovimientoInventarioEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val productoId:Long,val tipo:String,val cantidad:Double,val fecha:Long=System.currentTimeMillis(),val observacion:String="", @ColumnInfo(defaultValue = "0") val stockResultante: Double = 0.0)

@Entity(tableName = "consumos_hoja_inventario", foreignKeys = [ForeignKey(entity = HojaCampoEntity::class, parentColumns = ["id"], childColumns = ["hojaCampoId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = ProductoEntity::class, parentColumns = ["id"], childColumns = ["productoId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["hojaCampoId"]), Index(value = ["productoId"]), Index(value = ["hojaCampoId", "productoId"], unique = true)])
data class ConsumoHojaInventarioEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val hojaCampoId: Long, val productoId: Long, val cantidad: Double)

@Entity(
    tableName = "cotizaciones",
    foreignKeys = [ForeignKey(entity = ClienteEntity::class, parentColumns = ["id"], childColumns = ["clienteId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index(value = ["numeroCotizacion"], unique = true), Index(value = ["clienteId"])]
)
data class CotizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numeroCotizacion: String,
    val clienteId: Long,
    val fechaCreacion: Long,
    val fechaValidez: Long,
    val condicionPago: String = "Contado",
    val vendedor: String = "",
    val observaciones: String = "",
    val descuentoGlobalPorcentaje: Double = 0.0,
    val ivaPorcentaje: Double = 15.0,
    val subtotalBrutoCentavos: Long,
    val descuentoItemsCentavos: Long,
    val descuentoGlobalCentavos: Long,
    val baseImponibleCentavos: Long,
    val valorIvaCentavos: Long,
    val totalFinalCentavos: Long,
    val estado: String = EstadoCotizacion.BORRADOR.name,
    val fechaUltimaModificacion: Long
)

@Entity(
    tableName = "items_cotizacion",
    foreignKeys = [
        ForeignKey(entity = CotizacionEntity::class, parentColumns = ["id"], childColumns = ["cotizacionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ProductoEntity::class, parentColumns = ["id"], childColumns = ["productoId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["cotizacionId"]), Index(value = ["productoId"])]
)
data class ItemCotizacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cotizacionId: Long,
    val productoId: Long,
    val nombreProducto: String,
    val categoriaProducto: String,
    val cantidad: Int,
    val precioUnitarioCentavos: Long,
    val descuentoPorcentaje: Double,
    val subtotalCentavos: Long,
    val descuentoCentavos: Long,
    val totalLineaCentavos: Long
)

enum class EstadoCotizacion { BORRADOR, ENVIADA, APROBADA, RECHAZADA }
