package com.example.cggestion.data

import com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity

object HojaCampoValidaciones {
    fun mediciones(mediciones: MedicionesHojaCampoEntity): String? {
        val campos = listOf(
            "L1-L2" to mediciones.l1l2,
            "L2-L3" to mediciones.l2l3,
            "L3-L1" to mediciones.l3l1,
            "LN" to mediciones.ln,
            "AMP L1" to mediciones.ampL1,
            "AMP L2" to mediciones.ampL2,
            "AMP L3" to mediciones.ampL3,
            "Hz vacío" to mediciones.hzVacio,
            "Hz con carga" to mediciones.hzCarga,
            "RPM vacío" to mediciones.rpmVacio,
            "RPM con carga" to mediciones.rpmCarga,
            "Presión de aceite" to mediciones.presionAceite,
            "Temperatura del motor" to mediciones.temperaturaMotor,
            "Carga del alternador" to mediciones.cargaAlternador,
            "Voltaje de batería" to mediciones.voltajeBateria
        )
        campos.firstOrNull { (_, valor) -> numeroNoNegativo(valor) == null && valor.isNotBlank() }
            ?.let { return "${it.first} debe ser un número mayor o igual a cero." }
        val combustible = numeroNoNegativo(mediciones.combustible)
        if (mediciones.combustible.isNotBlank() && (combustible == null || combustible !in 0.0..100.0)) {
            return "El nivel de combustible debe estar entre 0 % y 100 %."
        }
        return null
    }

    fun horometro(valor: String): String? = when {
        valor.isBlank() -> null
        numeroNoNegativo(valor) == null -> "El horómetro debe ser un número mayor o igual a cero."
        else -> null
    }

    fun jornada(inicio: String, fin: String, tecnicos: String): String? {
        if (inicio.isBlank() || fin.isBlank()) return "Completa la hora de inicio y finalización de la jornada."
        if (tecnicos.isBlank()) return "Indica el técnico de la jornada."
        val desde = minutos(inicio) ?: return "La hora de inicio debe usar el formato HH:mm."
        val hasta = minutos(fin) ?: return "La hora de finalización debe usar el formato HH:mm."
        return if (hasta <= desde) "La hora final debe ser posterior a la hora inicial." else null
    }

    fun minutos(hora: String): Int? {
        val partes = hora.split(":")
        val horas = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val minutos = partes.getOrNull(1)?.toIntOrNull() ?: return null
        return if (horas in 0..23 && minutos in 0..59) horas * 60 + minutos else null
    }

    private fun numeroNoNegativo(valor: String): Double? = valor.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
}
