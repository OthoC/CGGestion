package com.example.cggestion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipoMantenimiento { PREVENTIVO, CORRECTIVO, INSPECCION }
enum class EstadoMantenimiento { PENDIENTE, EN_PROCESO, COMPLETADO, CANCELADO }
enum class PrioridadMantenimiento { BAJA, MEDIA, ALTA }

@Entity(
    tableName = "mantenimientos",
    foreignKeys = [
        ForeignKey(entity = ClienteEntity::class, parentColumns = ["id"], childColumns = ["clienteId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = EquipoEntity::class, parentColumns = ["id"], childColumns = ["equipoId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = HojaCampoEntity::class, parentColumns = ["id"], childColumns = ["hojaCampoId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index(value = ["clienteId"]), Index(value = ["equipoId"]), Index(value = ["hojaCampoId"]), Index(value = ["estado"]), Index(value = ["fechaProgramada"])]
)
data class MantenimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clienteId: Long,
    val equipoId: Long,
    val tipo: String = TipoMantenimiento.PREVENTIVO.name,
    val descripcion: String = "",
    val prioridad: String = PrioridadMantenimiento.MEDIA.name,
    val fechaProgramada: Long,
    val periodicidadDias: Int? = null,
    val estado: String = EstadoMantenimiento.PENDIENTE.name,
    val hojaCampoId: Long? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaModificacion: Long = System.currentTimeMillis()
)

data class MantenimientoResumen(
    val id: Long,
    val clienteId: Long,
    val equipoId: Long,
    val tipo: String,
    val descripcion: String,
    val prioridad: String,
    val fechaProgramada: Long,
    val periodicidadDias: Int?,
    val estado: String,
    val hojaCampoId: Long?,
    val fechaCreacion: Long,
    val fechaModificacion: Long,
    val clienteNombre: String,
    val equipoNombre: String
)
