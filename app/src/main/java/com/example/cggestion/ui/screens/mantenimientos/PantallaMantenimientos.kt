package com.example.cggestion.ui.screens.mantenimientos

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.EquipoEntity
import com.example.cggestion.data.local.entity.EstadoMantenimiento
import com.example.cggestion.data.local.entity.MantenimientoEntity
import com.example.cggestion.data.local.entity.MantenimientoResumen
import com.example.cggestion.data.local.entity.PrioridadMantenimiento
import com.example.cggestion.data.local.entity.TipoMantenimiento
import com.example.cggestion.viewmodel.ContextoMantenimientoHoja
import com.example.cggestion.viewmodel.MantenimientoViewModel
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private enum class Filtro { TODOS, PROXIMOS, VENCIDOS, PENDIENTES, EN_PROCESO, COMPLETADOS, CANCELADOS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMantenimientos(viewModel: MantenimientoViewModel, volver: () -> Unit, crearHoja: (ContextoMantenimientoHoja) -> Unit) {
    val lista by viewModel.mantenimientos.collectAsStateWithLifecycle()
    val evento by viewModel.eventoHoja.collectAsStateWithLifecycle()
    var busqueda by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(Filtro.TODOS) }
    var editor by remember { mutableStateOf<MantenimientoEntity?>(null) }
    val hoy = inicioDia(System.currentTimeMillis())
    val filtrados = lista.filter { item ->
        val coincide = busqueda.isBlank() || listOf(item.clienteNombre, item.equipoNombre, item.descripcion, item.tipo).any { it.contains(busqueda, true) }
        coincide && when (filtro) {
            Filtro.TODOS -> true
            Filtro.PROXIMOS -> item.estado in listOf("PENDIENTE", "EN_PROCESO") && item.fechaProgramada in hoy..(hoy + 7 * DIA)
            Filtro.VENCIDOS -> item.estado in listOf("PENDIENTE", "EN_PROCESO") && item.fechaProgramada < hoy
            Filtro.PENDIENTES -> item.estado == "PENDIENTE"
            Filtro.EN_PROCESO -> item.estado == "EN_PROCESO"
            Filtro.COMPLETADOS -> item.estado == "COMPLETADO"
            Filtro.CANCELADOS -> item.estado == "CANCELADO"
        }
    }
    LaunchedEffect(evento) { evento?.let { crearHoja(it); viewModel.consumirEventoHoja() } }
    Scaffold(containerColor = FondoPrincipal, topBar = { Column { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White), navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White) } }, title = { Text("MANTENIMIENTOS") }); HorizontalDivider(color = RojoCG.copy(alpha = .6f)) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            val vencidos = lista.count { it.estado in listOf("PENDIENTE", "EN_PROCESO") && it.fechaProgramada < hoy }
            val proximos = lista.count { it.estado in listOf("PENDIENTE", "EN_PROCESO") && it.fechaProgramada in hoy..(hoy + 7 * DIA) }
            Text("$vencidos vencido(s) · $proximos próximo(s) en 7 días", color = if (vencidos > 0) RojoCG else TextoSecundario)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { viewModel.nuevo(); editor = MantenimientoEntity(clienteId = 0, equipoId = 0, fechaProgramada = System.currentTimeMillis()) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) { Text("NUEVO MANTENIMIENTO", color = Color.White) }
            Campo(busqueda, "Buscar cliente, equipo o servicio") { busqueda = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(Filtro.TODOS, Filtro.PROXIMOS, Filtro.VENCIDOS).forEach { opcion -> TextButton(onClick = { filtro = opcion }) { Text(opcion.name, color = if (filtro == opcion) RojoCG else Color.White) } }
            }
            LazyColumn(Modifier.weight(1f)) {
                items(filtrados, key = { it.id }) { item -> Tarjeta(item, hoy, abrir = { viewModel.editar(it); editor = it }, iniciarHoja = viewModel::iniciarHoja, cambiarEstado = { estado -> viewModel.cambiarEstado(item.id, estado) }) }
                if (filtrados.isEmpty()) item { Text("No hay mantenimientos para este filtro.", color = TextoSecundario, modifier = Modifier.padding(top = 18.dp)) }
            }
        }
    }
    editor?.let { Editor(viewModel, cerrar = { editor = null }) }
}

@Composable private fun Tarjeta(item: MantenimientoResumen, hoy: Long, abrir: (MantenimientoEntity) -> Unit, iniciarHoja: (Long) -> Unit, cambiarEstado: (EstadoMantenimiento) -> Unit) {
    val estadoVisual = if (item.estado in listOf("PENDIENTE", "EN_PROCESO") && item.fechaProgramada < hoy) "VENCIDO" else item.estado
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) { Column(Modifier.padding(13.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(item.tipo, color = RojoCG); Text(estadoVisual, color = if (estadoVisual == "VENCIDO") RojoCG else Color.White) }
        Text(item.descripcion, color = Color.White); Text("${item.clienteNombre} · ${item.equipoNombre}", color = TextoSecundario)
        Text("Programado: ${fecha(item.fechaProgramada)} · ${item.prioridad}", color = TextoSecundario)
        Row { TextButton(onClick = { abrir(item.aEntidad()) }) { Text("EDITAR", color = Color.White) }; if (item.hojaCampoId == null && item.estado !in listOf("COMPLETADO", "CANCELADO")) TextButton(onClick = { iniciarHoja(item.id) }) { Text("INICIAR HOJA", color = RojoCG) }; if (item.estado == "PENDIENTE") TextButton(onClick = { cambiarEstado(EstadoMantenimiento.CANCELADO) }) { Text("CANCELAR", color = TextoSecundario) } }
    } }
}

@Composable private fun Editor(vm: MantenimientoViewModel, cerrar: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle(); val clientes by vm.clientes.collectAsStateWithLifecycle(); val equipos by vm.equipos.collectAsStateWithLifecycle(); val context = LocalContext.current
    var clientesAbiertos by remember { mutableStateOf(false) }; var equiposAbiertos by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text(if (ui.mantenimiento.id == 0L) "Nuevo mantenimiento" else "Editar mantenimiento", color = Color.White) }, text = { Column {
        Selector("Cliente", clientes.firstOrNull { it.id == ui.mantenimiento.clienteId }?.nombre ?: "Seleccionar", clientesAbiertos, { clientesAbiertos = !clientesAbiertos }) { clientes.forEach { cliente -> TextButton(onClick = { vm.seleccionarCliente(cliente); clientesAbiertos = false }) { Text(cliente.nombre, color = Color.White) } } }
        Selector("Equipo", equipos.firstOrNull { it.id == ui.mantenimiento.equipoId }?.let { "${it.marca} ${it.modelo}" } ?: "Seleccionar", equiposAbiertos, { equiposAbiertos = !equiposAbiertos }) { equipos.forEach { equipo -> TextButton(onClick = { vm.seleccionarEquipo(equipo); equiposAbiertos = false }) { Text("${equipo.marca} ${equipo.modelo}", color = Color.White) } } }
        Campo(ui.mantenimiento.descripcion, "Descripción del servicio") { texto -> vm.actualizar { actual -> actual.copy(descripcion = texto) } }
        SelectorEnum("Tipo", ui.mantenimiento.tipo, TipoMantenimiento.entries.map { it.name }) { nuevo -> vm.actualizar { it.copy(tipo = nuevo) } }
        SelectorEnum("Prioridad", ui.mantenimiento.prioridad, PrioridadMantenimiento.entries.map { it.name }) { nuevo -> vm.actualizar { it.copy(prioridad = nuevo) } }
        TextButton(onClick = { selectorFecha(context, ui.mantenimiento.fechaProgramada) { fecha -> vm.actualizar { it.copy(fechaProgramada = fecha) } } }) { Text("Fecha: ${fecha(ui.mantenimiento.fechaProgramada)}", color = RojoCG) }
        Campo(ui.mantenimiento.periodicidadDias?.toString().orEmpty(), "Periodicidad en días (opcional)", KeyboardType.Number) { valor -> vm.actualizar { it.copy(periodicidadDias = valor.toIntOrNull()?.takeIf { dias -> dias > 0 }) } }
        ui.mensaje?.let { Text(it, color = RojoCG) }
    } }, confirmButton = { Button(onClick = { vm.guardar(cerrar) }, enabled = !ui.guardando, colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) { Text(if (ui.guardando) "GUARDANDO…" else "GUARDAR") } }, dismissButton = { TextButton(onClick = cerrar) { Text("CANCELAR", color = Color.White) } })
}

@Composable private fun Selector(etiqueta: String, valor: String, abierto: Boolean, alternar: () -> Unit, contenido: @Composable () -> Unit) { Text("$etiqueta: $valor", color = TextoSecundario); TextButton(onClick = alternar) { Text("SELECCIONAR", color = RojoCG) }; if (abierto) Column { contenido() } }
@Composable private fun SelectorEnum(etiqueta: String, valor: String, opciones: List<String>, cambiar: (String) -> Unit) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("$etiqueta: $valor", color = Color.White); opciones.filter { it != valor }.forEach { TextButton(onClick = { cambiar(it) }) { Text(it.take(3), color = RojoCG) } } } }
@Composable private fun Campo(valor: String, etiqueta: String, teclado: KeyboardType = KeyboardType.Text, cambio: (String) -> Unit) { OutlinedTextField(value = valor, onValueChange = cambio, label = { Text(etiqueta) }, keyboardOptions = KeyboardOptions(keyboardType = teclado), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RojoCG, unfocusedBorderColor = TextoSecundario, focusedLabelColor = RojoCG, unfocusedLabelColor = TextoSecundario)) }
private fun MantenimientoResumen.aEntidad() = MantenimientoEntity(id, clienteId, equipoId, tipo, descripcion, prioridad, fechaProgramada, periodicidadDias, estado, hojaCampoId, fechaCreacion, fechaModificacion)
private fun fecha(valor: Long) = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(valor)); private const val DIA = 86_400_000L
private fun inicioDia(valor: Long): Long = Calendar.getInstance().apply { timeInMillis = valor; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun selectorFecha(context: android.content.Context, inicial: Long, guardar: (Long) -> Unit) { val calendario = Calendar.getInstance().apply { timeInMillis = inicial }; DatePickerDialog(context, { _, ano, mes, dia -> guardar(Calendar.getInstance().apply { set(ano, mes, dia, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis) }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)).show() }
