package com.example.cggestion

import com.example.cggestion.data.ItemCotizacion
import com.example.cggestion.data.HojaCampoValidaciones
import com.example.cggestion.data.Producto
import com.example.cggestion.data.calcularTotales
import com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity
import com.example.cggestion.util.pdf.PdfFileName
import com.example.cggestion.data.repository.InventarioRules
import com.example.cggestion.data.repository.ReporteOperativo
import com.example.cggestion.data.repository.siguienteConsecutivoHoja
import com.example.cggestion.data.local.entity.CotizacionEntity
import com.example.cggestion.data.local.entity.CotizacionResumen
import com.example.cggestion.data.local.entity.EstadoCotizacion
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.data.local.entity.HojaCampoEntity
import com.example.cggestion.data.local.entity.HojaCampoResumen
import com.example.cggestion.data.local.entity.ProductoEntity
import com.example.cggestion.viewmodel.PeriodoReporte
import com.example.cggestion.viewmodel.calcularMetricas
import com.example.cggestion.viewmodel.filtrarPorPeriodo
import org.junit.Assert.assertEquals
import org.junit.Test

class CotizacionCalculosTest {
    @Test
    fun calcula_totales_en_centavos_con_descuentos_e_iva() {
        val producto = Producto(id = 1, nombre = "Filtro", categoria = "Filtro", precio = 10.0)
        val item = ItemCotizacion(producto, cantidad = 2, precioUnitarioTexto = "10", descuentoTexto = "10")

        val resultado = calcularTotales(listOf(item), descuentoGlobal = 10.0, iva = 15.0)

        assertEquals(2_000L, resultado.subtotalBruto)
        assertEquals(200L, resultado.descuentoItems)
        assertEquals(180L, resultado.descuentoGlobal)
        assertEquals(1_620L, resultado.baseImponible)
        assertEquals(243L, resultado.valorIva)
        assertEquals(1_863L, resultado.totalFinal)
    }

    @Test
    fun genera_nombre_de_pdf_seguro_y_estable() {
        assertEquals("Cotizacion_CG-2026-0001.pdf", PdfFileName.paraCotizacion("CG-2026-0001"))
        assertEquals("Cotizacion_CG_2026_0001.pdf", PdfFileName.paraCotizacion("CG/2026:0001"))
    }

    @Test
    fun valida_cantidades_de_inventario_sin_aceptar_ceros_o_texto_invalido() {
        assertEquals(2.5, InventarioRules.cantidadDesdeTexto("2,5"))
        assertEquals(null, InventarioRules.cantidadDesdeTexto("0"))
        assertEquals(null, InventarioRules.cantidadDesdeTexto("-1"))
        assertEquals(null, InventarioRules.cantidadDesdeTexto("abc"))
        assertEquals(0.0, InventarioRules.cantidadNoNegativaDesdeTexto("0"))
        assertEquals(null, InventarioRules.cantidadNoNegativaDesdeTexto("-1"))
    }

    @Test
    fun valida_jornadas_y_porcentaje_de_combustible() {
        assertEquals(null, HojaCampoValidaciones.jornada("08:00", "09:30", "Alex"))
        assertEquals("La hora final debe ser posterior a la hora inicial.", HojaCampoValidaciones.jornada("09:00", "08:00", "Alex"))
        assertEquals("Indica el técnico de la jornada.", HojaCampoValidaciones.jornada("08:00", "09:00", ""))
        assertEquals(null, HojaCampoValidaciones.mediciones(MedicionesHojaCampoEntity(hojaCampoId = 1, l1l2 = "220", combustible = "100")))
        assertEquals(
            "El nivel de combustible debe estar entre 0 % y 100 %.",
            HojaCampoValidaciones.mediciones(MedicionesHojaCampoEntity(hojaCampoId = 1, combustible = "120"))
        )
    }

    @Test
    fun calcula_metricas_de_reportes_y_respeta_el_periodo() {
        val ahora = 1_000_000_000L
        val aprobada = CotizacionEntity(
            id = 1,
            numeroCotizacion = "CG-2026-0001",
            clienteId = 1,
            fechaCreacion = ahora - 1_000L,
            fechaValidez = ahora,
            subtotalBrutoCentavos = 10_000,
            descuentoItemsCentavos = 0,
            descuentoGlobalCentavos = 0,
            baseImponibleCentavos = 10_000,
            valorIvaCentavos = 1_500,
            totalFinalCentavos = 11_500,
            estado = EstadoCotizacion.APROBADA.name,
            fechaUltimaModificacion = ahora
        )
        val borradorAntiguo = aprobada.copy(
            id = 2,
            numeroCotizacion = "CG-2026-0002",
            fechaCreacion = ahora - 40L * 24 * 60 * 60 * 1_000,
            estado = EstadoCotizacion.BORRADOR.name
        )
        val hoja = HojaCampoEntity(
            id = 1,
            numeroHoja = "Q 0000100",
            fecha = ahora,
            clienteId = 1,
            estado = EstadoHoja.COMPLETADA.name,
            fechaCreacion = ahora,
            fechaModificacion = ahora
        )
        val reporte = ReporteOperativo(
            cotizaciones = listOf(CotizacionResumen(aprobada, "Cliente"), CotizacionResumen(borradorAntiguo, "Cliente")),
            hojas = listOf(HojaCampoResumen(hoja, "Cliente")),
            stockBajo = listOf(ProductoEntity(id = 1, nombre = "Filtro", categoria = "Filtros", stockActual = 2.0, stockMinimo = 3.0)),
            productos = listOf(
                ProductoEntity(id = 1, nombre = "Filtro", categoria = "Filtros", precioPredeterminadoCentavos = 1_000, stockActual = 2.0),
                ProductoEntity(id = 2, nombre = "Aceite", categoria = "Lubricantes", precioPredeterminadoCentavos = 500, stockActual = 3.0)
            )
        )

        val mensual = reporte.filtrarPorPeriodo(PeriodoReporte.MES, ahora).calcularMetricas()

        assertEquals(1, mensual.cotizacionesAprobadas)
        assertEquals(0, mensual.cotizacionesPendientes)
        assertEquals(11_500L, mensual.valorAprobadoCentavos)
        assertEquals(1, mensual.hojasCompletadas)
        assertEquals(1, mensual.productosStockBajo)
        assertEquals(3_500L, mensual.valorInventarioCentavos)
    }

    @Test
    fun numeracion_de_hojas_comienza_en_cien_y_avanza_desde_el_ultimo_registro() {
        assertEquals(100, siguienteConsecutivoHoja(null))
        assertEquals(100, siguienteConsecutivoHoja("Q 0000099"))
        assertEquals(101, siguienteConsecutivoHoja("Q 0000100"))
        assertEquals(1_250, siguienteConsecutivoHoja("Q 0001249"))
    }
}
