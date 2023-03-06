package gentle.hilt.playground.presentation.ui.alarm.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.net.toUri
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI.Companion.SMPLR_ALARM_REQUEST_ID
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.RINGTONE_ALARM
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel.Companion.ALARM
import timber.log.Timber

class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val receivedAlarmUri = intent.getStringExtra(ALARM)?.toUri()

        val requestId = intent.getIntExtra(SMPLR_ALARM_REQUEST_ID, -1)
        Timber.i("Alarm received with id: $requestId")

        if (receivedAlarmUri != null) {
            RINGTONE_ALARM = RingtoneManager.getRingtone(context, receivedAlarmUri)

            RINGTONE_ALARM!!.play()
        }
    }
}
