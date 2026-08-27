package com.example.cggestion

import android.graphics.Color as AndroidColor
import android.os.Bundle
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cggestion.auth.ModuloRestringible
import com.example.cggestion.auth.PerfilUsuario
import com.example.cggestion.auth.PermisosUsuario
import com.example.cggestion.auth.RolUsuarioFirebase
import com.example.cggestion.ui.screens.actualizaciones.PantallaActualizaciones
import com.example.cggestion.ui.screens.auth.PantallaCargaSesion
import com.example.cggestion.ui.screens.auth.PantallaInicioSesion
import com.example.cggestion.ui.screens.clientes.PantallaClientes
import com.example.cggestion.ui.screens.cotizaciones.PantallaCotizaciones
import com.example.cggestion.ui.screens.historial.PantallaHistorial
import com.example.cggestion.ui.screens.hojascampo.PantallaHojasCampo
import com.example.cggestion.ui.screens.inventario.PantallaInventario
import com.example.cggestion.ui.screens.mantenimientos.PantallaMantenimientos
import com.example.cggestion.ui.screens.reportes.PantallaReportes
import com.example.cggestion.ui.screens.respaldos.PantallaRespaldos
import com.example.cggestion.ui.theme.CGGestionTheme
import com.example.cggestion.viewmodel.ActualizacionViewModel
import com.example.cggestion.viewmodel.ClientesViewModel
import com.example.cggestion.viewmodel.CotizacionViewModel
import com.example.cggestion.viewmodel.EstadoAutenticacion
import com.example.cggestion.viewmodel.FirebaseAuthViewModel
import com.example.cggestion.viewmodel.HistorialViewModel
import com.example.cggestion.viewmodel.HojaCampoPdfViewModel
import com.example.cggestion.viewmodel.HojaCampoViewModel
import com.example.cggestion.viewmodel.InventarioViewModel
import com.example.cggestion.viewmodel.MantenimientoViewModel
import com.example.cggestion.viewmodel.PdfViewModel
import com.example.cggestion.viewmodel.ReportesViewModel
import com.example.cggestion.viewmodel.RespaldosViewModel

val FondoPrincipal = Color(0xFF0A0A0A)
val FondoBarraSuperior = Color(0xFF161616)
val FondoTarjeta = Color(0xFF181818)
val RojoCG = Color(0xFFE02020)
val TextoSecundario = Color(0xFF9E9E9E)

data class OpcionInicio(
    val titulo: String,
    val descripcion: String,
    val simbolo: String,
    val modulo: ModuloRestringible
)

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(22, 22, 22)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent { CGGestionTheme { AplicacionCG() } }
    }
}

private enum class Pantalla {
    INICIO,
    COTIZACIONES,
    HISTORIAL,
    HOJAS,
    INVENTARIO,
    CLIENTES,
    RESPALDOS,
    REPORTES,
    MANTENIMIENTOS,
    ACTUALIZACIONES
}

@Composable
fun AplicacionCG() {
    val app = LocalContext.current.applicationContext as CGGestionApplication
    val authViewModel: FirebaseAuthViewModel = viewModel(
        factory = FirebaseAuthViewModel.factory(app.firebaseAuthRepository)
    )
    val estado by authViewModel.estado.collectAsStateWithLifecycle()

    when (val actual = estado) {
        EstadoAutenticacion.Inicializando -> PantallaCargaSesion()
        is EstadoAutenticacion.SinSesion -> PantallaInicioSesion(
            estado = actual,
            iniciarSesion = authViewModel::iniciarSesion,
            recuperarClave = authViewModel::recuperarClave,
            limpiarAvisos = authViewModel::limpiarAvisos
        )
        is EstadoAutenticacion.Autenticado -> AplicacionAutenticada(
            perfil = actual.perfil,
            cerrarSesion = authViewModel::cerrarSesion
        )
    }
}

@Composable
private fun AplicacionAutenticada(perfil: PerfilUsuario, cerrarSesion: () -> Unit) {
    var pantallaActual by remember(perfil.uid) { mutableStateOf(Pantalla.INICIO) }
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
    val hojaPdfViewModel: HojaCampoPdfViewModel = viewModel(
        factory = HojaCampoPdfViewModel.factory(app.hojaCampoRepository, app.hojaCampoPdfGenerator)
    )
    val mantenimientoViewModel: MantenimientoViewModel = viewModel(
        factory = MantenimientoViewModel.factory(app.mantenimientoRepository)
    )
    val actualizacionViewModel: ActualizacionViewModel = viewModel(
        factory = ActualizacionViewModel.factory(app.actualizacionRepository)
    )
    val estadoActualizacion by actualizacionViewModel.ui.collectAsStateWithLifecycle()

    fun abrir(pantalla: Pantalla) {
        val modulo = pantalla.moduloRestringible()
        pantallaActual = if (modulo == null || PermisosUsuario.puedeAcceder(perfil, modulo)) {
            pantalla
        } else {
            Pantalla.INICIO
        }
    }

    val moduloActual = pantallaActual.moduloRestringible()
    if (moduloActual != null && !PermisosUsuario.puedeAcceder(perfil, moduloActual)) {
        LaunchedEffect(pantallaActual, perfil.rol) { pantallaActual = Pantalla.INICIO }
        PantallaInicio(perfil, cerrarSesion) { abrir(it.pantallaDestino()) }
        return
    }

    when (pantallaActual) {
        Pantalla.INICIO -> PantallaInicio(perfil, cerrarSesion) { opcion ->
            if (opcion.modulo == ModuloRestringible.COTIZACIONES) cotizacionEnEdicion = null
            abrir(opcion.pantallaDestino())
        }
        Pantalla.COTIZACIONES -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaCotizaciones(
                cotizacionViewModel,
                pdfViewModel,
                cotizacionEnEdicion,
                volver = { abrir(Pantalla.INICIO) },
                crearHoja = { id ->
                    hojaViewModel.crearDesdeCotizacion(id)
                    abrirHojaDesdeCotizacion = true
                    abrir(Pantalla.HOJAS)
                }
            )
        }
        Pantalla.HISTORIAL -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaHistorial(
                historialViewModel,
                pdfViewModel,
                volver = { abrir(Pantalla.INICIO) },
                abrir = { id ->
                    cotizacionEnEdicion = id
                    abrir(Pantalla.COTIZACIONES)
                }
            )
        }
        Pantalla.HOJAS -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaHojasCampo(
                hojaViewModel,
                hojaPdfViewModel,
                volver = { abrir(Pantalla.INICIO) },
                abrirFormularioInicial = abrirHojaDesdeCotizacion,
                consumirAbrirFormulario = { abrirHojaDesdeCotizacion = false }
            )
        }
        Pantalla.INVENTARIO -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaInventario(inventarioViewModel) { abrir(Pantalla.INICIO) }
        }
        Pantalla.CLIENTES -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaClientes(clientesViewModel) { abrir(Pantalla.INICIO) }
        }
        Pantalla.RESPALDOS -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaRespaldos(
                viewModel = respaldosViewModel,
                puedeRestaurar = PermisosUsuario.puedeAcceder(perfil, ModuloRestringible.RESTAURAR_RESPALDO),
                volver = { abrir(Pantalla.INICIO) }
            )
        }
        Pantalla.REPORTES -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaReportes(reportesViewModel) { abrir(Pantalla.INICIO) }
        }
        Pantalla.MANTENIMIENTOS -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaMantenimientos(mantenimientoViewModel, volver = { abrir(Pantalla.INICIO) }) { contexto ->
                hojaViewModel.crearDesdeMantenimiento(contexto)
                abrirHojaDesdeCotizacion = true
                abrir(Pantalla.HOJAS)
            }
        }
        Pantalla.ACTUALIZACIONES -> {
            BackHandler { abrir(Pantalla.INICIO) }
            PantallaActualizaciones(actualizacionViewModel) { abrir(Pantalla.INICIO) }
        }
    }

    if (
        pantallaActual == Pantalla.INICIO &&
        estadoActualizacion.disponible != null &&
        !anuncioActualizacionCerrado
    ) {
        AlertDialog(
            onDismissRequest = { anuncioActualizacionCerrado = true },
            containerColor = FondoTarjeta,
            title = { Text("Actualización disponible", color = Color.White) },
            text = {
                Text(
                    "La versión ${estadoActualizacion.disponible!!.versionName} está disponible para CG Gestión.",
                    color = Color.White
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    anuncioActualizacionCerrado = true
                    abrir(Pantalla.ACTUALIZACIONES)
                }) { Text("VER", color = RojoCG) }
            },
            dismissButton = {
                TextButton(onClick = { anuncioActualizacionCerrado = true }) {
                    Text("MÁS TARDE", color = Color.White)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(
    perfil: PerfilUsuario,
    cerrarSesion: () -> Unit,
    alSeleccionar: (OpcionInicio) -> Unit
) {
    val opciones = opcionesInicio().filter { PermisosUsuario.puedeAcceder(perfil, it.modulo) }
    Scaffold(
        containerColor = FondoPrincipal,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FondoBarraSuperior,
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(38.dp).background(RojoCG, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("CG", color = Color.White, fontWeight = FontWeight.Bold) }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text("CG GESTIÓN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Versión ${BuildConfig.VERSION_NAME} · Sesión protegida", color = TextoSecundario, fontSize = 11.sp)
                            }
                        }
                    },
                    actions = {
                        TextButton(onClick = cerrarSesion) {
                            Text("SALIR", color = RojoCG, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                HorizontalDivider(thickness = 1.dp, color = RojoCG.copy(alpha = .6f))
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text("${perfil.nombre} · ${perfil.rol.nombreVisible()}", color = Color.White, fontWeight = FontWeight.Bold)
            Text(perfil.email, color = TextoSecundario, fontSize = 12.sp)
            Text("Panel principal", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
            Text("Selecciona el módulo que deseas utilizar", color = TextoSecundario, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(opciones, key = { it.modulo.name }) { TarjetaOpcion(it, alSeleccionar) }
            }
        }
    }
}

private fun opcionesInicio() = listOf(
    OpcionInicio("Hojas de campo", "Registrar trabajos y mediciones", "HC", ModuloRestringible.HOJAS_CAMPO),
    OpcionInicio("Cotizaciones", "Crear y consultar cotizaciones", "$", ModuloRestringible.COTIZACIONES),
    OpcionInicio("Clientes", "Administrar clientes y equipos", "CL", ModuloRestringible.CLIENTES),
    OpcionInicio("Inventario", "Productos, precios y repuestos", "IN", ModuloRestringible.INVENTARIO),
    OpcionInicio("Historial", "Consultar registros anteriores", "HI", ModuloRestringible.HISTORIAL),
    OpcionInicio("Reportes", "Indicadores y alertas operativas", "RP", ModuloRestringible.REPORTES),
    OpcionInicio("Mantenimientos", "Agenda preventiva por equipos", "MT", ModuloRestringible.MANTENIMIENTOS),
    OpcionInicio("Actualizaciones", "Buscar nueva versión de la app", "UP", ModuloRestringible.ACTUALIZACIONES),
    OpcionInicio("Respaldos", "Exportar copias y fotografías", "RE", ModuloRestringible.RESPALDOS)
)

@Composable
private fun TarjetaOpcion(opcion: OpcionInicio, alSeleccionar: (OpcionInicio) -> Unit) {
    Card(
        onClick = { alSeleccionar(opcion) },
        modifier = Modifier.fillMaxWidth().height(165.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.size(46.dp).background(RojoCG, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text(opcion.simbolo, color = Color.White, fontWeight = FontWeight.Bold) }
            Column {
                Text(opcion.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(opcion.descripcion, color = TextoSecundario, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun Pantalla.moduloRestringible(): ModuloRestringible? = when (this) {
    Pantalla.INICIO -> null
    Pantalla.COTIZACIONES -> ModuloRestringible.COTIZACIONES
    Pantalla.HISTORIAL -> ModuloRestringible.HISTORIAL
    Pantalla.HOJAS -> ModuloRestringible.HOJAS_CAMPO
    Pantalla.INVENTARIO -> ModuloRestringible.INVENTARIO
    Pantalla.CLIENTES -> ModuloRestringible.CLIENTES
    Pantalla.RESPALDOS -> ModuloRestringible.RESPALDOS
    Pantalla.REPORTES -> ModuloRestringible.REPORTES
    Pantalla.MANTENIMIENTOS -> ModuloRestringible.MANTENIMIENTOS
    Pantalla.ACTUALIZACIONES -> ModuloRestringible.ACTUALIZACIONES
}

private fun OpcionInicio.pantallaDestino(): Pantalla = when (modulo) {
    ModuloRestringible.HOJAS_CAMPO -> Pantalla.HOJAS
    ModuloRestringible.COTIZACIONES -> Pantalla.COTIZACIONES
    ModuloRestringible.HISTORIAL -> Pantalla.HISTORIAL
    ModuloRestringible.CLIENTES -> Pantalla.CLIENTES
    ModuloRestringible.MANTENIMIENTOS -> Pantalla.MANTENIMIENTOS
    ModuloRestringible.RESPALDOS,
    ModuloRestringible.RESTAURAR_RESPALDO -> Pantalla.RESPALDOS
    ModuloRestringible.ACTUALIZACIONES -> Pantalla.ACTUALIZACIONES
    ModuloRestringible.INVENTARIO -> Pantalla.INVENTARIO
    ModuloRestringible.REPORTES -> Pantalla.REPORTES
}

private fun RolUsuarioFirebase.nombreVisible(): String = when (this) {
    RolUsuarioFirebase.SUPERUSUARIO -> "SUPERUSUARIO"
    RolUsuarioFirebase.TECNICO -> "TÉCNICO"
}

@Preview(showBackground = true)
@Composable
fun PantallaInicioPreview() {
    CGGestionTheme {
        PantallaInicio(
            perfil = PerfilUsuario("preview", "tecnico@cgrepuestos.com", "Técnico", RolUsuarioFirebase.TECNICO, true),
            cerrarSesion = {},
            alSeleccionar = {}
        )
    }
}
