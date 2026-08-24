package com.example.cggestion.ui.screens.historial

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.data.aDolares
import com.example.cggestion.viewmodel.HistorialViewModel
import com.example.cggestion.viewmodel.PdfViewModel
import com.example.cggestion.viewmodel.AccionPdf
import com.example.cggestion.util.pdf.PdfIntents
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(viewModel: HistorialViewModel, pdfViewModel: PdfViewModel, volver: () -> Unit, abrir: (Long) -> Unit) {
    val cotizaciones by viewModel.cotizaciones.collectAsStateWithLifecycle()
    var busqueda by remember { mutableStateOf("") }
    val estadoPdf by pdfViewModel.estado.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(estadoPdf.accionPendiente) { estadoPdf.accionPendiente?.let { pendiente -> val resultado = if (pendiente.accion == AccionPdf.VER) PdfIntents.ver(context, pendiente.archivo) else PdfIntents.compartir(context, pendiente.archivo, pendiente.numero, pendiente.cliente); pdfViewModel.consumirAccion(); resultado.exceptionOrNull()?.message?.let(pdfViewModel::mostrarError) } }
    val filtradas = cotizaciones.filter { it.cotizacion.numeroCotizacion.contains(busqueda, true) || it.clienteNombre.contains(busqueda, true) }
    Scaffold(containerColor = FondoPrincipal, topBar = { Column { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White, navigationIconContentColor = Color.White), navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White, fontSize = 26.sp) } }, title = { Column { Text("HISTORIAL", fontWeight = FontWeight.Bold); Text("Cotizaciones guardadas", color = TextoSecundario, fontSize = 11.sp) } }); HorizontalDivider(thickness = 1.dp, color = RojoCG.copy(alpha = 0.6f)) } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp)) {
            OutlinedTextField(value = busqueda, onValueChange = { busqueda = it }, label = { Text("Buscar por número o cliente") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RojoCG, unfocusedBorderColor = Color(0xFF555555), focusedLabelColor = RojoCG, unfocusedLabelColor = TextoSecundario))
            Spacer(Modifier.height(14.dp))
            if (filtradas.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text(if (busqueda.isBlank()) "Todavía no existen cotizaciones guardadas." else "No se encontraron cotizaciones.", color = TextoSecundario) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(filtradas, key = { it.cotizacion.id }) { resumen -> HistorialCard(resumen, abrir, pdfViewModel) } }
        }
    }
    estadoPdf.mensaje?.let { DialogoPdf(it, pdfViewModel::limpiarMensaje) }; estadoPdf.error?.let { DialogoPdf(it, pdfViewModel::limpiarMensaje) }
}
@Composable private fun HistorialCard(resumen: com.example.cggestion.data.local.entity.CotizacionResumen, abrir: (Long) -> Unit, pdf: PdfViewModel) { var expandido by remember { mutableStateOf(false) }; Card(Modifier.fillMaxWidth().clickable { abrir(resumen.cotizacion.id) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) { Column(Modifier.padding(14.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(resumen.cotizacion.numeroCotizacion, color = RojoCG, fontWeight = FontWeight.Bold); Box { TextButton(onClick = { expandido = true }) { Text("⋮", color = Color.White, fontSize = 20.sp) }; DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) { DropdownMenuItem(text = { Text("Abrir") }, onClick = { expandido = false; abrir(resumen.cotizacion.id) }); DropdownMenuItem(text = { Text("Generar PDF") }, onClick = { expandido = false; pdf.generar(resumen.cotizacion.id) }); DropdownMenuItem(text = { Text("Ver PDF") }, onClick = { expandido = false; pdf.preparar(resumen.cotizacion.id, AccionPdf.VER) }); DropdownMenuItem(text = { Text("Compartir PDF") }, onClick = { expandido = false; pdf.preparar(resumen.cotizacion.id, AccionPdf.COMPARTIR) }) } } }; Text(resumen.clienteNombre, color = Color.White, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(fecha(resumen.cotizacion.fechaCreacion), color = TextoSecundario, fontSize = 12.sp); Text(moneda(resumen.cotizacion.totalFinalCentavos.aDolares()), color = Color.White, fontWeight = FontWeight.Bold) } } } }
@Composable private fun DialogoPdf(texto:String,cerrar:()->Unit){AlertDialog(onDismissRequest=cerrar,title={Text("PDF",color=Color.White)},text={Text(texto,color=Color.White)},containerColor=FondoTarjeta,confirmButton={TextButton(onClick=cerrar){Text("ACEPTAR",color=RojoCG)}})}
private fun fecha(millis: Long) = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
private fun moneda(valor: Double) = String.format(Locale.US, "\$%.2f", valor)
