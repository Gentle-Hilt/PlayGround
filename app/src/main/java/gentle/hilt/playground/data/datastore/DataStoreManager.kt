package gentle.hilt.playground.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.COUNT_DOWN_TIMER
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.DARK_MODE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.DISCRETE_SLIDER
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.FINGER_VISIBILITY
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.FIRST_POINT
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.FIRST_POINT_LATITUDE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.FIRST_POINT_LONGITUDE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.FUTURE_TIME_TIMER
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.USER_PRELOAD_LOCATION_CHOICE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.PREVIOUS_TITLE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.ROUTE_TRANSPORT_TYPE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.SECOND_POINT
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.SECOND_POINT_LATITUDE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.SECOND_POINT_LONGITUDE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.TURN_USER_TRACKING
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.USED_IMAGES
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.USER_CAMERA_LOCATION_LATITUDE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.USER_CAMERA_LOCATION_LONGITUDE
import gentle.hilt.playground.data.datastore.DataStoreManager.PreferencesKeys.USER_ZOOM_CHANGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

val Context.dataStore by preferencesDataStore("datastore")

class DataStoreManager @Inject constructor(
    @ApplicationContext appContext: Context
) {
    val dataStore = appContext.dataStore

    private object PreferencesKeys {
        val USED_IMAGES = stringPreferencesKey("used_images")
        val FINGER_VISIBILITY = booleanPreferencesKey("finger_visibility")
        val PREVIOUS_TITLE = stringPreferencesKey("previous_title")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val DISCRETE_SLIDER = intPreferencesKey("discrete_slider")
        val COUNT_DOWN_TIMER = longPreferencesKey("count_down_timer")
        val FUTURE_TIME_TIMER = longPreferencesKey("future_time_timer")

        val FIRST_POINT = stringPreferencesKey("first_point")
        val FIRST_POINT_LONGITUDE = doublePreferencesKey("first_point_longitude")
        val FIRST_POINT_LATITUDE = doublePreferencesKey("first_point_latitude")

        val SECOND_POINT = stringPreferencesKey("second_point")
        val SECOND_POINT_LONGITUDE = doublePreferencesKey("second_point_longitude")
        val SECOND_POINT_LATITUDE = doublePreferencesKey("second_point_latitude")

        val USER_CAMERA_LOCATION_LATITUDE = doublePreferencesKey("user_camera_location_latitude")
        val USER_CAMERA_LOCATION_LONGITUDE = doublePreferencesKey("user_camera_location_longitude")

        val USER_PRELOAD_LOCATION_CHOICE = intPreferencesKey("user_preload_location_choice")
        val ROUTE_TRANSPORT_TYPE = intPreferencesKey("route_transport_type")

        val USER_ZOOM_CHANGE = floatPreferencesKey("user_zoom_change")
        val TURN_USER_TRACKING = booleanPreferencesKey("turn_user_tracking")
    }

    companion object {
        const val USER_ROUTE_TYPE_DEFAULT_CAR = 2
        const val USER_ZOOM_DEFAULT_MIN = 14f
        const val USER_PRELOAD_LOCATION_CAMERA_DEFAULT = 3
        const val SNOOZE_TIME_15_MINUTES = 0
    }

    suspend fun savePreviousAlarmUserImage(string: String) = dataStore.edit { preferences ->
        preferences[USED_IMAGES] = string
    }

    val readPreviousAlarmUserImage: Flow<String> = dataStore.data.map { preferences ->
        preferences[USED_IMAGES] ?: ""
    }

    suspend fun saveFingerState(boolean: Boolean) = dataStore.edit { preferences ->
        preferences[FINGER_VISIBILITY] = boolean
    }

    val readFingerState: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[FINGER_VISIBILITY] ?: true
    }

    suspend fun saveUserAlarmTittle(tittle: String) = dataStore.edit { preferences ->
        preferences[PREVIOUS_TITLE] = tittle
    }

    val readUserAlarmTittle: Flow<String> = dataStore.data.map { preferences ->
        preferences[PREVIOUS_TITLE] ?: ""
    }

    suspend fun setDarkMode(enabled: Boolean) = runBlocking {
        withContext(Dispatchers.Default) {
            dataStore.edit { preferences ->
                preferences[DARK_MODE] = enabled
            }
        }
    }

    val darkModeEnabled: Flow<Boolean> = runBlocking {
        withContext(Dispatchers.Default) {
            dataStore.data.map { preferences ->
                preferences[DARK_MODE] ?: false
            }
        }
    }

    suspend fun saveDiscreteSlider(discreteSlider: Int) = dataStore.edit { preferences ->
        preferences[DISCRETE_SLIDER] = discreteSlider
    }

    val readDiscreteSlider: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DISCRETE_SLIDER] ?: SNOOZE_TIME_15_MINUTES
    }

    suspend fun saveCountDownTimeInSeconds(countDown: Long) = dataStore.edit { preferences ->
        preferences[COUNT_DOWN_TIMER] = countDown
    }

    val readCountDownTimeInSeconds: Flow<Long> = dataStore.data.map { preferences ->
        preferences[COUNT_DOWN_TIMER] ?: 0
    }

    suspend fun saveFutureTimeInSeconds(futureTime: Long) = dataStore.edit { preferences ->
        preferences[FUTURE_TIME_TIMER] = futureTime
    }

    val readFutureTimeInSeconds: Flow<Long> = dataStore.data.map { preferences ->
        preferences[FUTURE_TIME_TIMER] ?: 0
    }

    suspend fun saveFirstPoint(firstPoint: String) = dataStore.edit { preferences ->
        preferences[FIRST_POINT] = firstPoint
    }

    suspend fun deleteFirstPoint() = dataStore.edit { preferences ->
        preferences.remove(FIRST_POINT)
    }

    val readFirstPoint: Flow<String> = dataStore.data.map { preferences ->
        preferences[FIRST_POINT] ?: ""
    }

    suspend fun saveFirstPointLongitude(firstPointLongitude: Double) = dataStore.edit { preferences ->
        preferences[FIRST_POINT_LONGITUDE] = firstPointLongitude
    }

    suspend fun deleteFirstPointLongitude() = dataStore.edit { preferences ->
        preferences.remove(FIRST_POINT_LONGITUDE)
    }

    val readFirstPointLongitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[FIRST_POINT_LONGITUDE] ?: 0.0
    }

    suspend fun saveFirstPointLatitude(firstPointLatitude: Double) = dataStore.edit { preferences ->
        preferences[FIRST_POINT_LATITUDE] = firstPointLatitude
    }

    suspend fun deleteFirstPointLatitude() = dataStore.edit { preferences ->
        preferences.remove(FIRST_POINT_LATITUDE)
    }

    val readFirstPointLatitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[FIRST_POINT_LATITUDE] ?: 0.0
    }

    suspend fun saveSecondPoint(secondPoint: String) = dataStore.edit { preferences ->
        preferences[SECOND_POINT] = secondPoint
    }

    suspend fun deleteSecondPoint() = dataStore.edit { preferences ->
        preferences.remove(SECOND_POINT)
    }

    val readSecondPoint: Flow<String> = dataStore.data.map { preferences ->
        preferences[SECOND_POINT] ?: ""
    }

    suspend fun saveSecondPointLongitude(secondPointLongitude: Double) = dataStore.edit { preferences ->
        preferences[SECOND_POINT_LONGITUDE] = secondPointLongitude
    }

    suspend fun deleteSecondPointLongitude() = dataStore.edit { preferences ->
        preferences.remove(SECOND_POINT_LONGITUDE)
    }

    val readSecondPointLongitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[SECOND_POINT_LONGITUDE] ?: 0.0
    }

    suspend fun saveSecondPointLatitude(secondPointLatitude: Double) = dataStore.edit { preferences ->
        preferences[SECOND_POINT_LATITUDE] = secondPointLatitude
    }

    suspend fun deleteSecondPointLatitude() = dataStore.edit { preferences ->
        preferences.remove(SECOND_POINT_LATITUDE)
    }

    val readSecondPointLatitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[SECOND_POINT_LATITUDE] ?: 0.0
    }

    suspend fun savePointLocation(userChoice: Int) = dataStore.edit { preferences ->
        preferences[USER_PRELOAD_LOCATION_CHOICE] = userChoice
    }

    val readPreloadLocationChoice: Flow<Int> = dataStore.data.map { preferences ->
        preferences[USER_PRELOAD_LOCATION_CHOICE] ?: USER_PRELOAD_LOCATION_CAMERA_DEFAULT
    }

    suspend fun saveRouteType(userChoice: Int) = dataStore.edit { preferences ->
        preferences[ROUTE_TRANSPORT_TYPE] = userChoice
    }

    val readRouteType: Flow<Int> = dataStore.data.map { preferences ->
        preferences[ROUTE_TRANSPORT_TYPE] ?: USER_ROUTE_TYPE_DEFAULT_CAR
    }

    suspend fun saveUserZoomChange(userZoom: Float) = dataStore.edit { preferences ->
        preferences[USER_ZOOM_CHANGE] = userZoom
    }

    val readUserZoomChange: Flow<Float> = dataStore.data.map { preferences ->
        preferences[USER_ZOOM_CHANGE] ?: USER_ZOOM_DEFAULT_MIN
    }

    suspend fun saveUserTrackingChoice(userTrackingChoice: Boolean) = dataStore.edit { preferences ->
        preferences[TURN_USER_TRACKING] = userTrackingChoice
    }

    val readUserTrackingChoice: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[TURN_USER_TRACKING] ?: false
    }

    suspend fun saveUserCameraLocationLatitude(latitude: Double) = dataStore.edit { preferences ->
        preferences[USER_CAMERA_LOCATION_LATITUDE] = latitude
    }

    suspend fun deleteUserCameraLocationLatitude() = dataStore.edit { preferences ->
        preferences.remove(USER_CAMERA_LOCATION_LATITUDE)
    }

    val readUserCameraPositionLatitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[USER_CAMERA_LOCATION_LATITUDE] ?: 0.0
    }

    suspend fun saveUserCameraLocationLongitude(longitude: Double) = dataStore.edit { preferences ->
        preferences[USER_CAMERA_LOCATION_LONGITUDE] = longitude
    }

    suspend fun deleteUserCameraLocationLongitude() = dataStore.edit { preferences ->
        preferences.remove(USER_CAMERA_LOCATION_LONGITUDE)
    }

    val readUserCameraLocationLongitude: Flow<Double> = dataStore.data.map { preferences ->
        preferences[USER_CAMERA_LOCATION_LONGITUDE] ?: 0.0
    }
}
