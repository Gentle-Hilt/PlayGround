package gentle.hilt.playground.presentation.ui.timer

import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.media.RingtoneManager
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.RINGTONE_TIMER
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.RINGTONE_TONE_URI
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.CANCEL_FROM_NOTIFICATION
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.NOTIFICATION_CHANNEL_ID
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.NOTIFICATION_CHANNEL_TITTLE
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.NOTIFICATION_ID
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.START_FROM_NOTIFICATION
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.START_SERVICE_BUTTON_MESSAGE
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.STOP_SERVICE_BUTTON_MESSAGE
import gentle.hilt.playground.presentation.ui.timer.TimerFragment.Companion.UPDATE_NOTIFICATION_TITTLE
import kotlinx.coroutines.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class ForegroundTimerService : Service() {
    @Inject
    lateinit var dataStore: DataStoreManager

    private var countDownTimer: CountDownTimer? = null
    private var futureTimeInMillis: Long = 0
    private var countDownTime: Long = 0

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        const val ONE_SECOND: Long = 1000
    }

    override fun onCreate() {
        super.onCreate()

        applicationScope.launch {
            launch {
                dataStore.readFutureTimeInSeconds.collect { futureTimeInSeconds ->
                    futureTimeInMillis = futureTimeInSeconds * ONE_SECOND
                }
            }
            launch {
                dataStore.readCountDownTimeInSeconds.collect { countDownTimeInSeconds ->
                    countDownTime = countDownTimeInSeconds
                    updateNotification()
                }
            }
        }

        startForeGround()
        startTimer()
    }

    override fun onDestroy() {
        applicationScope.cancel()
        countDownTimer?.cancel()
        sendBroadcast(Intent().setAction("refresh"))
        RINGTONE_TIMER?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(futureTimeInMillis, ONE_SECOND) {
            override fun onTick(millisUntilFinished: Long) {
                applicationScope.launch {
                    launch {
                        dataStore.saveCountDownTimeInSeconds(millisUntilFinished / ONE_SECOND)
                    }
                    launch {
                        dataStore.saveFutureTimeInSeconds(millisUntilFinished / ONE_SECOND)
                    }
                }
            }

            override fun onFinish() {
                RINGTONE_TIMER = RingtoneManager.getRingtone(applicationContext, RINGTONE_TONE_URI)
                RINGTONE_TIMER?.play()
                sendBroadcast(Intent().setAction("refresh"))
                applicationScope.cancel()
            }
        }

        (countDownTimer as CountDownTimer).start()
    }

    private fun startForeGround() {
        val pendingIntent: PendingIntent =
            Intent(this, TimerFragment::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    FLAG_IMMUTABLE
                )
            }

        val notification: Notification = Notification.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_self_improvement)
            .setContentIntent(pendingIntent)
            .build()

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_TITTLE,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.setSound(null, null)
        val notificationManager: NotificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification() {
        val stopSelf = Intent(this, ForegroundTimerService::class.java)
        stopSelf.putExtra(CANCEL_FROM_NOTIFICATION, true)
        val builder = Notification.Action.Builder(
            null,
            STOP_SERVICE_BUTTON_MESSAGE,
            PendingIntent.getForegroundService(
                this,
                1,
                stopSelf,
                FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE
            )
        )
        val actionCancel = builder.build()

        val startSelf = Intent(this, ForegroundTimerService::class.java)
        stopSelf.putExtra(START_FROM_NOTIFICATION, true)
        val builderStart = Notification.Action.Builder(
            null,
            START_SERVICE_BUTTON_MESSAGE,
            PendingIntent.getForegroundService(
                this,
                2,
                startSelf,
                FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE
            )
        )
        val actionStart = builderStart.build()

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(UPDATE_NOTIFICATION_TITTLE)
            .setContentText(countDownTime.seconds.inWholeSeconds.seconds.toString())
            .setSmallIcon(R.drawable.ic_self_improvement)
            .addAction(actionCancel)
            .addAction(actionStart)
            .build()

        NotificationManagerCompat.from(application).notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sendBroadcast(Intent().setAction("refresh"))

        val cancelFromNotification = intent!!.getBooleanExtra(
            CANCEL_FROM_NOTIFICATION,
            false
        )
        if (cancelFromNotification) {
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }

        val startFromNotification = intent.getBooleanExtra(
            START_FROM_NOTIFICATION,
            false
        )
        if (startFromNotification) {
            startForegroundService(Intent(this, ForegroundTimerService::class.java))
        }

        return START_NOT_STICKY
    }
}
