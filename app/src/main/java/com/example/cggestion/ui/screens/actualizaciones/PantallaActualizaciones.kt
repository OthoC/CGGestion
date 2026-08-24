package com.example.cggestion.ui.screens.actualizaciones

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.viewmodel.ActualizacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaActualizaciones(viewModel: ActualizacionViewModel, volver: () -> Unit) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(containerColor = FondoPrincipal, topBar = { Column { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White), navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White) } }, title = { Text("ACTUALIZACIONES") }); HorizontalDivider(color = RojoCG.copy(alpha = .6f)) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CG GESTIÓN", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Versión instalada: ${estado.versionActual}", color = TextoSecundario)
            Spacer(Modifier.height(24.dp))
            when {
                estado.comprobando -> Text("Buscando actualización…", color = Color.White)
                estado.descargando -> Text("Descargando y verificando APK…", color = Color.White)
                estado.disponible != null -> {
                    val update = estado.disponible!!
                    Text("Nueva versión ${update.versionName}", color = RojoCG, fontWeight = FontWeight.Bold)
                    if (update.fecha.isNotBlank()) Text(update.fecha, color = TextoSecundario)
                    if (update.notas.isNotBlank()) Text(update.notas, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.height(12.dp))
                    if (estado.archivoListo == null) Button(onClick = viewModel::descargar, colors = ButtonDefaults.buttonColors(containerColor = RojoCG), modifier = Modifier.fillMaxWidth()) { Text("DESCARGAR ACTUALIZACIÓN") }
                    else Button(onClick = { instalar(context, estado.archivoListo!!, viewModel) }, colors = ButtonDefaults.buttonColors(containerColor = RojoCG), modifier = Modifier.fillMaxWidth()) { Text("INSTALAR ACTUALIZACIÓN") }
                }
                else -> Text("No hay actualizaciones disponibles.", color = Color.White)
            }
            estado.mensaje?.let { Text(it, color = Color.White, modifier = Modifier.padding(top = 12.dp)) }
            estado.error?.let { Text(it, color = RojoCG, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { viewModel.comprobar() }, enabled = !estado.comprobando && !estado.descargando) { Text("BUSCAR ACTUALIZACIÓN") }
            Text("La instalación requiere tu confirmación en Android.", color = TextoSecundario, modifier = Modifier.padding(top = 18.dp))
        }
    }
}

private fun instalar(context: android.content.Context, archivo: java.io.File, viewModel: ActualizacionViewModel) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        context.startActivity(intent)
        Toast.makeText(context, "Autoriza a CG Gestión para instalar actualizaciones y vuelve a intentarlo.", Toast.LENGTH_LONG).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent); viewModel.consumirInstalacion() }
        .onFailure { Toast.makeText(context, "No se pudo abrir el instalador de Android.", Toast.LENGTH_LONG).show() }
}
