package com.example.cggestion.ui.screens.reportes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.data.local.entity.EstadoCotizacion
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.viewmodel.PeriodoReporte
import com.example.cggestion.viewmodel.ReportesViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PantallaReportes(viewModel: ReportesViewModel, volver: () -> Unit) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val cotizaciones = estado.reporte.cotizaciones
    val hojas = estado.reporte.hojas
    val aprobadas = cotizaciones.filter { it.cotizacion.estado == EstadoCotizacion.APROBADA.name }.sumOf { it.cotizacion.totalFinalCentavos }
    val completadas = hojas.count { it.hoja.estado == EstadoHoja.COMPLETADA.name }
    Scaffold(containerColor = FondoPrincipal, topBar = { Column { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White), title = { Text("REPORTES") }, navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White) } }); HorizontalDivider(color = RojoCG.copy(alpha = .6f)) } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { PeriodoReporte.entries.forEach { p -> OutlinedButton(onClick = { viewModel.seleccionarPeriodo(p) }) { Text(p.name, color = if (p == estado.periodo) RojoCG else Color.White) } } } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { TarjetaResumen("COTIZACIONES", cotizaciones.size.toString(), Modifier.weight(1f)); TarjetaResumen("HOJAS COMPLETADAS", completadas.toString(), Modifier.weight(1f)) } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { TarjetaResumen("APROBADO", moneda(aprobadas), Modifier.weight(1f)); TarjetaResumen("STOCK BAJO", estado.reporte.stockBajo.size.toString(), Modifier.weight(1f)) } }
            item { Text("COTIZACIONES RECIENTES", color = RojoCG) }
            items(cotizaciones.take(5), key = { it.cotizacion.id }) { c -> ItemReporte(c.cotizacion.numeroCotizacion, "${c.clienteNombre} · ${c.cotizacion.estado}", moneda(c.cotizacion.totalFinalCentavos)) }
            item { Text("HOJAS RECIENTES", color = RojoCG, modifier = Modifier.padding(top = 8.dp)) }
            items(hojas.take(5), key = { it.hoja.id }) { h -> ItemReporte(h.hoja.numeroHoja, "${h.clienteNombre} · ${h.hoja.estado}", "${h.evidenciasCantidad} foto(s)") }
            item { Text("ALERTAS DE INVENTARIO", color = RojoCG, modifier = Modifier.padding(top = 8.dp)) }
            items(estado.reporte.stockBajo, key = { it.id }) { p -> ItemReporte(p.nombre, "Stock ${p.stockActual} ${p.unidad}", "Mínimo ${p.stockMinimo}") }
        }
    }
}
@Composable private fun TarjetaResumen(titulo: String, valor: String, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) { Column(Modifier.padding(12.dp)) { Text(titulo, color = TextoSecundario); Text(valor, color = Color.White) } } }
@Composable private fun ItemReporte(titulo: String, detalle: String, valor: String) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = FondoTarjeta)) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(titulo, color = Color.White); Text(detalle, color = TextoSecundario) }; Text(valor, color = Color.White) } } }
private fun moneda(centavos: Long) = "$" + String.format(Locale.US, "%.2f", centavos / 100.0)
