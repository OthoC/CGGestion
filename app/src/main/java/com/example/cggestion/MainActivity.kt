package com.example.cggestion

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cggestion.ui.screens.cotizaciones.PantallaCotizaciones
import com.example.cggestion.ui.screens.historial.PantallaHistorial
import com.example.cggestion.ui.theme.CGGestionTheme
import com.example.cggestion.viewmodel.CotizacionViewModel
import com.example.cggestion.viewmodel.HistorialViewModel
import com.example.cggestion.viewmodel.PdfViewModel
import com.example.cggestion.viewmodel.HojaCampoViewModel
import com.example.cggestion.ui.screens.hojascampo.PantallaHojasCampo
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import com.example.cggestion.viewmodel.InventarioViewModel
import com.example.cggestion.ui.screens.inventario.PantallaInventario
import com.example.cggestion.viewmodel.ClientesViewModel
import com.example.cggestion.ui.screens.clientes.PantallaClientes
import com.example.cggestion.viewmodel.RespaldosViewModel
import com.example.cggestion.ui.screens.respaldos.PantallaRespaldos
import com.example.cggestion.viewmodel.ReportesViewModel
import com.example.cggestion.ui.screens.reportes.PantallaReportes
import com.example.cggestion.viewmodel.HojaCampoPdfViewModel
import com.example.cggestion.viewmodel.MantenimientoViewModel
import com.example.cggestion.ui.screens.mantenimientos.PantallaMantenimientos
import com.example.cggestion.viewmodel.ActualizacionViewModel
import com.example.cggestion.ui.screens.actualizaciones.PantallaActualizaciones

val FondoPrincipal = Color(0xFF0A0A0A)
val FondoBarraSuperior = Color(0xFF161616)
val FondoTarjeta = Color(0xFF181818)
val RojoCG = Color(0xFFE02020)
val TextoSecundario = Color(0xFF9E9E9E)

data class OpcionInicio(val titulo: String, val descripcion: String, val simbolo: String)

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(22, 22, 22)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent { CGGestionTheme { AplicacionCG() } }
    }
}

private enum class Pantalla { INICIO, COTIZACIONES, HISTORIAL, HOJAS, INVENTARIO, CLIENTES, RESPALDOS, REPORTES, MANTENIMIENTOS, ACTUALIZACIONES }

@Composable
fun AplicacionCG() {
    var pantallaActual by remember { mutableStateOf(Pantalla.INICIO) }
    var cotizacionEnEdicion by remember { mutableStateOf<Long?>(null) }
    var abrirHojaDesdeCotizacion by remember { mutableStateOf(false) }
    var anuncioActualizacionCerrado by remember { mutableStateOf(false) }
    val app = LocalContext.current.applicationContext as CGGestionApplication
    val cotizacionViewModel: CotizacionViewModel = viewModel(factory = CotizacionViewModel.factory(app.repository))
    val historialViewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.factory(app.repository))
    val pdfViewModel: PdfViewModel = viewModel(factory = PdfViewModel.factory(app.repository, app.pdfGenerator))
    val hojaViewModel: HojaCampoViewModel = viewModel(factory = HojaCampoViewModel.factory(app.hojaCampoRepository))
    val inventarioViewModel: InventarioViewModel = viewModel(factory = InventarioViewModel.factory(app.inventarioRepository))
    val clientesViewModel: ClientesViewModel = viewModel(factory = ClientesViewModel.factory(app.clienteRepository))
    val respaldosViewModel: RespaldosViewModel = viewModel(factory = RespaldosViewModel.factory(app.backupManager))
    val reportesViewModel: ReportesViewModel = viewModel(factory = ReportesViewModel.factory(app.reportesRepository))
    val hojaPdfViewModel: HojaCampoPdfViewModel = viewModel(factory = HojaCampoPdfViewModel.factory(app.hojaCampoRepository, app.hojaCampoPdfGenerator))
    val mantenimientoViewModel: MantenimientoViewModel = viewModel(factory = MantenimientoViewModel.factory(app.mantenimientoRepository))
    val actualizacionViewModel: ActualizacionViewModel = viewModel(factory = ActualizacionViewModel.factory(app.actualizacionRepository))
    val estadoActualizacion by actualizacionViewModel.ui.collectAsStateWithLifecycle()
    when (pantallaActual) {
        Pantalla.INICIO -> PantallaInicio { opcion ->
            when (opcion.titulo) {
                "Cotizaciones" -> { cotizacionEnEdicion = null; pantallaActual = Pantalla.COTIZACIONES }
                "Historial" -> pantallaActual = Pantalla.HISTORIAL
                "Hojas de campo" -> pantallaActual = Pantalla.HOJAS
                "Inventario" -> pantallaActual = Pantalla.INVENTARIO
                "Clientes" -> pantallaActual = Pantalla.CLIENTES
                "Respaldos" -> pantallaActual = Pantalla.RESPALDOS
                "Reportes" -> pantallaActual = Pantalla.REPORTES
                "Mantenimientos" -> pantallaActual = Pantalla.MANTENIMIENTOS
                "Actualizaciones" -> pantallaActual = Pantalla.ACTUALIZACIONES
            }
        }
        Pantalla.COTIZACIONES -> {
            BackHandler { pantallaActual = Pantalla.INICIO }
            PantallaCotizaciones(
                cotizacionViewModel,
                pdfViewModel,
                cotizacionEnEdicion,
                volver = { pantallaActual = Pantalla.INICIO },
                crearHoja = { cotizacionId ->
                    hojaViewModel.crearDesdeCotizacion(cotizacionId)
                    abrirHojaDesdeCotizacion = true
                    pantallaActual = Pantalla.HOJAS
                }
            )
        }
        Pantalla.HISTORIAL -> {
            BackHandler { pantallaActual = Pantalla.INICIO }
            PantallaHistorial(historialViewModel, pdfViewModel, volver = { pantallaActual = Pantalla.INICIO }, abrir = { id -> cotizacionEnEdicion = id; pantallaActual = Pantalla.COTIZACIONES })
        }
        Pantalla.HOJAS -> {
            BackHandler { pantallaActual = Pantalla.INICIO }
            PantallaHojasCampo(
                viewModel = hojaViewModel,
                pdfViewModel = hojaPdfViewModel,
                volver = { pantallaActual = Pantalla.INICIO },
                abrirFormularioInicial = abrirHojaDesdeCotizacion,
                consumirAbrirFormulario = { abrirHojaDesdeCotizacion = false }
            )
        }
        Pantalla.INVENTARIO -> { BackHandler { pantallaActual = Pantalla.INICIO }; PantallaInventario(inventarioViewModel) { pantallaActual = Pantalla.INICIO } }
        Pantalla.CLIENTES -> { BackHandler { pantallaActual = Pantalla.INICIO }; PantallaClientes(clientesViewModel) { pantallaActual = Pantalla.INICIO } }
        Pantalla.RESPALDOS -> { BackHandler { pantallaActual = Pantalla.INICIO }; PantallaRespaldos(respaldosViewModel) { pantallaActual = Pantalla.INICIO } }
        Pantalla.REPORTES -> { BackHandler { pantallaActual = Pantalla.INICIO }; PantallaReportes(reportesViewModel) { pantallaActual = Pantalla.INICIO } }
        Pantalla.MANTENIMIENTOS -> {
            BackHandler { pantallaActual = Pantalla.INICIO }
            PantallaMantenimientos(mantenimientoViewModel, volver = { pantallaActual = Pantalla.INICIO }) { contexto ->
                hojaViewModel.crearDesdeMantenimiento(contexto)
                abrirHojaDesdeCotizacion = true
                pantallaActual = Pantalla.HOJAS
            }
        }
        Pantalla.ACTUALIZACIONES -> { BackHandler { pantallaActual = Pantalla.INICIO }; PantallaActualizaciones(actualizacionViewModel) { pantallaActual = Pantalla.INICIO } }
    }
    if (pantallaActual == Pantalla.INICIO && estadoActualizacion.disponible != null && !anuncioActualizacionCerrado) {
        AlertDialog(
            onDismissRequest = { anuncioActualizacionCerrado = true },
            containerColor = FondoTarjeta,
            title = { Text("Actualización disponible", color = Color.White) },
            text = { Text("La versión ${estadoActualizacion.disponible!!.versionName} está disponible para CG Gestión.", color = Color.White) },
            confirmButton = { TextButton(onClick = { anuncioActualizacionCerrado = true; pantallaActual = Pantalla.ACTUALIZACIONES }) { Text("VER", color = RojoCG) } },
            dismissButton = { TextButton(onClick = { anuncioActualizacionCerrado = true }) { Text("MÁS TARDE", color = Color.White) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(alSeleccionar: (OpcionInicio) -> Unit) {
    val opciones = listOf(
        OpcionInicio("Hojas de campo", "Registrar trabajos y mediciones", "HC"),
        OpcionInicio("Cotizaciones", "Crear y consultar cotizaciones", "$") ,
        OpcionInicio("Clientes", "Administrar clientes y equipos", "CL"),
        OpcionInicio("Inventario", "Productos, precios y repuestos", "IN"),
        OpcionInicio("Historial", "Consultar registros anteriores", "HI"),
        OpcionInicio("Reportes", "Indicadores y alertas operativas", "RP"),
        OpcionInicio("Mantenimientos", "Agenda preventiva por equipos", "MT"),
        OpcionInicio("Actualizaciones", "Buscar nueva versión de la app", "UP"),
        OpcionInicio("Respaldos", "Exportar Excel, PDF y fotografías", "RE")
    )
    Scaffold(containerColor = FondoPrincipal, topBar = {
        Column {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White, navigationIconContentColor = Color.White), title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(RojoCG, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("CG", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text("CG GESTIÓN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Trabajos y cotizaciones", color = TextoSecundario, fontSize = 11.sp)
                }
            }
        })
        androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = RojoCG.copy(alpha = 0.6f))
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Panel principal", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Selecciona el módulo que deseas utilizar", color = TextoSecundario, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(opciones) { TarjetaOpcion(it, alSeleccionar) }
            }
        }
    }
}

@Composable
private fun TarjetaOpcion(opcion: OpcionInicio, alSeleccionar: (OpcionInicio) -> Unit) {
    Card(onClick = { alSeleccionar(opcion) }, modifier = Modifier.fillMaxWidth().height(165.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(46.dp).background(RojoCG, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(opcion.simbolo, color = Color.White, fontWeight = FontWeight.Bold) }
            Column {
                Text(opcion.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(opcion.descripcion, color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PantallaInicioPreview() { CGGestionTheme { PantallaInicio {} } }
