package com.example.cggestion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EstadoHoja { BORRADOR, COMPLETADA, ANULADA }
enum class EstadoControl { NO_REVISADO, CORRECTO, REQUIERE_ATENCION, NO_APLICA }
enum class TipoEvidencia { ANTES, DURANTE, DESPUES, MEDICION, REPUESTO, OTRO }

@Entity(tableName="hojas_campo", foreignKeys=[ForeignKey(entity=ClienteEntity::class,parentColumns=["id"],childColumns=["clienteId"],onDelete=ForeignKey.RESTRICT),ForeignKey(entity=CotizacionEntity::class,parentColumns=["id"],childColumns=["cotizacionId"],onDelete=ForeignKey.SET_NULL)], indices=[Index(value=["numeroHoja"],unique=true),Index(value=["clienteId"]),Index(value=["cotizacionId"]),Index(value=["equipoId"])])
data class HojaCampoEntity(
 @PrimaryKey(autoGenerate=true) val id:Long=0,val numeroHoja:String,val fecha:Long,val clienteId:Long,val cotizacionId:Long?=null,val equipoId:Long?=null,val estado:String=EstadoHoja.BORRADOR.name,val fechaCreacion:Long,val fechaModificacion:Long,
 val direccion:String="",val telefono:String="",val ordenTrabajo:String="",val tecnicos:String="",
 val alternadorMarca:String="",val alternadorModelo:String="",val alternadorSerie:String="",val rpm:String="",val kva:String="",val volt:String="",val kw:String="",val amp:String="",val hz:String="",
 val motorMarca:String="",val motorModelo:String="",val motorSerie:String="",val horometro:String="",val tipoTablero:String="AUT",
 val trabajosRealizados:String="",val observaciones:String="",val horaInicioPruebas:String="",val horaFinPruebas:String="",val nombreClienteResponsable:String="",val estadoFirma:String="PENDIENTE"
)
@Entity(tableName="mediciones_hoja_campo", foreignKeys=[ForeignKey(entity=HojaCampoEntity::class,parentColumns=["id"],childColumns=["hojaCampoId"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["hojaCampoId"],unique=true)])
data class MedicionesHojaCampoEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val hojaCampoId:Long,val l1l2:String="",val l2l3:String="",val l3l1:String="",val ln:String="",val ampL1:String="",val ampL2:String="",val ampL3:String="",val hzVacio:String="",val hzCarga:String="",val rpmVacio:String="",val rpmCarga:String="",val presionAceite:String="",val temperaturaMotor:String="",val cargaAlternador:String="",val nivelAceite:String="",val nivelRefrigerante:String="",val voltajeBateria:String="",val combustible:String="",val limpieza:String=EstadoControl.NO_REVISADO.name,val electrolitos:String=EstadoControl.NO_REVISADO.name,val mantenedorBateria:String=EstadoControl.NO_REVISADO.name,val precalentadorBlock:String=EstadoControl.NO_REVISADO.name)
@Entity(tableName="repuestos_usados", foreignKeys=[ForeignKey(entity=HojaCampoEntity::class,parentColumns=["id"],childColumns=["hojaCampoId"],onDelete=ForeignKey.CASCADE),ForeignKey(entity=ProductoEntity::class,parentColumns=["id"],childColumns=["productoId"],onDelete=ForeignKey.SET_NULL)],indices=[Index(value=["hojaCampoId"]),Index(value=["productoId"])])
data class RepuestoUsadoEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val hojaCampoId:Long,val productoId:Long?=null,val codigo:String="",val nombre:String,val unidad:String="Unidad",val cantidad:Double=1.0,val costoCentavos:Long=0)
@Entity(tableName="jornadas_trabajo",foreignKeys=[ForeignKey(entity=HojaCampoEntity::class,parentColumns=["id"],childColumns=["hojaCampoId"],onDelete=ForeignKey.CASCADE)],indices=[Index(value=["hojaCampoId"])])
data class JornadaTrabajoEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val hojaCampoId:Long,val fecha:Long,val horaInicio:String,val horaFin:String,val minutosTotales:Long,val tecnicos:String,val observacion:String="")

@Entity(
 tableName = "evidencias",
 foreignKeys = [ForeignKey(entity = HojaCampoEntity::class, parentColumns = ["id"], childColumns = ["hojaCampoId"], onDelete = ForeignKey.CASCADE)],
 indices = [Index(value = ["hojaCampoId"])]
)
data class EvidenciaEntity(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val hojaCampoId: Long,
 val rutaInterna: String,
 val nombreArchivo: String,
 val descripcion: String = "",
 val tipoEvidencia: String = TipoEvidencia.OTRO.name,
 val fechaHoraCaptura: Long = System.currentTimeMillis(),
 val fechaHoraRegistro: Long = System.currentTimeMillis(),
 val orden: Int = 0,
 val tamanoBytes: Long = 0,
 val ancho: Int? = null,
 val alto: Int? = null
)
