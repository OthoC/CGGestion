package com.example.cggestion.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.cggestion.data.CatalogoInicial
import com.example.cggestion.data.local.dao.ClienteDao
import com.example.cggestion.data.local.dao.CotizacionDao
import com.example.cggestion.data.local.dao.ProductoDao
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.CotizacionEntity
import com.example.cggestion.data.local.entity.ItemCotizacionEntity
import com.example.cggestion.data.local.entity.ProductoEntity
import com.example.cggestion.data.local.entity.HojaCampoEntity
import com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity
import com.example.cggestion.data.local.entity.RepuestoUsadoEntity
import com.example.cggestion.data.local.entity.JornadaTrabajoEntity
import com.example.cggestion.data.local.entity.EvidenciaEntity
import com.example.cggestion.data.local.entity.MovimientoInventarioEntity
import com.example.cggestion.data.local.entity.ConsumoHojaInventarioEntity
import com.example.cggestion.data.local.entity.EquipoEntity
import com.example.cggestion.data.local.entity.MantenimientoEntity
import com.example.cggestion.data.local.entity.UsuarioEntity
import com.example.cggestion.data.local.dao.HojaCampoDao
import com.example.cggestion.data.local.dao.EvidenciaDao
import kotlin.math.roundToLong

@Database(entities = [ClienteEntity::class, ProductoEntity::class, CotizacionEntity::class, ItemCotizacionEntity::class, HojaCampoEntity::class, MedicionesHojaCampoEntity::class, JornadaTrabajoEntity::class, EvidenciaEntity::class, MovimientoInventarioEntity::class, RepuestoUsadoEntity::class, ConsumoHojaInventarioEntity::class, EquipoEntity::class, MantenimientoEntity::class, UsuarioEntity::class], version = 12, exportSchema = false)
abstract class CGGestionDatabase : RoomDatabase() {
    abstract fun clienteDao(): ClienteDao
    abstract fun productoDao(): ProductoDao
    abstract fun cotizacionDao(): CotizacionDao
    abstract fun hojaCampoDao(): HojaCampoDao
    abstract fun evidenciaDao(): EvidenciaDao
    abstract fun movimientoInventarioDao(): com.example.cggestion.data.local.dao.MovimientoInventarioDao
    abstract fun consumoHojaInventarioDao(): com.example.cggestion.data.local.dao.ConsumoHojaInventarioDao
    abstract fun equipoDao(): com.example.cggestion.data.local.dao.EquipoDao
    abstract fun mantenimientoDao(): com.example.cggestion.data.local.dao.MantenimientoDao
    abstract fun usuarioDao(): com.example.cggestion.data.local.dao.UsuarioDao

    companion object {
        fun crear(context: Context): CGGestionDatabase = Room.databaseBuilder(context, CGGestionDatabase::class.java, "cg_gestion.db")
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CatalogoInicial.productos.forEach { producto ->
                        val nombre = producto.nombre.replace("'", "''")
                        val categoria = producto.categoria.replace("'", "''")
                        val precio = (producto.precio * 100).roundToLong()
                        db.execSQL("INSERT INTO productos (nombre, categoria, precioPredeterminadoCentavos, unidad, stockActual, activo, actualizadoEn) VALUES ('$nombre', '$categoria', $precio, 'Unidad', 0, 1, ${System.currentTimeMillis()})")
                    }
                }
            })
            .addMigrations(migracion1a2(), migracion2a3(), migracion3a4(), migracion4a5(), migracion5a6(), migracion6a7(), migracion7a8(), migracion8a9(), migracion9a10(), migracion10a11(), migracion11a12()).build()
    fun migracion1a2()=object:Migration(1,2){override fun migrate(db:SupportSQLiteDatabase){
      db.execSQL("CREATE TABLE IF NOT EXISTS hojas_campo (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, numeroHoja TEXT NOT NULL, fecha INTEGER NOT NULL, clienteId INTEGER NOT NULL, cotizacionId INTEGER, estado TEXT NOT NULL, fechaCreacion INTEGER NOT NULL, fechaModificacion INTEGER NOT NULL, direccion TEXT NOT NULL, telefono TEXT NOT NULL, ordenTrabajo TEXT NOT NULL, tecnicos TEXT NOT NULL, alternadorMarca TEXT NOT NULL, alternadorModelo TEXT NOT NULL, alternadorSerie TEXT NOT NULL, rpm TEXT NOT NULL, kva TEXT NOT NULL, volt TEXT NOT NULL, kw TEXT NOT NULL, amp TEXT NOT NULL, hz TEXT NOT NULL, motorMarca TEXT NOT NULL, motorModelo TEXT NOT NULL, motorSerie TEXT NOT NULL, horometro TEXT NOT NULL, tipoTablero TEXT NOT NULL, trabajosRealizados TEXT NOT NULL, observaciones TEXT NOT NULL, horaInicioPruebas TEXT NOT NULL, horaFinPruebas TEXT NOT NULL, nombreClienteResponsable TEXT NOT NULL, estadoFirma TEXT NOT NULL, FOREIGN KEY(clienteId) REFERENCES clientes(id) ON DELETE RESTRICT, FOREIGN KEY(cotizacionId) REFERENCES cotizaciones(id) ON DELETE SET NULL)")
      db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_hojas_campo_numeroHoja ON hojas_campo(numeroHoja)");db.execSQL("CREATE INDEX IF NOT EXISTS index_hojas_campo_clienteId ON hojas_campo(clienteId)");db.execSQL("CREATE INDEX IF NOT EXISTS index_hojas_campo_cotizacionId ON hojas_campo(cotizacionId)")
      db.execSQL("CREATE TABLE IF NOT EXISTS mediciones_hoja_campo (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hojaCampoId INTEGER NOT NULL, l1l2 TEXT NOT NULL, l2l3 TEXT NOT NULL, l3l1 TEXT NOT NULL, ln TEXT NOT NULL, ampL1 TEXT NOT NULL, ampL2 TEXT NOT NULL, ampL3 TEXT NOT NULL, hzVacio TEXT NOT NULL, hzCarga TEXT NOT NULL, rpmVacio TEXT NOT NULL, rpmCarga TEXT NOT NULL, presionAceite TEXT NOT NULL, temperaturaMotor TEXT NOT NULL, cargaAlternador TEXT NOT NULL, nivelAceite TEXT NOT NULL, nivelRefrigerante TEXT NOT NULL, voltajeBateria TEXT NOT NULL, combustible TEXT NOT NULL, limpieza TEXT NOT NULL, electrolitos TEXT NOT NULL, mantenedorBateria TEXT NOT NULL, precalentadorBlock TEXT NOT NULL, FOREIGN KEY(hojaCampoId) REFERENCES hojas_campo(id) ON DELETE CASCADE)");db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_mediciones_hoja_campo_hojaCampoId ON mediciones_hoja_campo(hojaCampoId)")
      db.execSQL("CREATE TABLE IF NOT EXISTS repuestos_usados (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hojaCampoId INTEGER NOT NULL, productoId INTEGER, codigo TEXT NOT NULL, nombre TEXT NOT NULL, unidad TEXT NOT NULL, cantidad REAL NOT NULL, costoCentavos INTEGER NOT NULL, FOREIGN KEY(hojaCampoId) REFERENCES hojas_campo(id) ON DELETE CASCADE, FOREIGN KEY(productoId) REFERENCES productos(id) ON DELETE SET NULL)");db.execSQL("CREATE INDEX IF NOT EXISTS index_repuestos_usados_hojaCampoId ON repuestos_usados(hojaCampoId)");db.execSQL("CREATE INDEX IF NOT EXISTS index_repuestos_usados_productoId ON repuestos_usados(productoId)")
      db.execSQL("CREATE TABLE IF NOT EXISTS jornadas_trabajo (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hojaCampoId INTEGER NOT NULL, fecha INTEGER NOT NULL, horaInicio TEXT NOT NULL, horaFin TEXT NOT NULL, minutosTotales INTEGER NOT NULL, tecnicos TEXT NOT NULL, observacion TEXT NOT NULL, FOREIGN KEY(hojaCampoId) REFERENCES hojas_campo(id) ON DELETE CASCADE)");db.execSQL("CREATE INDEX IF NOT EXISTS index_jornadas_trabajo_hojaCampoId ON jornadas_trabajo(hojaCampoId)")
    }}
    fun migracion2a3() = object : Migration(2, 3) { override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("CREATE TABLE IF NOT EXISTS evidencias (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hojaCampoId INTEGER NOT NULL, rutaInterna TEXT NOT NULL, nombreArchivo TEXT NOT NULL, descripcion TEXT NOT NULL, tipoEvidencia TEXT NOT NULL, fechaHoraCaptura INTEGER NOT NULL, fechaHoraRegistro INTEGER NOT NULL, orden INTEGER NOT NULL, tamanoBytes INTEGER NOT NULL, ancho INTEGER, alto INTEGER, FOREIGN KEY(hojaCampoId) REFERENCES hojas_campo(id) ON DELETE CASCADE)")
      db.execSQL("CREATE INDEX IF NOT EXISTS index_evidencias_hojaCampoId ON evidencias(hojaCampoId)")
    }}
    fun migracion3a4()=object:Migration(3,4){override fun migrate(db:SupportSQLiteDatabase){db.execSQL("ALTER TABLE productos ADD COLUMN unidad TEXT NOT NULL DEFAULT 'Unidad'");db.execSQL("ALTER TABLE productos ADD COLUMN stockActual REAL NOT NULL DEFAULT 0");db.execSQL("CREATE TABLE IF NOT EXISTS movimientos_inventario (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, productoId INTEGER NOT NULL, tipo TEXT NOT NULL, cantidad REAL NOT NULL, fecha INTEGER NOT NULL, observacion TEXT NOT NULL, FOREIGN KEY(productoId) REFERENCES productos(id) ON DELETE CASCADE)");db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_inventario_productoId ON movimientos_inventario(productoId)")}}
    fun migracion4a5() = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // La migración 3→4 añadió columnas con DEFAULT. Room espera el esquema final
            // sin esos defaults, por lo que se reconstruye la tabla y sus tres dependientes.
            db.execSQL("ALTER TABLE productos RENAME TO productos_legacy")
            db.execSQL("CREATE TABLE productos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, categoria TEXT NOT NULL, precioPredeterminadoCentavos INTEGER NOT NULL, unidad TEXT NOT NULL, stockActual REAL NOT NULL, activo INTEGER NOT NULL, actualizadoEn INTEGER NOT NULL)")
            db.execSQL("INSERT INTO productos (id, nombre, categoria, precioPredeterminadoCentavos, unidad, stockActual, activo, actualizadoEn) SELECT id, nombre, categoria, precioPredeterminadoCentavos, unidad, stockActual, activo, actualizadoEn FROM productos_legacy")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_nombre ON productos(nombre)")

            db.execSQL("ALTER TABLE items_cotizacion RENAME TO items_cotizacion_legacy")
            db.execSQL("CREATE TABLE items_cotizacion (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, cotizacionId INTEGER NOT NULL, productoId INTEGER NOT NULL, nombreProducto TEXT NOT NULL, categoriaProducto TEXT NOT NULL, cantidad INTEGER NOT NULL, precioUnitarioCentavos INTEGER NOT NULL, descuentoPorcentaje REAL NOT NULL, subtotalCentavos INTEGER NOT NULL, descuentoCentavos INTEGER NOT NULL, totalLineaCentavos INTEGER NOT NULL, FOREIGN KEY(cotizacionId) REFERENCES cotizaciones(id) ON DELETE CASCADE, FOREIGN KEY(productoId) REFERENCES productos(id) ON DELETE RESTRICT)")
            db.execSQL("INSERT INTO items_cotizacion (id, cotizacionId, productoId, nombreProducto, categoriaProducto, cantidad, precioUnitarioCentavos, descuentoPorcentaje, subtotalCentavos, descuentoCentavos, totalLineaCentavos) SELECT id, cotizacionId, productoId, nombreProducto, categoriaProducto, cantidad, precioUnitarioCentavos, descuentoPorcentaje, subtotalCentavos, descuentoCentavos, totalLineaCentavos FROM items_cotizacion_legacy")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_items_cotizacion_cotizacionId ON items_cotizacion(cotizacionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_items_cotizacion_productoId ON items_cotizacion(productoId)")
            db.execSQL("DROP TABLE items_cotizacion_legacy")

            db.execSQL("ALTER TABLE repuestos_usados RENAME TO repuestos_usados_legacy")
            db.execSQL("CREATE TABLE repuestos_usados (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hojaCampoId INTEGER NOT NULL, productoId INTEGER, codigo TEXT NOT NULL, nombre TEXT NOT NULL, unidad TEXT NOT NULL, cantidad REAL NOT NULL, costoCentavos INTEGER NOT NULL, FOREIGN KEY(hojaCampoId) REFERENCES hojas_campo(id) ON DELETE CASCADE, FOREIGN KEY(productoId) REFERENCES productos(id) ON DELETE SET NULL)")
            db.execSQL("INSERT INTO repuestos_usados (id, hojaCampoId, productoId, codigo, nombre, unidad, cantidad, costoCentavos) SELECT id, hojaCampoId, productoId, codigo, nombre, unidad, cantidad, costoCentavos FROM repuestos_usados_legacy")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_repuestos_usados_hojaCampoId ON repuestos_usados(hojaCampoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_repuestos_usados_productoId ON repuestos_usados(productoId)")
            db.execSQL("DROP TABLE repuestos_usados_legacy")

            db.execSQL("ALTER TABLE movimientos_inventario RENAME TO movimientos_inventario_legacy")
            db.execSQL("CREATE TABLE movimientos_inventario (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, productoId INTEGER NOT NULL, tipo TEXT NOT NULL, cantidad REAL NOT NULL, fecha INTEGER NOT NULL, observacion TEXT NOT NULL, FOREIGN KEY(productoId) REFERENCES productos(id) ON DELETE CASCADE)")
            db.execSQL("INSERT INTO movimientos_inventario (id, productoId, tipo, cantidad, fecha, observacion) SELECT id, productoId, tipo, cantidad, fecha, observacion FROM movimientos_inventario_legacy")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_inventario_productoId ON movimientos_inventario(productoId)")
            db.execSQL("DROP TABLE movimientos_inventario_legacy")

            db.execSQL("DROP TABLE productos_legacy")
        }
    }
    fun migracion5a6() = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Los nombres de índice se conservan durante ALTER TABLE ... RENAME.
            // Esta migración asegura los índices requeridos una vez eliminadas las tablas temporales.
            db.execSQL("CREATE INDEX IF NOT EXISTS index_productos_nombre ON productos(nombre)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_items_cotizacion_cotizacionId ON items_cotizacion(cotizacionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_items_cotizacion_productoId ON items_cotizacion(productoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_repuestos_usados_hojaCampoId ON repuestos_usados(hojaCampoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_repuestos_usados_productoId ON repuestos_usados(productoId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_inventario_productoId ON movimientos_inventario(productoId)")
        }
    }
    fun migracion6a7() = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE productos ADD COLUMN codigo TEXT")
            db.execSQL("ALTER TABLE productos ADD COLUMN stockMinimo REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE movimientos_inventario ADD COLUMN stockResultante REAL NOT NULL DEFAULT 0")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_productos_codigo ON productos(codigo)")
        }
    }
    fun migracion7a8() = object : Migration(7, 8) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS consumos_hoja_inventario (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hojaCampoId INTEGER NOT NULL, productoId INTEGER NOT NULL, cantidad REAL NOT NULL, FOREIGN KEY(hojaCampoId) REFERENCES hojas_campo(id) ON DELETE CASCADE, FOREIGN KEY(productoId) REFERENCES productos(id) ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_consumos_hoja_inventario_hojaCampoId ON consumos_hoja_inventario(hojaCampoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_consumos_hoja_inventario_productoId ON consumos_hoja_inventario(productoId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_consumos_hoja_inventario_hojaCampoId_productoId ON consumos_hoja_inventario(hojaCampoId, productoId)")
    }}
    fun migracion8a9() = object : Migration(8, 9) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS equipos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, clienteId INTEGER NOT NULL, tipo TEXT NOT NULL, marca TEXT NOT NULL, modelo TEXT NOT NULL, serie TEXT, potenciaKva TEXT NOT NULL, motorMarca TEXT NOT NULL, motorModelo TEXT NOT NULL, alternadorMarca TEXT NOT NULL, alternadorModelo TEXT NOT NULL, ubicacion TEXT NOT NULL, observaciones TEXT NOT NULL, activo INTEGER NOT NULL, actualizadoEn INTEGER NOT NULL, FOREIGN KEY(clienteId) REFERENCES clientes(id) ON DELETE RESTRICT)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_equipos_clienteId ON equipos(clienteId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_equipos_serie ON equipos(serie)")
        db.execSQL("ALTER TABLE hojas_campo ADD COLUMN equipoId INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_hojas_campo_equipoId ON hojas_campo(equipoId)")
    }}
    fun migracion9a10() = object : Migration(9, 10) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS mantenimientos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, clienteId INTEGER NOT NULL, equipoId INTEGER NOT NULL, tipo TEXT NOT NULL, descripcion TEXT NOT NULL, prioridad TEXT NOT NULL, fechaProgramada INTEGER NOT NULL, periodicidadDias INTEGER, estado TEXT NOT NULL, hojaCampoId INTEGER, fechaCreacion INTEGER NOT NULL, fechaModificacion INTEGER NOT NULL, FOREIGN KEY(clienteId) REFERENCES clientes(id) ON DELETE RESTRICT, FOREIGN KEY(equipoId) REFERENCES equipos(id) ON DELETE RESTRICT, FOREIGN KEY(hojaCampoId) REFERENCES hojas_campo(id) ON DELETE SET NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mantenimientos_clienteId ON mantenimientos(clienteId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mantenimientos_equipoId ON mantenimientos(equipoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mantenimientos_hojaCampoId ON mantenimientos(hojaCampoId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mantenimientos_estado ON mantenimientos(estado)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_mantenimientos_fechaProgramada ON mantenimientos(fechaProgramada)")
    }}
    fun migracion10a11() = object : Migration(10, 11) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, usuario TEXT NOT NULL, passwordHash TEXT NOT NULL, sal TEXT NOT NULL, iteraciones INTEGER NOT NULL, rol TEXT NOT NULL, activo INTEGER NOT NULL, fechaCreacion INTEGER NOT NULL, fechaActualizacion INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_usuario ON usuarios(usuario)")
    }}
    fun migracion11a12() = object : Migration(11, 12) { override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM usuarios")
    }}
    }
}
