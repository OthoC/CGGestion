package com.example.cggestion.data

data class Producto(
    val id: Long,
    val nombre: String,
    val categoria: String,
    val precio: Double = 0.0,
)
