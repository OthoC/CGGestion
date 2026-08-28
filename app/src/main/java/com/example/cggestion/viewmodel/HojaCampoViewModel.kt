package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.ClienteEntity
import com.example.cggestion.data.local.entity.EstadoHoja
import com.example.cggestion.data.local.entity.HojaCampoEntity
import com.example.cggestion.data.local.entity.JornadaTrabajoEntity
import com.example.cggestion.data.local.entity.MedicionesHojaCampoEntity
import com.example.cggestion.data.local.entity.RepuestoUsadoEntity
import com.example.cggestion.data.local.entity.EvidenciaEntity
import com.example.cggestion.data.local.entity.TipoEvidencia
import com.example.cggestion.data.repository.HojaCampoRepository
import com.example.cggestion.data.repository.PrepararHojaDesdeCotizacion
import com.example.cggestion.data.HojaCampoValidaciones
import com.example.cggestion.util.firma.TrazoFirma
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

data class HojaUi(
    val hoja: HojaCampoEntity = HojaCampoEntity(numeroHoja = "", fecha = System.currentTimeMillis(), clienteId = 0, fechaCreacion = 0, fechaModificacion = 0),
    val cliente: ClienteEntity = ClienteEntity(nombre = ""),
    val mediciones: MedicionesHojaCampoEntity = MedicionesHojaCampoEntity(hojaCampoId = 0),
    val repuestos: List<RepuestoUsadoEntity> = emptyList(),
    val jornadas: List<JornadaTrabajoEntity> = emptyList(),
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val procesandoFirma: Boolean = false,
    val tieneCambios: Boolean = false,
    val mantenimientoOrigenId: Long? = null,
    val mensaje: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HojaCampoViewModel(private val repository: HojaCampoRepository) : ViewModel() {
    private val _ui = MutableStateFlow(HojaUi())
    val ui: StateFlow<HojaUi> = _ui.asStateFlow()
    val hojas = repository.resumenes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val clientes = repository.clientes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val productos = repository.productosActivos().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val equipos = ui.map { it.hoja.clienteId }.distinctUntilChanged().flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else repository.equipos(id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val evidencias = ui.map { it.hoja.id }.distinctUntilChanged().flatMapLatest { id -> if (id == 0L) flowOf(emptyList()) else repository.evidencias(id) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun nueva() = viewModelScope.launch {
        val ahora = System.currentTimeMillis()
        _ui.value = HojaUi(
            hoja = HojaCampoEntity(numeroHoja = repository.siguienteNumero(), fecha = ahora, clienteId = 0, fechaCreacion = ahora, fechaModificacion = ahora)
        )
    }

    fun cargar(id: Long) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, mensaje = null)
        runCatching { repository.completa(id) }
            .onSuccess { completa ->
                _ui.value = if (completa == null) {
                    _ui.value.copy(cargando = false, mensaje = "No se encontró la hoja seleccionada.")
                } else {
                    HojaUi(
                        hoja = completa.hoja,
                        cliente = completa.cliente,
                        mediciones = completa.mediciones ?: MedicionesHojaCampoEntity(hojaCampoId = completa.hoja.id),
                        repuestos = completa.repuestos,
                        jornadas = completa.jornadas
                    )
                }
            }
            .onFailure {
                _ui.value = _ui.value.copy(cargando = false, mensaje = "No se pudo cargar la hoja. Inténtalo nuevamente.")
            }
    }

    fun crearDesdeCotizacion(cotizacionId: Long) = viewModelScope.launch {
        _ui.value = _ui.value.copy(cargando = true, mensaje = null)
        when (val resultado = repository.prepararDesdeCotizacion(cotizacionId)) {
            is PrepararHojaDesdeCotizacion.Existente -> cargar(resultado.hojaId)
            is PrepararHojaDesdeCotizacion.Nueva -> {
                _ui.value = HojaUi(hoja = resultado.hoja, cliente = resultado.cliente)
            }
            is PrepararHojaDesdeCotizacion.Error -> {
                _ui.value = _ui.value.copy(cargando = false, mensaje = resultado.mensaje)
            }
        }
    }

    fun crearDesdeMantenimiento(contexto: ContextoMantenimientoHoja) {
        val ahora = System.currentTimeMillis()
        val equipo = contexto.equipo
        _ui.value = HojaUi(
            hoja = HojaCampoEntity(
                numeroHoja = "",
                fecha = ahora,
                clienteId = contexto.cliente.id,
                equipoId = equipo.id,
                fechaCreacion = ahora,
                fechaModificacion = ahora,
                direccion = equipo.ubicacion.ifBlank { contexto.cliente.direccion },
                telefono = contexto.cliente.telefono,
                ordenTrabajo = "Mantenimiento #${contexto.mantenimiento.id}",
                alternadorMarca = equipo.alternadorMarca,
                alternadorModelo = equipo.alternadorModelo,
                alternadorSerie = equipo.serie.orEmpty(),
                motorMarca = equipo.motorMarca,
                motorModelo = equipo.motorModelo,
                kva = equipo.potenciaKva
            ),
            cliente = contexto.cliente,
            mantenimientoOrigenId = contexto.mantenimiento.id
        )
    }

    fun seleccionarCliente(cliente: ClienteEntity) {
        _ui.value = _ui.value.copy(
            cliente = cliente,
            hoja = _ui.value.hoja.copy(
                clienteId = cliente.id,
                direccion = cliente.direccion,
                telefono = cliente.telefono,
                equipoId = null
            ),
            mensaje = null,
            tieneCambios = true
        )
    }

    fun seleccionarEquipo(equipo: com.example.cggestion.data.local.entity.EquipoEntity) {
        if (equipo.clienteId != _ui.value.hoja.clienteId) { mensajeError("El equipo no pertenece al cliente seleccionado."); return }
        actualizarHoja { h -> h.copy(equipoId = equipo.id, alternadorMarca = equipo.alternadorMarca, alternadorModelo = equipo.alternadorModelo, alternadorSerie = equipo.serie.orEmpty(), motorMarca = equipo.motorMarca, motorModelo = equipo.motorModelo, kva = equipo.potenciaKva, direccion = equipo.ubicacion.ifBlank { h.direccion }) }
    }

    fun agregarProductoComoRepuesto(producto: com.example.cggestion.data.local.entity.ProductoEntity) {
        val actuales = _ui.value.repuestos
        val indice = actuales.indexOfFirst { it.productoId == producto.id }
        val nuevos = if (indice >= 0) {
            actuales.mapIndexed { index, item -> if (index == indice) item.copy(cantidad = item.cantidad + 1) else item }
        } else {
            actuales + RepuestoUsadoEntity(
                hojaCampoId = 0,
                productoId = producto.id,
                nombre = producto.nombre,
                unidad = "Unidad",
                cantidad = 1.0,
                costoCentavos = producto.precioPredeterminadoCentavos
            )
        }
        actualizarRepuestos(nuevos)
    }

    fun prepararCaptura(): String? {
        if (_ui.value.hoja.id == 0L) { mensajeError("Guarda primero la hoja antes de añadir fotografías."); return null }
        return runCatching { repository.crearArchivoTemporal().absolutePath }.getOrElse { mensajeError("No se pudo preparar la cámara."); null }
    }

    fun puedeAgregarEvidencias(): Boolean {
        if (_ui.value.hoja.id != 0L) return true
        mensajeError("Guarda primero la hoja antes de añadir fotografías.")
        return false
    }

    fun guardarFirmaCliente(trazos: List<TrazoFirma>) {
        val estado = _ui.value
        if (estado.hoja.id == 0L || estado.procesandoFirma) {
            if (estado.hoja.id == 0L) mensajeError("Guarda primero la hoja antes de registrar la firma.")
            return
        }
        viewModelScope.launch {
            _ui.value = _ui.value.copy(procesandoFirma = true, mensaje = null)
            runCatching { repository.guardarFirmaCliente(estado.hoja.id, trazos) }
                .onSuccess { ruta ->
                    _ui.value = _ui.value.copy(
                        hoja = _ui.value.hoja.copy(firmaClienteRuta = ruta, estadoFirma = "FIRMADA"),
                        procesandoFirma = false,
                        mensaje = "Firma del cliente guardada."
                    )
                }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        procesandoFirma = false,
                        mensaje = "No se pudo guardar la firma del cliente."
                    )
                }
        }
    }

    fun eliminarFirmaCliente() {
        val estado = _ui.value
        if (estado.hoja.id == 0L || estado.procesandoFirma) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(procesandoFirma = true, mensaje = null)
            runCatching { repository.eliminarFirmaCliente(estado.hoja.id) }
                .onSuccess {
                    _ui.value = _ui.value.copy(
                        hoja = _ui.value.hoja.copy(firmaClienteRuta = null, estadoFirma = "PENDIENTE"),
                        procesandoFirma = false,
                        mensaje = "Firma del cliente eliminada."
                    )
                }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        procesandoFirma = false,
                        mensaje = "No se pudo eliminar la firma del cliente."
                    )
                }
        }
    }

    fun cancelarCaptura(ruta: String) { repository.eliminarTemporal(ruta) }

    fun guardarEvidenciaTemporal(ruta: String, descripcion: String, tipo: TipoEvidencia) = viewModelScope.launch {
        val estado = _ui.value
        if (estado.hoja.id == 0L) return@launch
        runCatching { repository.guardarEvidencia(estado.hoja.id, estado.hoja.numeroHoja, ruta, descripcion, tipo.name) }
            .onSuccess { _ui.value = _ui.value.copy(mensaje = "Evidencia guardada.") }
            .onFailure { repository.eliminarTemporal(ruta); _ui.value = _ui.value.copy(mensaje = "No se pudo guardar la evidencia.") }
    }

    fun importarEvidenciasGaleria(uris: List<android.net.Uri>) = viewModelScope.launch {
        val estado = _ui.value
        if (estado.hoja.id == 0L) return@launch
        val unicas = uris.distinct()
        var guardadas = 0
        unicas.forEach { uri ->
            if (runCatching { repository.guardarEvidenciaGaleria(estado.hoja.id, estado.hoja.numeroHoja, uri) }.isSuccess) guardadas++
        }
        _ui.value = _ui.value.copy(mensaje = if (guardadas == 0) "No se pudieron importar las imágenes." else "$guardadas evidencia(s) importada(s).")
    }

    fun actualizarEvidencia(evidencia: EvidenciaEntity, descripcion: String, tipo: TipoEvidencia) = viewModelScope.launch {
        runCatching { repository.actualizarEvidencia(evidencia.copy(descripcion = descripcion, tipoEvidencia = tipo.name)) }
            .onFailure { _ui.value = _ui.value.copy(mensaje = "No se pudo actualizar la evidencia.") }
    }

    fun guardarEvidenciaGaleria(uri: android.net.Uri, descripcion: String, tipo: TipoEvidencia) = viewModelScope.launch {
        val estado = _ui.value
        if (estado.hoja.id == 0L) return@launch
        runCatching {
            repository.guardarEvidenciaGaleria(
                estado.hoja.id,
                estado.hoja.numeroHoja,
                uri,
                descripcion,
                tipo.name
            )
        }.onSuccess {
            _ui.value = _ui.value.copy(mensaje = "Evidencia guardada.")
        }.onFailure {
            _ui.value = _ui.value.copy(mensaje = "No se pudo importar la imagen.")
        }
    }

    fun eliminarEvidencia(evidencia: EvidenciaEntity) = viewModelScope.launch {
        runCatching { repository.eliminarEvidencia(evidencia) }
            .onSuccess { _ui.value = _ui.value.copy(mensaje = "Evidencia eliminada.") }
            .onFailure { _ui.value = _ui.value.copy(mensaje = "No se pudo eliminar la evidencia.") }
    }

    fun moverEvidencia(evidencia: EvidenciaEntity, desplazamiento: Int) = viewModelScope.launch {
        runCatching { repository.moverEvidencia(evidencia, desplazamiento) }
            .onFailure { _ui.value = _ui.value.copy(mensaje = "No se pudo cambiar el orden de la evidencia.") }
    }

    fun actualizarHoja(cambio: (HojaCampoEntity) -> HojaCampoEntity) {
        _ui.value = _ui.value.copy(hoja = cambio(_ui.value.hoja), mensaje = null, tieneCambios = true)
    }

    fun actualizarCliente(cambio: (ClienteEntity) -> ClienteEntity) {
        _ui.value = _ui.value.copy(cliente = cambio(_ui.value.cliente), mensaje = null, tieneCambios = true)
    }

    fun actualizarMediciones(cambio: (MedicionesHojaCampoEntity) -> MedicionesHojaCampoEntity) {
        _ui.value = _ui.value.copy(mediciones = cambio(_ui.value.mediciones), mensaje = null, tieneCambios = true)
    }

    fun actualizarRepuestos(items: List<RepuestoUsadoEntity>) { _ui.value = _ui.value.copy(repuestos = items, tieneCambios = true) }
    fun actualizarJornadas(items: List<JornadaTrabajoEntity>) { _ui.value = _ui.value.copy(jornadas = items, tieneCambios = true) }
    fun limpiarMensaje() { _ui.value = _ui.value.copy(mensaje = null) }

    fun guardar(solicitaCompletar: Boolean) {
        val estado = _ui.value
        if (estado.guardando) return
        val completar = solicitaCompletar && estado.hoja.estado != EstadoHoja.ANULADA.name
        HojaCampoValidaciones.mediciones(estado.mediciones)?.let(::mensajeError)?.let { return }
        HojaCampoValidaciones.horometro(estado.hoja.horometro)?.let(::mensajeError)?.let { return }
        if (completar && estado.cliente.nombre.isBlank()) return mensajeError("Indica el cliente para completar la hoja.")
        if (completar && estado.hoja.tecnicos.isBlank()) return mensajeError("Indica al menos un técnico.")
        if (completar && estado.hoja.trabajosRealizados.isBlank() && estado.hoja.observaciones.isBlank()) return mensajeError("Registra el trabajo realizado u observaciones.")
        if (estado.mediciones.combustible.isNotBlank() && estado.mediciones.combustible.toDoubleOrNull()?.let { it !in 0.0..100.0 } != false) return mensajeError("El nivel de combustible debe estar entre 0 % y 100 %.")
        if (estado.hoja.horometro.isNotBlank() && estado.hoja.horometro.toDoubleOrNull()?.let { it < 0.0 } != false) return mensajeError("El horómetro no puede ser negativo.")

        viewModelScope.launch {
            _ui.value = estado.copy(guardando = true, mensaje = null)
            runCatching {
                repository.guardar(
                    hoja = estado.hoja.copy(
                        estado = when {
                            !solicitaCompletar -> EstadoHoja.BORRADOR.name
                            estado.hoja.estado == EstadoHoja.ANULADA.name -> EstadoHoja.ANULADA.name
                            else -> EstadoHoja.COMPLETADA.name
                        }
                    ),
                    cliente = estado.cliente,
                    mediciones = estado.mediciones,
                    repuestos = estado.repuestos,
                    jornadas = estado.jornadas
                )
            }.onSuccess { resultado ->
                if (estado.mantenimientoOrigenId != null) {
                    repository.vincularMantenimiento(estado.mantenimientoOrigenId, resultado.id, solicitaCompletar)
                }
                _ui.value = _ui.value.copy(
                    hoja = _ui.value.hoja.copy(id = resultado.id, numeroHoja = resultado.numero),
                    guardando = false,
                    tieneCambios = false,
                    mensaje = "Hoja ${resultado.numero} guardada correctamente."
                )
            }.onFailure {
                _ui.value = _ui.value.copy(guardando = false, mensaje = "No se pudo guardar la hoja. Inténtalo nuevamente.")
            }
        }
    }

    private fun mensajeError(texto: String) { _ui.value = _ui.value.copy(mensaje = texto) }

    companion object {
        fun factory(repository: HojaCampoRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = HojaCampoViewModel(repository) as T
        }
    }
}
