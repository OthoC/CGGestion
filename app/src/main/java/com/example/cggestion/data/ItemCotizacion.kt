package com.example.cggestion.data

data class ItemCotizacion(
    val producto: Producto,
    val cantidad: Int = 1,
    val precioUnitarioTexto: String = producto.precio.toString(),
    val descuentoTexto: String = "0"
) {
    val precioUnitario: Double get() = precioUnitarioTexto.aNumero().coerceAtLeast(0.0)
    val descuento: Double get() = descuentoTexto.aNumero().coerceIn(0.0, 100.0)
    val subtotalLinea: Double get() = cantidad.coerceAtLeast(1) * precioUnitario
    val descuentoLinea: Double get() = subtotalLinea * descuento / 100.0
    val totalLinea: Double get() = subtotalLinea - descuentoLinea
}

fun String.aNumero(): Double = replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
