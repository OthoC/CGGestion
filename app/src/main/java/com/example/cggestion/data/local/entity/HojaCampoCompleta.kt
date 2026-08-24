package com.example.cggestion.data.local.entity
import androidx.room.Embedded
import androidx.room.Relation
data class HojaCampoCompleta(@Embedded val hoja:HojaCampoEntity,@Relation(parentColumn="clienteId",entityColumn="id") val cliente:ClienteEntity,@Relation(parentColumn="id",entityColumn="hojaCampoId") val mediciones:MedicionesHojaCampoEntity?,@Relation(parentColumn="id",entityColumn="hojaCampoId") val repuestos:List<RepuestoUsadoEntity>,@Relation(parentColumn="id",entityColumn="hojaCampoId") val jornadas:List<JornadaTrabajoEntity>,@Relation(parentColumn="id",entityColumn="hojaCampoId") val evidencias:List<EvidenciaEntity>)
data class HojaCampoResumen(@Embedded val hoja:HojaCampoEntity,val clienteNombre:String,val evidenciasCantidad:Int = 0)
