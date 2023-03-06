package gentle.hilt.playground.presentation.ui.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OptionsViewModel @Inject constructor(
    val dataStore: DataStoreManager,
    @DefaultDispatcher val dispatcher: CoroutineDispatcher
) : ViewModel() {

    fun saveDiscreteSlider(discreteSlider: Int) = viewModelScope.launch(dispatcher) {
        dataStore.saveDiscreteSlider(discreteSlider)
    }

    val readDiscreteSliderValue = dataStore.readDiscreteSlider.asLiveData()

    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        dataStore.setDarkMode(enabled)
    }
}
