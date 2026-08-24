package com.example.cggestion.data

import kotlin.math.roundToLong

data class TotalesCotizacion(
    val subtotalBruto: Long,
    val descuentoItems: Long,
    val descuentoGlobal: Long,
    val baseImponible: Long,
    val valorIva: Long,
    val totalFinal: Long
)

fun Double.aCentavos(): Long = (coerceAtLeast(0.0) * 100).roundToLong()
fun Long.aDolares(): Double = this / 100.0

fun calcularTotales(items: List<ItemCotizacion>, descuentoGlobal: Double, iva: Double): TotalesCotizacion {
    val subtotal = items.sumOf { it.subtotalLinea.aCentavos() }
    val descuentoItems = items.sumOf { it.descuentoLinea.aCentavos() }
    val despuesDescuentos = subtotal - descuentoItems
    val descuentoGlobalValor = (despuesDescuentos * descuentoGlobal.coerceIn(0.0, 100.0) / 100.0).roundToLong()
    val base = despuesDescuentos - descuentoGlobalValor
    val valorIva = (base * iva.coerceIn(0.0, 100.0) / 100.0).roundToLong()
    return TotalesCotizacion(subtotal, descuentoItems, descuentoGlobalValor, base, valorIva, base + valorIva)
}
