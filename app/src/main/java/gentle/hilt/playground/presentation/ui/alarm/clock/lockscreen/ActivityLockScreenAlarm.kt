package gentle.hilt.playground.presentation.ui.alarm.clock.lockscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import app.futured.hauler.setOnDragDismissedListener
import dagger.hilt.android.AndroidEntryPoint
import de.coldtea.smplr.smplralarm.alarmNotification
import de.coldtea.smplr.smplralarm.channel
import de.coldtea.smplr.smplralarm.smplrAlarmSet
import gentle.hilt.playground.R
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity
import gentle.hilt.playground.databinding.ActivityLockScreenAlarmBinding
import gentle.hilt.playground.presentation.app.PlayGroundActivity
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.ALARM_TONE_URI
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.REQUEST_CODE
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.RINGTONE_ALARM
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.ALARM
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.CREATION_DATE
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.GOAL
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.HOUR
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.IMAGE
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.MINUTE
import gentle.hilt.playground.presentation.ui.alarm.clock.AlarmBroadcastReceiver
import gentle.hilt.playground.presentation.ui.alarm.clock.AlarmReceiver
import gentle.hilt.playground.presentation.ui.sleepStatistic.SleepStatisticViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ActivityLockScreenAlarm : AppCompatActivity() {

    @Inject
    lateinit var dataStore: DataStoreManager

    private val viewModel: SleepStatisticViewModel by viewModels()

    private lateinit var binding: ActivityLockScreenAlarmBinding

    companion object {
        const val FIVE_MINUTES = -200
        const val TEN_MINUTES = -100
        const val FIFTEEN_MINUTES = 0
        const val TWENTY_MINUTES = 100
        const val TWENTY_FIVE_MINUTES = 200
        const val THIRTY_MINUTES = 300
        const val TWELVE_HOURS = 12
    }

    private fun Activity.activateLockScreen() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        with(getSystemService(KEYGUARD_SERVICE) as KeyguardManager) {
            requestDismissKeyguard(this@activateLockScreen, null)
        }
    }

    private fun Activity.deactivateLockScreen() {
        setShowWhenLocked(false)
        setTurnScreenOn(false)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var minutes = 5

        lifecycleScope.launch {
            dataStore.readDiscreteSlider.collect() {
                minutes = when (it) {
                    FIVE_MINUTES -> 5
                    TEN_MINUTES -> 10
                    FIFTEEN_MINUTES -> 15
                    TWENTY_MINUTES -> 20
                    TWENTY_FIVE_MINUTES -> 25
                    THIRTY_MINUTES -> 30
                    else -> {
                        FIFTEEN_MINUTES
                    }
                }
            }
        }

        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        binding = ActivityLockScreenAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val currentTime = Calendar.getInstance()
        val currentHour = getCalendar().get(Calendar.HOUR_OF_DAY)
        val currentMinute = getCalendar().get(Calendar.MINUTE) + minutes

        val alarmSetHour = intent.getIntExtra(HOUR, 0)
        val alarmSetMinute = intent.getIntExtra(MINUTE, 0)

        val creationDate = intent.getLongExtra(CREATION_DATE, 0)
        val snoozeCreationDate = Calendar.getInstance().time
        val goal = intent.getStringExtra(GOAL)
        val id = Calendar.getInstance().time.time.toInt()
        val image = intent.getStringExtra(IMAGE)

        hideSystemUI()

        binding.apply {
            textSnooze.text = "${applicationContext.getString(R.string.Swipe)} $minutes ${applicationContext.getString(R.string.minutes)}"
            tvGoal.text = "${applicationContext.getString(R.string.it_s_time_for)} $goal"

            if (DateFormat.is24HourFormat(applicationContext)) {
                lockScreenAmPm.text = null
                lockScreenAlarmTime.text = formattedDate24Hours(currentTime.time)
                lockScreenWeekDay.text = formattedDate24Week(currentTime.time)
            } else {
                lockScreenAmPm.text = if (currentHour < TWELVE_HOURS) "AM" else "PM"
                lockScreenAlarmTime.text = formattedDatePmAm(currentTime.time)
                lockScreenWeekDay.text = formattedDateAmPmWeek(currentTime.time)
            }
            dragDownView.setOnDragDismissedListener {
                NotificationManagerCompat.from(applicationContext).cancelAll()

                viewModel.insertSleepingTrackEntityIntoDB(
                    SleepTrackingEntity(
                        id = id,
                        hour = alarmSetHour,
                        minute = alarmSetMinute,
                        creationDate = Date(creationDate),
                        image = image
                    )
                )

                setFullScreenIntentAlarm(
                    currentHour,
                    currentMinute,
                    applicationContext,
                    ALARM_TONE_URI.toString(),
                    snoozeCreationDate.time,
                    goal.toString(),
                    image
                )

                RINGTONE_ALARM?.stop()
                finishAffinity()
            }

            turnOffAlarm.setOnClickListener {
                viewModel.insertSleepingTrackEntityIntoDB(
                    SleepTrackingEntity(
                        id = id,
                        hour = alarmSetHour,
                        minute = alarmSetMinute,
                        creationDate = Date(creationDate),
                        image = image
                    )
                )

                NotificationManagerCompat.from(applicationContext).cancelAll()
                finishAffinity()
            }
        }

        activateLockScreen()
    }

    override fun onDestroy() {
        super.onDestroy()
        RINGTONE_ALARM?.stop()
        deactivateLockScreen()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.dragDownView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun getCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = Date()
        return calendar
    }

    private fun setFullScreenIntentAlarm(
        hour: Int,
        minute: Int,
        applicationContext: Context,
        alarmTone: String,
        snoozeCreationDate: Long,
        goal: String,
        image: String?
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
            action = AlarmsViewModel.ACTION_SNOOZE

            putExtra(HOUR, hour)
            putExtra(MINUTE, minute)
            putExtra(CREATION_DATE, snoozeCreationDate)
            putExtra(GOAL, goal)
            putExtra(IMAGE, image)
        }
        val dismissIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
            action = AlarmsViewModel.ACTION_DISMISS
            putExtra(ALARM, alarmTone)

            putExtra(HOUR, hour)
            putExtra(MINUTE, minute)
            putExtra(CREATION_DATE, snoozeCreationDate)
            putExtra(IMAGE, image)
        }
        val notificationDismissIntent =
            Intent(applicationContext, AlarmReceiver::class.java).apply {
                action = AlarmsViewModel.ACTION_NOTIFICATION_DISMISS

                putExtra(HOUR, hour)
                putExtra(MINUTE, minute)
                putExtra(CREATION_DATE, snoozeCreationDate)
                putExtra(IMAGE, image)
            }
        alarmReceivedIntent.putExtra(ALARM, alarmTone)
        fullScreenIntent.putExtra(ALARM, alarmTone)
        fullScreenIntent.putExtra(HOUR, hour)
        fullScreenIntent.putExtra(MINUTE, minute)
        fullScreenIntent.putExtra(CREATION_DATE, snoozeCreationDate)
        fullScreenIntent.putExtra(GOAL, goal)
        fullScreenIntent.putExtra(IMAGE, image)

        return smplrAlarmSet(applicationContext) {
            requestCode { REQUEST_CODE }
            hour { hour }
            min { minute }
            contentIntent { onClickShortcutIntent }
            receiverIntent { fullScreenIntent }
            alarmReceivedIntent { alarmReceivedIntent }
            notification {
                alarmNotification {
                    smallIcon { de.coldtea.smplr.smplralarm.R.drawable.ic_baseline_notifications_active_24 }
                    title { context.getString(R.string.Alarm) }
                    bigText { "${context.getString(R.string.it_s_time_for)} $goal" }
                    autoCancel { true }
                    firstButtonText { context.getString(R.string.snooze_tittle) }
                    secondButtonText { context.getString(R.string.turnOff_tittle) }
                    firstButtonIntent { snoozeIntent }
                    secondButtonIntent { dismissIntent }
                    notificationDismissedIntent { notificationDismissIntent }
                }
            }
            notificationChannel {
                channel {
                    importance { NotificationManager.IMPORTANCE_HIGH }
                    showBadge { false }
                    name { context.getString(R.string.alarm) }
                    description { context.getString(R.string.your_alarm) }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun formattedDatePmAm(date: Date): String? {
        val sdf = SimpleDateFormat("hh:mm")
        return sdf.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    private fun formattedDate24Hours(date: Date): String? {
        val sdf = SimpleDateFormat("HH:mm")
        return sdf.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    private fun formattedDate24Week(date: Date): String? {
        val sdf = SimpleDateFormat("EEEE dd:MM")
        return sdf.format(date)
    }

    @SuppressLint("SimpleDateFormat")
    private fun formattedDateAmPmWeek(date: Date): String? {
        val sdf = SimpleDateFormat("EEEE MM:dd")
        return sdf.format(date)
    }
}
