package com.example.cggestion.ui.screens.administracion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cggestion.FondoBarraSuperior
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.FondoTarjeta
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.data.local.entity.RolUsuario
import com.example.cggestion.data.local.entity.UsuarioEntity
import com.example.cggestion.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAdministracion(viewModel: AuthViewModel, volver: () -> Unit) {
    val usuarios by viewModel.usuarios.collectAsStateWithLifecycle(emptyList())
    val estado by viewModel.ui.collectAsStateWithLifecycle()
    var crear by remember { mutableStateOf(false) }
    var cambiarClave by remember { mutableStateOf<UsuarioEntity?>(null) }
    Scaffold(containerColor = FondoPrincipal, topBar = { Column {
        TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = FondoBarraSuperior, titleContentColor = Color.White), title = { Text("ADMINISTRACIÓN") }, navigationIcon = { TextButton(onClick = volver) { Text("←", color = Color.White) } })
        HorizontalDivider(color = RojoCG.copy(alpha = .6f))
    } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Usuarios y permisos", color = Color.White)
            Text("Los operadores no pueden acceder a módulos sensibles.", color = TextoSecundario, modifier = Modifier.padding(top = 4.dp))
            Button(onClick = { crear = true }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), colors = ButtonDefaults.buttonColors(containerColor = RojoCG)) { Text("NUEVO USUARIO", color = Color.White) }
            estado.error?.let { Text(it, color = RojoCG, modifier = Modifier.padding(top = 8.dp)) }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(usuarios, key = { it.id }) { usuario ->
                    Card(colors = CardDefaults.cardColors(containerColor = FondoTarjeta), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(usuario.usuario, color = Color.White)
                            Text("${usuario.rol} · ${if (usuario.activo) "ACTIVO" else "INACTIVO"}", color = if (usuario.activo) TextoSecundario else RojoCG)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { cambiarClave = usuario }) { Text("CONTRASEÑA", color = RojoCG) }
                                TextButton(onClick = { viewModel.cambiarEstado(usuario.id, !usuario.activo) }) { Text(if (usuario.activo) "DESACTIVAR" else "ACTIVAR", color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (crear) DialogoUsuario("Nuevo usuario", { crear = false }) { usuario, clave, rol -> viewModel.crearUsuario(usuario, clave, rol); crear = false }
    cambiarClave?.let { usuario -> DialogoClave(usuario.usuario, { cambiarClave = null }) { clave -> viewModel.cambiarClave(usuario.id, clave); cambiarClave = null } }
}

@Composable private fun DialogoUsuario(titulo: String, cerrar: () -> Unit, guardar: (String, String, String) -> Unit) {
    var usuario by remember { mutableStateOf("") }; var clave by remember { mutableStateOf("") }; var rol by remember { mutableStateOf(RolUsuario.OPERADOR.name) }
    AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text(titulo, color = Color.White) }, text = { Column {
        Campo(usuario, { usuario = it }, "Usuario"); Campo(clave, { clave = it }, "Contraseña", true)
        Row { TextButton(onClick = { rol = RolUsuario.OPERADOR.name }) { Text("OPERADOR", color = if (rol == RolUsuario.OPERADOR.name) RojoCG else Color.White) }; TextButton(onClick = { rol = RolUsuario.ADMINISTRADOR.name }) { Text("ADMIN", color = if (rol == RolUsuario.ADMINISTRADOR.name) RojoCG else Color.White) } }
    } }, confirmButton = { TextButton(onClick = { guardar(usuario, clave, rol) }) { Text("GUARDAR", color = RojoCG) } }, dismissButton = { TextButton(onClick = cerrar) { Text("CANCELAR", color = Color.White) } })
}
@Composable private fun DialogoClave(usuario: String, cerrar: () -> Unit, guardar: (String) -> Unit) { var clave by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = cerrar, containerColor = FondoTarjeta, title = { Text("Contraseña · $usuario", color = Color.White) }, text = { Campo(clave, { clave = it }, "Nueva contraseña", true) }, confirmButton = { TextButton(onClick = { guardar(clave) }) { Text("GUARDAR", color = RojoCG) } }, dismissButton = { TextButton(onClick = cerrar) { Text("CANCELAR", color = Color.White) } }) }
@Composable private fun Campo(valor: String, cambiar: (String) -> Unit, etiqueta: String, clave: Boolean = false) = OutlinedTextField(value = valor, onValueChange = cambiar, label = { Text(etiqueta) }, visualTransformation = if (clave) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = RojoCG, unfocusedLabelColor = TextoSecundario, focusedBorderColor = RojoCG, unfocusedBorderColor = TextoSecundario))
