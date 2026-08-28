package com.example.cggestion.util.firma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

data class PuntoFirma(val x: Float, val y: Float)
typealias TrazoFirma = List<PuntoFirma>

class FirmaClienteStorage(private val context: Context) {
    private val raiz get() = File(context.filesDir, "firmas_hoja_campo")

    fun archivo(rutaInterna: String?): File? {
        if (rutaInterna.isNullOrBlank()) return null
        val archivo = File(context.filesDir, rutaInterna)
        return runCatching {
            archivo.canonicalPath.takeIf { it.startsWith(raiz.canonicalPath + File.separator) }
                ?.let(::File)
        }.getOrNull()
    }

    fun existe(rutaInterna: String?): Boolean = archivo(rutaInterna)?.isFile == true

    fun guardar(numeroHoja: String, trazos: List<TrazoFirma>): String {
        require(trazos.any { it.isNotEmpty() }) { "La firma no contiene trazos." }
        raiz.mkdirs()
        val nombre = "${numeroSeguro(numeroHoja)}.png"
        val destino = File(raiz, nombre)
        val temporal = File(raiz, ".${nombre}.tmp")
        val respaldo = File(raiz, ".${nombre}.bak")
        val bitmap = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888)
        try {
            Canvas(bitmap).apply {
                drawColor(Color.WHITE)
                val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(25, 33, 39)
                    style = Paint.Style.STROKE
                    strokeWidth = 8f
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                trazos.forEach { trazo -> dibujarTrazo(this, trazo, pintura, bitmap.width, bitmap.height) }
            }
            FileOutputStream(temporal).use { salida ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida)) { "No se pudo guardar la firma." }
            }
            if (destino.exists()) destino.copyTo(respaldo, overwrite = true)
            if (destino.exists() && !destino.delete()) error("No se pudo reemplazar la firma anterior.")
            if (!temporal.renameTo(destino)) error("No se pudo finalizar la firma.")
            respaldo.delete()
            return "firmas_hoja_campo/$nombre"
        } catch (error: Throwable) {
            temporal.delete()
            if (!destino.exists() && respaldo.exists()) respaldo.copyTo(destino, overwrite = true)
            throw error
        } finally {
            respaldo.delete()
            bitmap.recycle()
        }
    }

    fun eliminar(rutaInterna: String?): Boolean = archivo(rutaInterna)?.let { !it.exists() || it.delete() } ?: true

    private fun dibujarTrazo(canvas: Canvas, puntos: TrazoFirma, pintura: Paint, ancho: Int, alto: Int) {
        if (puntos.isEmpty()) return
        fun x(punto: PuntoFirma) = min(1f, max(0f, punto.x)) * ancho
        fun y(punto: PuntoFirma) = min(1f, max(0f, punto.y)) * alto
        if (puntos.size == 1) {
            canvas.drawCircle(x(puntos.first()), y(puntos.first()), pintura.strokeWidth / 2, pintura)
            return
        }
        val ruta = Path().apply {
            moveTo(x(puntos.first()), y(puntos.first()))
            puntos.drop(1).forEach { lineTo(x(it), y(it)) }
        }
        canvas.drawPath(ruta, pintura)
    }

    private fun numeroSeguro(numero: String): String = numero.filter(Char::isLetterOrDigit).ifBlank { "hoja" }
}
