package com.example.cggestion.ui.screens.hojascampo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.data.HojaCampoValidaciones
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.data.local.entity.EstadoControl
import com.example.cggestion.data.local.entity.JornadaTrabajoEntity
import com.example.cggestion.data.local.entity.RepuestoUsadoEntity
import com.example.cggestion.data.local.entity.TipoEvidencia
import java.io.File
import android.content.Intent
import android.graphics.BitmapFactory
import com.example.cggestion.data.local.entity.EvidenciaEntity
import com.example.cggestion.viewmodel.HojaCampoViewModel
import com.example.cggestion.viewmodel.HojaCampoPdfViewModel
import java.text.DateFormat
import java.util.Date
import android.widget.Toast
import coil.compose.AsyncImage

private enum class FiltroHoja { TODAS, BORRADOR, COMPLETADA, ANULADA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHojasCampo(
    viewModel: HojaCampoViewModel,
    pdfViewModel: HojaCampoPdfViewModel,
    volver: () -> Unit,
    abrirFormularioInicial: Boolean = false,
    consumirAbrirFormulario: () -> Unit = {}
) {
    val hojas by viewModel.hojas.collectAsStateWithLifecycle()
    var editando by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroHoja.TODAS) }
    var confirmarApertura by remember { mutableStateOf<Long?>(null) }
    val filtradas = hojas.filter { resumen ->
        val coincideTexto = busqueda.isBlank() || listOf(
            resumen.hoja.numeroHoja,
            resumen.clienteNombre,
            resumen.hoja.tecnicos,
            resumen.hoja.cotizacionId?.toString().orEmpty(),
            formatoFecha(resumen.hoja.fecha)
        ).any { it.contains(busqueda, ignoreCase = true) }
        val coincideEstado = filtro == FiltroHoja.TODAS || resumen.hoja.estado == filtro.name
        coincideTexto && coincideEstado
    }

    LaunchedEffect(abrirFormularioInicial) {
        if (abrirFormularioInicial) {
            editando = true
            consumirAbrirFormulario()
        }
    }

    if (editando) {
        FormularioHoja(viewModel, pdfViewModel) { editando = false }
        return
    }

    Scaffold(
        containerColor = FondoPrincipal,
        topBar = { BarraHoja(titulo = "HOJAS DE CAMPO", alVolver = volver) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
            Button(
                onClick = { viewModel.nueva(); editando = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RojoCG)
            ) { Text("NUEVA HOJA") }
            Spacer(Modifier.height(12.dp))
            Campo("Buscar por número, cliente, técnico o fecha", busqueda) { busqueda = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FiltroHoja.entries.forEach { opcion ->
                    val seleccionado = filtro == opcion
                    OutlinedButton(
                        onClick = { filtro = opcion },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (seleccionado) RojoCG else Color.White)
                    ) { Text(opcion.name) }
                }
            }
            if (hojas.isEmpty()) {
                Text("Aún no hay hojas de campo guardadas.", color = TextoSecundario)
            } else {
                if (filtradas.isEmpty()) {
                    Text("No se encontraron hojas de campo.", color = TextoSecundario, modifier = Modifier.padding(top = 12.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtradas, key = { it.hoja.id }) { resumen ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (resumen.hoja.estado == EstadoHoja.COMPLETADA.name) {
                                    confirmarApertura = resumen.hoja.id
                                } else {
                                    viewModel.cargar(resumen.hoja.id)
                                    editando = true
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(resumen.hoja.numeroHoja, color = RojoCG)
                                Text(resumen.clienteNombre, color = Color.White)
                                Text(
                                    "${resumen.evidenciasCantidad} evidencia(s) fotográfica(s)",
                                    color = TextoSecundario
                                )
                                Text(
                                    "${formatoFecha(resumen.hoja.fecha)} · ${resumen.hoja.estado} · ${resumen.hoja.tecnicos.ifBlank { "Sin técnico" }}",
                                    color = TextoSecundario
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    confirmarApertura?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmarApertura = null },
            containerColor = FondoTarjeta,
            title = { Text("Editar hoja completada", color = Color.White) },
            text = { Text("Esta hoja está completada. ¿Deseas abrirla para editarla?", color = Color.White) },
            confirmButton = { TextButton(onClick = { viewModel.cargar(id); editando = true; confirmarApertura = null }) { Text("ABRIR", color = RojoCG) } },
            dismissButton = { TextButton(onClick = { confirmarApertura = null }) { Text("CANCELAR", color = Color.White) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FormularioHoja(viewModel: HojaCampoViewModel, pdfViewModel: HojaCampoPdfViewModel, volver: () -> Unit) {
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    val estadoPdf by pdfViewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var seccion by remember { mutableIntStateOf(0) }
    var capturaPendiente by remember { mutableStateOf<String?>(null) }
    var registrarEvidencia by remember { mutableStateOf<String?>(null) }
    var evidenciasGaleriaPendientes by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var confirmarSalida by remember { mutableStateOf(false) }
    var accionPdfPendiente by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        val ruta = capturaPendiente
        capturaPendiente = null
        if (ruta != null) if (exito) registrarEvidencia = ruta else viewModel.cancelarCaptura(ruta)
    }
    val galeriaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        evidenciasGaleriaPendientes = uris.distinct()
    }
    val permisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        val ruta = capturaPendiente
        if (concedido && ruta != null) {
            cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(ruta)))
        } else if (ruta != null) {
            viewModel.cancelarCaptura(ruta); capturaPendiente = null
        }
    }
    val titulos = listOf("Cliente", "Equipo", "Mediciones", "Trabajo", "Repuestos", "Jornadas", "Revisión")
    BackHandler {
        if (estado.tieneCambios) confirmarSalida = true else volver()
    }

    Scaffold(
        containerColor = FondoPrincipal,
        topBar = {
            BarraHoja(
                titulo = "${estado.hoja.numeroHoja.ifBlank { "NUEVA HOJA" }} · ${seccion + 1}/${titulos.size}",
                alVolver = { if (estado.tieneCambios) confirmarSalida = true else volver() }
            )
        }
    ) { padding ->
        if (estado.cargando) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = RojoCG, modifier = Modifier.padding(24.dp))
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Text(titulos[seccion], color = Color.White)
            Text("Los cambios se conservan al guardar la hoja.", color = TextoSecundario)
            Spacer(Modifier.height(10.dp))
            when (seccion) {
                0 -> SeccionCliente(viewModel)
                1 -> SeccionEquipo(viewModel)
                2 -> SeccionMediciones(viewModel)
                3 -> SeccionTrabajo(viewModel)
                4 -> SeccionRepuestos(viewModel, estado.repuestos)
                5 -> SeccionJornadas(viewModel, estado.jornadas)
                else -> { SeccionRevision(viewModel); SeccionEvidenciasMejorada(viewModel, seleccionarGaleria = {
                    if (viewModel.puedeAgregarEvidencias()) galeriaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    val ruta = viewModel.prepararCaptura()
                    if (ruta != null) {
                        capturaPendiente = ruta
                        permisoCamara.launch(android.Manifest.permission.CAMERA)
                    }
                } }
            }
            estado.mensaje?.let { Text(it, color = RojoCG, modifier = Modifier.padding(top = 12.dp)) }
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (seccion > 0) OutlinedButton(onClick = { seccion-- }) { Text("ANTERIOR") }
                if (seccion < titulos.lastIndex) Button(onClick = { seccion++ }, colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) { Text("SIGUIENTE") }
                OutlinedButton(onClick = { viewModel.guardar(false) }, enabled = !estado.guardando) { Text("GUARDAR BORRADOR") }
                Button(onClick = { viewModel.guardar(true) }, enabled = !estado.guardando, colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) {
                    Text(if (estado.guardando) "GUARDANDO…" else "GUARDAR HOJA")
                }
            }
            if (estado.hoja.id != 0L) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pdfViewModel.generar(estado.hoja.id) }, enabled = !estadoPdf.generando) {
                        Text(if (estadoPdf.generando) "GENERANDO PDF…" else "GENERAR PDF")
                    }
                    OutlinedButton(onClick = {
                        if (estadoPdf.archivo?.exists() == true) abrirPdf(context, estadoPdf.archivo!!)
                        else { accionPdfPendiente = "VER"; pdfViewModel.generar(estado.hoja.id) }
                    }, enabled = !estadoPdf.generando) { Text("VER PDF") }
                    OutlinedButton(onClick = {
                        if (estadoPdf.archivo?.exists() == true) compartirPdf(context, estadoPdf.archivo!!, estado.hoja.numeroHoja)
                        else { accionPdfPendiente = "COMPARTIR"; pdfViewModel.generar(estado.hoja.id) }
                    }, enabled = !estadoPdf.generando) { Text("COMPARTIR PDF") }
                }
                estadoPdf.mensaje?.let { Text(it, color = Color.White, modifier = Modifier.padding(top = 8.dp)) }
                estadoPdf.error?.let { Text(it, color = RojoCG, modifier = Modifier.padding(top = 8.dp)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    LaunchedEffect(estadoPdf.archivo, accionPdfPendiente) {
        val archivo = estadoPdf.archivo
        val accion = accionPdfPendiente
        if (archivo != null && archivo.exists() && accion != null) {
            if (accion == "VER") abrirPdf(context, archivo) else compartirPdf(context, archivo, estado.hoja.numeroHoja)
            accionPdfPendiente = null
        }
    }
    registrarEvidencia?.let { ruta -> DialogoRegistrarEvidencia(
        guardar = { descripcion, tipo -> viewModel.guardarEvidenciaTemporal(ruta, descripcion, tipo); registrarEvidencia = null },
        cancelar = { viewModel.cancelarCaptura(ruta); registrarEvidencia = null }
    ) }
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            containerColor = FondoTarjeta,
            title = { Text("Cambios sin guardar", color = Color.White) },
            text = { Text("Si sales ahora, se perderán los cambios no guardados.", color = Color.White) },
            confirmButton = { TextButton(onClick = { confirmarSalida = false; volver() }) { Text("SALIR", color = RojoCG) } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("SEGUIR EDITANDO", color = Color.White) } }
        )
    }
    evidenciasGaleriaPendientes.firstOrNull()?.let { uri ->
        DialogoRegistrarEvidencia(
            guardar = { descripcion, tipo ->
                viewModel.guardarEvidenciaGaleria(uri, descripcion, tipo)
                evidenciasGaleriaPendientes = evidenciasGaleriaPendientes.drop(1)
            },
            cancelar = { evidenciasGaleriaPendientes = evidenciasGaleriaPendientes.drop(1) }
        )
    }
}

private fun abrirPdf(context: android.content.Context, archivo: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/pdf")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "No hay una aplicación compatible para abrir PDF.", Toast.LENGTH_LONG).show() }
}

private fun compartirPdf(context: android.content.Context, archivo: File, numeroHoja: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_SEND)
        .setType("application/pdf")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, "Hoja de campo $numeroHoja")
        .putExtra(Intent.EXTRA_TEXT, "Comparto la hoja de campo $numeroHoja.")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(Intent.createChooser(intent, "Compartir PDF")) }
        .onFailure { Toast.makeText(context, "No hay una aplicación compatible para compartir PDF.", Toast.LENGTH_LONG).show() }
}

@Composable
private fun SeccionCliente(vm: HojaCampoViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    var mostrarClientes by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { mostrarClientes = true }, modifier = Modifier.fillMaxWidth()) {
        Text("SELECCIONAR CLIENTE")
    }
    Campo("Cliente o empresa", s.cliente.nombre) { vm.actualizarCliente { c -> c.copy(nombre = it) } }
    Campo("RUC o cédula", s.cliente.rucCedula, teclado = KeyboardType.Number) { vm.actualizarCliente { c -> c.copy(rucCedula = it) } }
    Campo("Teléfono", s.cliente.telefono, teclado = KeyboardType.Phone) { vm.actualizarCliente { c -> c.copy(telefono = it) } }
    Campo("Dirección", s.cliente.direccion) { vm.actualizarCliente { c -> c.copy(direccion = it) } }
    Campo("Correo electrónico", s.cliente.correoElectronico.orEmpty(), teclado = KeyboardType.Email) { vm.actualizarCliente { c -> c.copy(correoElectronico = it.ifBlank { null }) } }
    Campo("Orden de trabajo", s.hoja.ordenTrabajo) { vm.actualizarHoja { h -> h.copy(ordenTrabajo = it) } }
    Campo("Técnicos", s.hoja.tecnicos) { vm.actualizarHoja { h -> h.copy(tecnicos = it) } }
    if (mostrarClientes) SelectorCliente(vm, { mostrarClientes = false })
}

@Composable
private fun SeccionEquipo(vm: HojaCampoViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    var mostrarEquipos by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { mostrarEquipos = true }, enabled = s.hoja.clienteId != 0L, modifier = Modifier.fillMaxWidth()) { Text("SELECCIONAR EQUIPO DEL CLIENTE") }
    if (s.hoja.clienteId == 0L) Text("Selecciona primero un cliente para ver sus equipos.", color = TextoSecundario)
    TituloSeccion("Alternador")
    Campo("Marca", s.hoja.alternadorMarca) { vm.actualizarHoja { h -> h.copy(alternadorMarca = it) } }
    Campo("Modelo", s.hoja.alternadorModelo) { vm.actualizarHoja { h -> h.copy(alternadorModelo = it) } }
    Campo("Serie", s.hoja.alternadorSerie) { vm.actualizarHoja { h -> h.copy(alternadorSerie = it) } }
    Campo("RPM", s.hoja.rpm, KeyboardType.Decimal) { vm.actualizarHoja { h -> h.copy(rpm = it) } }
    Campo("KVA", s.hoja.kva, KeyboardType.Decimal) { vm.actualizarHoja { h -> h.copy(kva = it) } }
    Campo("Volt", s.hoja.volt, KeyboardType.Decimal) { vm.actualizarHoja { h -> h.copy(volt = it) } }
    Campo("KW", s.hoja.kw, KeyboardType.Decimal) { vm.actualizarHoja { h -> h.copy(kw = it) } }
    Campo("Amp", s.hoja.amp, KeyboardType.Decimal) { vm.actualizarHoja { h -> h.copy(amp = it) } }
    Campo("Hz", s.hoja.hz, KeyboardType.Decimal) { vm.actualizarHoja { h -> h.copy(hz = it) } }
    TituloSeccion("Motor")
    Campo("Marca", s.hoja.motorMarca) { vm.actualizarHoja { h -> h.copy(motorMarca = it) } }
    Campo("Modelo", s.hoja.motorModelo) { vm.actualizarHoja { h -> h.copy(motorModelo = it) } }
    Campo("Serie", s.hoja.motorSerie) { vm.actualizarHoja { h -> h.copy(motorSerie = it) } }
    Campo("Horómetro", s.hoja.horometro, KeyboardType.Decimal) { vm.actualizarHoja { h -> h.copy(horometro = it) } }
    Campo("Tipo de tablero (AUT o MAN)", s.hoja.tipoTablero) { vm.actualizarHoja { h -> h.copy(tipoTablero = it.uppercase().take(3)) } }
    if (mostrarEquipos) SelectorEquipo(vm) { mostrarEquipos = false }
}

@Composable
private fun SelectorEquipo(vm: HojaCampoViewModel, cerrar: () -> Unit) {
    val equipos by vm.equipos.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text("Seleccionar equipo", color = Color.White) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            equipos.filter { it.activo }.forEach { equipo ->
                Card(Modifier.fillMaxWidth().padding(top = 6.dp).clickable { vm.seleccionarEquipo(equipo); cerrar() }, colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))) {
                    Column(Modifier.padding(12.dp)) { Text("${equipo.marca} ${equipo.modelo}".trim(), color = Color.White); Text("Serie: ${equipo.serie.orEmpty().ifBlank { "Sin serie" } }", color = TextoSecundario) }
                }
            }
            if (equipos.isEmpty()) Text("No hay equipos registrados para este cliente.", color = TextoSecundario)
        }
    }, confirmButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } })
}

@Composable
private fun SeccionMediciones(vm: HojaCampoViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    fun actualizar(cambio: (com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity) -> com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity) = vm.actualizarMediciones(cambio)
    Campo("L1-L2 (V)", s.mediciones.l1l2, KeyboardType.Decimal) { actualizar { m -> m.copy(l1l2 = it) } }
    Campo("L2-L3 (V)", s.mediciones.l2l3, KeyboardType.Decimal) { actualizar { m -> m.copy(l2l3 = it) } }
    Campo("L3-L1 (V)", s.mediciones.l3l1, KeyboardType.Decimal) { actualizar { m -> m.copy(l3l1 = it) } }
    Campo("LN (V)", s.mediciones.ln, KeyboardType.Decimal) { actualizar { m -> m.copy(ln = it) } }
    Campo("AMP L1", s.mediciones.ampL1, KeyboardType.Decimal) { actualizar { m -> m.copy(ampL1 = it) } }
    Campo("AMP L2", s.mediciones.ampL2, KeyboardType.Decimal) { actualizar { m -> m.copy(ampL2 = it) } }
    Campo("AMP L3", s.mediciones.ampL3, KeyboardType.Decimal) { actualizar { m -> m.copy(ampL3 = it) } }
    Campo("Hz vacío", s.mediciones.hzVacio, KeyboardType.Decimal) { actualizar { m -> m.copy(hzVacio = it) } }
    Campo("Hz con carga", s.mediciones.hzCarga, KeyboardType.Decimal) { actualizar { m -> m.copy(hzCarga = it) } }
    Campo("RPM vacío", s.mediciones.rpmVacio, KeyboardType.Decimal) { actualizar { m -> m.copy(rpmVacio = it) } }
    Campo("RPM con carga", s.mediciones.rpmCarga, KeyboardType.Decimal) { actualizar { m -> m.copy(rpmCarga = it) } }
    Campo("Presión de aceite", s.mediciones.presionAceite, KeyboardType.Decimal) { actualizar { m -> m.copy(presionAceite = it) } }
    Campo("Temperatura del motor (°C)", s.mediciones.temperaturaMotor, KeyboardType.Decimal) { actualizar { m -> m.copy(temperaturaMotor = it) } }
    Campo("Carga del alternador", s.mediciones.cargaAlternador, KeyboardType.Decimal) { actualizar { m -> m.copy(cargaAlternador = it) } }
    Campo("Nivel de aceite", s.mediciones.nivelAceite) { actualizar { m -> m.copy(nivelAceite = it) } }
    Campo("Nivel de refrigerante", s.mediciones.nivelRefrigerante) { actualizar { m -> m.copy(nivelRefrigerante = it) } }
    Campo("Voltaje de batería", s.mediciones.voltajeBateria, KeyboardType.Decimal) { actualizar { m -> m.copy(voltajeBateria = it) } }
    Campo("Combustible (%)", s.mediciones.combustible, KeyboardType.Decimal) { actualizar { m -> m.copy(combustible = it) } }
    Text("CONTROLES", color = RojoCG, modifier = Modifier.padding(top = 12.dp))
    SelectorControl("Limpieza de generador", s.mediciones.limpieza) { valor -> actualizar { m -> m.copy(limpieza = valor) } }
    SelectorControl("Llenado de electrolitos", s.mediciones.electrolitos) { valor -> actualizar { m -> m.copy(electrolitos = valor) } }
    SelectorControl("Mantenedor de batería", s.mediciones.mantenedorBateria) { valor -> actualizar { m -> m.copy(mantenedorBateria = valor) } }
    SelectorControl("Precalentador de block", s.mediciones.precalentadorBlock) { valor -> actualizar { m -> m.copy(precalentadorBlock = valor) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectorControl(titulo: String, valor: String, cambiar: (String) -> Unit) {
    Text(titulo, color = Color.White, modifier = Modifier.padding(top = 10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EstadoControl.entries.forEach { opcion ->
            OutlinedButton(onClick = { cambiar(opcion.name) }) {
                Text(
                    text = when (opcion) {
                        EstadoControl.NO_REVISADO -> "SIN REVISAR"
                        EstadoControl.CORRECTO -> "CORRECTO"
                        EstadoControl.REQUIERE_ATENCION -> "ATENCIÓN"
                        EstadoControl.NO_APLICA -> "N/A"
                    },
                    color = if (valor == opcion.name) RojoCG else Color.White
                )
            }
        }
    }
}

@Composable
private fun SeccionTrabajo(vm: HojaCampoViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    Campo("Trabajos realizados", s.hoja.trabajosRealizados, lineas = 5) { vm.actualizarHoja { h -> h.copy(trabajosRealizados = it) } }
    Campo("Hora inicio de pruebas", s.hoja.horaInicioPruebas) { vm.actualizarHoja { h -> h.copy(horaInicioPruebas = it) } }
    Campo("Hora fin de pruebas", s.hoja.horaFinPruebas) { vm.actualizarHoja { h -> h.copy(horaFinPruebas = it) } }
}

@Composable
private fun SeccionRepuestos(vm: HojaCampoViewModel, repuestos: List<RepuestoUsadoEntity>) {
    var nombre by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var mostrarCatalogo by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { mostrarCatalogo = true }, modifier = Modifier.fillMaxWidth()) {
        Text("SELECCIONAR PRODUCTO DEL CATÁLOGO")
    }
    Campo("Repuesto o descripción", nombre) { nombre = it }
    Campo("Cantidad", cantidad, KeyboardType.Decimal) { cantidad = it }
    Button(onClick = {
        val existente = repuestos.indexOfFirst { it.nombre.equals(nombre.trim(), true) }
        val valor = cantidad.toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
        val nuevos = if (nombre.isBlank()) repuestos else if (existente >= 0) repuestos.mapIndexed { index, item -> if (index == existente) item.copy(cantidad = item.cantidad + valor) else item } else repuestos + RepuestoUsadoEntity(hojaCampoId = 0, nombre = nombre.trim(), cantidad = valor)
        vm.actualizarRepuestos(nuevos); nombre = ""; cantidad = "1"
    }, colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) { Text("AGREGAR REPUESTO") }
    repuestos.forEachIndexed { index, item ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${item.nombre} · ${item.cantidad}", color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.actualizarRepuestos(repuestos.filterIndexed { i, _ -> i != index }) }) { Text("QUITAR", color = RojoCG) }
        }
    }
    if (mostrarCatalogo) SelectorProducto(vm, { mostrarCatalogo = false })
}

@Composable
private fun SelectorCliente(vm: HojaCampoViewModel, cerrar: () -> Unit) {
    val clientes by vm.clientes.collectAsStateWithLifecycle()
    var busqueda by remember { mutableStateOf("") }
    val filtrados = clientes.filter {
        it.nombre.contains(busqueda, true) || it.rucCedula.contains(busqueda, true)
    }
    AlertDialog(
        onDismissRequest = cerrar,
        containerColor = FondoTarjeta,
        title = { Text("Seleccionar cliente", color = Color.White) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Campo("Buscar por nombre o RUC", busqueda) { busqueda = it }
                filtrados.forEach { cliente ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable {
                            vm.seleccionarCliente(cliente)
                            cerrar()
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(cliente.nombre, color = Color.White)
                            if (cliente.rucCedula.isNotBlank()) Text(cliente.rucCedula, color = TextoSecundario)
                        }
                    }
                }
                if (filtrados.isEmpty()) Text("No hay clientes guardados.", color = TextoSecundario, modifier = Modifier.padding(top = 12.dp))
            }
        },
        confirmButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } }
    )
}

@Composable
private fun SelectorProducto(vm: HojaCampoViewModel, cerrar: () -> Unit) {
    val productos by vm.productos.collectAsStateWithLifecycle()
    var busqueda by remember { mutableStateOf("") }
    val filtrados = productos.filter {
        it.nombre.contains(busqueda, true) || it.categoria.contains(busqueda, true)
    }
    AlertDialog(
        onDismissRequest = cerrar,
        containerColor = FondoTarjeta,
        title = { Text("Seleccionar repuesto", color = Color.White) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Campo("Buscar por nombre o categoría", busqueda) { busqueda = it }
                filtrados.forEach { producto ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).clickable {
                            vm.agregarProductoComoRepuesto(producto)
                            cerrar()
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(producto.nombre, color = Color.White)
                            Text(producto.categoria, color = TextoSecundario)
                        }
                    }
                }
                if (filtrados.isEmpty()) Text("No hay productos disponibles.", color = TextoSecundario, modifier = Modifier.padding(top = 12.dp))
            }
        },
        confirmButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } }
    )
}

@Composable
private fun SeccionJornadas(vm: HojaCampoViewModel, jornadas: List<JornadaTrabajoEntity>) {
    var inicio by remember { mutableStateOf("") }
    var fin by remember { mutableStateOf("") }
    var tecnico by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Campo("Hora de inicio", inicio) { inicio = it }
    Campo("Hora de finalización", fin) { fin = it }
    Campo("Técnico(s)", tecnico) { tecnico = it }
    Button(onClick = {
        val validacion = HojaCampoValidaciones.jornada(inicio, fin, tecnico)
        if (validacion == null) {
            vm.actualizarJornadas(jornadas + JornadaTrabajoEntity(hojaCampoId = 0, fecha = System.currentTimeMillis(), horaInicio = inicio, horaFin = fin, minutosTotales = minutosEntre(inicio, fin), tecnicos = tecnico))
            inicio = ""; fin = ""; tecnico = ""; error = null
        } else {
            error = validacion
        }
    }, colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) { Text("AGREGAR JORNADA") }
    error?.let { Text(it, color = RojoCG, modifier = Modifier.padding(top = 6.dp)) }
    jornadas.forEachIndexed { index, jornada ->
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${jornada.horaInicio} - ${jornada.horaFin} · ${jornada.tecnicos}", color = Color.White, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.actualizarJornadas(jornadas.filterIndexed { i, _ -> i != index }) }) { Text("QUITAR", color = RojoCG) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionRevision(vm: HojaCampoViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    Campo("Observaciones", s.hoja.observaciones, lineas = 5) { vm.actualizarHoja { h -> h.copy(observaciones = it) } }
    Campo("Responsable del cliente", s.hoja.nombreClienteResponsable) { vm.actualizarHoja { h -> h.copy(nombreClienteResponsable = it) } }
    Text("Estado", color = Color.White, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EstadoHoja.entries.forEach { opcion ->
            val activo = s.hoja.estado == opcion.name
            OutlinedButton(onClick = { vm.actualizarHoja { h -> h.copy(estado = opcion.name) } }, colors = ButtonDefaults.outlinedButtonColors(contentColor = if (activo) RojoCG else Color.White)) { Text(opcion.name) }
        }
    }
    Text("Las evidencias se guardan de forma privada en este dispositivo.", color = TextoSecundario, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun SeccionEvidencias(vm: HojaCampoViewModel, seleccionarGaleria: () -> Unit, tomarFoto: () -> Unit) {
    val evidencias by vm.evidencias.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editar by remember { mutableStateOf<EvidenciaEntity?>(null) }
    var eliminar by remember { mutableStateOf<EvidenciaEntity?>(null) }
    var visor by remember { mutableStateOf<EvidenciaEntity?>(null) }
    TituloSeccion("EVIDENCIAS FOTOGRÁFICAS (${evidencias.size})")
    Button(onClick = tomarFoto, colors = ButtonDefaults.buttonColors(containerColor = RojoCG), modifier = Modifier.fillMaxWidth()) { Text("TOMAR FOTO") }
    OutlinedButton(onClick = seleccionarGaleria, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("SELECCIONAR DE GALERÍA") }
    evidencias.forEach { evidencia ->
        Card(Modifier.fillMaxWidth().padding(top = 8.dp).clickable { visor = evidencia }, colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))) {
            Row(Modifier.padding(12.dp)) {
                MiniaturaEvidencia(File(context.filesDir, evidencia.rutaInterna))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                Text(evidencia.tipoEvidencia, color = RojoCG)
                Text(evidencia.descripcion.ifBlank { evidencia.nombreArchivo }, color = Color.White)
                Row {
                    TextButton(onClick = { editar = evidencia }) { Text("EDITAR", color = Color.White) }
                    TextButton(onClick = {
                        val archivo = File(context.filesDir, evidencia.rutaInterna)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "image/jpeg"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Compartir evidencia"))
                    }) { Text("COMPARTIR", color = Color.White) }
                    TextButton(onClick = { eliminar = evidencia }) { Text("ELIMINAR", color = RojoCG) }
                }
                }
            }
        }
    }
    visor?.let { evidencia -> VisorEvidencia(evidencia, File(context.filesDir, evidencia.rutaInterna), { visor = null }) }
    editar?.let { evidencia -> DialogoEditarEvidencia(evidencia, { descripcion, tipo -> vm.actualizarEvidencia(evidencia, descripcion, tipo); editar = null }, { editar = null }) }
    eliminar?.let { evidencia -> AlertDialog(onDismissRequest = { eliminar = null }, containerColor = FondoTarjeta, title = { Text("¿Eliminar evidencia?", color = Color.White) }, text = { Text("Esta acción quitará la fotografía de la hoja de campo.", color = Color.White) }, confirmButton = { TextButton(onClick = { vm.eliminarEvidencia(evidencia); eliminar = null }) { Text("ELIMINAR", color = RojoCG) } }, dismissButton = { TextButton(onClick = { eliminar = null }) { Text("CANCELAR", color = Color.White) } }) }
}

@Composable
private fun MiniaturaEvidencia(archivo: File) {
    val bitmap = remember(archivo.path) { BitmapFactory.decodeFile(archivo.path)?.asImageBitmap() }
    if (bitmap == null) {
        Card(Modifier.width(72.dp).height(72.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF333333))) { Text("Sin foto", color = Color.White, modifier = Modifier.padding(8.dp)) }
    } else Image(bitmap = bitmap, contentDescription = "Evidencia", contentScale = ContentScale.Crop, modifier = Modifier.width(72.dp).height(72.dp))
}

@Composable
private fun VisorEvidencia(evidencia: EvidenciaEntity, archivo: File, cerrar: () -> Unit) {
    val bitmap = remember(archivo.path) { BitmapFactory.decodeFile(archivo.path)?.asImageBitmap() }
    AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text(evidencia.tipoEvidencia, color = Color.White) }, text = { Column { if (bitmap == null) Text("La imagen ya no está disponible.", color = RojoCG) else Image(bitmap = bitmap, contentDescription = evidencia.descripcion, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().height(360.dp)); if (evidencia.descripcion.isNotBlank()) Text(evidencia.descripcion, color = Color.White, modifier = Modifier.padding(top = 10.dp)) } }, confirmButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } })
}

@Composable
private fun DialogoEditarEvidencia(evidencia: EvidenciaEntity, guardar: (String, TipoEvidencia) -> Unit, cancelar: () -> Unit) {
    var descripcion by remember { mutableStateOf(evidencia.descripcion) }
    var tipo by remember { mutableStateOf(runCatching { TipoEvidencia.valueOf(evidencia.tipoEvidencia) }.getOrDefault(TipoEvidencia.OTRO)) }
    AlertDialog(onDismissRequest = cancelar, containerColor = FondoTarjeta, title = { Text("Editar evidencia", color = Color.White) }, text = { Column { Campo("Descripción", descripcion) { descripcion = it }; FlowRow { TipoEvidencia.entries.forEach { opcion -> TextButton(onClick = { tipo = opcion }) { Text(opcion.name, color = if (tipo == opcion) RojoCG else Color.White) } } } } }, confirmButton = { TextButton(onClick = { guardar(descripcion, tipo) }) { Text("GUARDAR", color = RojoCG) } }, dismissButton = { TextButton(onClick = cancelar) { Text("CANCELAR", color = Color.White) } })
}

@Composable
private fun DialogoRegistrarEvidencia(guardar: (String, TipoEvidencia) -> Unit, cancelar: () -> Unit) {
    var descripcion by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoEvidencia.OTRO) }
    AlertDialog(
        onDismissRequest = cancelar,
        containerColor = FondoTarjeta,
        title = { Text("Registrar evidencia", color = Color.White) },
        text = {
            Column {
                Campo("Descripción", descripcion) { descripcion = it }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TipoEvidencia.entries.forEach { opcion ->
                        OutlinedButton(onClick = { tipo = opcion }, colors = ButtonDefaults.outlinedButtonColors(contentColor = if (tipo == opcion) RojoCG else Color.White)) { Text(opcion.name) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { guardar(descripcion, tipo) }) { Text("GUARDAR", color = RojoCG) } },
        dismissButton = { TextButton(onClick = cancelar) { Text("CANCELAR", color = Color.White) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionEvidenciasMejorada(
    vm: HojaCampoViewModel,
    seleccionarGaleria: () -> Unit,
    tomarFoto: () -> Unit
) {
    val evidencias by vm.evidencias.collectAsStateWithLifecycle()
    val estado by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editar by remember { mutableStateOf<EvidenciaEntity?>(null) }
    var eliminar by remember { mutableStateOf<EvidenciaEntity?>(null) }
    var visorId by remember { mutableStateOf<Long?>(null) }

    TituloSeccion("EVIDENCIAS FOTOGRÁFICAS (${evidencias.size})")
    Button(onClick = tomarFoto, colors = ButtonDefaults.buttonColors(containerColor = RojoCG), modifier = Modifier.fillMaxWidth()) {
        Text("TOMAR FOTO")
    }
    OutlinedButton(onClick = seleccionarGaleria, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text("SELECCIONAR DE GALERÍA")
    }
    if (evidencias.isEmpty()) {
        Text("Aún no hay fotografías adjuntas.", color = TextoSecundario, modifier = Modifier.padding(top = 12.dp))
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            evidencias.forEachIndexed { indice, evidencia ->
                val archivo = File(context.filesDir, evidencia.rutaInterna)
                Card(
                    modifier = Modifier.width(156.dp).clickable { visorId = evidencia.id },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))
                ) {
                    Column(Modifier.padding(8.dp)) {
                        AsyncImage(
                            model = archivo,
                            contentDescription = evidencia.descripcion.ifBlank { "Evidencia ${indice + 1}" },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(112.dp)
                        )
                        Text(evidencia.tipoEvidencia, color = RojoCG)
                        Text(
                            evidencia.descripcion.ifBlank { evidencia.nombreArchivo },
                            color = Color.White,
                            maxLines = 2
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(onClick = { vm.moverEvidencia(evidencia, -1) }, enabled = indice > 0) { Text("↑", color = Color.White) }
                            TextButton(onClick = { vm.moverEvidencia(evidencia, 1) }, enabled = indice < evidencias.lastIndex) { Text("↓", color = Color.White) }
                            TextButton(onClick = { editar = evidencia }) { Text("EDITAR", color = Color.White) }
                            TextButton(onClick = { compartirEvidencia(context, archivo, estado.hoja.numeroHoja, evidencia.descripcion) }) { Text("COMPARTIR", color = Color.White) }
                            TextButton(onClick = { eliminar = evidencia }) { Text("ELIMINAR", color = RojoCG) }
                        }
                    }
                }
            }
        }
    }

    visorId?.let { id -> VisorEvidencias(
        evidencias = evidencias,
        inicial = evidencias.indexOfFirst { it.id == id }.coerceAtLeast(0),
        numeroHoja = estado.hoja.numeroHoja,
        cerrar = { visorId = null }
    ) }
    editar?.let { evidencia ->
        DialogoEditarEvidencia(evidencia, { descripcion, tipo ->
            vm.actualizarEvidencia(evidencia, descripcion, tipo)
            editar = null
        }, { editar = null })
    }
    eliminar?.let { evidencia ->
        AlertDialog(
            onDismissRequest = { eliminar = null },
            containerColor = FondoTarjeta,
            title = { Text("¿Eliminar evidencia?", color = Color.White) },
            text = { Text("Esta acción quitará la fotografía de la hoja de campo.", color = Color.White) },
            confirmButton = { TextButton(onClick = { vm.eliminarEvidencia(evidencia); eliminar = null }) { Text("ELIMINAR", color = RojoCG) } },
            dismissButton = { TextButton(onClick = { eliminar = null }) { Text("CANCELAR", color = Color.White) } }
        )
    }
}

@Composable
private fun VisorEvidencias(
    evidencias: List<EvidenciaEntity>,
    inicial: Int,
    numeroHoja: String,
    cerrar: () -> Unit
) {
    val context = LocalContext.current
    var indice by remember(inicial, evidencias.size) { mutableIntStateOf(inicial) }
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamientoX by remember { mutableFloatStateOf(0f) }
    var desplazamientoY by remember { mutableFloatStateOf(0f) }
    val evidencia = evidencias.getOrNull(indice) ?: return
    val archivo = File(context.filesDir, evidencia.rutaInterna)
    val transformacion = rememberTransformableState { zoom, desplazamiento, _ ->
        escala = (escala * zoom).coerceIn(1f, 4f)
        desplazamientoX += desplazamiento.x
        desplazamientoY += desplazamiento.y
    }

    AlertDialog(
        onDismissRequest = cerrar,
        containerColor = FondoTarjeta,
        title = { Text("$numeroHoja · ${indice + 1} de ${evidencias.size}", color = Color.White) },
        text = {
            Column {
                if (!archivo.exists()) {
                    Text("La imagen ya no está disponible.", color = RojoCG)
                } else {
                    AsyncImage(
                        model = archivo,
                        contentDescription = evidencia.descripcion,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(360.dp)
                            .transformable(transformacion)
                            .graphicsLayer(
                                scaleX = escala,
                                scaleY = escala,
                                translationX = desplazamientoX,
                                translationY = desplazamientoY
                            )
                    )
                }
                Text(evidencia.tipoEvidencia, color = RojoCG, modifier = Modifier.padding(top = 8.dp))
                if (evidencia.descripcion.isNotBlank()) Text(evidencia.descripcion, color = Color.White)
                Text(formatoFecha(evidencia.fechaHoraCaptura), color = TextoSecundario)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { indice--; escala = 1f; desplazamientoX = 0f; desplazamientoY = 0f }, enabled = indice > 0) { Text("ANTERIOR", color = Color.White) }
                    TextButton(onClick = { indice++; escala = 1f; desplazamientoX = 0f; desplazamientoY = 0f }, enabled = indice < evidencias.lastIndex) { Text("SIGUIENTE", color = Color.White) }
                }
            }
        },
        confirmButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } }
    )
}

private fun compartirEvidencia(context: android.content.Context, archivo: File, numeroHoja: String, descripcion: String) {
    if (!archivo.exists()) {
        Toast.makeText(context, "La imagen ya no está disponible.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        context.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Evidencia $numeroHoja")
                putExtra(Intent.EXTRA_TEXT, listOf("Evidencia de $numeroHoja", descripcion.takeIf { it.isNotBlank() }).filterNotNull().joinToString("\n"))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            "Compartir evidencia"
        ))
    }.onFailure {
        Toast.makeText(context, "No hay una aplicación compatible para compartir.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun Campo(etiqueta: String, valor: String, teclado: KeyboardType = KeyboardType.Text, lineas: Int = 1, alCambiar: (String) -> Unit) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        minLines = lineas,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = RojoCG, unfocusedLabelColor = Color.White, cursorColor = RojoCG),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun TituloSeccion(texto: String) { Text(texto, color = RojoCG, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraHoja(titulo: String, alVolver: () -> Unit) {
    Column {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White, navigationIconContentColor = Color.White),
            title = { Text(titulo, color = Color.White) },
            navigationIcon = { TextButton(onClick = alVolver) { Text("←", color = Color.White) } }
        )
        HorizontalDivider(thickness = 1.dp, color = RojoCG.copy(alpha = .6f))
    }
}

private fun formatoFecha(fecha: Long): String = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(fecha))

private fun minutosEntre(inicio: String, fin: String): Long {
    fun minutos(hora: String): Int? {
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
        val m = partes.getOrNull(1)?.toIntOrNull() ?: return null
        return if (h in 0..23 && m in 0..59) h * 60 + m else null
    }
    val desde = minutos(inicio) ?: return 0
    val hasta = minutos(fin) ?: return 0
    return (hasta - desde).takeIf { it >= 0 }?.toLong() ?: 0
}
