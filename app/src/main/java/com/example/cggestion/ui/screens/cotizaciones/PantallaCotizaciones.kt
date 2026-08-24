package com.example.cggestion.ui.screens.cotizaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.data.ItemCotizacion
import com.example.cggestion.data.aDolares
import com.example.cggestion.data.calcularTotales
import com.example.cggestion.data.local.entity.EstadoCotizacion
import com.example.cggestion.viewmodel.CotizacionViewModel
import com.example.cggestion.viewmodel.EditorCotizacionState
import com.example.cggestion.viewmodel.PdfViewModel
import com.example.cggestion.viewmodel.AccionPdf
import com.example.cggestion.util.pdf.PdfIntents
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCotizaciones(viewModel: CotizacionViewModel, pdfViewModel: PdfViewModel, cotizacionId: Long?, volver: () -> Unit, crearHoja: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var mostrarCatalogo by rememberSaveable { mutableStateOf(false) }
    val estadoPdf by pdfViewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(estadoPdf.accionPendiente) { estadoPdf.accionPendiente?.let { pendiente -> val resultado = if (pendiente.accion == AccionPdf.VER) PdfIntents.ver(context, pendiente.archivo) else PdfIntents.compartir(context, pendiente.archivo, pendiente.numero, pendiente.cliente); pdfViewModel.consumirAccion(); resultado.exceptionOrNull()?.message?.let(pdfViewModel::mostrarError) } }
    LaunchedEffect(cotizacionId) { if (cotizacionId == null) viewModel.nuevaCotizacion() else viewModel.cargar(cotizacionId) }
    Scaffold(containerColor = FondoPrincipal, topBar = { Column { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White, navigationIconContentColor = Color.White), title = { Column { Text("COTIZACIONES", fontWeight = FontWeight.Bold); Text("CG Repuestos", color = TextoSecundario, fontSize = 11.sp) } }, navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White, fontSize = 26.sp) } }); HorizontalDivider(thickness = 1.dp, color = RojoCG.copy(alpha = 0.6f)) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(18.dp)); Text(if (state.id == 0L) "Nueva cotización" else "Editar cotización", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text(state.numero, color = RojoCG, fontSize = 14.sp)
            Seccion("DATOS DEL CLIENTE")
            Campo(state.cliente, "Nombre o empresa") { viewModel.actualizarCliente(nombre = it) }
            Espacio(); Campo(state.ruc, "RUC / Cédula") { viewModel.actualizarCliente(ruc = it) }
            Espacio(); Campo(state.telefono, "Teléfono") { viewModel.actualizarCliente(telefono = it) }
            Espacio(); Campo(state.direccion, "Dirección") { viewModel.actualizarCliente(direccion = it) }
            Espacio(); Campo(state.correo, "Correo electrónico") { viewModel.actualizarCliente(correo = it) }
            Seccion("ESTADO Y DETALLES")
            SelectorEstado(state.estado) { viewModel.actualizarExtras(estado = it) }
            Espacio(); Campo(state.condicionPago, "Condición de pago") { viewModel.actualizarExtras(condicionPago = it) }
            Espacio(); Campo(state.vendedor, "Vendedor") { viewModel.actualizarExtras(vendedor = it) }
            Espacio(); Campo(state.observaciones, "Observaciones") { viewModel.actualizarExtras(observaciones = it) }
            Seccion("PRODUCTOS Y SERVICIOS")
            if (state.items.isEmpty()) TarjetaVacia() else state.items.forEachIndexed { index, item -> ItemCard(index + 1, item, { viewModel.actualizarItem(index, it) }, { viewModel.eliminarItem(index) }); Spacer(Modifier.height(10.dp)) }
            Button(onClick = { mostrarCatalogo = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) { Text("+ AGREGAR PRODUCTO", fontWeight = FontWeight.Bold) }
            Seccion("RESUMEN DE COTIZACIÓN")
            Resumen(state, viewModel)
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::guardar, enabled = !state.guardando, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RojoCG), contentPadding = PaddingValues(vertical = 15.dp)) { Text(if (state.guardando) "GUARDANDO..." else "GUARDAR COTIZACIÓN", fontWeight = FontWeight.Bold) }
            if (state.id != 0L) { Spacer(Modifier.height(10.dp)); PdfBotones(state.id, estadoPdf.generando, pdfViewModel) }
            if (state.id != 0L && state.estado == EstadoCotizacion.APROBADA) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { crearHoja(state.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("CREAR HOJA DE CAMPO")
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
    if (mostrarCatalogo) CatalogoDialog(state, { mostrarCatalogo = false }) { viewModel.agregarProducto(it); mostrarCatalogo = false }
    state.mensaje?.let { MensajeDialog("Listo", it, viewModel::limpiarMensaje) }; state.error?.let { MensajeDialog("Revisa la cotización", it, viewModel::limpiarMensaje) }
    estadoPdf.mensaje?.let { MensajeDialog("PDF", it, pdfViewModel::limpiarMensaje) }; estadoPdf.error?.let { MensajeDialog("PDF", it, pdfViewModel::limpiarMensaje) }
}

@Composable private fun PdfBotones(id: Long, generando: Boolean, vm: PdfViewModel) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { vm.generar(id) }, enabled = !generando, modifier = Modifier.fillMaxWidth()) { Text(if (generando) "GENERANDO PDF..." else "GENERAR PDF") }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { OutlinedButton(onClick = { vm.preparar(id, AccionPdf.VER) }, enabled = !generando, modifier = Modifier.weight(1f)) { Text("VER PDF") }; OutlinedButton(onClick = { vm.preparar(id, AccionPdf.COMPARTIR) }, enabled = !generando, modifier = Modifier.weight(1f)) { Text("COMPARTIR PDF") } } } }

@Composable private fun Seccion(texto: String) { Spacer(Modifier.height(22.dp)); Text(texto, color = RojoCG, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp)) }
@Composable private fun Espacio() = Spacer(Modifier.height(8.dp))
@Composable private fun TarjetaVacia() { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) { Text("Todavía no se han agregado productos.", color = TextoSecundario, modifier = Modifier.padding(18.dp)) } }

@Composable
private fun ItemCard(numero: Int, item: ItemCotizacion, actualizar: (ItemCotizacion) -> Unit, eliminar: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) { Column(Modifier.padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).background(RojoCG, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("$numero", color = Color.White, fontWeight = FontWeight.Bold) }; Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(item.producto.nombre, color = Color.White, fontWeight = FontWeight.Bold); Text(item.producto.categoria, color = TextoSecundario, fontSize = 12.sp) }; TextButton(onClick = eliminar) { Text("ELIMINAR", color = RojoCG, fontSize = 10.sp) } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) { Text("Cantidad", color = TextoSecundario, fontSize = 12.sp); TextButton(onClick = { if (item.cantidad > 1) actualizar(item.copy(cantidad = item.cantidad - 1)) }) { Text("−", color = Color.White, fontSize = 22.sp) }; Text(item.cantidad.toString(), color = Color.White, fontWeight = FontWeight.Bold); TextButton(onClick = { actualizar(item.copy(cantidad = item.cantidad + 1)) }) { Text("+", color = RojoCG, fontSize = 22.sp) } }
        CampoDecimal(item.precioUnitarioTexto, "Precio unitario") { actualizar(item.copy(precioUnitarioTexto = noNegativo(it))) }; Espacio(); CampoDecimal(item.descuentoTexto, "Descuento individual (%)") { actualizar(item.copy(descuentoTexto = porcentaje(it))) }
        Fila("Subtotal línea", moneda(item.subtotalLinea)); Fila("Descuento línea", "- ${moneda(item.descuentoLinea)}"); Fila("Total línea", moneda(item.totalLinea), true)
    } }
}

@Composable private fun Resumen(state: EditorCotizacionState, viewModel: CotizacionViewModel) {
    val totales = calcularTotales(state.items, numero(state.descuentoGlobalTexto).coerceIn(0.0, 100.0), numero(state.ivaTexto).coerceIn(0.0, 100.0))
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) { Column(Modifier.padding(14.dp)) { Fila("Subtotal bruto", moneda(totales.subtotalBruto.aDolares())); Fila("Descuento por productos", "- ${moneda(totales.descuentoItems.aDolares())}"); Espacio(); CampoDecimal(state.descuentoGlobalTexto, "Descuento global (%)") { viewModel.actualizarExtras(descuento = porcentaje(it)) }; Fila("Base imponible", moneda(totales.baseImponible.aDolares())); Espacio(); CampoDecimal(state.ivaTexto, "IVA (%)") { viewModel.actualizarExtras(iva = porcentaje(it)) }; Fila("Valor IVA", moneda(totales.valorIva.aDolares())); Row(Modifier.fillMaxWidth().background(RojoCG, RoundedCornerShape(8.dp)).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("TOTAL FINAL", color = Color.White, fontWeight = FontWeight.Bold); Text(moneda(totales.totalFinal.aDolares()), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) } } }
}
@Composable private fun Fila(nombre: String, valor: String, destacado: Boolean = false) { Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(nombre, color = TextoSecundario, fontSize = 12.sp); Text(valor, color = if (destacado) Color.White else TextoSecundario, fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal) } }

@Composable private fun SelectorEstado(actual: EstadoCotizacion, cambiar: (EstadoCotizacion) -> Unit) { var expandido by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { expandido = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Estado: ${actual.name}") }; DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) { EstadoCotizacion.entries.forEach { estado -> DropdownMenuItem(text = { Text(estado.name) }, onClick = { cambiar(estado); expandido = false }) } } } }

@Composable private fun CatalogoDialog(state: EditorCotizacionState, cerrar: () -> Unit, seleccionar: (com.example.cggestion.data.Producto) -> Unit) { var busqueda by remember { mutableStateOf("") }; val productos = state.catalogo.filter { it.nombre.contains(busqueda, true) || it.categoria.contains(busqueda, true) }; AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text("Agregar producto", color = Color.White) }, text = { Column { Campo(busqueda, "Buscar producto o categoría") { busqueda = it }; Spacer(Modifier.height(8.dp)); Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) { productos.forEach { producto -> Card(onClick = { seleccionar(producto) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF222222))) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(producto.nombre, color = Color.White, fontWeight = FontWeight.Bold); Text(producto.categoria, color = TextoSecundario, fontSize = 12.sp) }; Text(moneda(producto.precio), color = RojoCG) } } } } } }, confirmButton = {}, dismissButton = { TextButton(onClick = cerrar) { Text("CERRAR", color = RojoCG) } }) }
@Composable private fun MensajeDialog(titulo: String, texto: String, cerrar: () -> Unit) { AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text(titulo, color = Color.White) }, text = { Text(texto, color = Color.White) }, confirmButton = { TextButton(onClick = cerrar) { Text("ACEPTAR", color = RojoCG) } }) }
@Composable private fun Campo(valor: String, etiqueta: String, cambiar: (String) -> Unit) = CampoBase(valor, etiqueta, KeyboardType.Text, cambiar)
@Composable private fun CampoDecimal(valor: String, etiqueta: String, cambiar: (String) -> Unit) = CampoBase(valor, etiqueta, KeyboardType.Decimal, cambiar)
@Composable private fun CampoBase(valor: String, etiqueta: String, tipo: KeyboardType, cambiar: (String) -> Unit) { OutlinedTextField(value = valor, onValueChange = cambiar, label = { Text(etiqueta) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = tipo), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RojoCG, unfocusedBorderColor = Color(0xFF555555), focusedLabelColor = RojoCG, unfocusedLabelColor = TextoSecundario, cursorColor = RojoCG)) }
private fun numero(texto: String) = texto.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
private fun noNegativo(texto: String): String = limitar(texto, 0.0, Double.MAX_VALUE)
private fun porcentaje(texto: String): String = limitar(texto, 0.0, 100.0)
private fun limitar(texto: String, minimo: Double, maximo: Double): String { val valor = texto.replace(',', '.').toDoubleOrNull(); return if (valor != null && valor.isFinite()) valor.coerceIn(minimo, maximo).toString() else texto }
private fun moneda(valor: Double) = String.format(Locale.US, "\$%.2f", valor)
