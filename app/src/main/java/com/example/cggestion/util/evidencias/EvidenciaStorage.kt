package com.example.cggestion.util.evidencias

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

data class ArchivoEvidencia(
    val rutaInterna: String,
    val nombre: String,
    val tamano: Long,
    val ancho: Int?,
    val alto: Int?
)

class EvidenciaStorage(private val context: Context) {
    private val raiz get() = File(context.filesDir, "evidencias")
    private val temporal get() = File(raiz, "temporal")

    fun crearTemporal(): File {
        temporal.mkdirs()
        return File(temporal, "captura_${UUID.randomUUID()}.jpg")
    }

    fun eliminarTemporal(ruta: String) {
        File(ruta).takeIf { it.parentFile?.canonicalPath == temporal.canonicalPath }?.delete()
    }

    fun moverTemporal(hojaNumero: String, temporalRuta: String): ArchivoEvidencia {
        val origen = File(temporalRuta)
        require(origen.parentFile?.canonicalPath == temporal.canonicalPath && origen.exists()) {
            "No se encontró la fotografía temporal."
        }
        return procesar(origen, hojaNumero).also { origen.delete() }
    }

    fun importarUri(hojaNumero: String, uri: Uri): ArchivoEvidencia {
        val copia = crearTemporal()
        try {
            context.contentResolver.openInputStream(uri)?.use { entrada ->
                copia.outputStream().use { salida -> entrada.copyTo(salida) }
            } ?: throw IllegalArgumentException("No se pudo acceder a la imagen seleccionada.")
            return procesar(copia, hojaNumero)
        } finally {
            copia.delete()
        }
    }

    fun archivo(rutaInterna: String): File = File(context.filesDir, rutaInterna)

    fun eliminar(rutaInterna: String): Boolean = archivo(rutaInterna).let { !it.exists() || it.delete() }

    private fun procesar(origen: File, hojaNumero: String): ArchivoEvidencia {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(origen.absolutePath, opciones)
        val anchoOriginal = opciones.outWidth.takeIf { it > 0 }
        val altoOriginal = opciones.outHeight.takeIf { it > 0 }
        require(anchoOriginal != null && altoOriginal != null) { "El archivo seleccionado no es una imagen válida." }

        var muestra = 1
        while (max(anchoOriginal / muestra, altoOriginal / muestra) > LADO_MAXIMO * 2) muestra *= 2
        val bitmap = BitmapFactory.decodeFile(
            origen.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = muestra }
        ) ?: throw IllegalArgumentException("No se pudo procesar la imagen.")

        val corregido = try {
            val rotacion = ExifInterface(origen).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            transformarSegunExif(bitmap, rotacion)
        } catch (_: Exception) {
            bitmap
        }
        val final = reducirSiNecesario(corregido)
        val carpeta = carpetaHoja(hojaNumero).apply { mkdirs() }
        val nombre = siguienteNombre(carpeta, hojaNumero)
        val destino = File(carpeta, nombre)
        try {
            FileOutputStream(destino).use { salida ->
                check(final.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)) {
                    "No se pudo guardar la fotografía."
                }
            }
        } catch (error: Throwable) {
            destino.delete()
            throw error
        } finally {
            if (final !== corregido) final.recycle()
            if (corregido !== bitmap) corregido.recycle()
            bitmap.recycle()
        }
        return ArchivoEvidencia(
            rutaInterna = "evidencias/${carpeta.name}/$nombre",
            nombre = nombre,
            tamano = destino.length(),
            ancho = anchoOriginal,
            alto = altoOriginal
        )
    }

    private fun carpetaHoja(numero: String): File = File(raiz, numero.filter(Char::isLetterOrDigit).ifBlank { "sin_numero" })

    private fun siguienteNombre(carpeta: File, numero: String): String {
        val prefijo = numero.filter(Char::isLetterOrDigit).ifBlank { "sin_numero" }
        val expresion = Regex("${Regex.escape(prefijo)}_(\\d{3})\\.jpg")
        val siguiente = carpeta.listFiles().orEmpty().mapNotNull { archivo ->
            expresion.matchEntire(archivo.name)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }.maxOrNull()?.plus(1) ?: 1
        return "${prefijo}_${siguiente.toString().padStart(3, '0')}.jpg"
    }

    private fun reducirSiNecesario(bitmap: Bitmap): Bitmap {
        val ladoMayor = max(bitmap.width, bitmap.height)
        if (ladoMayor <= LADO_MAXIMO) return bitmap
        val factor = LADO_MAXIMO.toFloat() / ladoMayor
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt(), (bitmap.height * factor).toInt(), true)
    }

    private fun transformarSegunExif(bitmap: Bitmap, orientacion: Int): Bitmap {
        val matriz = Matrix().apply {
            when (orientacion) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                else -> return bitmap
            }
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    private companion object {
        const val LADO_MAXIMO = 2048
        const val CALIDAD_JPEG = 85
    }
}
