package gentle.hilt.playground.presentation.ui.alarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.coldtea.smplr.smplralarm.R
import de.coldtea.smplr.smplralarm.alarmNotification
import de.coldtea.smplr.smplralarm.channel
import de.coldtea.smplr.smplralarm.smplrAlarmSet
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.data.room.entity.AlarmEntity
import gentle.hilt.playground.data.room.repository.AlarmEntityRepository
import gentle.hilt.playground.di.DefaultDispatcher
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.REQUEST_CODE
import gentle.hilt.playground.presentation.ui.alarm.clock.AlarmBroadcastReceiver
import gentle.hilt.playground.presentation.ui.alarm.clock.AlarmReceiver
import gentle.hilt.playground.presentation.ui.alarm.clock.lockscreen.ActivityLockScreenAlarm
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    val dataStore: DataStoreManager,
    private val alarmRepository: AlarmEntityRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AlarmEntity>>(UiState.Loading)
    val uiState: StateFlow<UiState<AlarmEntity>> = _uiState

    val observeAlarmEntities = alarmRepository.observeAllAlarmEntities()

    fun saveUserAlarmTittle(title: String) {
        viewModelScope.launch(dispatcher) {
            dataStore.saveUserAlarmTittle(title)
        }
    }

    val readUserAlarmTittle = dataStore.readUserAlarmTittle.asLiveData()

    fun savePreviousAlarmUserImage(image: Uri) = viewModelScope.launch {
        dataStore.savePreviousAlarmUserImage(image.toString())
    }

    val userPreviousImage = dataStore.readPreviousAlarmUserImage.asLiveData()

    fun saveFingerState(state: Boolean) = viewModelScope.launch {
        dataStore.saveFingerState(state)
    }

    val readFingerState = dataStore.readFingerState.asLiveData()

    fun updateAlarmInDb(data: AlarmEntity) = viewModelScope.launch {
        alarmRepository.update(data)
    }

    fun insertEntityAlarmIntoDb(alarm: AlarmEntity) = viewModelScope.launch(dispatcher) {
        alarmRepository.insertEntityAlarm(alarm)

        _uiState.value = UiState.Success(alarm)
    }

    fun deleteEntityAlarmFromDb(alarm: AlarmEntity) = viewModelScope.launch {
        alarmRepository.deleteEntityAlarm(alarm)
    }

    fun createAlarm(
        id: Int,
        timerGoal: String,
        image: String,
        hour: Int,
        minute: Int,
        requestCode: String,
        isEnabled: Boolean,
        creationDate: Date
    ) {
        val itemAlarmEntity = AlarmEntity(
            id,
            timerGoal,
            image,
            hour,
            minute,
            requestCode,
            isEnabled,
            creationDate
        )
        insertEntityAlarmIntoDb(itemAlarmEntity)
    }

    fun setFullScreenIntentAlarm(
        hour: Int,
        minute: Int,
        applicationContext: Context,
        alarmTone: String,
        creationDate: Long,
        goal: String,
        image: String
    ): Int {
        val fullScreenIntent = Intent(
            applicationContext,
            ActivityLockScreenAlarm::class.java
        )

        val alarmReceivedIntent = Intent(
            applicationContext,
            AlarmBroadcastReceiver::class.java
        )

        val snoozeIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE

            putExtra(HOUR, hour)
            putExtra(MINUTE, minute)
            putExtra(CREATION_DATE, creationDate)
            putExtra(GOAL, goal)
            putExtra(IMAGE, image)
        }

        val dismissIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(ALARM, alarmTone)

            putExtra(HOUR, hour)
            putExtra(MINUTE, minute)
            putExtra(CREATION_DATE, creationDate)
            putExtra(IMAGE, image)
        }

        val notificationDismissIntent =
            Intent(applicationContext, AlarmReceiver::class.java).apply {
                action = ACTION_NOTIFICATION_DISMISS

                putExtra(HOUR, hour)
                putExtra(MINUTE, minute)
                putExtra(CREATION_DATE, creationDate)
                putExtra(IMAGE, image)
            }

        alarmReceivedIntent.putExtra(ALARM, alarmTone)

        fullScreenIntent.putExtra(HOUR, hour)
        fullScreenIntent.putExtra(MINUTE, minute)
        fullScreenIntent.putExtra(CREATION_DATE, creationDate)
        fullScreenIntent.putExtra(GOAL, goal)
        fullScreenIntent.putExtra(IMAGE, image)

        return smplrAlarmSet(applicationContext) {
            requestCode { REQUEST_CODE }
            hour { hour }
            min { minute }
            receiverIntent { fullScreenIntent }
            alarmReceivedIntent { alarmReceivedIntent }
            notification {
                alarmNotification {
                    smallIcon { R.drawable.ic_baseline_notifications_active_24 }
                    title { "Alarm" }
                    bigText { "it's time for $goal" }
                    autoCancel { true }
                    firstButtonText { "Snooze" }
                    secondButtonText { "Dismiss" }
                    firstButtonIntent { snoozeIntent }
                    secondButtonIntent { dismissIntent }
                    notificationDismissedIntent { notificationDismissIntent }
                }
            }
            notificationChannel {
                channel {
                    importance { NotificationManager.IMPORTANCE_HIGH }
                    showBadge { false }
                    name { "Alarm" }
                    description { "it's your Alarm!" }
                }
            }
        }
    }

    companion object {
        internal const val ACTION_SNOOZE = "action_snooze"
        internal const val ACTION_DISMISS = "action_dismiss"
        internal const val ACTION_NOTIFICATION_DISMISS = "action_notification_dismiss"
        internal const val ALARM = "alarmTone"
        internal const val HOUR = "hour"
        internal const val MINUTE = "minute"
        internal const val CREATION_DATE = "creation_date"
        internal const val GOAL = "goal"
        internal const val IMAGE = "image"
    }
}
