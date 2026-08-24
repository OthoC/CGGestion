package com.example.cggestion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "equipos", foreignKeys = [ForeignKey(entity = ClienteEntity::class, parentColumns = ["id"], childColumns = ["clienteId"], onDelete = ForeignKey.RESTRICT)], indices = [Index(value = ["clienteId"]), Index(value = ["serie"], unique = true)])
data class EquipoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val tipo: String = "GENERADOR",
    val marca: String = "",
    val modelo: String = "",
    val serie: String? = null,
    val potenciaKva: String = "",
    val motorMarca: String = "",
    val motorModelo: String = "",
    val alternadorMarca: String = "",
    val alternadorModelo: String = "",
    val ubicacion: String = "",
    val observaciones: String = "",
    val activo: Boolean = true,
    val actualizadoEn: Long = System.currentTimeMillis()
)
