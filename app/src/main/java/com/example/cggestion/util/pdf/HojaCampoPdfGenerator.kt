package com.example.cggestion.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.cggestion.R
import com.example.cggestion.data.local.entity.EvidenciaEntity
import com.example.cggestion.data.local.entity.HojaCampoCompleta
import com.example.cggestion.data.local.entity.JornadaTrabajoEntity
import com.example.cggestion.data.local.entity.RepuestoUsadoEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class HojaCampoPdfGenerator(private val context: Context) {
    private val logo: Bitmap? by lazy {
        runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.logo_cg) }.getOrNull()
    }

    fun archivoPara(numero: String): File = File(
        File(context.filesDir, "hojas_campo_pdf").apply { mkdirs() },
        "Hoja_Campo_${numero.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"
    )

    fun generar(datos: HojaCampoCompleta): PdfResultado {
        val archivo = archivoPara(datos.hoja.numeroHoja)
        val temporal = File(archivo.parentFile, "${archivo.name}.tmp")
        return try {
            val paginas = crearPaginas(datos)
            val documento = PdfDocument()
            try {
                paginas.forEachIndexed { index, contenido ->
                    val pagina = documento.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create())
                    try {
                        dibujarPagina(pagina.canvas, datos, contenido, index + 1, paginas.size)
                    } finally {
                        documento.finishPage(pagina)
                    }
                }
                FileOutputStream(temporal).use(documento::writeTo)
            } finally {
                documento.close()
            }
            if (archivo.exists() && !archivo.delete()) {
                temporal.delete()
                error("No se pudo reemplazar el PDF anterior.")
            }
            if (!temporal.renameTo(archivo)) {
                temporal.delete()
                error("No se pudo finalizar el archivo PDF.")
            }
            PdfResultado.Exito(archivo)
        } catch (e: Exception) {
            temporal.delete()
            PdfResultado.Error(e.message ?: "No se pudo generar el PDF de la hoja.")
        }
    }

    private fun crearPaginas(datos: HojaCampoCompleta): List<Pagina> {
        val result = mutableListOf<Pagina>(Pagina.Principal)
        val hoja = datos.hoja
        val workLines = lineas(na(hoja.trabajosRealizados), paint(8.2f, INK), WORK_WIDTH - 18f)
        val observationLines = lineas(na(hoja.observaciones), paint(8f, INK), CONTENT_WIDTH - 18f)
        agregarTextoExcedente(result, "TRABAJOS REALIZADOS (CONTINUACIÓN)", workLines.drop(MAX_WORK_LINES))
        agregarTextoExcedente(result, "OBSERVACIONES (CONTINUACIÓN)", observationLines.drop(MAX_OBSERVATION_LINES))
        datos.repuestos.drop(MAX_PARTS_FIRST_PAGE).chunked(MAX_PARTS_CONTINUATION).forEach {
            result += Pagina.Repuestos(it)
        }
        datos.jornadas.drop(MAX_JOURNEYS_FIRST_PAGE).chunked(MAX_JOURNEYS_CONTINUATION).forEach {
            result += Pagina.Jornadas(it)
        }
        datos.evidencias.chunked(EVIDENCES_PER_PAGE).forEach { result += Pagina.Evidencias(it) }
        return result
    }

    private fun agregarTextoExcedente(result: MutableList<Pagina>, title: String, lines: List<String>) {
        lines.chunked(MAX_TEXT_LINES_CONTINUATION).filter { it.isNotEmpty() }.forEach {
            result += Pagina.Texto(title, it)
        }
    }

    private fun dibujarPagina(canvas: Canvas, datos: HojaCampoCompleta, pagina: Pagina, current: Int, total: Int) {
        canvas.drawColor(Color.WHITE)
        when (pagina) {
            Pagina.Principal -> dibujarPrincipal(canvas, datos)
            is Pagina.Texto -> dibujarTextoContinuacion(canvas, datos, pagina)
            is Pagina.Repuestos -> dibujarRepuestosContinuacion(canvas, datos, pagina.items)
            is Pagina.Jornadas -> dibujarJornadasContinuacion(canvas, datos, pagina.items)
            is Pagina.Evidencias -> dibujarEvidencias(canvas, datos, pagina.items)
        }
        dibujarPie(canvas, current, total)
    }

    private fun dibujarPrincipal(canvas: Canvas, datos: HojaCampoCompleta) {
        dibujarEncabezado(canvas, datos, "HOJA DE CAMPO")
        dibujarPaneles(canvas, datos, 78f)
        dibujarTrabajoYMediciones(canvas, datos, 204f)
        dibujarObservaciones(canvas, datos, 473f)
        dibujarRepuestosPrimeraPagina(canvas, datos.repuestos, 529f)
        dibujarJornadasPrimeraPagina(canvas, datos.jornadas, 602f)
        dibujarLegalYFirmas(canvas, datos, 708f)
    }

    private fun dibujarEncabezado(canvas: Canvas, datos: HojaCampoCompleta, title: String) {
        dibujarMarca(canvas, 22f, 20f, 44f)
        texto(canvas, "CG Repuestos", 75f, 38f, 21f, INK, CONDENSED_BOLD)
        texto(canvas, "Repuestos y servicio técnico para generadores", 75f, 52f, 7.5f, INK)
        textoDerecha(canvas, "VENTA DE REPUESTOS, INSTALACIÓN, MANTENIMIENTO Y REPARACIÓN", RIGHT, 25f, 5.8f, MUTED)
        textoDerecha(canvas, "DE MOTORES DIÉSEL Y GRUPOS ELECTRÓGENOS", RIGHT, 34f, 5.8f, MUTED)
        textoDerecha(canvas, title, RIGHT, 54f, 16.5f, INK, SANS_BOLD)
        textoDerecha(canvas, datos.hoja.numeroHoja, RIGHT, 72f, 14.5f, RED, SANS)
        canvas.drawLine(LEFT, 76f, RIGHT, 76f, line(INK, 1.4f))
    }

    private fun dibujarMarca(canvas: Canvas, x: Float, y: Float, size: Float) {
        logo?.let {
            canvas.drawBitmap(it, null, RectF(x, y, x + size, y + size), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            return
        }
        val path = Path().apply {
            moveTo(x, y); lineTo(x + size, y); lineTo(x + size - 3f, y + size); lineTo(x + 3f, y + size); close()
        }
        canvas.drawPath(path, fill(RED))
        textoCentro(canvas, "CG", x + size / 2f, y + size / 2f + 6f, 16f, Color.WHITE, SANS_BOLD)
    }

    private fun dibujarPaneles(canvas: Canvas, datos: HojaCampoCompleta, top: Float) {
        val col = CONTENT_WIDTH / 3f
        val bottom = 197f
        borde(canvas, LEFT, top, RIGHT, bottom, LINE)
        canvas.drawLine(LEFT + col, top, LEFT + col, bottom, line(LINE, 1f))
        canvas.drawLine(LEFT + col * 2f, top, LEFT + col * 2f, bottom, line(LINE, 1f))
        val h = datos.hoja
        val c = datos.cliente
        val x1 = LEFT + 7f; val x2 = LEFT + col + 7f; val x3 = LEFT + col * 2f + 7f
        val phone = h.telefono.ifBlank { c.telefono }
        val direction = h.direccion.ifBlank { c.direccion }
        campo(canvas, "CLIENTE", c.nombre, x1, 92f, col - 14f)
        campo(canvas, "DIRECCIÓN", direction, x1, 109f, col - 14f)
        campoDoble(canvas, "TELF.", phone, "O.T.", h.ordenTrabajo, x1, 126f, col - 14f)
        campo(canvas, "TÉCNICOS", h.tecnicos, x1, 143f, col - 14f)
        campo(canvas, "FECHA", fecha(h.fecha), x1, 160f, col - 14f)
        campo(canvas, "ESTADO", h.estado, x1, 177f, col - 14f)

        tituloCaja(canvas, "ALTERNADOR", x2 + (col - 14f) / 2f, 91f)
        campo(canvas, "MARCA", h.alternadorMarca, x2, 106f, col - 14f)
        campo(canvas, "MODELO", h.alternadorModelo, x2, 121f, col - 14f)
        campo(canvas, "SERIE", h.alternadorSerie, x2, 136f, col - 14f)
        campoDoble(canvas, "R.P.M.", h.rpm, "KVA", h.kva, x2, 151f, col - 14f)
        campoDoble(canvas, "VOLT", h.volt, "KW", h.kw, x2, 166f, col - 14f)
        campoDoble(canvas, "AMP.", h.amp, "HZ", h.hz, x2, 181f, col - 14f)

        tituloCaja(canvas, "MOTOR", x3 + (col - 14f) / 2f, 91f)
        campo(canvas, "MARCA", h.motorMarca, x3, 108f, col - 14f)
        campo(canvas, "MODELO", h.motorModelo, x3, 124f, col - 14f)
        campo(canvas, "SERIE", h.motorSerie, x3, 140f, col - 14f)
        campo(canvas, "HORÓMETRO", h.horometro, x3, 156f, col - 14f)
        val tablero = h.tipoTablero.ifBlank { "AUT" }
        texto(canvas, "TABLERO:", x3, 176f, 6.8f, INK, SANS_BOLD)
        etiquetaEstado(canvas, "AUT", x3 + 50f, 168f, tablero.equals("AUT", true))
        etiquetaEstado(canvas, "MAN", x3 + 83f, 168f, tablero.equals("MAN", true))
    }

    private fun dibujarTrabajoYMediciones(canvas: Canvas, datos: HojaCampoCompleta, top: Float) {
        val bottom = 466f; val split = LEFT + CONTENT_WIDTH / 2f
        borde(canvas, LEFT, top, RIGHT, bottom, LINE)
        canvas.drawLine(split, top, split, bottom, line(LINE, 1f))
        texto(canvas, "TRABAJOS REALIZADOS", LEFT + 8f, top + 13f, 8f, INK, SANS_BOLD)
        textoCentro(canvas, "MEDICIONES / GENERADOR", split + CONTENT_WIDTH / 4f, top + 13f, 8f, INK, SANS_BOLD)
        canvas.drawLine(LEFT, top + 18f, RIGHT, top + 18f, line(LINE, 0.7f))
        val work = lineas(na(datos.hoja.trabajosRealizados), paint(8.2f, INK), WORK_WIDTH - 18f).take(MAX_WORK_LINES)
        dibujarLineasEscritura(canvas, LEFT + 9f, top + 34f, WORK_WIDTH - 18f, 10, 20f, work)
        dibujarMediciones(canvas, datos, split + 8f, top + 31f)
    }

    private fun dibujarMediciones(canvas: Canvas, datos: HojaCampoCompleta, x: Float, y: Float) {
        val m = datos.mediciones
        val filas = listOf(
            ("L1-L2" to valorUnidad(m?.l1l2, "V")) to ("AMP L1" to na(m?.ampL1)),
            ("L2-L3" to valorUnidad(m?.l2l3, "V")) to ("AMP L2" to na(m?.ampL2)),
            ("L3-L1" to valorUnidad(m?.l3l1, "V")) to ("AMP L3" to na(m?.ampL3)),
            ("LN" to valorUnidad(m?.ln, "V")) to ("" to ""),
            ("Hz vacío" to na(m?.hzVacio)) to ("Hz con carga" to na(m?.hzCarga)),
            ("RPM vacío" to na(m?.rpmVacio)) to ("RPM con carga" to na(m?.rpmCarga)),
            ("Presión de aceite" to na(m?.presionAceite)) to ("T° motor" to na(m?.temperaturaMotor)),
            ("Carga alternador" to na(m?.cargaAlternador)) to ("Voltaje batería" to na(m?.voltajeBateria)),
            ("Nivel oil (%)" to na(m?.nivelAceite)) to ("Nivel refrig. (%)" to na(m?.nivelRefrigerante)),
            ("Nivel combustible (%)" to na(m?.combustible)) to ("" to ""),
            ("Limpieza de generador" to estadoControl(m?.limpieza)) to ("" to ""),
            ("Llenado electrolitos" to estadoControl(m?.electrolitos)) to ("" to ""),
            ("Mantenedor de batería" to estadoControl(m?.mantenedorBateria)) to ("" to ""),
            ("Precalentador de block" to estadoControl(m?.precalentadorBlock)) to ("" to "")
        )
        filas.forEachIndexed { row, cells ->
            val baseline = y + 9f + row * 15f
            canvas.drawLine(x, baseline + 4f, RIGHT - 8f, baseline + 4f, line(LIGHT_LINE, 0.45f))
            texto(canvas, cells.first.first, x, baseline, 6.6f, INK, SANS_BOLD)
            textoDerecha(canvas, cells.first.second, x + 102f, baseline, 6.8f, INK)
            if (cells.second.first.isNotBlank()) {
                texto(canvas, cells.second.first, x + 118f, baseline, 6.6f, INK, SANS_BOLD)
                textoDerecha(canvas, cells.second.second, RIGHT - 8f, baseline, 6.8f, INK)
            }
        }
    }

    private fun dibujarObservaciones(canvas: Canvas, datos: HojaCampoCompleta, top: Float) {
        val bottom = 523f
        borde(canvas, LEFT, top, RIGHT, bottom, LINE)
        texto(canvas, "OBSERVACIONES", LEFT + 8f, top + 12f, 8f, INK, SANS_BOLD)
        val lines = lineas(na(datos.hoja.observaciones), paint(8f, INK), CONTENT_WIDTH - 18f).take(MAX_OBSERVATION_LINES)
        dibujarLineasEscritura(canvas, LEFT + 9f, top + 28f, CONTENT_WIDTH - 18f, 2, 13f, lines)
    }

    private fun dibujarRepuestosPrimeraPagina(canvas: Canvas, items: List<RepuestoUsadoEntity>, top: Float) {
        val bottom = 596f; val third = CONTENT_WIDTH / 3f
        borde(canvas, LEFT, top, RIGHT, bottom, LINE)
        texto(canvas, "REPUESTOS", LEFT + 8f, top + 12f, 8f, INK, SANS_BOLD)
        canvas.drawLine(LEFT + third, top + 16f, LEFT + third, bottom, line(LINE, 0.6f))
        canvas.drawLine(LEFT + third * 2f, top + 16f, LEFT + third * 2f, bottom, line(LINE, 0.6f))
        items.take(MAX_PARTS_FIRST_PAGE).forEachIndexed { index, item ->
            val column = index / 4; val row = index % 4
            val x = LEFT + column * third + 7f
            val y = top + 30f + row * 13f
            texto(canvas, "${cantidad(item.cantidad)} ${item.unidad} · ${item.nombre}", x, y, 6.8f, INK)
            canvas.drawLine(x, y + 3f, x + third - 12f, y + 3f, line(LIGHT_LINE, 0.45f))
        }
        if (items.isEmpty()) {
            texto(canvas, "N/A", LEFT + 7f, top + 30f, 6.8f, INK)
        }
    }

    private fun dibujarJornadasPrimeraPagina(canvas: Canvas, jornadas: List<JornadaTrabajoEntity>, top: Float) {
        val bottom = 702f
        borde(canvas, LEFT, top, RIGHT, bottom, LINE)
        textoCentro(canvas, "HORAS DE TRABAJO", LEFT + CONTENT_WIDTH / 2f, top + 12f, 8f, INK, SANS_BOLD)
        val columns = floatArrayOf(LEFT, 45f, 89f, 135f, 178f, 222f, 350f, RIGHT)
        val headers = listOf("#", "FECHA", "INICIO", "FIN", "HORAS", "TÉCNICOS", "OBSERVACIÓN")
        dibujarTablaHeader(canvas, columns, headers, top + 18f)
        if (jornadas.isEmpty()) {
            tablaFila(canvas, columns, top + 34f, List(headers.size) { "N/A" })
        } else jornadas.take(MAX_JOURNEYS_FIRST_PAGE).forEachIndexed { index, item ->
            dibujarJornadaFila(canvas, columns, item, index, top + 34f + index * 9.5f)
        }
        repeat(MAX_JOURNEYS_FIRST_PAGE - jornadas.size.coerceAtMost(MAX_JOURNEYS_FIRST_PAGE)) { index ->
            val y = top + 34f + (jornadas.size.coerceAtMost(MAX_JOURNEYS_FIRST_PAGE) + index) * 9.5f
            canvas.drawLine(LEFT, y + 4.5f, RIGHT, y + 4.5f, line(LIGHT_LINE, 0.45f))
        }
    }

    private fun dibujarLegalYFirmas(canvas: Canvas, datos: HojaCampoCompleta, top: Float) {
        val legal = "Por el presente reporte, certifico haber recibido a mi entera satisfacción el servicio y estoy de acuerdo con la descripción del trabajo, con la cantidad de horas utilizadas, repuestos según cotización adjunta y me comprometo a cancelar la factura al momento de su presentación."
        lineas(legal, paint(5.6f, INK), CONTENT_WIDTH).take(3).forEachIndexed { index, line ->
            texto(canvas, line, LEFT, top + index * 7f, 5.6f, INK)
        }
        val panelTop = top + 23f; val panelBottom = 816f
        borde(canvas, LEFT, panelTop, RIGHT, panelBottom, LINE)
        texto(canvas, "NOMBRE:", LEFT + 10f, panelTop + 13f, 6.5f, INK, SANS_BOLD)
        texto(canvas, na(datos.hoja.nombreClienteResponsable), LEFT + 48f, panelTop + 13f, 6.8f, INK)
        texto(canvas, "FECHA:", 355f, panelTop + 13f, 6.5f, INK, SANS_BOLD)
        texto(canvas, fecha(datos.hoja.fecha), 389f, panelTop + 13f, 6.8f, INK)
        texto(canvas, "RECIBIDO CONFORME", LEFT + 10f, panelTop + 29f, 7.3f, INK, SANS_BOLD)
        texto(canvas, "Firma del cliente por la recepción del trabajo", LEFT + 10f, panelTop + 39f, 6.2f, MUTED)
        borde(canvas, LEFT + 10f, panelTop + 45f, 335f, panelBottom - 10f, LIGHT_LINE)
        val firmaBitmap = bitmapFirmaCliente(datos.hoja.firmaClienteRuta)
        if (firmaBitmap == null) {
            textoCentro(canvas, "ESPACIO PARA FIRMA DEL CLIENTE", (LEFT + 10f + 335f) / 2f, panelBottom - 15f, 6.2f, MUTED, SANS_BOLD)
        } else {
            dibujarBitmapAjustado(canvas, firmaBitmap, Rect((LEFT + 15f).toInt(), (panelTop + 48f).toInt(), 330, (panelBottom - 20f).toInt()))
            firmaBitmap.recycle()
        }
        firma(canvas, 420f, panelBottom - 18f, "TÉCNICO")
    }

    private fun dibujarTextoContinuacion(canvas: Canvas, datos: HojaCampoCompleta, page: Pagina.Texto) {
        dibujarEncabezado(canvas, datos, "HOJA DE CAMPO · CONTINUACIÓN")
        cajaTitulo(canvas, page.title, 92f)
        dibujarLineasEscritura(canvas, LEFT + 10f, 123f, CONTENT_WIDTH - 20f, MAX_TEXT_LINES_CONTINUATION, 13f, page.lines)
    }

    private fun dibujarRepuestosContinuacion(canvas: Canvas, datos: HojaCampoCompleta, items: List<RepuestoUsadoEntity>) {
        dibujarEncabezado(canvas, datos, "HOJA DE CAMPO · REPUESTOS")
        cajaTitulo(canvas, "REPUESTOS UTILIZADOS", 92f)
        val columns = floatArrayOf(LEFT, 50f, 300f, 365f, 435f, RIGHT)
        dibujarTablaHeader(canvas, columns, listOf("#", "DESCRIPCIÓN", "CANT.", "UNIDAD", "CÓDIGO"), 116f)
        items.forEachIndexed { index, item ->
            val y = 137f + index * 21f
            tablaFila(canvas, columns, y, listOf((index + 1).toString(), item.nombre, cantidad(item.cantidad), item.unidad, item.codigo))
        }
    }

    private fun dibujarJornadasContinuacion(canvas: Canvas, datos: HojaCampoCompleta, items: List<JornadaTrabajoEntity>) {
        dibujarEncabezado(canvas, datos, "HOJA DE CAMPO · JORNADAS")
        cajaTitulo(canvas, "HORAS DE TRABAJO", 92f)
        val columns = floatArrayOf(LEFT, 43f, 100f, 155f, 210f, 260f, 385f, RIGHT)
        dibujarTablaHeader(canvas, columns, listOf("#", "FECHA", "INICIO", "FIN", "HORAS", "TÉCNICOS", "OBSERVACIÓN"), 116f)
        items.forEachIndexed { index, item -> dibujarJornadaFila(canvas, columns, item, index, 137f + index * 21f) }
    }

    private fun dibujarEvidencias(canvas: Canvas, datos: HojaCampoCompleta, evidencias: List<EvidenciaEntity>) {
        dibujarEncabezado(canvas, datos, "HOJA DE CAMPO · EVIDENCIAS")
        cajaTitulo(canvas, "EVIDENCIAS FOTOGRÁFICAS", 92f)
        evidencias.forEachIndexed { index, evidencia ->
            val column = index % 2; val row = index / 2
            val left = LEFT + column * 271f; val top = 117f + row * 320f
            borde(canvas, left, top, left + 252f, top + 300f, LINE)
            val imageRect = Rect(left.toInt() + 7, top.toInt() + 7, left.toInt() + 245, top.toInt() + 235)
            val bitmap = bitmapEvidencia(evidencia)
            if (bitmap != null) {
                dibujarBitmapAjustado(canvas, bitmap, imageRect)
                bitmap.recycle()
            } else {
                textoCentro(canvas, "IMAGEN NO DISPONIBLE", left + 126f, top + 122f, 8f, MUTED, SANS_BOLD)
            }
            texto(canvas, evidencia.tipoEvidencia.replace('_', ' '), left + 8f, top + 252f, 7.2f, RED, SANS_BOLD)
            val description = evidencia.descripcion.ifBlank { evidencia.nombreArchivo }
            lineas(description, paint(7.2f, INK), 236f).take(3).forEachIndexed { line, value ->
                texto(canvas, value, left + 8f, top + 266f + line * 10f, 7.2f, INK)
            }
            texto(canvas, fechaHora(evidencia.fechaHoraCaptura), left + 8f, top + 292f, 6.3f, MUTED)
        }
    }

    private fun bitmapEvidencia(evidencia: EvidenciaEntity): Bitmap? {
        val file = File(context.filesDir, evidencia.rutaInterna)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val sample = calcularMuestra(bounds.outWidth, bounds.outHeight, 520, 460)
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun bitmapFirmaCliente(rutaInterna: String?): Bitmap? {
        if (rutaInterna.isNullOrBlank()) return null
        val archivo = File(context.filesDir, rutaInterna)
        val carpeta = File(context.filesDir, "firmas_hoja_campo")
        if (!archivo.isFile || !runCatching { archivo.canonicalPath.startsWith(carpeta.canonicalPath + File.separator) }.getOrDefault(false)) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(archivo.path, bounds)
        val sample = calcularMuestra(bounds.outWidth, bounds.outHeight, 640, 160)
        return BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun dibujarBitmapAjustado(canvas: Canvas, bitmap: Bitmap, target: Rect) {
        val scale = minOf(target.width().toFloat() / bitmap.width, target.height().toFloat() / bitmap.height)
        val width = (bitmap.width * scale).toInt(); val height = (bitmap.height * scale).toInt()
        val left = target.left + (target.width() - width) / 2; val top = target.top + (target.height() - height) / 2
        canvas.drawBitmap(bitmap, null, Rect(left, top, left + width, top + height), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun dibujarPie(canvas: Canvas, current: Int, total: Int) {
        textoCentro(canvas, "CG REPUESTOS · QUITO, ECUADOR · 0960 809 770 / 0968 601 925 · alexandercantos161@gmail.com", LEFT + CONTENT_WIDTH / 2f, 833f, 5.8f, MUTED)
        textoDerecha(canvas, "Página $current de $total", RIGHT, 823f, 6.5f, MUTED, MONO)
    }

    private fun cajaTitulo(canvas: Canvas, title: String, top: Float) {
        borde(canvas, LEFT, top, RIGHT, top + 22f, LINE)
        rect(canvas, LEFT, top, RIGHT, top + 3f, RED)
        texto(canvas, title, LEFT + 9f, top + 16f, 9f, INK, SANS_BOLD)
    }

    private fun dibujarLineasEscritura(canvas: Canvas, x: Float, y: Float, width: Float, rows: Int, spacing: Float, values: List<String>) {
        repeat(rows) { index ->
            val baseline = y + index * spacing
            canvas.drawLine(x, baseline + 4f, x + width, baseline + 4f, line(LIGHT_LINE, 0.45f))
            values.getOrNull(index)?.let { texto(canvas, it, x, baseline, 8f, INK) }
        }
    }

    private fun dibujarTablaHeader(canvas: Canvas, columns: FloatArray, labels: List<String>, top: Float) {
        require(columns.size == labels.size + 1) { "La tabla tiene límites de columnas inválidos." }
        rect(canvas, columns.first(), top, columns.last(), top + 15f, SOFT)
        labels.forEachIndexed { index, label ->
            textoCentro(canvas, label, (columns[index] + columns[index + 1]) / 2f, top + 10f, 5.9f, INK, SANS_BOLD)
            canvas.drawLine(columns[index], top, columns[index], top + 15f, line(LINE, 0.55f))
        }
        canvas.drawLine(columns.last(), top, columns.last(), top + 15f, line(LINE, 0.55f))
        canvas.drawLine(columns.first(), top + 15f, columns.last(), top + 15f, line(LINE, 0.55f))
    }

    private fun dibujarJornadaFila(canvas: Canvas, columns: FloatArray, item: JornadaTrabajoEntity, index: Int, top: Float) {
        tablaFila(canvas, columns, top, listOf((index + 1).toString(), fecha(item.fecha), item.horaInicio, item.horaFin, duracion(item.minutosTotales), item.tecnicos, item.observacion))
    }

    private fun tablaFila(canvas: Canvas, columns: FloatArray, top: Float, values: List<String>) {
        require(columns.size == values.size + 1) { "La fila tiene límites de columnas inválidos." }
        val height = 18f
        values.forEachIndexed { index, value ->
            val center = (columns[index] + columns[index + 1]) / 2f
            textoCentro(canvas, ajustar(value, paint(6.3f, INK), columns[index + 1] - columns[index] - 4f), center, top + 11f, 6.3f, INK)
            canvas.drawLine(columns[index], top, columns[index], top + height, line(LINE, 0.45f))
        }
        canvas.drawLine(columns.last(), top, columns.last(), top + height, line(LINE, 0.45f))
        canvas.drawLine(columns.first(), top + height, columns.last(), top + height, line(LINE, 0.45f))
    }

    private fun campo(canvas: Canvas, label: String, value: String, x: Float, baseline: Float, width: Float) {
        val labelWidth = 42f
        texto(canvas, "$label:", x, baseline, 6.3f, INK, SANS_BOLD)
        texto(canvas, ajustar(na(value), paint(6.8f, INK), width - labelWidth), x + labelWidth, baseline, 6.8f, INK)
        canvas.drawLine(x + labelWidth, baseline + 3f, x + width, baseline + 3f, line(LIGHT_LINE, 0.45f))
    }

    private fun campoDoble(canvas: Canvas, label1: String, value1: String, label2: String, value2: String, x: Float, baseline: Float, width: Float) {
        val half = width / 2f
        campo(canvas, label1, value1, x, baseline, half - 3f)
        campo(canvas, label2, value2, x + half + 3f, baseline, half - 3f)
    }

    private fun tituloCaja(canvas: Canvas, title: String, x: Float, baseline: Float) = textoCentro(canvas, title, x, baseline, 8f, INK, SANS_BOLD)

    private fun etiquetaEstado(canvas: Canvas, value: String, x: Float, y: Float, selected: Boolean) {
        val width = 27f
        if (selected) rect(canvas, x, y, x + width, y + 13f, RED) else borde(canvas, x, y, x + width, y + 13f, MUTED)
        textoCentro(canvas, value, x + width / 2f, y + 9f, 6.2f, if (selected) Color.WHITE else MUTED, SANS_BOLD)
    }

    private fun firma(canvas: Canvas, center: Float, baseline: Float, label: String) {
        canvas.drawLine(center - 60f, baseline - 10f, center + 60f, baseline - 10f, line(MUTED, 0.5f))
        textoCentro(canvas, label, center, baseline, 6.3f, INK, SANS_BOLD)
    }

    private fun estadoControl(value: String?): String = when (value) {
        "CORRECTO" -> "Correcto"
        "REQUIERE_ATENCION" -> "Requiere atención"
        "NO_APLICA" -> "N/A"
        else -> "N/A"
    }

    private fun na(value: String?) = value?.trim().takeUnless { it.isNullOrEmpty() } ?: "N/A"
    private fun valorUnidad(value: String?, unit: String) = value?.takeIf { it.isNotBlank() }?.let { "$it $unit" } ?: "N/A"
    private fun cantidad(value: Double) = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.2f", value)
    private fun duracion(minutes: Long): String = "${minutes / 60}h ${minutes % 60}m"
    private fun fecha(value: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(value))
    private fun fechaHora(value: Long) = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(value))

    private fun calcularMuestra(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var sample = 1; var currentWidth = width; var currentHeight = height
        while (currentWidth / 2 >= targetWidth && currentHeight / 2 >= targetHeight) { sample *= 2; currentWidth /= 2; currentHeight /= 2 }
        return sample
    }

    private fun lineas(text: String, paint: Paint, width: Float): List<String> {
        if (text.isBlank()) return emptyList()
        return text.replace("\r\n", "\n").replace('\r', '\n').split('\n').flatMap { paragraph ->
            if (paragraph.isBlank()) listOf("") else envolver(paragraph.trim(), paint, width)
        }
    }

    private fun envolver(text: String, paint: Paint, width: Float): List<String> {
        val result = mutableListOf<String>(); var remaining = text
        while (remaining.isNotEmpty()) {
            var count = paint.breakText(remaining, true, width, null).coerceAtLeast(1)
            if (count < remaining.length) remaining.take(count).lastIndexOfAny(charArrayOf(' ', '-', '/')).takeIf { it > 0 }?.let { count = it + 1 }
            result += remaining.take(count).trimEnd(); remaining = remaining.drop(count).trimStart()
        }
        return result
    }

    private fun ajustar(value: String, paint: Paint, width: Float): String {
        if (paint.measureText(value) <= width) return value
        var result = value
        while (result.isNotEmpty() && paint.measureText("$result…") > width) result = result.dropLast(1)
        return "$result…"
    }

    private fun texto(canvas: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, typeface: Typeface = SANS) = canvas.drawText(value, x, y, paint(size, color, typeface))
    private fun textoDerecha(canvas: Canvas, value: String, right: Float, y: Float, size: Float, color: Int, typeface: Typeface = SANS) { val p = paint(size, color, typeface); canvas.drawText(value, right - p.measureText(value), y, p) }
    private fun textoCentro(canvas: Canvas, value: String, center: Float, y: Float, size: Float, color: Int, typeface: Typeface = SANS) { val p = paint(size, color, typeface); canvas.drawText(value, center - p.measureText(value) / 2f, y, p) }
    private fun paint(size: Float, color: Int, typeface: Typeface = SANS) = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size; this.color = color; this.typeface = typeface }
    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    private fun line(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; strokeWidth = width; style = Paint.Style.STROKE }
    private fun rect(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) = canvas.drawRect(left, top, right, bottom, fill(color))
    private fun borde(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) = canvas.drawRect(left, top, right, bottom, line(color, 0.8f))

    private sealed interface Pagina {
        data object Principal : Pagina
        data class Texto(val title: String, val lines: List<String>) : Pagina
        data class Repuestos(val items: List<RepuestoUsadoEntity>) : Pagina
        data class Jornadas(val items: List<JornadaTrabajoEntity>) : Pagina
        data class Evidencias(val items: List<EvidenciaEntity>) : Pagina
    }

    private companion object {
        const val PAGE_WIDTH = 595; const val PAGE_HEIGHT = 842
        const val LEFT = 20f; const val RIGHT = 575f; const val CONTENT_WIDTH = RIGHT - LEFT
        const val WORK_WIDTH = CONTENT_WIDTH / 2f
        const val MAX_WORK_LINES = 10; const val MAX_OBSERVATION_LINES = 2
        const val MAX_TEXT_LINES_CONTINUATION = 48
        const val MAX_PARTS_FIRST_PAGE = 12; const val MAX_PARTS_CONTINUATION = 25
        const val MAX_JOURNEYS_FIRST_PAGE = 7; const val MAX_JOURNEYS_CONTINUATION = 26
        const val EVIDENCES_PER_PAGE = 4
        val RED = Color.rgb(224, 32, 32); val INK = Color.rgb(31, 42, 50); val LINE = Color.rgb(86, 99, 108)
        val LIGHT_LINE = Color.rgb(196, 201, 204); val MUTED = Color.rgb(96, 107, 114); val SOFT = Color.rgb(238, 242, 244)
        val SANS = Typeface.create("sans-serif", Typeface.NORMAL); val SANS_BOLD = Typeface.create("sans-serif", Typeface.BOLD)
        val CONDENSED_BOLD = Typeface.create("sans-serif-condensed", Typeface.BOLD); val MONO = Typeface.create("monospace", Typeface.NORMAL)
    }
}
