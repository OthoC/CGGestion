package com.example.cggestion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.CotizacionCompleta
import com.example.cggestion.data.local.entity.CotizacionEntity
import com.example.cggestion.data.local.entity.CotizacionResumen
import com.example.cggestion.data.local.entity.ItemCotizacionEntity
import com.example.cggestion.data.local.entity.ProductoEntity
import com.example.cggestion.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(cliente: ClienteEntity): Long
    @Update suspend fun actualizar(cliente: ClienteEntity)
    @Query("SELECT * FROM clientes WHERE rucCedula = :ruc LIMIT 1") suspend fun porRuc(ruc: String): ClienteEntity?
    @Query("SELECT * FROM clientes WHERE nombre LIKE '%' || :texto || '%' ORDER BY nombre") fun buscarPorNombre(texto: String): Flow<List<ClienteEntity>>
    @Query("SELECT * FROM clientes ORDER BY nombre") fun todos(): Flow<List<ClienteEntity>>
    @Query("SELECT * FROM clientes WHERE id = :id LIMIT 1") suspend fun porId(id: Long): ClienteEntity?
}

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(usuario: UsuarioEntity): Long
    @Update suspend fun actualizar(usuario: UsuarioEntity)
    @Query("SELECT * FROM usuarios WHERE usuario = :usuario LIMIT 1") suspend fun porUsuario(usuario: String): UsuarioEntity?
    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1") suspend fun porId(id: Long): UsuarioEntity?
    @Query("SELECT * FROM usuarios ORDER BY activo DESC, usuario") fun todos(): Flow<List<UsuarioEntity>>
    @Query("SELECT COUNT(*) FROM usuarios") suspend fun cantidad(): Int
    @Query("SELECT COUNT(*) FROM usuarios WHERE activo = 1 AND rol = 'ADMINISTRADOR'") suspend fun administradoresActivos(): Int
}

@Dao
interface ProductoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertarTodos(productos: List<ProductoEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(producto: ProductoEntity): Long
    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY nombre") fun activos(): Flow<List<ProductoEntity>>
    @Query("SELECT * FROM productos WHERE activo = 1 AND (nombre LIKE '%' || :texto || '%' OR categoria LIKE '%' || :texto || '%') ORDER BY nombre") fun buscar(texto: String): Flow<List<ProductoEntity>>
    @Query("SELECT * FROM productos ORDER BY activo DESC, nombre") fun todos(): Flow<List<ProductoEntity>>
    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1") suspend fun porId(id: Long): ProductoEntity?
    @Query("SELECT * FROM productos WHERE activo = 1 AND stockMinimo > 0 AND stockActual <= stockMinimo ORDER BY stockActual ASC, nombre") fun bajoMinimo(): Flow<List<ProductoEntity>>
    @Update suspend fun actualizar(producto: ProductoEntity)
    @Query("UPDATE productos SET stockActual = stockActual + :cantidad, actualizadoEn = :fecha WHERE id = :id")
    suspend fun ajustarStock(id: Long, cantidad: Double, fecha: Long = System.currentTimeMillis()): Int

    @Query("UPDATE productos SET stockActual = stockActual - :cantidad, actualizadoEn = :fecha WHERE id = :id AND stockActual >= :cantidad")
    suspend fun descontarStockSiDisponible(id: Long, cantidad: Double, fecha: Long = System.currentTimeMillis()): Int
}
@Dao interface MovimientoInventarioDao { @Insert suspend fun insertar(movimiento:MovimientoInventarioEntity):Long; @Query("SELECT * FROM movimientos_inventario WHERE productoId=:id ORDER BY fecha DESC, id DESC") fun porProducto(id:Long):Flow<List<MovimientoInventarioEntity>> }
@Dao interface ConsumoHojaInventarioDao {
 @Query("SELECT * FROM consumos_hoja_inventario WHERE hojaCampoId = :hojaCampoId") suspend fun porHoja(hojaCampoId: Long): List<ConsumoHojaInventarioEntity>
 @Insert suspend fun insertarTodos(items: List<ConsumoHojaInventarioEntity>)
 @Query("DELETE FROM consumos_hoja_inventario WHERE hojaCampoId = :hojaCampoId") suspend fun eliminarPorHoja(hojaCampoId: Long)
}
@Dao interface EquipoDao {
 @Insert suspend fun insertar(equipo: EquipoEntity): Long
 @Update suspend fun actualizar(equipo: EquipoEntity)
 @Query("SELECT * FROM equipos WHERE clienteId = :clienteId ORDER BY activo DESC, marca, modelo") fun porCliente(clienteId: Long): Flow<List<EquipoEntity>>
 @Query("SELECT * FROM equipos WHERE id = :id LIMIT 1") suspend fun porId(id: Long): EquipoEntity?
}

@Dao
interface CotizacionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(cotizacion: CotizacionEntity): Long
    @Update suspend fun actualizar(cotizacion: CotizacionEntity)
    @Insert suspend fun insertarItems(items: List<ItemCotizacionEntity>)
    @Query("DELETE FROM items_cotizacion WHERE cotizacionId = :cotizacionId") suspend fun eliminarItems(cotizacionId: Long)
    @Query("SELECT * FROM cotizaciones ORDER BY fechaUltimaModificacion DESC") fun todas(): Flow<List<CotizacionEntity>>
    @Query("SELECT cotizaciones.*, clientes.nombre AS clienteNombre FROM cotizaciones INNER JOIN clientes ON clientes.id = cotizaciones.clienteId ORDER BY cotizaciones.fechaUltimaModificacion DESC") fun resumenes(): Flow<List<CotizacionResumen>>
    @Transaction @Query("SELECT * FROM cotizaciones WHERE id = :id") suspend fun completaPorId(id: Long): CotizacionCompleta?
    @Query("SELECT EXISTS(SELECT 1 FROM cotizaciones WHERE numeroCotizacion = :numero)") suspend fun existeNumero(numero: String): Boolean
    @Query("SELECT numeroCotizacion FROM cotizaciones WHERE numeroCotizacion LIKE :prefijo || '%' ORDER BY numeroCotizacion DESC LIMIT 1") suspend fun ultimoNumero(prefijo: String): String?
}

@Dao interface HojaCampoDao {
 @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun insertar(hoja:HojaCampoEntity):Long
 @Update suspend fun actualizar(hoja:HojaCampoEntity)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun guardarMediciones(mediciones:MedicionesHojaCampoEntity)
 @Insert suspend fun insertarRepuestos(items:List<RepuestoUsadoEntity>)
 @Insert suspend fun insertarJornadas(items:List<JornadaTrabajoEntity>)
 @Query("DELETE FROM repuestos_usados WHERE hojaCampoId=:id") suspend fun eliminarRepuestos(id:Long)
 @Query("DELETE FROM jornadas_trabajo WHERE hojaCampoId=:id") suspend fun eliminarJornadas(id:Long)
 @Query("SELECT * FROM hojas_campo WHERE id=:id") @Transaction suspend fun completa(id:Long):HojaCampoCompleta?
 @Query("SELECT hojas_campo.*, clientes.nombre AS clienteNombre, COUNT(evidencias.id) AS evidenciasCantidad FROM hojas_campo INNER JOIN clientes ON clientes.id=hojas_campo.clienteId LEFT JOIN evidencias ON evidencias.hojaCampoId=hojas_campo.id GROUP BY hojas_campo.id ORDER BY hojas_campo.fechaModificacion DESC") fun resumenes():Flow<List<HojaCampoResumen>>
 @Query("SELECT EXISTS(SELECT 1 FROM hojas_campo WHERE numeroHoja=:numero)") suspend fun existeNumero(numero:String):Boolean
 @Query("SELECT numeroHoja FROM hojas_campo ORDER BY numeroHoja DESC LIMIT 1") suspend fun ultimoNumero():String?
 @Query("SELECT id FROM hojas_campo WHERE cotizacionId=:cotizacionId LIMIT 1") suspend fun porCotizacion(cotizacionId:Long):Long?
 @Query("SELECT * FROM hojas_campo WHERE equipoId = :equipoId ORDER BY fechaModificacion DESC") fun porEquipo(equipoId: Long): Flow<List<HojaCampoEntity>>
}

@Dao interface EvidenciaDao {
 @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(evidencia: EvidenciaEntity): Long
 @Update suspend fun actualizar(evidencia: EvidenciaEntity)
 @Delete suspend fun eliminar(evidencia: EvidenciaEntity)
 @Query("SELECT * FROM evidencias WHERE hojaCampoId = :hojaCampoId ORDER BY orden, id") fun porHoja(hojaCampoId: Long): Flow<List<EvidenciaEntity>>
 @Query("SELECT * FROM evidencias WHERE hojaCampoId = :hojaCampoId ORDER BY orden, id") suspend fun listaPorHoja(hojaCampoId: Long): List<EvidenciaEntity>
 @Query("SELECT COALESCE(MAX(orden), 0) + 1 FROM evidencias WHERE hojaCampoId = :hojaCampoId") suspend fun siguienteOrden(hojaCampoId: Long): Int
 @Query("UPDATE evidencias SET orden = :orden WHERE id = :id") suspend fun actualizarOrden(id: Long, orden: Int)
}

@Dao interface MantenimientoDao {
 @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertar(mantenimiento: MantenimientoEntity): Long
 @Update suspend fun actualizar(mantenimiento: MantenimientoEntity)
 @Query("SELECT m.*, c.nombre AS clienteNombre, TRIM(e.marca || ' ' || e.modelo) AS equipoNombre FROM mantenimientos m INNER JOIN clientes c ON c.id = m.clienteId INNER JOIN equipos e ON e.id = m.equipoId ORDER BY m.fechaProgramada ASC, m.id DESC") fun resumenes(): Flow<List<MantenimientoResumen>>
 @Query("SELECT * FROM mantenimientos WHERE id = :id LIMIT 1") suspend fun porId(id: Long): MantenimientoEntity?
 @Query("UPDATE mantenimientos SET hojaCampoId = :hojaId, estado = :estado, fechaModificacion = :fecha WHERE id = :id") suspend fun vincularHoja(id: Long, hojaId: Long, estado: String, fecha: Long = System.currentTimeMillis())
}
