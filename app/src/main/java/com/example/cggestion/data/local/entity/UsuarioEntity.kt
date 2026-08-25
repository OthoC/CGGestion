package com.example.cggestion.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RolUsuario { ADMINISTRADOR, OPERADOR }

@Entity(tableName = "usuarios", indices = [Index(value = ["usuario"], unique = true)])
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuario: String,
    val passwordHash: String,
    val sal: String,
    val iteraciones: Int,
    val rol: String = RolUsuario.OPERADOR.name,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val fechaActualizacion: Long = System.currentTimeMillis()
)

data class SesionUsuario(val id: Long, val usuario: String, val rol: String) {
    val esAdministrador: Boolean get() = rol == RolUsuario.ADMINISTRADOR.name
}
