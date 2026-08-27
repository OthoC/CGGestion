package com.example.cggestion.auth

enum class RolUsuarioFirebase {
    SUPERUSUARIO,
    TECNICO;

    companion object {
        fun desde(valor: String?): RolUsuarioFirebase? = entries.firstOrNull {
            it.name.equals(valor?.trim(), ignoreCase = true)
        }
    }
}

data class PerfilUsuario(
    val uid: String,
    val email: String,
    val nombre: String,
    val rol: RolUsuarioFirebase,
    val activo: Boolean
) {
    val esSuperusuario: Boolean get() = rol == RolUsuarioFirebase.SUPERUSUARIO
}

enum class ModuloRestringible {
    HOJAS_CAMPO,
    COTIZACIONES,
    HISTORIAL,
    CLIENTES,
    MANTENIMIENTOS,
    RESPALDOS,
    ACTUALIZACIONES,
    INVENTARIO,
    REPORTES,
    RESTAURAR_RESPALDO
}

object PermisosUsuario {
    fun puedeAcceder(perfil: PerfilUsuario, modulo: ModuloRestringible): Boolean =
        perfil.activo && when (modulo) {
            ModuloRestringible.INVENTARIO,
            ModuloRestringible.REPORTES,
            ModuloRestringible.RESTAURAR_RESPALDO -> perfil.esSuperusuario

            else -> true
        }
}
