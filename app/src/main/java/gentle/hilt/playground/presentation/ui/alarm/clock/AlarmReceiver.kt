package gentle.hilt.playground.presentation.ui.alarm.clock

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import de.coldtea.smplr.smplralarm.R
import de.coldtea.smplr.smplralarm.alarmNotification
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI.Companion.SMPLR_ALARM_NOTIFICATION_ID
import de.coldtea.smplr.smplralarm.channel
import de.coldtea.smplr.smplralarm.smplrAlarmSet
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity
import gentle.hilt.playground.data.room.repository.AlarmEntityRepository
import gentle.hilt.playground.data.room.repository.SleepTrackingRepository
import gentle.hilt.playground.presentation.app.PlayGroundActivity
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.ALARM_TONE_URI
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.RINGTONE_ALARM
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.ACTION_DISMISS
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.ACTION_NOTIFICATION_DISMISS
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.ACTION_SNOOZE
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.ALARM
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.CREATION_DATE
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.GOAL
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.HOUR
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.IMAGE
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.MINUTE
import gentle.hilt.playground.presentation.ui.alarm.clock.lockscreen.ActivityLockScreenAlarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.Duration
import org.joda.time.LocalDate
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var dataStore: DataStoreManager

    @Inject
    lateinit var sleepRepo: SleepTrackingRepository

    @Inject
    lateinit var alarmRepo: AlarmEntityRepository

    companion object {
        var MINUTES: Int = 5

        const val FIVE_MINUTES = -200
        const val TEN_MINUTES = -100
        const val FIFTEEN_MINUTES = 0
        const val TWENTY_MINUTES = 100
        const val TWENTY_FIVE_MINUTES = 200
        const val THIRTY_MINUTES = 300

        const val SIXTY_MINUTES = 60

        const val TWENTY_THREE_HOURS = 23
    }

    override fun onReceive(context: Context, intent: Intent) {
        applicationScope.launch {
            dataStore.readDiscreteSlider.collect() {
                MINUTES = when (it) {
                    FIVE_MINUTES -> 5
                    TEN_MINUTES -> 10
                    FIFTEEN_MINUTES -> 15
                    TWENTY_MINUTES -> 20
                    TWENTY_FIVE_MINUTES -> 25
                    THIRTY_MINUTES -> 30
                    else -> {
                        15
                    }
                }
            }
        }

        val notificationId = intent.getIntExtra(SMPLR_ALARM_NOTIFICATION_ID, -1)
        RINGTONE_ALARM?.stop()

        val alarmSetHour = intent.getIntExtra(HOUR, 0)
        val alarmSetMinute = intent.getIntExtra(MINUTE, 0)
        val goal = intent.getStringExtra(GOAL)
        val snoozeCreationDate = Calendar.getInstance().time
        val creationDateLong = intent.getLongExtra(CREATION_DATE, 0)
        val id = Calendar.getInstance().time.time.toInt()
        val image = intent.getStringExtra(IMAGE)

        // Work around my saved Date inside intent, need to display statistic properly
        // for the same alarm that will ring more than several times
        val dayRightNow = DateTime(LocalDate.now().toDate()).dayOfYear
        val creationDate = Date(creationDateLong)

        val newCreationDate = Calendar.getInstance()
        newCreationDate.time = creationDate
        newCreationDate.set(Calendar.DAY_OF_YEAR, dayRightNow)

        val rightNow = DateTime(Calendar.getInstance().time)
        val tillNow = DateTime(creationDate.time)
        val durationHours = Duration(tillNow, rightNow).standardHours

        when (durationHours > 12) {
            true -> {
                applicationScope.launch {
                    sleepRepo.insertSleepTracking(
                        SleepTrackingEntity(
                            id = id,
                            hour = alarmSetHour,
                            minute = alarmSetMinute,
                            creationDate = newCreationDate.time,
                            image = image
                        )
                    )
                }
            }
            false -> {
                applicationScope.launch {
                    sleepRepo.insertSleepTracking(
                        SleepTrackingEntity(
                            id = id,
                            hour = alarmSetHour,
                            minute = alarmSetMinute,
                            creationDate = Date(creationDateLong),
                            image = image
                        )
                    )
                }
            }
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val now = getHourAndMinute()

        if (intent.action == ACTION_SNOOZE) {
            val updatedTime = addSnoozeTime(
                intent.getIntExtra(HOUR, now.first),
                intent.getIntExtra(MINUTE, now.second)
            )

            notificationManager.cancel(notificationId)

            setFullScreenIntentAlarm(
                updatedTime.first,
                updatedTime.second,
                context,
                ALARM_TONE_URI.toString(),
                snoozeCreationDate.time,
                goal.toString()
            )
        }
        if (intent.action == ACTION_DISMISS) {
            notificationManager.cancel(notificationId)
        }
        if (intent.action == ACTION_NOTIFICATION_DISMISS) {
            notificationManager.cancel(notificationId)
        }
    }

    private fun addSnoozeTime(hour: Int, minute: Int): Pair<Int, Int> {
        var mMinute = minute + MINUTES
        var mHour = hour

        if (mMinute == SIXTY_MINUTES) {
            mMinute -= SIXTY_MINUTES
        }
        mHour += 1

        if (mHour > TWENTY_THREE_HOURS) mHour = 0

        return mHour to mMinute
    }

    private fun getHourAndMinute(): Pair<Int, Int> = Calendar.getInstance().let {
        it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
    }

    private fun setFullScreenIntentAlarm(
        hour: Int,
        minute: Int,
        applicationContext: Context,
        alarmTone: String,
        creationDateSnooze: Long,
        goal: String
    ): Int {
        val onClickShortcutIntent = Intent(
            applicationContext,
            PlayGroundActivity::class.java
        )
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
            putExtra(CREATION_DATE, creationDateSnooze)
            putExtra(GOAL, goal)
        }
        val dismissIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS

            putExtra(HOUR, hour)
            putExtra(MINUTE, minute)
            putExtra(CREATION_DATE, creationDateSnooze)
        }
        val notificationDismissIntent =
            Intent(applicationContext, AlarmReceiver::class.java).apply {
                action = ACTION_NOTIFICATION_DISMISS

                putExtra(HOUR, hour)
                putExtra(MINUTE, minute)
                putExtra(CREATION_DATE, creationDateSnooze)
            }
        alarmReceivedIntent.putExtra(ALARM, alarmTone)

        fullScreenIntent.putExtra(ALARM, alarmTone)
        fullScreenIntent.putExtra(HOUR, hour)
        fullScreenIntent.putExtra(MINUTE, minute)
        fullScreenIntent.putExtra(CREATION_DATE, creationDateSnooze)
        fullScreenIntent.putExtra(GOAL, goal)

        return smplrAlarmSet(applicationContext) {
            hour { hour }
            min { minute }
            contentIntent { onClickShortcutIntent }
            receiverIntent { fullScreenIntent }
            alarmReceivedIntent { alarmReceivedIntent }
            notification {
                alarmNotification {
                    smallIcon { R.drawable.ic_baseline_notifications_active_24 }
                    title { "Snooze for $goal" }
                    bigText { "It's time to wake up already, no? " }
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
                    name { "AlarmReceiver" }
                    description { "AlarmReceiver description" }
                }
            }
        }
    }
}
