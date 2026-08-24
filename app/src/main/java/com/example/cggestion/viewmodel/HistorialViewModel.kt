package com.example.cggestion.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cggestion.data.local.entity.CotizacionResumen
import com.example.cggestion.data.repository.CotizacionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistorialViewModel(repository: CotizacionRepository) : ViewModel() {
    val cotizaciones: StateFlow<List<CotizacionResumen>> = repository.resumenesCotizaciones().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    companion object { fun factory(repository: CotizacionRepository) = object : ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = HistorialViewModel(repository) as T } }
}
