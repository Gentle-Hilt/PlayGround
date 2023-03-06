package gentle.hilt.playground.presentation.ui.sleepStatistic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gentle.hilt.playground.data.room.repository.SleepTrackingRepository
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SleepStatisticViewModel @Inject constructor(
    private val repository: SleepTrackingRepository
) : ViewModel() {

    val sleepTrackingEntities = repository.observeAllSleepingTrackingEntities()

    fun insertSleepingTrackEntityIntoDB(trackingEntity: SleepTrackingEntity) {
        viewModelScope.launch {
            repository.insertSleepTracking(trackingEntity)
        }
    }

    fun deleteSleepingStatistic() = viewModelScope.launch {
        repository.deleteSleepTrackingStatistic()
    }
}
