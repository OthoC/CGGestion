package com.example.cggestion.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla heredada de una versión anterior con inicio de sesión local.
 * Se conserva en el esquema para que las instalaciones existentes puedan abrirse sin
 * eliminar datos durante una migración; la aplicación ya no la consulta ni autentica usuarios.
 */
@Entity(tableName = "usuarios", indices = [Index(value = ["usuario"], unique = true)])
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuario: String,
    val passwordHash: String,
    val sal: String,
    val iteraciones: Int,
    val rol: String = "OPERADOR",
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)
