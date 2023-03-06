package gentle.hilt.playground.presentation.ui.timer

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
class TimerViewModel @Inject constructor(
    val dataStore: DataStoreManager,
    @DefaultDispatcher val dispatcher: CoroutineDispatcher
) : ViewModel() {
    val futureTime = dataStore.readFutureTimeInSeconds.asLiveData()
    val countDownTime = dataStore.readCountDownTimeInSeconds.asLiveData()

    fun saveFutureTimeInSeconds(time: Long) = viewModelScope.launch(dispatcher) {
        dataStore.saveFutureTimeInSeconds(time)
    }
}
