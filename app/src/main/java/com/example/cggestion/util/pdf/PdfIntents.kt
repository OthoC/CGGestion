package com.example.cggestion.util.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object PdfIntents {
    fun ver(context: Context, archivo: File): Result<Unit> = ejecutar {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun compartir(context: Context, archivo: File, numero: String, cliente: String): Result<Unit> = ejecutar {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val enviar = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, "Cotización $numero").putExtra(Intent.EXTRA_TEXT, "Comparto la cotización $numero para $cliente.").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(enviar, "Compartir cotización").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun ejecutar(accion: () -> Unit): Result<Unit> = try { accion(); Result.success(Unit) } catch (_: ActivityNotFoundException) { Result.failure(IllegalStateException("No hay una aplicación compatible instalada.")) } catch (e: Exception) { Result.failure(IllegalStateException("No se pudo completar la acción con el PDF.", e)) }
}
