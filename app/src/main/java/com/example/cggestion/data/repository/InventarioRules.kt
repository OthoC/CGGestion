package com.example.cggestion.data.repository

object InventarioRules {
    fun cantidadDesdeTexto(texto: String): Double? = texto
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }

    fun cantidadNoNegativaDesdeTexto(texto: String): Double? = texto
        .trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
}
