package com.example.cggestion

import com.example.cggestion.data.ItemCotizacion
import com.example.cggestion.data.HojaCampoValidaciones
import com.example.cggestion.data.Producto
import com.example.cggestion.data.calcularTotales
import com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity
import com.example.cggestion.util.pdf.PdfFileName
import com.example.cggestion.data.repository.InventarioRules
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
}
