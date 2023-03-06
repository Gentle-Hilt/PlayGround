package gentle.hilt.playground.presentation.ui.alarm

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import gentle.hilt.playground.R
import gentle.hilt.playground.data.room.entity.AlarmEntity
import gentle.hilt.playground.databinding.ItemAlarmBinding
import org.joda.time.DateTime
import org.joda.time.Duration
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class AlarmAdapter @Inject constructor(
    private var onSwitchClicked: (AlarmEntity, isChecked: Boolean) -> Unit,
    private val glide: RequestManager
) : ListAdapter<AlarmEntity, AlarmAdapter.MyViewHolder>(

    object : DiffUtil.ItemCallback<AlarmEntity>() {
        override fun areItemsTheSame(oldItem: AlarmEntity, newItem: AlarmEntity): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: AlarmEntity, newItem: AlarmEntity): Boolean {
            return oldItem == newItem
        }
    }
) {

    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        context = parent.context
        val binding = ItemAlarmBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding, onSwitchClicked = { alarmEntity, isChecked ->
            onSwitchClicked(getItem(alarmEntity).copy(isEnabled = isChecked), isChecked)
        })
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setData(getItem(position), context, glide)
    }

    class MyViewHolder(
        private val binding: ItemAlarmBinding,
        val onSwitchClicked: ((Int, Boolean) -> Unit)
    ) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.toggleAlarm.setOnCheckedChangeListener { _, isChecked ->
                onSwitchClicked.invoke(absoluteAdapterPosition, isChecked)
            }
        }

        companion object {
            const val HOURS_24_IN_MINUTES = 1440
            const val TWELVE_HOURS = 12
        }

        @SuppressLint("SetTextI18n")
        fun setData(alarm: AlarmEntity, context: Context, glide: RequestManager) {
            val alarmDate = Calendar.getInstance()
            alarmDate.set(Calendar.HOUR_OF_DAY, alarm.hour)
            alarmDate.set(Calendar.MINUTE, alarm.minute)

            val rightNow = DateTime(Calendar.getInstance().time)
            val tillNow = DateTime(alarmDate.time)

            val durationDay = Duration(rightNow, tillNow).standardMinutes

            val durationNight = Duration(rightNow, tillNow).abs().toStandardMinutes()
            val difference = HOURS_24_IN_MINUTES - durationNight.minutes

            binding.apply {
                etTitleTimer.text = alarm.timerGoal

                toggleAlarm.isChecked = alarm.isEnabled == true

                glide.load(alarm.image).into(ivTimerImage)

                if (DateFormat.is24HourFormat(context)) {
                    tvPmAm.text = null
                    etTimerTime.text = formattedDate24Hours(alarmDate.time)
                } else {
                    tvPmAm.text = if (alarm.hour < TWELVE_HOURS) "AM" else "PM"
                    etTimerTime.text = formattedDatePmAm(alarmDate.time)
                }

                if (alarm.isEnabled == true) {
                    when {
                        rightNow < tillNow -> {
                            tvAlarmIn.text = "Alarm in ${durationDay.minutes}"
                        }
                        rightNow > tillNow -> {
                            tvAlarmIn.text = "Alarm in ${difference.minutes}"
                        }
                        rightNow == tillNow -> {
                            tvAlarmIn.text = "Alarm in 24 hours 00 minute"
                        }
                    }

                    etTimerTime.setTextColor(ContextCompat.getColor(context, R.color.black))
                    tvPmAm.setTextColor(ContextCompat.getColor(context, R.color.black))
                    etTitleTimer.setTextColor(
                        ContextCompat.getColor(context, R.color.black)
                    )
                } else {
                    tvAlarmIn.text = "Off"
                    etTimerTime.setTextColor(Color.parseColor("#808080"))
                    tvPmAm.setTextColor(Color.parseColor("#808080"))
                    etTitleTimer.setTextColor(Color.parseColor("#808080"))
                }
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
