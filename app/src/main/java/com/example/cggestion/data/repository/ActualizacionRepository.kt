package com.example.cggestion.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.example.cggestion.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class ActualizacionDisponible(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notas: String = "",
    val fecha: String = ""
)

class ActualizacionRepository(private val context: Context) {
    companion object {
        const val MANIFIESTO_URL = "https://github.com/OthoC/CGGestion/releases/latest/download/update.json"
    }

    suspend fun comprobar(): ActualizacionDisponible? = withContext(Dispatchers.IO) {
        val texto = descargarTexto(MANIFIESTO_URL)
        val json = JSONObject(texto)
        val disponible = ActualizacionDisponible(
            versionCode = json.getInt("versionCode"),
            versionName = json.getString("versionName"),
            apkUrl = json.getString("apkUrl"),
            sha256 = json.getString("sha256").lowercase(),
            notas = json.optString("notes"),
            fecha = json.optString("date")
        )
        require(disponible.apkUrl.startsWith("https://")) { "La URL de actualización no es segura." }
        require(disponible.sha256.matches(Regex("[a-f0-9]{64}"))) { "El hash de la actualización no es válido." }
        disponible.takeIf { it.versionCode > BuildConfig.VERSION_CODE }
    }

    suspend fun descargar(info: ActualizacionDisponible): File = withContext(Dispatchers.IO) {
        val directorio = File(context.cacheDir, "updates").apply { mkdirs() }
        val temporal = File(directorio, "CGGestion-${info.versionCode}.apk.part")
        val final = File(directorio, "CGGestion-${info.versionCode}.apk")
        temporal.delete()
        final.delete()
        try {
            val conexion = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "CGGestion/${BuildConfig.VERSION_NAME}")
            }
            conexion.inputStream.use { entrada -> temporal.outputStream().use { salida -> entrada.copyTo(salida) } }
            require(sha256(temporal).equals(info.sha256, ignoreCase = true)) { "La verificación de seguridad de la APK falló." }
            require(temporal.renameTo(final)) { "No se pudo guardar la actualización." }
            final
        } catch (error: Throwable) {
            temporal.delete()
            final.delete()
            throw error
        }
    }

    private fun descargarTexto(url: String): String {
        val conexion = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "CGGestion/${BuildConfig.VERSION_NAME}")
        }
        return conexion.inputStream.bufferedReader().use { it.readText() }
    }

    private fun sha256(archivo: File): String = MessageDigest.getInstance("SHA-256").digest(archivo.readBytes()).joinToString("") { "%02x".format(it) }
}
