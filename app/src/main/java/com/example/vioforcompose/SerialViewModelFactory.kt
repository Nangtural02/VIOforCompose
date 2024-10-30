package com.example.vioforcompose

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.vioforcompose.ui.theme.SerialViewModel

class SerialViewModelFactory(
    private val application: Application,
    private val arViewModel: ARViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SerialViewModel::class.java)) {
            return SerialViewModel(application, arViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}