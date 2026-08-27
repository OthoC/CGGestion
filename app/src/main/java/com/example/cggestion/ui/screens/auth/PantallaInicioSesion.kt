package com.example.cggestion.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.example.cggestion.FondoPrincipal
import com.example.cggestion.R
import com.example.cggestion.RojoCG
import com.example.cggestion.TextoSecundario
import com.example.cggestion.viewmodel.EstadoAutenticacion

@Composable
fun PantallaInicioSesion(
    estado: EstadoAutenticacion.SinSesion,
    iniciarSesion: (String, String) -> Unit,
    recuperarClave: (String) -> Unit,
    limpiarAvisos: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var mostrarPassword by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier.fillMaxSize().background(FondoPrincipal).padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_cg),
                contentDescription = "Logo de CG Gestión",
                modifier = Modifier.size(96.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text("CG GESTIÓN", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Acceso al sistema", color = TextoSecundario, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    limpiarAvisos()
                },
                enabled = !estado.procesando && estado.firebaseConfigurado,
                label = { Text("Correo electrónico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                colors = coloresCampoAcceso()
            )
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    limpiarAvisos()
                },
                enabled = !estado.procesando && estado.firebaseConfigurado,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                        Icon(
                            if (mostrarPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (mostrarPassword) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = RojoCG
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    iniciarSesion(email, password)
                }),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                colors = coloresCampoAcceso()
            )
            estado.error?.let {
                Text(it, color = RojoCG, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            estado.mensaje?.let {
                Text(it, color = Color.White, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
            Button(
                onClick = { iniciarSesion(email, password) },
                enabled = !estado.procesando && estado.firebaseConfigurado,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RojoCG)
            ) {
                if (estado.procesando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("INICIAR SESIÓN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(
                onClick = { recuperarClave(email) },
                enabled = !estado.procesando && estado.firebaseConfigurado
            ) {
                Text("OLVIDÉ MI CONTRASEÑA", color = if (estado.firebaseConfigurado) RojoCG else TextoSecundario)
            }
            Text(
                "El primer acceso de cada cuenta requiere conexión a internet.",
                color = TextoSecundario,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun coloresCampoAcceso() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    disabledTextColor = TextoSecundario,
    focusedLabelColor = RojoCG,
    unfocusedLabelColor = TextoSecundario,
    focusedBorderColor = RojoCG,
    unfocusedBorderColor = TextoSecundario,
    cursorColor = RojoCG
)

@Composable
fun PantallaCargaSesion() {
    Box(
        modifier = Modifier.fillMaxSize().background(FondoPrincipal),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = RojoCG)
            Text("Validando sesión…", color = Color.White, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
