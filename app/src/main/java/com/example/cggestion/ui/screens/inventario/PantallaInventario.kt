package com.example.cggestion.ui.screens.inventario

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.data.local.entity.ProductoEntity
import com.example.cggestion.viewmodel.InventarioViewModel
import java.util.Locale
import java.text.DateFormat
import java.util.Date

private enum class FiltroInventario { TODOS, ACTIVOS, INACTIVOS, STOCK_BAJO }
private data class ProductoEditado(val producto: ProductoEntity, val stockInicial: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaInventario(viewModel: InventarioViewModel, volver: () -> Unit) {
    val productos by viewModel.productos.collectAsStateWithLifecycle()
    val productosBajoMinimo by viewModel.productosBajoMinimo.collectAsStateWithLifecycle()
    var busqueda by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroInventario.ACTIVOS) }
    var productoEnEdicion by remember { mutableStateOf<ProductoEntity?>(null) }
    var productoEnMovimientoId by remember { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val filtrados = productos.filter { producto ->
        val coincideFiltro = when (filtro) {
            FiltroInventario.TODOS -> true
            FiltroInventario.ACTIVOS -> producto.activo
            FiltroInventario.INACTIVOS -> !producto.activo
            FiltroInventario.STOCK_BAJO -> producto.activo && producto.stockMinimo > 0 && producto.stockActual <= producto.stockMinimo
        }
        coincideFiltro && (producto.nombre.contains(busqueda, true) || producto.categoria.contains(busqueda, true) || producto.codigo.orEmpty().contains(busqueda, true))
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
                    title = { Text("INVENTARIO") },
                    navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White) } }
                )
                HorizontalDivider(color = RojoCG.copy(alpha = 0.6f))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Button(
                onClick = { productoEnEdicion = ProductoEntity(nombre = "", categoria = "") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RojoCG)
            ) { Text("NUEVO PRODUCTO", color = Color.White) }

            Spacer(Modifier.height(8.dp))
            CampoOscuro(busqueda, { busqueda = it }, "Buscar producto, código o categoría", modifier = Modifier.fillMaxWidth())
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                FiltroInventario.entries.forEach { opcion ->
                    OutlinedButton(onClick = { filtro = opcion }) {
                        Text(opcion.name, color = if (filtro == opcion) RojoCG else Color.White)
                    }
                }
            }
            if (productosBajoMinimo.isNotEmpty()) {
                Text("${productosBajoMinimo.size} producto(s) requieren reposición.", color = RojoCG, modifier = Modifier.padding(top = 8.dp))
            }

            if (filtrados.isEmpty()) {
                Text("No se encontraron productos.", color = TextoSecundario, modifier = Modifier.padding(top = 20.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtrados, key = { it.id }) { producto ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(producto.nombre, color = Color.White)
                                        Text(
                                            "${producto.categoria} · ${formatoMoneda(producto.precioPredeterminadoCentavos)}",
                                            color = TextoSecundario
                                        )
                                        producto.codigo?.takeIf { it.isNotBlank() }?.let { Text("Código: $it", color = TextoSecundario) }
                                        Text("Stock: ${formatoCantidad(producto.stockActual)} ${producto.unidad}", color = Color.White)
                                        if (producto.stockMinimo > 0) Text(
                                            "Mínimo: ${formatoCantidad(producto.stockMinimo)} ${producto.unidad}",
                                            color = if (producto.stockActual <= producto.stockMinimo) RojoCG else TextoSecundario
                                        )
                                    }
                                    Column {
                                        TextButton(onClick = { productoEnEdicion = producto }) { Text("EDITAR", color = Color.White) }
                                        TextButton(onClick = { viewModel.alternar(producto) { error = it } }) {
                                            Text(if (producto.activo) "ACTIVO" else "INACTIVO", color = RojoCG)
                                        }
                                    }
                                }
                                TextButton(onClick = { productoEnMovimientoId = producto.id }) {
                                    Text("MOVIMIENTOS", color = RojoCG)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    productoEnEdicion?.let { producto ->
        EditorProducto(
            producto = producto,
            cerrar = { productoEnEdicion = null },
            guardar = { actualizado ->
                viewModel.guardar(actualizado.producto, actualizado.stockInicial, { error = it }) { productoEnEdicion = null }
            }
        )
    }
    productoEnMovimientoId?.let { id ->
        productos.firstOrNull { it.id == id }?.let { producto ->
            DialogoMovimiento(
                producto = producto,
                viewModel = viewModel,
                cerrar = { productoEnMovimientoId = null; viewModel.limpiarMovimientos() },
                error = { error = it }
            )
        }
    }
    error?.let { mensaje ->
        AlertDialog(
            onDismissRequest = { error = null },
            containerColor = FondoTarjeta,
            title = { Text("Inventario", color = Color.White) },
            text = { Text(mensaje, color = Color.White) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("ACEPTAR", color = RojoCG) } }
        )
    }
}

@Composable
private fun EditorProducto(producto: ProductoEntity, cerrar: () -> Unit, guardar: (ProductoEditado) -> Unit) {
    var nombre by remember(producto.id) { mutableStateOf(producto.nombre) }
    var codigo by remember(producto.id) { mutableStateOf(producto.codigo.orEmpty()) }
    var categoria by remember(producto.id) { mutableStateOf(producto.categoria) }
    var unidad by remember(producto.id) { mutableStateOf(producto.unidad) }
    var precio by remember(producto.id) { mutableStateOf((producto.precioPredeterminadoCentavos / 100.0).toString()) }
    var stockMinimo by remember(producto.id) { mutableStateOf(producto.stockMinimo.toString()) }
    var stockInicial by remember(producto.id) { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = cerrar,
        containerColor = FondoTarjeta,
        title = { Text("Producto", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoOscuro(nombre, { nombre = it }, "Nombre")
                CampoOscuro(codigo, { codigo = it }, "Código (opcional)")
                CampoOscuro(categoria, { categoria = it }, "Categoría")
                CampoOscuro(unidad, { unidad = it }, "Unidad")
                CampoOscuro(precio, { precio = it }, "Precio", KeyboardType.Decimal)
                CampoOscuro(stockMinimo, { stockMinimo = it }, "Stock mínimo", KeyboardType.Decimal)
                if (producto.id == 0L) CampoOscuro(stockInicial, { stockInicial = it }, "Stock inicial", KeyboardType.Decimal)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val centavos = ((precio.trim().replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).toLong()
                guardar(ProductoEditado(producto.copy(
                    codigo = codigo.trim().ifBlank { null },
                    nombre = nombre,
                    categoria = categoria,
                    unidad = unidad.trim().ifBlank { "Unidad" },
                    precioPredeterminadoCentavos = centavos,
                    stockMinimo = stockMinimo.trim().replace(',', '.').toDoubleOrNull() ?: -1.0
                ), stockInicial))
            }) { Text("GUARDAR", color = RojoCG) }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("CANCELAR", color = Color.White) } }
    )
}

@Composable
private fun DialogoMovimiento(
    producto: ProductoEntity,
    viewModel: InventarioViewModel,
    cerrar: () -> Unit,
    error: (String) -> Unit
) {
    var cantidad by remember(producto.id) { mutableStateOf("") }
    var observacion by remember(producto.id) { mutableStateOf("") }
    var tipo by remember(producto.id) { mutableStateOf("ENTRADA") }
    val movimientos by viewModel.movimientos.collectAsStateWithLifecycle()

    LaunchedEffect(producto.id) { viewModel.cargarMovimientos(producto.id) }

    AlertDialog(
        onDismissRequest = cerrar,
        containerColor = FondoTarjeta,
        title = { Text("Movimientos · ${producto.nombre}", color = Color.White) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Stock actual: ${formatoCantidad(producto.stockActual)} ${producto.unidad}", color = Color.White)
                Row {
                    TextButton(onClick = { tipo = "ENTRADA" }) { Text("ENTRADA", color = if (tipo == "ENTRADA") RojoCG else Color.White) }
                    TextButton(onClick = { tipo = "SALIDA" }) { Text("SALIDA", color = if (tipo == "SALIDA") RojoCG else Color.White) }
                    TextButton(onClick = { tipo = "AJUSTE" }) { Text("AJUSTE", color = if (tipo == "AJUSTE") RojoCG else Color.White) }
                }
                Text(
                    if (tipo == "AJUSTE") "El ajuste establece el nuevo stock físico." else "La cantidad se suma o resta del stock actual.",
                    color = TextoSecundario
                )
                CampoOscuro(cantidad, { cantidad = it }, if (tipo == "AJUSTE") "Nuevo stock" else "Cantidad", KeyboardType.Decimal)
                CampoOscuro(observacion, { observacion = it }, "Observación")
                Text("KARDEX", color = RojoCG, modifier = Modifier.padding(top = 8.dp))
                if (movimientos.isEmpty()) Text("Sin movimientos registrados.", color = TextoSecundario)
                movimientos.forEach {
                    val signo = if (it.cantidad > 0) "+" else ""
                    Text(
                        "${formatoFecha(it.fecha)} · ${it.tipo}: $signo${formatoCantidad(it.cantidad)} · Saldo ${formatoCantidad(it.stockResultante)}${if (it.observacion.isBlank()) "" else "\n${it.observacion}"}",
                        color = TextoSecundario
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.registrarMovimiento(producto, cantidad, tipo, observacion, error) {
                    cantidad = ""
                    observacion = ""
                }
            }) { Text("REGISTRAR", color = RojoCG) }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = Color.White) } }
    )
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

private fun formatoMoneda(centavos: Long) = "$" + String.format(Locale.US, "%.2f", centavos / 100.0)

private fun formatoCantidad(valor: Double) = String.format(Locale.US, "%.2f", valor)
private fun formatoFecha(valor: Long) = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(valor))
