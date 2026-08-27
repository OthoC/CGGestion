package com.example.cggestion.util.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.cggestion.R
import com.example.cggestion.data.aDolares
import com.example.cggestion.data.local.entity.CotizacionCompleta
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

sealed interface PdfResultado {
    data class Exito(val archivo: File) : PdfResultado
    data class Error(val mensaje: String) : PdfResultado
}

class CotizacionPdfGenerator(private val context: Context) {
    private val logo: Bitmap? by lazy {
        runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.logo_cg) }.getOrNull()
    }

    fun archivoPara(numero: String): File = File(
        File(context.filesDir, "cotizaciones").apply { mkdirs() },
        PdfFileName.paraCotizacion(numero)
    )

    fun generar(datos: CotizacionCompleta): PdfResultado = try {
        require(datos.items.isNotEmpty()) { "La cotización no tiene productos." }
        val archivo = archivoPara(datos.cotizacion.numeroCotizacion)
        val temporal = File(archivo.parentFile, "${archivo.name}.tmp")
        val filas = datos.items.mapIndexed { indice, item ->
            Fila(
                numero = indice + 1,
                categoria = item.categoriaProducto,
                nombre = item.nombreProducto,
                precio = dinero(item.precioUnitarioCentavos),
                cantidad = item.cantidad.toString(),
                descuento = "${porcentaje(item.descuentoPorcentaje)}%",
                total = dinero(item.totalLineaCentavos)
            )
        }
        val paginas = crearPaginas(datos, filas)
        val documento = PdfDocument()
        try {
            paginas.forEachIndexed { indice, contenido ->
                val pagina = documento.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, indice + 1).create()
                )
                dibujarPagina(pagina.canvas, datos, contenido, indice + 1, paginas.size)
                documento.finishPage(pagina)
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
        PdfResultado.Error(e.message ?: "No se pudo crear el PDF.")
    }

    private fun crearPaginas(datos: CotizacionCompleta, filas: List<Fila>): MutableList<Pagina> {
        val paginas = mutableListOf<Pagina>()
        var indice = 0
        var primera = true
        while (indice < filas.size) {
            val inicio = inicioFilas(datos, primera)
            var y = inicio
            val filasPagina = mutableListOf<Fila>()
            while (indice < filas.size) {
                val alto = alturaFila(filas[indice])
                if (filasPagina.isNotEmpty() && y + alto > CONTENT_BOTTOM) break
                filasPagina += filas[indice++]
                y += alto
            }
            paginas += Pagina(filas = filasPagina, primera = primera)
            primera = false
        }

        val lineasObservacion = lineas(
            datos.cotizacion.observaciones.trim(),
            paintTexto(SIZE_BODY, TEXT),
            NOTES_WIDTH - PANEL_PADDING * 2
        )
        val ultima = paginas.last()
        val finFilas = inicioFilas(datos, ultima.primera) + ultima.filas.sumOf { alturaFila(it).toInt() }
        val altoResumen = altoResumen(datos, lineasObservacion.size)
        if (finFilas + SECTION_GAP + altoResumen <= CONTENT_BOTTOM) {
            paginas[paginas.lastIndex] = ultima.copy(
                mostrarResumen = true,
                observaciones = lineasObservacion
            )
            return paginas
        }

        val maxLineasIntermedias = 56
        var pendientes = lineasObservacion
        while (pendientes.size > MAX_NOTES_FINAL_PAGE) {
            val cantidad = minOf(maxLineasIntermedias, pendientes.size - MAX_NOTES_FINAL_PAGE)
            paginas += Pagina(observaciones = pendientes.take(cantidad), soloObservaciones = true)
            pendientes = pendientes.drop(cantidad)
        }
        paginas += Pagina(mostrarResumen = true, observaciones = pendientes)
        return paginas
    }

    private fun dibujarPagina(
        canvas: Canvas,
        datos: CotizacionCompleta,
        pagina: Pagina,
        numeroPagina: Int,
        totalPaginas: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val inicioContenido = dibujarEncabezado(canvas, datos, pagina.primera)
        when {
            pagina.soloObservaciones -> dibujarObservacionesExtendidas(canvas, pagina.observaciones, inicioContenido)
            pagina.filas.isNotEmpty() -> {
                var y = dibujarEncabezadoTabla(canvas, inicioContenido)
                pagina.filas.forEachIndexed { indice, fila ->
                    val posicionGlobal = fila.numero - 1
                    y = dibujarFila(canvas, fila, y, posicionGlobal % 2 != 0)
                }
                if (pagina.mostrarResumen) {
                    dibujarResumen(canvas, datos, y + SECTION_GAP, pagina.observaciones)
                }
            }
            pagina.mostrarResumen -> dibujarResumen(canvas, datos, inicioContenido, pagina.observaciones)
        }
        dibujarPie(canvas, numeroPagina, totalPaginas)
    }

    private fun dibujarEncabezado(canvas: Canvas, datos: CotizacionCompleta, mostrarMetadatos: Boolean): Float {
        val cotizacion = datos.cotizacion
        dibujarMarca(canvas)
        texto(canvas, "CG REPUESTOS", 86f, 50f, 19f, TEXT, TYPE_CONDENSED_BOLD)
        texto(canvas, "Quito, Ecuador  |  0960 809 770 / 0968 601 925", 86f, 65f, 7.5f, MUTED)
        texto(canvas, "alexandercantos161@gmail.com", 86f, 77f, 7.5f, MUTED)

        textoDerecha(canvas, "COTIZACIÓN", RIGHT, 48f, 18f, RED, TYPE_CONDENSED_BOLD)
        textoDerecha(canvas, cotizacion.numeroCotizacion, RIGHT, 65f, 9f, TEXT, TYPE_MONO)
        textoDerecha(canvas, "Fecha: ${fecha(cotizacion.fechaCreacion)}", RIGHT, 78f, 7.5f, MUTED)
        rectangulo(canvas, LEFT, 91f, RIGHT, 94f, RED)

        if (!mostrarMetadatos) return 108f
        return dibujarMetadatos(canvas, datos, 105f) + 13f
    }

    private fun dibujarMarca(canvas: Canvas) {
        val bitmap = logo
        if (bitmap != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, null, RectF(36f, 29f, 78f, 71f), paint)
            return
        }
        val path = Path().apply {
            moveTo(36f, 29f)
            lineTo(76f, 29f)
            lineTo(76f, 61f)
            lineTo(56f, 71f)
            lineTo(36f, 61f)
            close()
        }
        canvas.drawPath(path, paintRelleno(RED))
        textoCentrado(canvas, "CG", 56f, 55f, 15f, Color.WHITE, TYPE_CONDENSED_BOLD)
    }

    private fun dibujarMetadatos(canvas: Canvas, datos: CotizacionCompleta, top: Float): Float {
        val gap = 8f
        val panelWidth = (RIGHT - LEFT - gap) / 2f
        val leftFields = camposCliente(datos)
        val rightFields = camposCotizacion(datos)
        val leftHeight = altoPanel(leftFields, panelWidth)
        val rightHeight = altoPanel(rightFields, panelWidth)
        val height = max(leftHeight, rightHeight)
        dibujarPanelDatos(canvas, LEFT, top, panelWidth, height, "DATOS DEL CLIENTE", leftFields)
        dibujarPanelDatos(canvas, LEFT + panelWidth + gap, top, panelWidth, height, "DATOS DE LA COTIZACIÓN", rightFields)
        return top + height
    }

    private fun camposCliente(datos: CotizacionCompleta): List<FilaCampos> {
        val cliente = datos.cliente
        return buildList {
            add(FilaCampos(Campo("NOMBRE / EMPRESA", cliente.nombre)))
            add(FilaCampos(Campo("RUC / CI", cliente.rucCedula), Campo("TELÉFONO", cliente.telefono)))
            add(FilaCampos(Campo("DIRECCIÓN", cliente.direccion)))
            cliente.correoElectronico?.takeIf(String::isNotBlank)?.let {
                add(FilaCampos(Campo("CORREO", it)))
            }
        }
    }

    private fun camposCotizacion(datos: CotizacionCompleta): List<FilaCampos> {
        val cotizacion = datos.cotizacion
        return listOf(
            FilaCampos(Campo("N° COTIZACIÓN", cotizacion.numeroCotizacion), Campo("FECHA", fecha(cotizacion.fechaCreacion))),
            FilaCampos(Campo("VÁLIDA HASTA", fecha(cotizacion.fechaValidez)), Campo("ESTADO", cotizacion.estado)),
            FilaCampos(
                Campo("CONDICIÓN DE PAGO", cotizacion.condicionPago.ifBlank { "No especificada" }),
                Campo("VENDEDOR", cotizacion.vendedor.ifBlank { "No especificado" })
            )
        )
    }

    private fun altoPanel(filas: List<FilaCampos>, width: Float): Float {
        val innerWidth = width - PANEL_PADDING * 2
        return 29f + filas.sumOf { fila ->
            val fieldWidth = if (fila.segundo == null) innerWidth else (innerWidth - FIELD_GAP) / 2f
            altoFilaCampos(fila, fieldWidth).toDouble()
        }.toFloat() + 6f
    }

    private fun altoFilaCampos(fila: FilaCampos, fieldWidth: Float): Float {
        val firstLines = lineas(fila.primero.valor.ifBlank { "-" }, paintTexto(SIZE_META_VALUE, TEXT), fieldWidth).size
        val secondLines = fila.segundo?.let {
            lineas(it.valor.ifBlank { "-" }, paintTexto(SIZE_META_VALUE, TEXT), fieldWidth).size
        } ?: 0
        return 13f + max(firstLines, secondLines) * META_LINE_HEIGHT + 5f
    }

    private fun dibujarPanelDatos(
        canvas: Canvas,
        x: Float,
        top: Float,
        width: Float,
        height: Float,
        titulo: String,
        filas: List<FilaCampos>
    ) {
        borde(canvas, x, top, x + width, top + height, BORDER)
        rectangulo(canvas, x, top, x + width, top + 2f, RED)
        texto(canvas, titulo, x + PANEL_PADDING, top + 17f, 9f, TEXT, TYPE_CONDENSED_BOLD)
        val innerWidth = width - PANEL_PADDING * 2
        var y = top + 28f
        filas.forEach { fila ->
            val fieldWidth = if (fila.segundo == null) innerWidth else (innerWidth - FIELD_GAP) / 2f
            val rowHeight = altoFilaCampos(fila, fieldWidth)
            dibujarCampo(canvas, fila.primero, x + PANEL_PADDING, y, fieldWidth)
            fila.segundo?.let {
                dibujarCampo(canvas, it, x + PANEL_PADDING + fieldWidth + FIELD_GAP, y, fieldWidth)
            }
            y += rowHeight
        }
    }

    private fun dibujarCampo(canvas: Canvas, campo: Campo, x: Float, y: Float, width: Float) {
        texto(canvas, campo.etiqueta, x, y + 7f, SIZE_META_LABEL, LIGHT_MUTED, TYPE_SANS_BOLD)
        val paint = paintTexto(SIZE_META_VALUE, TEXT)
        val wrapped = lineas(campo.valor.ifBlank { "-" }, paint, width)
        wrapped.forEachIndexed { index, value ->
            canvas.drawText(value, x, y + 18f + index * META_LINE_HEIGHT, paint)
        }
        val lineY = y + 20f + wrapped.size * META_LINE_HEIGHT
        canvas.drawLine(x, lineY, x + width, lineY, paintLinea(BORDER, 0.5f))
    }

    private fun inicioFilas(datos: CotizacionCompleta, primera: Boolean): Float {
        if (!primera) return 130f
        val gap = 8f
        val panelWidth = (RIGHT - LEFT - gap) / 2f
        val panelHeight = max(altoPanel(camposCliente(datos), panelWidth), altoPanel(camposCotizacion(datos), panelWidth))
        return 105f + panelHeight + 13f + TABLE_HEADER_HEIGHT
    }

    private fun dibujarEncabezadoTabla(canvas: Canvas, top: Float): Float {
        val bottom = top + TABLE_HEADER_HEIGHT
        rectangulo(canvas, LEFT, top, RIGHT, bottom, RED)
        val baseline = top + 14f
        textoCentrado(canvas, "#", 47f, baseline, 7f, Color.WHITE, TYPE_SANS_BOLD)
        texto(canvas, "CAT.", 62f, baseline, 7f, Color.WHITE, TYPE_SANS_BOLD)
        texto(canvas, "PRODUCTO / DESCRIPCIÓN", 132f, baseline, 7f, Color.WHITE, TYPE_SANS_BOLD)
        textoDerecha(canvas, "P. UNIT.", 365f, baseline, 7f, Color.WHITE, TYPE_SANS_BOLD)
        textoCentrado(canvas, "CANT.", 389f, baseline, 7f, Color.WHITE, TYPE_SANS_BOLD)
        textoCentrado(canvas, "DESC.", 435f, baseline, 7f, Color.WHITE, TYPE_SANS_BOLD)
        textoDerecha(canvas, "TOTAL", 554f, baseline, 7f, Color.WHITE, TYPE_SANS_BOLD)
        return bottom
    }

    private fun dibujarFila(canvas: Canvas, fila: Fila, top: Float, alterna: Boolean): Float {
        val height = alturaFila(fila)
        if (alterna) rectangulo(canvas, LEFT, top, RIGHT, top + height, ROW_ALT)
        val baseline = top + 14f
        textoCentrado(canvas, fila.numero.toString(), 47f, baseline, 7f, LIGHT_MUTED, TYPE_MONO)

        val categoryPaint = paintTexto(6.5f, Color.WHITE, TYPE_SANS_BOLD)
        val categoryLines = lineas(fila.categoria.ifBlank { "SIN CATEGORÍA" }, categoryPaint, 60f)
        categoryLines.forEachIndexed { index, value ->
            val lineTop = top + 6f + index * 10f
            val badgeWidth = minOf(62f, categoryPaint.measureText(value) + 7f)
            rectangulo(canvas, 61f, lineTop, 61f + badgeWidth, lineTop + 9f, RED)
            canvas.drawText(value, 64f, lineTop + 7f, categoryPaint)
        }

        val productPaint = paintTexto(SIZE_BODY, TEXT, TYPE_SANS_BOLD)
        lineas(fila.nombre, productPaint, 160f).forEachIndexed { index, value ->
            canvas.drawText(value, 132f, baseline + index * ROW_LINE_HEIGHT, productPaint)
        }
        textoDerecha(canvas, fila.precio, 365f, baseline, 8f, TEXT, TYPE_MONO)
        textoCentrado(canvas, fila.cantidad, 389f, baseline, 8f, TEXT, TYPE_MONO)
        textoCentrado(canvas, fila.descuento, 435f, baseline, 7.5f, TEXT, TYPE_MONO)
        textoDerecha(canvas, fila.total, 554f, baseline, 8.2f, TEXT, TYPE_MONO_BOLD)
        canvas.drawLine(LEFT, top + height, RIGHT, top + height, paintLinea(BORDER, 0.5f))
        return top + height
    }

    private fun alturaFila(fila: Fila): Float {
        val productLines = lineas(fila.nombre, paintTexto(SIZE_BODY, TEXT, TYPE_SANS_BOLD), 160f).size
        val categoryLines = lineas(fila.categoria.ifBlank { "SIN CATEGORÍA" }, paintTexto(6.5f, Color.WHITE, TYPE_SANS_BOLD), 60f).size
        return max(MIN_ROW_HEIGHT, max(productLines, categoryLines) * ROW_LINE_HEIGHT + 10f)
    }

    private fun altoResumen(datos: CotizacionCompleta, cantidadLineas: Int): Float {
        val notesHeight = if (cantidadLineas == 0) 0f else 31f + cantidadLineas * NOTES_LINE_HEIGHT
        return max(notesHeight, altoTotales(datos))
    }

    private fun altoTotales(datos: CotizacionCompleta): Float {
        val cotizacion = datos.cotizacion
        val regularRows = 3 +
            (if (cotizacion.descuentoItemsCentavos != 0L) 1 else 0) +
            (if (cotizacion.descuentoGlobalCentavos != 0L) 1 else 0)
        return 23f + regularRows * 18f + 29f
    }

    private fun dibujarResumen(
        canvas: Canvas,
        datos: CotizacionCompleta,
        top: Float,
        observaciones: List<String>
    ) {
        val totalX = 347f
        val notesRight = totalX - 8f
        val height = altoResumen(datos, observaciones.size)
        if (observaciones.isNotEmpty()) {
            borde(canvas, LEFT, top, notesRight, top + height, BORDER)
            texto(canvas, "OBSERVACIONES / CONDICIONES", LEFT + 9f, top + 16f, 7f, LIGHT_MUTED, TYPE_SANS_BOLD)
            val paint = paintTexto(SIZE_BODY, MUTED)
            observaciones.forEachIndexed { index, line ->
                canvas.drawText(line, LEFT + 9f, top + 31f + index * NOTES_LINE_HEIGHT, paint)
            }
        }
        dibujarTotales(canvas, datos, totalX, top)
    }

    private fun dibujarObservacionesExtendidas(canvas: Canvas, observaciones: List<String>, top: Float) {
        val height = 31f + observaciones.size * NOTES_LINE_HEIGHT
        borde(canvas, LEFT, top, RIGHT, top + height, BORDER)
        rectangulo(canvas, LEFT, top, RIGHT, top + 2f, RED)
        texto(canvas, "OBSERVACIONES / CONDICIONES (CONTINUACIÓN)", LEFT + 9f, top + 18f, 8f, TEXT, TYPE_CONDENSED_BOLD)
        val paint = paintTexto(SIZE_BODY, MUTED)
        observaciones.forEachIndexed { index, line ->
            canvas.drawText(line, LEFT + 9f, top + 32f + index * NOTES_LINE_HEIGHT, paint)
        }
    }

    private fun dibujarTotales(canvas: Canvas, datos: CotizacionCompleta, x: Float, top: Float) {
        val cotizacion = datos.cotizacion
        val height = altoTotales(datos)
        borde(canvas, x, top, RIGHT, top + height, BORDER)
        rectangulo(canvas, x, top, RIGHT, top + 2f, RED)
        texto(canvas, "RESUMEN DE COTIZACIÓN", x + 8f, top + 16f, 9f, TEXT, TYPE_CONDENSED_BOLD)
        canvas.drawLine(x, top + 23f, RIGHT, top + 23f, paintLinea(BORDER, 0.5f))
        var y = top + 23f

        fun row(etiqueta: String, valor: String, color: Int = MUTED) {
            texto(canvas, etiqueta.uppercase(Locale.getDefault()), x + 8f, y + 12f, 7.2f, color, TYPE_SANS_BOLD)
            textoDerecha(canvas, valor, RIGHT - 8f, y + 12f, 8f, color.takeIf { it != MUTED } ?: TEXT, TYPE_MONO_BOLD)
            y += 18f
            canvas.drawLine(x, y, RIGHT, y, paintLinea(BORDER, 0.5f))
        }

        row("Subtotal bruto", dinero(cotizacion.subtotalBrutoCentavos))
        if (cotizacion.descuentoItemsCentavos != 0L) {
            row("Desc. por ítem", "-${dinero(cotizacion.descuentoItemsCentavos)}", AMBER)
        }
        if (cotizacion.descuentoGlobalCentavos != 0L) {
            row(
                "Desc. global (${porcentaje(cotizacion.descuentoGlobalPorcentaje)}%)",
                "-${dinero(cotizacion.descuentoGlobalCentavos)}",
                AMBER
            )
        }
        row("Base imponible", dinero(cotizacion.baseImponibleCentavos))
        row("IVA (${porcentaje(cotizacion.ivaPorcentaje)}%)", dinero(cotizacion.valorIvaCentavos), BLUE)

        rectangulo(canvas, x, y, RIGHT, y + 29f, RED)
        texto(canvas, "TOTAL", x + 8f, y + 19f, 12f, Color.WHITE, TYPE_CONDENSED_BOLD)
        textoDerecha(canvas, dinero(cotizacion.totalFinalCentavos), RIGHT - 8f, y + 19f, 13f, Color.WHITE, TYPE_MONO_BOLD)
    }

    private fun dibujarPie(canvas: Canvas, numeroPagina: Int, totalPaginas: Int) {
        canvas.drawLine(LEFT, 786f, RIGHT, 786f, paintLinea(BORDER, 0.6f))
        texto(canvas, "Cotización generada por CG Repuestos - Quito, Ecuador", LEFT, 799f, 6.8f, LIGHT_MUTED)
        texto(canvas, "Esta cotización no constituye una factura. Precios sujetos a cambio sin previo aviso.", LEFT, 811f, 6.8f, LIGHT_MUTED)
        canvas.drawLine(394f, 805f, 520f, 805f, paintLinea(MUTED, 0.5f))
        textoCentrado(canvas, "Firma y sello", 457f, 816f, 6.8f, MUTED)
        textoDerecha(canvas, "Página $numeroPagina de $totalPaginas", RIGHT, 829f, 7f, MUTED, TYPE_MONO)
    }

    private fun lineas(text: String, paint: Paint, width: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val paragraphs = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        return paragraphs.flatMap { paragraph ->
            if (paragraph.isBlank()) listOf("") else envolverParrafo(paragraph.trim(), paint, width)
        }
    }

    private fun envolverParrafo(text: String, paint: Paint, width: Float): List<String> {
        val result = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            var count = paint.breakText(remaining, true, width, null).coerceAtLeast(1)
            if (count < remaining.length) {
                val candidate = remaining.take(count)
                val breakAt = candidate.lastIndexOfAny(charArrayOf(' ', '-', '/'))
                if (breakAt > 0) count = breakAt + 1
            }
            result += remaining.take(count).trimEnd()
            remaining = remaining.drop(count).trimStart()
        }
        return result
    }

    private fun texto(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        typeface: Typeface = TYPE_SANS
    ) = canvas.drawText(text, x, y, paintTexto(size, color, typeface))

    private fun textoDerecha(
        canvas: Canvas,
        text: String,
        right: Float,
        y: Float,
        size: Float,
        color: Int,
        typeface: Typeface = TYPE_SANS
    ) {
        val paint = paintTexto(size, color, typeface)
        canvas.drawText(text, right - paint.measureText(text), y, paint)
    }

    private fun textoCentrado(
        canvas: Canvas,
        text: String,
        center: Float,
        y: Float,
        size: Float,
        color: Int,
        typeface: Typeface = TYPE_SANS
    ) {
        val paint = paintTexto(size, color, typeface)
        canvas.drawText(text, center - paint.measureText(text) / 2f, y, paint)
    }

    private fun paintTexto(size: Float, color: Int, typeface: Typeface = TYPE_SANS) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
        }

    private fun paintRelleno(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun paintLinea(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = width
        style = Paint.Style.STROKE
    }

    private fun rectangulo(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        canvas.drawRect(left, top, right, bottom, paintRelleno(color))
    }

    private fun borde(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, color: Int) {
        canvas.drawRect(left, top, right, bottom, paintLinea(color, 0.7f))
    }

    private fun dinero(centavos: Long) = String.format(Locale.US, "\$%.2f", centavos.aDolares())
    private fun porcentaje(valor: Double) = String.format(Locale.US, "%.2f", valor)
    private fun fecha(valor: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(valor))

    private data class Fila(
        val numero: Int,
        val categoria: String,
        val nombre: String,
        val precio: String,
        val cantidad: String,
        val descuento: String,
        val total: String
    )

    private data class Campo(val etiqueta: String, val valor: String)
    private data class FilaCampos(val primero: Campo, val segundo: Campo? = null)
    private data class Pagina(
        val filas: List<Fila> = emptyList(),
        val primera: Boolean = false,
        val mostrarResumen: Boolean = false,
        val observaciones: List<String> = emptyList(),
        val soloObservaciones: Boolean = false
    )

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val LEFT = 36f
        const val RIGHT = 559f
        const val CONTENT_BOTTOM = 772f
        const val TABLE_HEADER_HEIGHT = 22f
        const val SECTION_GAP = 10f
        const val PANEL_PADDING = 8f
        const val FIELD_GAP = 7f
        const val SIZE_BODY = 8f
        const val SIZE_META_LABEL = 6.2f
        const val SIZE_META_VALUE = 7.8f
        const val META_LINE_HEIGHT = 9.5f
        const val ROW_LINE_HEIGHT = 10f
        const val MIN_ROW_HEIGHT = 25f
        const val NOTES_WIDTH = 303f
        const val NOTES_LINE_HEIGHT = 10f
        const val MAX_NOTES_FINAL_PAGE = 60

        val RED: Int = Color.rgb(224, 32, 32)
        val TEXT: Int = Color.rgb(17, 17, 17)
        val MUTED: Int = Color.rgb(85, 85, 85)
        val LIGHT_MUTED: Int = Color.rgb(145, 145, 145)
        val BORDER: Int = Color.rgb(221, 221, 221)
        val ROW_ALT: Int = Color.rgb(250, 250, 250)
        val AMBER: Int = Color.rgb(200, 120, 0)
        val BLUE: Int = Color.rgb(21, 101, 192)

        val TYPE_SANS: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        val TYPE_SANS_BOLD: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val TYPE_CONDENSED_BOLD: Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        val TYPE_MONO: Typeface = Typeface.create("monospace", Typeface.NORMAL)
        val TYPE_MONO_BOLD: Typeface = Typeface.create("monospace", Typeface.BOLD)
    }
}
