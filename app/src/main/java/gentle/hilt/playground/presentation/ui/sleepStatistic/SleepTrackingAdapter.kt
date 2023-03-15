package gentle.hilt.playground.presentation.ui.sleepStatistic

import android.annotation.SuppressLint
import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.RequestManager
import gentle.hilt.playground.R
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity
import gentle.hilt.playground.databinding.SleepTrackingItemBinding
import org.joda.time.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

class SleepTrackingAdapter @Inject constructor(
    val glide: RequestManager
) : ListAdapter<SleepTrackingEntity, SleepTrackingAdapter.MyViewHolder>(
    object : DiffUtil.ItemCallback<SleepTrackingEntity>() {

        override fun areItemsTheSame(
            oldItem: SleepTrackingEntity,
            newItem: SleepTrackingEntity
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: SleepTrackingEntity,
            newItem: SleepTrackingEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
) {
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        context = parent.context
        val binding = SleepTrackingItemBinding.inflate(LayoutInflater.from(context), parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setData(getItem(position), context, glide)
    }

    companion object {
        const val HOURS_24_IN_MINUTES = 1440
        const val HALF_AND_HOUR = 30
        const val TWELVE_HOURS = 12
    }

    class MyViewHolder(
        private val binding: SleepTrackingItemBinding
    ) : ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun setData(sleepTracking: SleepTrackingEntity, context: Context, glide: RequestManager) {
            val creationTime = DateTime(sleepTracking.creationDate.time)

            val endAlarmTime = Calendar.getInstance()
            endAlarmTime.time = sleepTracking.creationDate
            endAlarmTime.set(Calendar.HOUR_OF_DAY, sleepTracking.hour)
            endAlarmTime.set(Calendar.MINUTE, sleepTracking.minute)

            val endTime = DateTime(endAlarmTime.time)

            val durationDay = Duration(creationTime, endTime).standardMinutes
            val durationNight = Duration(creationTime, endTime).abs().toStandardMinutes()

            val difference = HOURS_24_IN_MINUTES - durationNight.minutes

            val intDurationDay = durationDay.minutes.toInt(DurationUnit.MINUTES)
            val intDurationNight = difference.minutes.toInt(DurationUnit.MINUTES)

            binding.apply {
                glide.load(sleepTracking.image).into(ivUsedImage)
                if (intDurationDay.absoluteValue < HALF_AND_HOUR || intDurationNight < HALF_AND_HOUR) {
                    tvSleepWas.text = context.getString(R.string.snooze_was)
                } else {
                    tvSleepWas.text = context.getString(R.string.sleep_time)
                }

                if (DateFormat.is24HourFormat(context)) {
                    tvPmAm.text = null
                    fullDate.text = formattedDate24H(creationTime.toDate())
                } else {
                    tvPmAm.text = if (sleepTracking.hour < TWELVE_HOURS) "AM" else "PM"
                    fullDate.text = formattedDateAmPm(creationTime.toDate())
                }

                val todayJodaTime = DateTime(LocalDate.now().toDate()).dayOfYear
                val creationDateJodaTime = DateTime(sleepTracking.creationDate).dayOfYear
                val yesterdayJodaTime = DateTime(LocalDate.now().minusDays(1).toDate()).dayOfYear

                when (creationDateJodaTime) {
                    LocalDate.now().dayOfYear -> {
                        tvDayWeekTittle.text = context.getString(R.string.Today)
                    }
                    yesterdayJodaTime -> {
                        tvDayWeekTittle.text = context.getString(R.string.Yesterday)
                    }
                    else -> {
                        val daysAgo = todayJodaTime - creationDateJodaTime
                        tvDayWeekTittle.text = "$daysAgo ${context.getString(R.string.days_ago)}"
                    }
                }

                when {
                    creationTime < endTime -> {
                        tvDurationNumbers.text = "${durationDay.minutes}"
                    }
                    creationTime > endTime -> {
                        tvDurationNumbers.text = "${(difference.minutes)}"
                    }
                    creationTime == endTime -> {
                        tvDurationNumbers.text = context.getString(R.string.twenty_four_hours)
                        tvSleepWas.text = context.getString(R.string.snooze_was)
                    }
                }
            }
        }
    }
}

@SuppressLint("SimpleDateFormat")
private fun formattedDate24H(date: Date): String? {
    val sdf = SimpleDateFormat("EEEE HH:mm")
    return sdf.format(date)
}

@SuppressLint("SimpleDateFormat")
private fun formattedDateAmPm(date: Date): String? {
    val sdf = SimpleDateFormat("EEEE hh:mm")
    return sdf.format(date)
}
