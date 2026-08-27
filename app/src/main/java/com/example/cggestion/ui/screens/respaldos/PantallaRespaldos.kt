package com.example.cggestion.ui.screens.respaldos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.viewmodel.RespaldosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaRespaldos(
    viewModel: RespaldosViewModel,
    puedeRestaurar: Boolean,
    volver: () -> Unit
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmarRestauracion by remember { mutableStateOf<android.net.Uri?>(null) }
    val abrir = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { confirmarRestauracion = it }
    }
    val seleccionarCarpetaNube = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.configurarCarpetaNube(it)
        }
    }

    LaunchedEffect(estado.restaurado) {
        if (estado.restaurado) context.actividad()?.recreate()
    }

    Scaffold(
        containerColor = FondoPrincipal,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FondoBarraSuperior,
                        titleContentColor = Color.White
                    ),
                    title = { Text("RESPALDOS") },
                    navigationIcon = {
                        TextButton(onClick = volver) { Text("←", color = Color.White) }
                    }
                )
                HorizontalDivider(color = RojoCG.copy(alpha = .6f))
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Respaldo local", color = Color.White)
            Text(
                "Incluye datos, PDF de cotizaciones y hojas de campo, además de evidencias fotográficas. El archivo contiene información privada.",
                color = TextoSecundario,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = viewModel::crear,
                enabled = !estado.trabajando,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RojoCG)
            ) { Text(if (estado.trabajando) "PROCESANDO…" else "CREAR RESPALDO") }
            estado.archivo?.let { archivo ->
                OutlinedButton(
                    onClick = { compartir(context, archivo) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("COMPARTIR ÚLTIMO RESPALDO", color = Color.White) }
            }
            Spacer(Modifier.height(24.dp))
            Text("Copia en Google Drive", color = Color.White)
            Text(
                if (estado.carpetaNubeConfigurada) {
                    "Carpeta configurada. Puedes subir el último respaldo cuando quieras."
                } else {
                    "Elige una carpeta de Google Drive para habilitar la carga manual."
                },
                color = TextoSecundario,
                modifier = Modifier.padding(top = 6.dp)
            )
            OutlinedButton(
                onClick = { seleccionarCarpetaNube.launch(null) },
                enabled = !estado.trabajando,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) {
                Text(
                    if (estado.carpetaNubeConfigurada) "CAMBIAR CARPETA DE DRIVE" else "ELEGIR CARPETA DE DRIVE",
                    color = RojoCG
                )
            }
            if (estado.carpetaNubeConfigurada) {
                Button(
                    onClick = viewModel::subirANube,
                    enabled = !estado.trabajando,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RojoCG)
                ) { Text("SUBIR ÚLTIMO RESPALDO", color = Color.White) }
            }
            if (puedeRestaurar) {
                Spacer(Modifier.height(24.dp))
                Text("Restaurar", color = Color.White)
                Text(
                    "La restauración reemplaza toda la información actual por la del respaldo seleccionado.",
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 6.dp)
                )
                OutlinedButton(
                    onClick = { abrir.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !estado.trabajando,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) { Text("SELECCIONAR RESPALDO", color = RojoCG) }
            }
            estado.mensaje?.let { Text(it, color = Color.White, modifier = Modifier.padding(top = 16.dp)) }
            estado.error?.let { Text(it, color = RojoCG, modifier = Modifier.padding(top = 16.dp)) }
        }
    }

    confirmarRestauracion?.takeIf { puedeRestaurar }?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmarRestauracion = null },
            containerColor = FondoTarjeta,
            title = { Text("¿Restaurar respaldo?", color = Color.White) },
            text = {
                Text(
                    "Los datos actuales de la aplicación serán reemplazados. Esta acción no se puede deshacer.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRestauracion = null
                    viewModel.restaurar(uri)
                }) { Text("RESTAURAR", color = RojoCG) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRestauracion = null }) {
                    Text("CANCELAR", color = Color.White)
                }
            }
        )
    }
}

private fun compartir(context: Context, archivo: java.io.File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("application/zip")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, "Respaldo CG Gestión")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                "Compartir respaldo"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private tailrec fun Context.actividad(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.actividad()
    else -> null
}
