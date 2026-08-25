package com.example.cggestion.data.repository

import com.example.cggestion.data.local.database.CGGestionDatabase
import com.example.cggestion.data.local.entity.RolUsuario
import com.example.cggestion.data.local.entity.SesionUsuario
import com.example.cggestion.data.local.entity.UsuarioEntity
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.flow.Flow

class AuthRepository(private val database: CGGestionDatabase) {
    private val dao get() = database.usuarioDao()

    fun usuarios(): Flow<List<UsuarioEntity>> = dao.todos()
    suspend fun hayUsuarios(): Boolean = dao.cantidad() > 0

    suspend fun crearPrimerAdministrador(usuario: String, clave: CharArray): SesionUsuario {
        check(!hayUsuarios()) { "La cuenta administradora ya fue configurada." }
        return crearUsuario(usuario, clave, RolUsuario.ADMINISTRADOR.name)
    }

    suspend fun crearUsuario(usuario: String, clave: CharArray, rol: String): SesionUsuario {
        val nombre = normalizarUsuario(usuario)
        validar(nombre, clave)
        check(dao.porUsuario(nombre) == null) { "Ese usuario ya existe." }
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = derivar(clave, sal, ITERACIONES)
        val id = dao.insertar(UsuarioEntity(usuario = nombre, passwordHash = codificar(hash), sal = codificar(sal), iteraciones = ITERACIONES, rol = rol))
        return SesionUsuario(id, nombre, rol)
    }

    suspend fun autenticar(usuario: String, clave: CharArray): SesionUsuario? {
        val cuenta = dao.porUsuario(normalizarUsuario(usuario)) ?: return null
        if (!cuenta.activo || !coincide(clave, cuenta)) return null
        return SesionUsuario(cuenta.id, cuenta.usuario, cuenta.rol)
    }

    suspend fun cambiarClave(id: Long, clave: CharArray) {
        val cuenta = dao.porId(id) ?: error("No se encontró el usuario.")
        validar(cuenta.usuario, clave)
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        dao.actualizar(cuenta.copy(passwordHash = codificar(derivar(clave, sal, ITERACIONES)), sal = codificar(sal), iteraciones = ITERACIONES, fechaActualizacion = System.currentTimeMillis()))
    }

    suspend fun cambiarEstado(id: Long, activo: Boolean) {
        val cuenta = dao.porId(id) ?: error("No se encontró el usuario.")
        if (!activo && cuenta.rol == RolUsuario.ADMINISTRADOR.name && dao.administradoresActivos() <= 1) {
            error("Debe permanecer al menos un administrador activo.")
        }
        dao.actualizar(cuenta.copy(activo = activo, fechaActualizacion = System.currentTimeMillis()))
    }

    private fun normalizarUsuario(valor: String): String = valor.trim().lowercase()
    private fun validar(usuario: String, clave: CharArray) {
        require(usuario.length >= 3) { "El usuario debe tener al menos 3 caracteres." }
        require(clave.size >= 8) { "La contraseña debe tener al menos 8 caracteres." }
    }
    private fun coincide(clave: CharArray, cuenta: UsuarioEntity): Boolean {
        val esperado = decodificar(cuenta.passwordHash)
        val obtenido = derivar(clave, decodificar(cuenta.sal), cuenta.iteraciones)
        if (esperado.size != obtenido.size) return false
        var diferencia = 0
        esperado.indices.forEach { diferencia = diferencia or (esperado[it].toInt() xor obtenido[it].toInt()) }
        return diferencia == 0
    }
    private fun derivar(clave: CharArray, sal: ByteArray, iteraciones: Int): ByteArray {
        val especificacion = PBEKeySpec(clave, sal, iteraciones, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).encoded } finally { especificacion.clearPassword() }
    }
    private fun codificar(valor: ByteArray): String = Base64.getEncoder().encodeToString(valor)
    private fun decodificar(valor: String): ByteArray = Base64.getDecoder().decode(valor)

    private companion object { const val ITERACIONES = 210_000 }
}
