package app.vibedeck.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ScreenViewModelFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <M : ViewModel> create(modelClass: Class<M>): M = create() as M
}
