package com.example.cggestion.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CotizacionCompleta(
    @Embedded val cotizacion: CotizacionEntity,
    @Relation(parentColumn = "clienteId", entityColumn = "id") val cliente: ClienteEntity,
    @Relation(parentColumn = "id", entityColumn = "cotizacionId") val items: List<ItemCotizacionEntity>
)

data class CotizacionResumen(
    @Embedded val cotizacion: CotizacionEntity,
    val clienteNombre: String
)
