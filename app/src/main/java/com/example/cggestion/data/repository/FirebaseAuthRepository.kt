package com.example.cggestion.data.repository

import android.content.Context
import com.example.cggestion.auth.PerfilUsuario
import com.example.cggestion.auth.RolUsuarioFirebase
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseAuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferencias = appContext.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
    private val firebaseApp: FirebaseApp? = runCatching {
        FirebaseApp.getApps(appContext).firstOrNull() ?: FirebaseApp.initializeApp(appContext)
    }.getOrNull()
    private val auth: FirebaseAuth? by lazy { firebaseApp?.let(FirebaseAuth::getInstance) }
    private val firestore: FirebaseFirestore? by lazy { firebaseApp?.let(FirebaseFirestore::getInstance) }

    val configurado: Boolean get() = firebaseApp != null

    suspend fun restaurarSesion(): PerfilUsuario? {
        val autenticacion = auth ?: throw ConfiguracionFirebaseException()
        val usuario = autenticacion.currentUser ?: return null
        return try {
            usuario.getIdToken(true).esperar()
            obtenerPerfilServidor(usuario.uid, usuario.email.orEmpty()).also(::guardarCache)
        } catch (error: Throwable) {
            if (error.esFalloDeRed()) {
                perfilCache(usuario.uid) ?: runCatching {
                    obtenerPerfilCacheFirestore(usuario.uid, usuario.email.orEmpty()).also(::guardarCache)
                }.getOrElse {
                    throw SesionSinConexionException()
                }
            } else {
                autenticacion.signOut()
                limpiarCache()
                throw error
            }
        }
    }

    suspend fun iniciarSesion(email: String, password: String): PerfilUsuario {
        val autenticacion = auth ?: throw ConfiguracionFirebaseException()
        val correo = email.trim().lowercase()
        require(correo.isNotBlank()) { "Ingresa tu correo electrónico." }
        require(password.isNotBlank()) { "Ingresa tu contraseña." }
        val usuario = autenticacion.signInWithEmailAndPassword(correo, password)
            .esperar()
            .user ?: throw IllegalStateException("Firebase no devolvió el usuario autenticado.")
        return try {
            obtenerPerfilServidor(usuario.uid, usuario.email.orEmpty()).also(::guardarCache)
        } catch (error: Throwable) {
            autenticacion.signOut()
            limpiarCache()
            throw error
        }
    }

    suspend fun enviarRestablecimiento(email: String) {
        val autenticacion = auth ?: throw ConfiguracionFirebaseException()
        val correo = email.trim().lowercase()
        require(correo.isNotBlank()) { "Ingresa tu correo para recuperar la contraseña." }
        autenticacion.setLanguageCode("es")
        autenticacion.sendPasswordResetEmail(correo).esperar()
    }

    fun cerrarSesion() {
        auth?.signOut()
        limpiarCache()
    }

    private suspend fun obtenerPerfilServidor(uid: String, emailAuth: String): PerfilUsuario =
        leerPerfil(uid, emailAuth, Source.SERVER)

    private suspend fun obtenerPerfilCacheFirestore(uid: String, emailAuth: String): PerfilUsuario =
        leerPerfil(uid, emailAuth, Source.CACHE)

    private suspend fun leerPerfil(uid: String, emailAuth: String, source: Source): PerfilUsuario {
        val base = firestore ?: throw ConfiguracionFirebaseException()
        val documento = base.collection(COLECCION_USUARIOS).document(uid).get(source).esperar()
        if (!documento.exists()) throw PerfilFirebaseException(
            "Tu cuenta existe, pero no tiene un perfil autorizado. Comunícate con el administrador."
        )
        val rol = RolUsuarioFirebase.desde(documento.getString("rol"))
            ?: throw PerfilFirebaseException("El perfil no tiene un rol válido.")
        val activo = documento.getBoolean("activo") ?: false
        if (!activo) throw PerfilFirebaseException("Esta cuenta está desactivada.")
        return PerfilUsuario(
            uid = uid,
            email = documento.getString("email")?.trim().orEmpty().ifBlank { emailAuth },
            nombre = documento.getString("nombre")?.trim().orEmpty().ifBlank { emailAuth.substringBefore('@') },
            rol = rol,
            activo = true
        )
    }

    private fun guardarCache(perfil: PerfilUsuario) {
        preferencias.edit()
            .putString(CLAVE_UID, perfil.uid)
            .putString(CLAVE_EMAIL, perfil.email)
            .putString(CLAVE_NOMBRE, perfil.nombre)
            .putString(CLAVE_ROL, perfil.rol.name)
            .putBoolean(CLAVE_ACTIVO, perfil.activo)
            .apply()
    }

    private fun perfilCache(uidEsperado: String): PerfilUsuario? {
        val uid = preferencias.getString(CLAVE_UID, null) ?: return null
        if (uid != uidEsperado) return null
        val rol = RolUsuarioFirebase.desde(preferencias.getString(CLAVE_ROL, null)) ?: return null
        if (!preferencias.getBoolean(CLAVE_ACTIVO, false)) return null
        return PerfilUsuario(
            uid = uid,
            email = preferencias.getString(CLAVE_EMAIL, "").orEmpty(),
            nombre = preferencias.getString(CLAVE_NOMBRE, "").orEmpty(),
            rol = rol,
            activo = true
        )
    }

    private fun limpiarCache() {
        preferencias.edit().clear().apply()
    }

    fun mensajeError(error: Throwable): String = when (error) {
        is ConfiguracionFirebaseException -> error.message.orEmpty()
        is SesionSinConexionException -> error.message.orEmpty()
        is PerfilFirebaseException -> error.message.orEmpty()
        is FirebaseNetworkException -> "No hay conexión. Comprueba internet e inténtalo nuevamente."
        is FirebaseAuthInvalidUserException -> "La cuenta no existe o fue desactivada."
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
            "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Correo o contraseña incorrectos."
            "ERROR_USER_DISABLED" -> "Esta cuenta fue desactivada."
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera unos minutos e inténtalo nuevamente."
            else -> "No se pudo iniciar sesión: ${error.localizedMessage ?: error.errorCode}"
        }
        is IllegalArgumentException -> error.message ?: "Revisa los datos ingresados."
        else -> error.localizedMessage ?: "Ocurrió un error de autenticación."
    }

    private fun Throwable.esFalloDeRed(): Boolean =
        this is FirebaseNetworkException ||
            (this is FirebaseFirestoreException && code in setOf(
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
            )) || cause?.esFalloDeRed() == true

    companion object {
        private const val COLECCION_USUARIOS = "usuarios"
        private const val PREFERENCIAS = "firebase_session_cache"
        private const val CLAVE_UID = "uid"
        private const val CLAVE_EMAIL = "email"
        private const val CLAVE_NOMBRE = "nombre"
        private const val CLAVE_ROL = "rol"
        private const val CLAVE_ACTIVO = "activo"
    }
}

class ConfiguracionFirebaseException : IllegalStateException(
    "Firebase aún no está configurado. Agrega app/google-services.json para habilitar el inicio de sesión."
)

class PerfilFirebaseException(message: String) : IllegalStateException(message)

class SesionSinConexionException : IllegalStateException(
    "Se necesita internet para validar esta cuenta por primera vez."
)

private suspend fun <T> Task<T>.esperar(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("La operación de Firebase falló."))
    }
}
