package com.example.cggestion.util.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.RoomDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupResultado(val archivo: File)

class BackupManager(
    private val context: Context,
    private val database: () -> RoomDatabase,
    private val cerrarBase: () -> Unit
) {
    private val raiz get() = context.filesDir
    private val carpetaRespaldos get() = File(raiz, "backups")
    private val baseDatos get() = context.getDatabasePath("cg_gestion.db")
    private val preferencias get() = context.getSharedPreferences("respaldos", Context.MODE_PRIVATE)

    fun carpetaNube(): Uri? = preferencias.getString("carpeta_nube", null)?.let(Uri::parse)
    fun guardarCarpetaNube(uri: Uri) { preferencias.edit().putString("carpeta_nube", uri.toString()).apply() }
    fun ultimoRespaldo(): File? = carpetaRespaldos.listFiles()?.filter { it.isFile && it.extension.equals("zip", true) }?.maxByOrNull { it.lastModified() }

    fun subirANube(archivo: File = ultimoRespaldo() ?: error("Primero crea un respaldo.")): Uri {
        require(archivo.exists()) { "No se encontró el respaldo a subir." }
        val arbol = carpetaNube() ?: error("Selecciona primero una carpeta de Google Drive.")
        val destino = DocumentsContract.buildDocumentUriUsingTree(arbol, DocumentsContract.getTreeDocumentId(arbol))
        val uri = DocumentsContract.createDocument(context.contentResolver, destino, "application/zip", archivo.name)
            ?: error("No se pudo crear el archivo en la carpeta seleccionada.")
        try {
            context.contentResolver.openOutputStream(uri)?.use { salida -> archivo.inputStream().use { it.copyTo(salida) } }
                ?: error("No se pudo escribir el respaldo en la nube.")
        } catch (error: Throwable) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            throw error
        }
        return uri
    }

    fun crear(): BackupResultado {
        carpetaRespaldos.mkdirs()
        checkpoint()
        val archivo = File(carpetaRespaldos, "CGGestion_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(archivo)).use { zip ->
            val propiedades = Properties().apply {
                setProperty("formato", "CG_GESTION_BACKUP")
                setProperty("version", "1")
                setProperty("roomVersion", "13")
                setProperty("creadoEn", System.currentTimeMillis().toString())
            }
            zip.putNextEntry(ZipEntry("manifest.properties")); propiedades.store(zip, "CG Gestion backup"); zip.closeEntry()
            agregar(zip, baseDatos, "database/cg_gestion.db")
            agregarSiExiste(zip, File(baseDatos.path + "-wal"), "database/cg_gestion.db-wal")
            agregarSiExiste(zip, File(baseDatos.path + "-shm"), "database/cg_gestion.db-shm")
            agregarCarpeta(zip, File(raiz, "cotizaciones"), "cotizaciones")
            agregarCarpeta(zip, File(raiz, "hojas_campo_pdf"), "hojas_campo_pdf")
            agregarCarpeta(zip, File(raiz, "evidencias"), "evidencias")
            agregarCarpeta(zip, File(raiz, "firmas_hoja_campo"), "firmas_hoja_campo")
        }
        return BackupResultado(archivo)
    }

    fun restaurar(uri: Uri) {
        val entrada = File(context.cacheDir, "restore_${System.currentTimeMillis()}.zip")
        val staging = File(context.cacheDir, "restore_staging")
        try {
            context.contentResolver.openInputStream(uri)?.use { origen -> entrada.outputStream().use(origen::copyTo) }
                ?: throw IllegalArgumentException("No se pudo leer el respaldo seleccionado.")
            staging.deleteRecursively(); staging.mkdirs()
            extraerYValidar(entrada, staging)
            val nuevaBase = File(staging, "database/cg_gestion.db")
            require(nuevaBase.exists() && nuevaBase.length() > 0) { "El respaldo no contiene la base de datos." }
            val recuperacion = File(context.cacheDir, "restore_previous")
            recuperacion.deleteRecursively(); recuperacion.mkdirs()
            copiarSiExiste(baseDatos, File(recuperacion, "cg_gestion.db"))
            copiarSiExiste(File(baseDatos.path + "-wal"), File(recuperacion, "cg_gestion.db-wal"))
            copiarSiExiste(File(baseDatos.path + "-shm"), File(recuperacion, "cg_gestion.db-shm"))
            copiarCarpeta(File(raiz, "cotizaciones"), File(recuperacion, "cotizaciones"))
            copiarCarpeta(File(raiz, "hojas_campo_pdf"), File(recuperacion, "hojas_campo_pdf"))
            copiarCarpeta(File(raiz, "evidencias"), File(recuperacion, "evidencias"))
            copiarCarpeta(File(raiz, "firmas_hoja_campo"), File(recuperacion, "firmas_hoja_campo"))
            try {
                cerrarBase()
                baseDatos.parentFile?.mkdirs()
                baseDatos.delete(); File(baseDatos.path + "-wal").delete(); File(baseDatos.path + "-shm").delete()
                nuevaBase.copyTo(baseDatos, overwrite = true)
                copiarSiExiste(File(staging, "database/cg_gestion.db-wal"), File(baseDatos.path + "-wal"))
                copiarSiExiste(File(staging, "database/cg_gestion.db-shm"), File(baseDatos.path + "-shm"))
                reemplazarCarpeta(File(staging, "cotizaciones"), File(raiz, "cotizaciones"))
                reemplazarCarpeta(File(staging, "hojas_campo_pdf"), File(raiz, "hojas_campo_pdf"))
                reemplazarCarpeta(File(staging, "evidencias"), File(raiz, "evidencias"))
                reemplazarCarpeta(File(staging, "firmas_hoja_campo"), File(raiz, "firmas_hoja_campo"))
            } catch (error: Throwable) {
                restaurarRecuperacion(recuperacion)
                throw error
            }
        } finally {
            entrada.delete(); staging.deleteRecursively()
        }
    }

    private fun checkpoint() {
        database().openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
    }
    private fun agregar(zip: ZipOutputStream, archivo: File, destino: String) {
        require(archivo.exists()) { "No se encontró la base de datos para respaldar." }
        zip.putNextEntry(ZipEntry(destino)); FileInputStream(archivo).use { it.copyTo(zip) }; zip.closeEntry()
    }
    private fun agregarSiExiste(zip: ZipOutputStream, archivo: File, destino: String) { if (archivo.exists()) agregar(zip, archivo, destino) }
    private fun agregarCarpeta(zip: ZipOutputStream, carpeta: File, prefijo: String) {
        carpeta.walkTopDown().filter { it.isFile && !it.name.startsWith("captura_") }.forEach { archivo ->
            agregar(zip, archivo, "$prefijo/${archivo.relativeTo(carpeta).invariantSeparatorsPath}")
        }
    }
    private fun extraerYValidar(zip: File, destino: File) {
        var manifest: Properties? = null
        var total = 0L
        ZipInputStream(FileInputStream(zip)).use { entrada ->
            var item = entrada.nextEntry
            while (item != null) {
                val nombre = item.name.replace('\\', '/')
                require(nombre == "manifest.properties" || nombre.startsWith("database/") || nombre.startsWith("cotizaciones/") || nombre.startsWith("hojas_campo_pdf/") || nombre.startsWith("evidencias/") || nombre.startsWith("firmas_hoja_campo/")) { "El respaldo contiene rutas no permitidas." }
                require(!nombre.contains("../") && !nombre.startsWith('/')) { "El respaldo contiene una ruta insegura." }
                if (!item.isDirectory) {
                    val archivo = File(destino, nombre)
                    require(archivo.canonicalPath.startsWith(destino.canonicalPath + File.separator)) { "Ruta insegura en respaldo." }
                    archivo.parentFile?.mkdirs()
                    archivo.outputStream().use { salida ->
                        val buffer = ByteArray(8192); var leidos: Int
                        while (entrada.read(buffer).also { leidos = it } > 0) { total += leidos; require(total <= TAMANO_MAXIMO) { "El respaldo supera el tamaño permitido." }; salida.write(buffer, 0, leidos) }
                    }
                    if (nombre == "manifest.properties") manifest = Properties().also { FileInputStream(archivo).use(it::load) }
                }
                entrada.closeEntry(); item = entrada.nextEntry
            }
        }
        require(manifest?.getProperty("formato") == "CG_GESTION_BACKUP") { "El archivo no es un respaldo válido de CG Gestión." }
        require((manifest?.getProperty("roomVersion")?.toIntOrNull() ?: 0) <= 13) { "El respaldo fue creado con una versión más reciente de la aplicación." }
    }
    private fun copiarSiExiste(origen: File, destino: File) { if (origen.exists()) { destino.parentFile?.mkdirs(); origen.copyTo(destino, true) } }
    private fun copiarCarpeta(origen: File, destino: File) { if (origen.exists()) origen.copyRecursively(destino, overwrite = true) }
    private fun reemplazarCarpeta(origen: File, destino: File) { destino.deleteRecursively(); if (origen.exists()) origen.copyRecursively(destino, overwrite = true) }
    private fun restaurarRecuperacion(origen: File) {
        copiarSiExiste(File(origen, "cg_gestion.db"), baseDatos)
        copiarSiExiste(File(origen, "cg_gestion.db-wal"), File(baseDatos.path + "-wal"))
        copiarSiExiste(File(origen, "cg_gestion.db-shm"), File(baseDatos.path + "-shm"))
        reemplazarCarpeta(File(origen, "cotizaciones"), File(raiz, "cotizaciones")); reemplazarCarpeta(File(origen, "hojas_campo_pdf"), File(raiz, "hojas_campo_pdf")); reemplazarCarpeta(File(origen, "evidencias"), File(raiz, "evidencias")); reemplazarCarpeta(File(origen, "firmas_hoja_campo"), File(raiz, "firmas_hoja_campo"))
    }
    private companion object { const val TAMANO_MAXIMO = 1_500L * 1024 * 1024 }
}
