package com.example.cggestion.util.pdf

object PdfFileName {
    fun paraCotizacion(numero: String): String = "Cotizacion_${numero.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"
}
