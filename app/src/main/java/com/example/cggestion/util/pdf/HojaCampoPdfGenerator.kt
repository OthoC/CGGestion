package com.example.cggestion.util.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.cggestion.data.local.entity.HojaCampoCompleta
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HojaCampoPdfGenerator(private val context: Context) {
    fun archivoPara(numero: String): File = File(File(context.filesDir, "hojas_campo_pdf").apply { mkdirs() }, "Hoja_Campo_${numero.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf")
    fun generar(datos: HojaCampoCompleta): PdfResultado = try {
        val archivo = archivoPara(datos.hoja.numeroHoja); val temporal = File(archivo.parentFile, "${archivo.name}.tmp"); val documento = PdfDocument()
        try {
            var pagina = 1; var actual = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, pagina).create()); var y = encabezado(actual.canvas, datos, pagina)
            fun nuevaPagina() { documento.finishPage(actual); pagina++; actual = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, pagina).create()); y = encabezado(actual.canvas, datos, pagina) }
            fun linea(etiqueta: String, valor: String) { if (valor.isBlank()) return; if (y > 775) nuevaPagina(); texto(actual.canvas, "$etiqueta: $valor", 42f, y, 9f, negro()); y += 15 }
            fun seccion(titulo: String) { if (y > 750) nuevaPagina(); texto(actual.canvas, titulo, 42f, y, 11f, rojo(), true); y += 17 }
            val h = datos.hoja; val c = datos.cliente
            seccion("CLIENTE Y SERVICIO"); linea("Cliente", c.nombre); linea("RUC/Cédula", c.rucCedula); linea("Teléfono", h.telefono.ifBlank { c.telefono }); linea("Dirección", h.direccion.ifBlank { c.direccion }); linea("Técnicos", h.tecnicos); linea("Orden", h.ordenTrabajo)
            seccion("ALTERNADOR"); linea("Marca / modelo", "${h.alternadorMarca} ${h.alternadorModelo}".trim()); linea("Serie", h.alternadorSerie); linea("RPM / KVA", "${h.rpm} / ${h.kva}".trim(' ', '/')); linea("Volt / KW / Amp / Hz", listOf(h.volt, h.kw, h.amp, h.hz).filter { it.isNotBlank() }.joinToString(" / "))
            seccion("MOTOR"); linea("Marca / modelo", "${h.motorMarca} ${h.motorModelo}".trim()); linea("Serie", h.motorSerie); linea("Horómetro", h.horometro); linea("Tablero", h.tipoTablero)
            seccion("MEDICIONES"); val m = datos.mediciones; if (m != null) { linea("Voltajes L1-L2 / L2-L3 / L3-L1 / LN", listOf(m.l1l2,m.l2l3,m.l3l1,m.ln).filter { it.isNotBlank() }.joinToString(" / ")); linea("Amperajes L1 / L2 / L3", listOf(m.ampL1,m.ampL2,m.ampL3).filter { it.isNotBlank() }.joinToString(" / ")); linea("Hz vacío / carga", "${m.hzVacio} / ${m.hzCarga}".trim(' ', '/')); linea("RPM vacío / carga", "${m.rpmVacio} / ${m.rpmCarga}".trim(' ', '/')); linea("Temperatura / presión", "${m.temperaturaMotor} / ${m.presionAceite}".trim(' ', '/')); linea("Combustible", m.combustible); linea("Controles", "Limpieza ${m.limpieza} · Electrolitos ${m.electrolitos} · Mantenedor ${m.mantenedorBateria} · Precalentador ${m.precalentadorBlock}") }
            seccion("TRABAJOS Y REPUESTOS"); linea("Trabajos realizados", h.trabajosRealizados); datos.repuestos.forEach { linea("Repuesto", "${it.nombre} · ${it.cantidad} ${it.unidad}") }; datos.jornadas.forEach { linea("Jornada", "${fecha(it.fecha)} ${it.horaInicio}-${it.horaFin} · ${it.tecnicos}") }; linea("Observaciones", h.observaciones)
            if (datos.evidencias.isNotEmpty()) { seccion("EVIDENCIAS FOTOGRÁFICAS"); datos.evidencias.forEach { e -> if (y > 610) nuevaPagina(); linea("${e.tipoEvidencia}", e.descripcion.ifBlank { e.nombreArchivo }); val f = File(context.filesDir, e.rutaInterna); val bitmap = BitmapFactory.decodeFile(f.path); if (bitmap != null) { val escala = minOf(500f / bitmap.width, 220f / bitmap.height, 1f); val w=(bitmap.width*escala).toInt(); val alto=(bitmap.height*escala).toInt(); actual.canvas.drawBitmap(bitmap, null, android.graphics.Rect(42, y.toInt(), 42+w, y.toInt()+alto), Paint()); bitmap.recycle(); y += alto + 12 } else linea("Imagen", "No disponible") } }
            texto(actual.canvas, "Esta hoja no constituye una factura · Firma y sello: ____________________", 42f, 813f, 8f, gris()); documento.finishPage(actual)
            FileOutputStream(temporal).use { documento.writeTo(it) }
        } finally { documento.close() }
        if (archivo.exists()) archivo.delete(); if (!temporal.renameTo(archivo)) error("No se pudo finalizar el PDF."); PdfResultado.Exito(archivo)
    } catch (e: Exception) { PdfResultado.Error(e.message ?: "No se pudo generar el PDF de la hoja.") }
    private fun encabezado(c: Canvas, d: HojaCampoCompleta, pagina: Int): Float { c.drawColor(Color.WHITE); c.drawRect(36f,32f,559f,38f,rojo()); texto(c,"CG REPUESTOS",42f,66f,20f,rojo(),true); texto(c,"HOJA DE CAMPO",42f,86f,12f,negro(),true); textoDerecha(c,d.hoja.numeroHoja,553f,66f,14f,negro(),true); textoDerecha(c,"${fecha(d.hoja.fecha)} · ${d.hoja.estado}",553f,84f,8f,gris()); textoDerecha(c,"Página $pagina",553f,102f,8f,gris()); return 125f }
    private fun rojo()=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(224,32,32)}; private fun negro()=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(40,40,40)}; private fun gris()=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.rgb(105,105,105)}
    private fun texto(c:Canvas,t:String,x:Float,y:Float,s:Float,p:Paint,b:Boolean=false){p.textSize=s;p.typeface=if(b)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;c.drawText(t,x,y,p)}; private fun textoDerecha(c:Canvas,t:String,x:Float,y:Float,s:Float,p:Paint,b:Boolean=false){p.textSize=s;p.typeface=if(b)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;c.drawText(t,x-p.measureText(t),y,p)}; private fun fecha(v:Long)=SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(Date(v))
}
