package gentle.hilt.playground.presentation.ui.maps

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
class MapScreenViewModel @Inject constructor(
    val dataStore: DataStoreManager,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    fun saveFirstPoint(firstPoint: String) = viewModelScope.launch(dispatcher) {
        dataStore.saveFirstPoint(firstPoint)
    }

    fun deleteFirstPoint() = viewModelScope.launch {
        dataStore.deleteFirstPoint()
    }

    val readFirstPoint = dataStore.readFirstPoint.asLiveData()

    fun saveFirstPointLongitude(firstPointLongitude: Double) = viewModelScope.launch {
        dataStore.saveFirstPointLongitude(firstPointLongitude)
    }

    fun deleteFirstPointLongitude() = viewModelScope.launch {
        dataStore.deleteFirstPointLongitude()
    }

    val readFirstPointLongitude = dataStore.readFirstPointLongitude.asLiveData()

    fun saveFirstPointLatitude(firstPointLatitude: Double) = viewModelScope.launch {
        dataStore.saveFirstPointLatitude(firstPointLatitude)
    }

    fun deleteFirstPointLatitude() = viewModelScope.launch {
        dataStore.deleteFirstPointLatitude()
    }

    val readFirstPointLatitude = dataStore.readFirstPointLatitude.asLiveData()

    fun saveSecondPoint(secondPoint: String) = viewModelScope.launch {
        dataStore.saveSecondPoint(secondPoint)
    }

    fun deleteSecondPoint() = viewModelScope.launch {
        dataStore.deleteSecondPoint()
    }

    val readSecondPoint = dataStore.readSecondPoint.asLiveData()

    fun saveSecondPointLongitude(secondPointLongitude: Double) = viewModelScope.launch {
        dataStore.saveSecondPointLongitude(secondPointLongitude)
    }

    fun deleteSecondPointLongitude() = viewModelScope.launch {
        dataStore.deleteSecondPointLongitude()
    }

    val readSecondPointLongitude = dataStore.readSecondPointLongitude.asLiveData()

    fun saveSecondPointLatitude(secondPointLatitude: Double) = viewModelScope.launch {
        dataStore.saveSecondPointLatitude(secondPointLatitude)
    }

    fun deleteSecondPointLatitude() = viewModelScope.launch {
        dataStore.deleteSecondPointLatitude()
    }

    val readSecondPointLatitude = dataStore.readSecondPointLatitude.asLiveData()

    fun savePreviouslyChosenLocation(location: Int) = viewModelScope.launch {
        dataStore.savePointLocation(location)
    }

    val readUserPreloadLocationChoice = dataStore.readPreloadLocationChoice.asLiveData()

    fun saveRouteType(userChoice: Int) = viewModelScope.launch {
        dataStore.saveRouteType(userChoice)
    }

    val readRouteType = dataStore.readRouteType.asLiveData()

    fun saveUserZoomChange(userZoom: Float) = viewModelScope.launch {
        dataStore.saveUserZoomChange(userZoom)
    }

    val readUserZoomChange = dataStore.readUserZoomChange.asLiveData()

    fun saveUserTrackingChoice(userTrackingChoice: Boolean) = viewModelScope.launch {
        dataStore.saveUserTrackingChoice(userTrackingChoice)
    }

    val readUserTrackingChoice = dataStore.readUserTrackingChoice.asLiveData()

    fun saveUserCameraLocationLatitude(cameraLatitude: Double) = viewModelScope.launch {
        dataStore.saveUserCameraLocationLatitude(cameraLatitude)
    }

    fun deleteUserCameraLocationLatitude() = viewModelScope.launch {
        dataStore.deleteUserCameraLocationLatitude()
    }

    val readUserCameraLocationLatitude = dataStore.readUserCameraPositionLatitude.asLiveData()

    fun saveUserCameraLocationLongitude(cameraLongitude: Double) = viewModelScope.launch {
        dataStore.saveUserCameraLocationLongitude(cameraLongitude)
    }

    fun deleteUserCameraLocationLongitude() = viewModelScope.launch {
        dataStore.deleteUserCameraLocationLongitude()
    }

    val readUserCameraLocationLongitude = dataStore.readUserCameraLocationLongitude.asLiveData()
}
