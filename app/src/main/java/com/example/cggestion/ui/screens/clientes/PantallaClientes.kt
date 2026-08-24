package com.example.cggestion.ui.screens.clientes

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.cggestion.viewmodel.ClientesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaClientes(viewModel: ClientesViewModel, volver: () -> Unit) {
    val clientes by viewModel.clientes.collectAsStateWithLifecycle()
    var busqueda by remember { mutableStateOf("") }
    var clienteEnEdicion by remember { mutableStateOf<ClienteEntity?>(null) }
    var clienteDetalle by remember { mutableStateOf<ClienteEntity?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val filtrados = clientes.filter {
        it.nombre.contains(busqueda, ignoreCase = true) ||
            it.rucCedula.contains(busqueda, ignoreCase = true)
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
                    title = { Text("CLIENTES") },
                    navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White) } }
                )
                HorizontalDivider(color = RojoCG.copy(alpha = 0.6f))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Button(
                onClick = { clienteEnEdicion = ClienteEntity(nombre = "") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RojoCG)
            ) { Text("NUEVO CLIENTE", color = Color.White) }

            Spacer(Modifier.height(10.dp))
            CampoOscuro(
                value = busqueda,
                onValueChange = { busqueda = it },
                label = "Buscar nombre o RUC",
                modifier = Modifier.fillMaxWidth()
            )

            if (filtrados.isEmpty()) {
                Text(
                    text = if (busqueda.isBlank()) "Todavía no hay clientes registrados." else "No se encontraron clientes.",
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    items(filtrados, key = { it.id }) { cliente ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(cliente.nombre, color = Color.White)
                                    val detalle = cliente.rucCedula.ifBlank { cliente.telefono }
                                    if (detalle.isNotBlank()) Text(detalle, color = TextoSecundario)
                                }
                                TextButton(onClick = { clienteEnEdicion = cliente }) {
                                    Text("EDITAR", color = RojoCG)
                                }
                                TextButton(onClick = { clienteDetalle = cliente }) {
                                    Text("EQUIPOS", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    clienteEnEdicion?.let { cliente ->
        EditorCliente(
            cliente = cliente,
            cerrar = { clienteEnEdicion = null },
            guardar = { actualizado ->
                viewModel.guardar(
                    cliente = actualizado,
                    alError = { error = it },
                    alExito = { clienteEnEdicion = null }
                )
            }
        )
    }
    clienteDetalle?.let { cliente -> DetalleCliente(cliente, viewModel, { clienteDetalle = null }, { error = it }) }

    error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = { error = null },
            containerColor = FondoTarjeta,
            title = { Text("Clientes", color = Color.White) },
            text = { Text(mensaje, color = Color.White) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("ACEPTAR", color = RojoCG) } }
        )
    }
}

@Composable
private fun EditorCliente(cliente: ClienteEntity, cerrar: () -> Unit, guardar: (ClienteEntity) -> Unit) {
    var nombre by remember(cliente.id) { mutableStateOf(cliente.nombre) }
    var rucCedula by remember(cliente.id) { mutableStateOf(cliente.rucCedula) }
    var telefono by remember(cliente.id) { mutableStateOf(cliente.telefono) }
    var direccion by remember(cliente.id) { mutableStateOf(cliente.direccion) }
    var correo by remember(cliente.id) { mutableStateOf(cliente.correoElectronico.orEmpty()) }

    AlertDialog(
        onDismissRequest = cerrar,
        containerColor = FondoTarjeta,
        title = { Text("Cliente", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoOscuro(nombre, { nombre = it }, "Nombre o empresa")
                CampoOscuro(rucCedula, { rucCedula = it }, "RUC o cédula")
                CampoOscuro(telefono, { telefono = it }, "Teléfono", KeyboardType.Phone)
                CampoOscuro(direccion, { direccion = it }, "Dirección")
                CampoOscuro(correo, { correo = it }, "Correo electrónico", KeyboardType.Email)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                guardar(cliente.copy(
                    nombre = nombre,
                    rucCedula = rucCedula,
                    telefono = telefono,
                    direccion = direccion,
                    correoElectronico = correo.trim().ifBlank { null }
                ))
            }) { Text("GUARDAR", color = RojoCG) }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("CANCELAR", color = Color.White) } }
    )
}

@Composable
private fun DetalleCliente(cliente: ClienteEntity, viewModel: ClientesViewModel, cerrar: () -> Unit, error: (String) -> Unit) {
    val equipos by viewModel.equipos(cliente.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var editor by remember { mutableStateOf<EquipoEntity?>(null) }
    var historial by remember { mutableStateOf<EquipoEntity?>(null) }
    AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text(cliente.nombre, color = Color.White) }, text = {
        Column {
            Text("EQUIPOS", color = RojoCG)
            Button(onClick = { editor = EquipoEntity(clienteId = cliente.id) }, colors = ButtonDefaults.buttonColors(containerColor = RojoCG), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("NUEVO EQUIPO") }
            LazyColumn(modifier = Modifier.height(260.dp).padding(top = 8.dp)) {
                items(equipos, key = { it.id }) { equipo ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text("${equipo.marca} ${equipo.modelo}".trim(), color = Color.White); Text("Serie: ${equipo.serie.orEmpty().ifBlank { "Sin serie" }}", color = TextoSecundario) }
                            Column { TextButton(onClick = { editor = equipo }) { Text("EDITAR", color = RojoCG) }; TextButton(onClick = { historial = equipo }) { Text("HISTORIAL", color = Color.White) } }
                        }
                    }
                }
            }
            if (equipos.isEmpty()) Text("No hay equipos registrados.", color = TextoSecundario)
        }
    }, confirmButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } })
    editor?.let { equipo -> EditorEquipo(equipo, { editor = null }, { actualizado -> viewModel.guardarEquipo(actualizado, error) { editor = null } }) }
    historial?.let { equipo -> HistorialEquipo(equipo, viewModel) { historial = null } }
}

@Composable
private fun EditorEquipo(equipo: EquipoEntity, cerrar: () -> Unit, guardar: (EquipoEntity) -> Unit) {
    var marca by remember(equipo.id) { mutableStateOf(equipo.marca) }; var modelo by remember(equipo.id) { mutableStateOf(equipo.modelo) }; var serie by remember(equipo.id) { mutableStateOf(equipo.serie.orEmpty()) }; var kva by remember(equipo.id) { mutableStateOf(equipo.potenciaKva) }; var motorMarca by remember(equipo.id) { mutableStateOf(equipo.motorMarca) }; var motorModelo by remember(equipo.id) { mutableStateOf(equipo.motorModelo) }; var alternadorMarca by remember(equipo.id) { mutableStateOf(equipo.alternadorMarca) }; var alternadorModelo by remember(equipo.id) { mutableStateOf(equipo.alternadorModelo) }; var ubicacion by remember(equipo.id) { mutableStateOf(equipo.ubicacion) }
    AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text("Equipo", color = Color.White) }, text = { Column { CampoOscuro(marca, { marca = it }, "Marca"); CampoOscuro(modelo, { modelo = it }, "Modelo"); CampoOscuro(serie, { serie = it }, "Serie"); CampoOscuro(kva, { kva = it }, "Potencia KVA", KeyboardType.Decimal); CampoOscuro(motorMarca, { motorMarca = it }, "Marca motor"); CampoOscuro(motorModelo, { motorModelo = it }, "Modelo motor"); CampoOscuro(alternadorMarca, { alternadorMarca = it }, "Marca alternador"); CampoOscuro(alternadorModelo, { alternadorModelo = it }, "Modelo alternador"); CampoOscuro(ubicacion, { ubicacion = it }, "Ubicación") } }, confirmButton = { TextButton(onClick = { guardar(equipo.copy(marca = marca.trim(), modelo = modelo.trim(), serie = serie.trim().ifBlank { null }, potenciaKva = kva.trim(), motorMarca = motorMarca.trim(), motorModelo = motorModelo.trim(), alternadorMarca = alternadorMarca.trim(), alternadorModelo = alternadorModelo.trim(), ubicacion = ubicacion.trim())) }) { Text("GUARDAR", color = RojoCG) } }, dismissButton = { TextButton(onClick = cerrar) { Text("CANCELAR", color = Color.White) } })
}

@Composable
private fun HistorialEquipo(equipo: EquipoEntity, viewModel: ClientesViewModel, cerrar: () -> Unit) {
    val hojas by viewModel.hojasEquipo(equipo.id).collectAsStateWithLifecycle(initialValue = emptyList())
    AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text("Historial · ${equipo.serie.orEmpty().ifBlank { equipo.modelo }}", color = Color.White) }, text = { Column { if (hojas.isEmpty()) Text("No hay hojas vinculadas.", color = TextoSecundario); hojas.forEach { hoja -> Text("${hoja.numeroHoja} · ${hoja.estado}", color = Color.White, modifier = Modifier.padding(vertical = 4.dp)) } } }, confirmButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } })
}

@Composable
private fun CampoOscuro(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = RojoCG,
            unfocusedLabelColor = TextoSecundario,
            focusedBorderColor = RojoCG,
            unfocusedBorderColor = TextoSecundario
        )
    )
}
