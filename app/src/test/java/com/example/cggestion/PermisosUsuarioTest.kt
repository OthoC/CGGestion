package com.example.cggestion

import com.example.cggestion.auth.ModuloRestringible
import com.example.cggestion.auth.PerfilUsuario
import com.example.cggestion.auth.PermisosUsuario
import com.example.cggestion.auth.RolUsuarioFirebase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermisosUsuarioTest {
    private val tecnico = PerfilUsuario("t1", "tecnico@empresa.com", "Técnico", RolUsuarioFirebase.TECNICO, true)
    private val superusuario = PerfilUsuario("s1", "admin@empresa.com", "Administrador", RolUsuarioFirebase.SUPERUSUARIO, true)

    @Test
    fun tecnicoPuedeUsarModulosOperativos() {
        val permitidos = listOf(
            ModuloRestringible.HOJAS_CAMPO,
            ModuloRestringible.COTIZACIONES,
            ModuloRestringible.HISTORIAL,
            ModuloRestringible.CLIENTES,
            ModuloRestringible.MANTENIMIENTOS,
            ModuloRestringible.RESPALDOS,
            ModuloRestringible.ACTUALIZACIONES
        )
        permitidos.forEach { assertTrue("El técnico debería acceder a $it", PermisosUsuario.puedeAcceder(tecnico, it)) }
    }

    @Test
    fun tecnicoNoPuedeUsarFuncionesReservadas() {
        assertFalse(PermisosUsuario.puedeAcceder(tecnico, ModuloRestringible.INVENTARIO))
        assertFalse(PermisosUsuario.puedeAcceder(tecnico, ModuloRestringible.REPORTES))
        assertFalse(PermisosUsuario.puedeAcceder(tecnico, ModuloRestringible.RESTAURAR_RESPALDO))
    }

    @Test
    fun superusuarioPuedeUsarTodosLosModulos() {
        ModuloRestringible.entries.forEach {
            assertTrue("El superusuario debería acceder a $it", PermisosUsuario.puedeAcceder(superusuario, it))
        }
    }

    @Test
    fun perfilInactivoNoPuedeAcceder() {
        val inactivo = tecnico.copy(activo = false)
        ModuloRestringible.entries.forEach { assertFalse(PermisosUsuario.puedeAcceder(inactivo, it)) }
    }
}
