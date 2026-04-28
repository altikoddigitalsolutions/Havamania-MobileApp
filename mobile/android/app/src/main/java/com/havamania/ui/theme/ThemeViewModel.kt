package com.havamania.ui.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentTheme = MutableStateFlow(AppTheme.DARK)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    init {
        // Uygulama başladığında kaydedilen temayı yükle
        viewModelScope.launch {
            ThemeManager.getTheme(getApplication()).collect {
                _currentTheme.value = it
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            ThemeManager.saveTheme(getApplication(), theme)
            _currentTheme.value = theme
        }
    }
}
