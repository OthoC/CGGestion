package com.example.cggestion.util.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.cggestion.data.aDolares
import com.example.cggestion.data.local.entity.CotizacionCompleta
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

sealed interface PdfResultado { data class Exito(val archivo: File) : PdfResultado; data class Error(val mensaje: String) : PdfResultado }

class CotizacionPdfGenerator(private val context: Context) {
    fun archivoPara(numero: String): File = File(File(context.filesDir, "cotizaciones").apply { mkdirs() }, PdfFileName.paraCotizacion(numero))

    fun generar(datos: CotizacionCompleta): PdfResultado = try {
        require(datos.items.isNotEmpty()) { "La cotización no tiene productos." }
        val archivo = archivoPara(datos.cotizacion.numeroCotizacion)
        val temporal = File(archivo.parentFile, "${archivo.name}.tmp")
        val documento = PdfDocument()
        try {
            val filas = datos.items.mapIndexed { indice, item -> Fila(indice + 1, item.nombreProducto, item.categoriaProducto, item.cantidad.toString(), dinero(item.precioUnitarioCentavos), "${porcentaje(item.descuentoPorcentaje)}%", dinero(item.totalLineaCentavos)) }
            val paginas = distribuirFilas(filas)
            paginas.forEachIndexed { paginaIndice, paginaFilas ->
                val pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, paginaIndice + 1).create())
                dibujarPagina(pagina.canvas, datos, paginaFilas, paginaIndice + 1, paginas.size, paginaIndice == paginas.lastIndex)
                documento.finishPage(pagina)
            }
            FileOutputStream(temporal).use { documento.writeTo(it) }
        } finally { documento.close() }
        if (archivo.exists()) archivo.delete()
        if (!temporal.renameTo(archivo)) throw IllegalStateException("No se pudo finalizar el archivo PDF.")
        PdfResultado.Exito(archivo)
    } catch (e: Exception) { PdfResultado.Error(e.message ?: "No se pudo crear el PDF.") }

    private fun distribuirFilas(filas: List<Fila>): List<List<Fila>> {
        val resultado = mutableListOf<MutableList<Fila>>(); var actual = mutableListOf<Fila>(); var alto = 292
        filas.forEach { fila ->
            val altoFila = alturaFila(fila)
            if (alto + altoFila > 590 && actual.isNotEmpty()) { resultado += actual; actual = mutableListOf(); alto = 292 }
            actual += fila; alto += altoFila
        }
        if (actual.isNotEmpty()) resultado += actual
        return resultado
    }
    private fun dibujarPagina(c: Canvas, datos: CotizacionCompleta, filas: List<Fila>, numeroPagina: Int, totalPaginas: Int, esUltima: Boolean) {
        val negro = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 40, 40) }; val gris = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100, 100, 100) }; val rojo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(210, 35, 35) }
        c.drawColor(Color.WHITE); rojo.style = Paint.Style.FILL; c.drawRect(36f, 32f, 559f, 38f, rojo)
        texto(c, "CG REPUESTOS", 36f, 66f, 21f, rojo, true); texto(c, "COTIZACIÓN", 36f, 88f, 13f, negro, true)
        val cot = datos.cotizacion; val cliente = datos.cliente
        textoDerecha(c, cot.numeroCotizacion, 559f, 66f, 15f, negro, true); textoDerecha(c, "Fecha: ${fecha(cot.fechaCreacion)}", 559f, 86f, 9f, gris)
        texto(c, "Quito, Ecuador | 0960 809 770 / 0968 601 925", 36f, 108f, 8.5f, gris); texto(c, "alexandercantos161@gmail.com", 36f, 121f, 8.5f, gris)
        caja(c, 36f, 137f, 559f, 213f, Color.rgb(245,245,245)); texto(c, "CLIENTE", 46f, 154f, 9f, rojo, true)
        texto(c, cliente.nombre, 46f, 171f, 11f, negro, true); val datosCliente = listOfNotNull(cliente.rucCedula.takeIf { it.isNotBlank() }?.let { "RUC/Cédula: $it" }, cliente.telefono.takeIf { it.isNotBlank() }?.let { "Teléfono: $it" }, cliente.direccion.takeIf { it.isNotBlank() }?.let { "Dirección: $it" }, cliente.correoElectronico?.takeIf { it.isNotBlank() }?.let { "Correo: $it" })
        textoEnvuelto(c, datosCliente.joinToString("   •   "), 46f, 187f, 500f, 8.5f, gris)
        texto(c, "Estado: ${cot.estado}   |   Pago: ${cot.condicionPago.ifBlank { "No especificada" }}   |   Vendedor: ${cot.vendedor.ifBlank { "No especificado" }}", 36f, 231f, 8.5f, negro)
        var y = 249f; rojo.color = Color.rgb(210,35,35); c.drawRect(36f, y, 559f, y + 20, rojo); val blanco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        listOf("#" to 42f, "PRODUCTO / DESCRIPCIÓN" to 64f, "CATEGORÍA" to 255f, "CANT." to 330f, "P. UNIT." to 375f, "DESC." to 440f, "TOTAL" to 500f).forEach { (t,x) -> texto(c,t,x,y+14,7.5f,blanco,true) }; y += 20
        filas.forEach { fila -> val alto = alturaFila(fila).toFloat(); c.drawLine(36f,y,559f,y,gris); val base = y + 13; texto(c,fila.numero.toString(),42f,base,8f,negro); textoEnvuelto(c,fila.nombre,64f,base,180f,8f,negro); textoEnvuelto(c,fila.categoria,255f,base,66f,8f,negro); texto(c,fila.cantidad,333f,base,8f,negro); textoDerecha(c,fila.precio,430f,base,8f,negro); textoDerecha(c,fila.descuento,480f,base,8f,negro); textoDerecha(c,fila.total,553f,base,8f,negro); y += alto }
        if (esUltima) { y += 12; dibujarTotales(c, datos, y, negro, gris, rojo); val pieY = 748f; if (cot.observaciones.isNotBlank()) { texto(c,"OBSERVACIONES",36f,pieY,8f,rojo,true); textoEnvuelto(c,cot.observaciones,36f,pieY+14,500f,8f,gris) }; texto(c,"Esta cotización no constituye una factura",36f,790f,8f,gris); texto(c,"Precios sujetos a cambio sin previo aviso",36f,803f,8f,gris); texto(c,"Firma y sello: ______________________________",355f,803f,8f,gris) }
        textoDerecha(c,"Página $numeroPagina de $totalPaginas",559f,824f,8f,gris)
    }
    private fun dibujarTotales(c: Canvas, d: CotizacionCompleta, y: Float, negro: Paint, gris: Paint, rojo: Paint) { val x = 350f; val cot = d.cotizacion; var pos = y; fun linea(t:String,v:String){texto(c,t,x,pos,9f,gris);textoDerecha(c,v,559f,pos,9f,negro);pos+=15}; linea("Subtotal bruto",dinero(cot.subtotalBrutoCentavos));linea("Descuento por ítems","- ${dinero(cot.descuentoItemsCentavos)}");linea("Descuento global (${porcentaje(cot.descuentoGlobalPorcentaje)}%)","- ${dinero(cot.descuentoGlobalCentavos)}");linea("Base imponible",dinero(cot.baseImponibleCentavos));linea("IVA (${porcentaje(cot.ivaPorcentaje)}%)",dinero(cot.valorIvaCentavos));rojo.color=Color.rgb(210,35,35);c.drawRect(x,pos-4,559f,pos+18,rojo);val blanco=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE};texto(c,"TOTAL FINAL",x+8,pos+11,11f,blanco,true);textoDerecha(c,dinero(cot.totalFinalCentavos),551f,pos+11,11f,blanco,true) }
    private fun alturaFila(f: Fila): Int = max(lineas(f.nombre,180f,8f), lineas(f.categoria,66f,8f)) * 10 + 8
    private fun lineas(texto:String, ancho:Float, tam:Float):Int { val p=Paint().apply{textSize=tam}; var restante=texto;var n=0;while(restante.isNotBlank()){val c=p.breakText(restante,true,ancho,null);restante=restante.drop(c).trimStart();n++};return max(1,n) }
    private fun texto(c:Canvas,t:String,x:Float,y:Float,s:Float,p:Paint,b:Boolean=false){p.textSize=s;p.typeface=if(b)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;c.drawText(t,x,y,p)}
    private fun textoDerecha(c:Canvas,t:String,x:Float,y:Float,s:Float,p:Paint,b:Boolean=false){p.textSize=s;p.typeface=if(b)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;c.drawText(t,x-p.measureText(t),y,p)}
    private fun textoEnvuelto(c:Canvas,t:String,x:Float,y:Float,a:Float,s:Float,p:Paint){p.textSize=s;var r=t;var yy=y;while(r.isNotBlank()){val n=p.breakText(r,true,a,null);c.drawText(r.take(n),x,yy,p);r=r.drop(n).trimStart();yy+=s+2}}
    private fun caja(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int){Paint().also{it.color=color}.let{c.drawRect(l,t,r,b,it)}}
    private fun dinero(c:Long)=String.format(Locale.US,"\$%.2f",c.aDolares()); private fun porcentaje(v:Double)=String.format(Locale.US,"%.2f",v);private fun fecha(v:Long)=SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(Date(v))
    private data class Fila(val numero:Int,val nombre:String,val categoria:String,val cantidad:String,val precio:String,val descuento:String,val total:String)
}
