package gentle.hilt.playground.presentation.ui.timer

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R
import gentle.hilt.playground.databinding.FragmentTimerBinding
import gentle.hilt.playground.databinding.SetTimeTimerDailogBinding
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.RINGTONE_TIMER
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@AndroidEntryPoint
class TimerFragment : Fragment() {
    private lateinit var binding: FragmentTimerBinding
    private val viewModel: TimerViewModel by viewModels()

    private var isRunning: Boolean = false

    private var blackAndWhite: Int = 0
    private var whiteAndBlack: Int = 0

    companion object {
        const val NOTIFICATION_ID = 1

        const val NOTIFICATION_CHANNEL_ID = "112"
        const val NOTIFICATION_CHANNEL_TITTLE = "Timer"

        const val UPDATE_NOTIFICATION_TITTLE = "Timer"

        const val STOP_SERVICE_BUTTON_MESSAGE = "Stop"
        const val START_SERVICE_BUTTON_MESSAGE = "Start"

        const val CANCEL_FROM_NOTIFICATION = "cancel from notification"
        const val START_FROM_NOTIFICATION = "start from notification"

        const val PROGRESS_BAR_WIDTH = 7f // in DP
        const val BACKGROUND_PROGRESS_BAR_WIDTH = 3f // in Dp

        const val MAX_MINUTE = 59
        const val MAX_HOUR = 23
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeTimer()
        changeColorInDarkMode()
        circularProgressBar()
        resizeTimerInLandscapeMode()
        setTimeInDialog(binding.ibSetNewTime)

        requireActivity().registerReceiver(broadcastReceiver, IntentFilter("refresh"))
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            refreshFragment()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    private fun observeTimer() {
        viewModel.futureTime.observe(viewLifecycleOwner) { futureTimeInSeconds ->
            binding.tvCountDown.text = futureTimeInSeconds.seconds.toString()
            timerFinishes()
            if (futureTimeInSeconds != 0L) {
                when (isRunning) {
                    true -> {
                        isRunningTrue()
                    }
                    false -> {
                        isRunningFalse()
                    }
                }
            }
        }
        viewModel.countDownTime.observe(viewLifecycleOwner) { countDownTimeInSeconds ->
            binding.circularProgressBar.progress = countDownTimeInSeconds.toFloat()

            if(lockScreen){
                refreshFragment()
                Timber.d("On Pause")
            }

            if (countDownTimeInSeconds != 0L) {
                isRunning = true
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    binding.circularProgressBar.progressMax =
                        viewModel.dataStore.readFutureTimeInSeconds.first().toFloat()
                }
            }
        }
    }

    private fun circularProgressBar() {
        binding.circularProgressBar.apply {
            progressBarColor = blackAndWhite
            backgroundProgressBarColor = whiteAndBlack

            // Set Width
            progressBarWidth = PROGRESS_BAR_WIDTH
            backgroundProgressBarWidth = BACKGROUND_PROGRESS_BAR_WIDTH
        }
    }

    private fun changeColorInDarkMode() {
        lifecycleScope.launch {
            viewModel.dataStore.darkModeEnabled.collect { isEnabled ->
                when (isEnabled) {
                    true -> {
                        blackAndWhite = Color.WHITE
                        whiteAndBlack = Color.BLACK
                    }
                    false -> {
                        blackAndWhite = Color.BLACK
                        whiteAndBlack = Color.WHITE
                    }
                }
            }
        }
    }

    private fun setTimeInDialog(ibSetNewTime: ImageButton) {
        ibSetNewTime.setOnClickListener {
            val setTimeTimerDialog = Dialog(requireContext())
            val binding = SetTimeTimerDailogBinding.inflate(layoutInflater)
            setTimeTimerDialog.setContentView(binding.root)
            setTimeTimerDialog.setOnDismissListener {
                refreshFragment()
            }

            var hours = 0.toDuration(DurationUnit.HOURS).inWholeHours.hours
            var minutes = 0.toDuration(DurationUnit.MINUTES).inWholeMinutes.minutes
            var seconds = 0.toDuration(DurationUnit.SECONDS).inWholeSeconds.seconds

            binding.apply {
                npHours.maxValue = MAX_HOUR
                npMinutes.maxValue = MAX_MINUTE
                npSeconds.maxValue = MAX_MINUTE

                npHours.setOnValueChangedListener { _, _, changedHour ->
                    hours = changedHour.hours
                }
                npMinutes.setOnValueChangedListener { _, _, changedMinute ->
                    minutes = changedMinute.minutes
                }
                npSeconds.setOnValueChangedListener { _, _, changedSecond ->
                    seconds = changedSecond.seconds
                }

                btnCancel.setOnClickListener {
                    setTimeTimerDialog.cancel()
                }
                btnSetTime.setOnClickListener {
                    val setFutureTime = ((hours + minutes + seconds).inWholeSeconds)

                    lifecycleScope.launch {
                        viewModel.saveFutureTimeInSeconds(setFutureTime)
                    }
                    setTimeTimerDialog.dismiss()
                }
            }
            setTimeTimerDialog.show()
        }
    }

    private fun resizeTimerInLandscapeMode() {
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val layoutParams = ConstraintLayout.LayoutParams(
                MATCH_PARENT,
                MATCH_PARENT
            )
            layoutParams.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID

            binding.llTimer.layoutParams = layoutParams
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTimerBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun isRunningTrue() {
        binding.ibTimerStartPause.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24)
        binding.ibSetNewTime.isVisible = false

        binding.ibTimerStartPause.setOnClickListener {
            requireActivity().stopService(
                Intent(
                    requireActivity(),
                    ForegroundTimerService::class.java
                )
            )

            NotificationManagerCompat.from(requireActivity()).cancelAll()
        }
    }

    private fun isRunningFalse() {
        val widthAndHeightSetNewTime = requireContext().resources.getDimensionPixelSize(R.dimen.set_new_time)
        binding.ibTimerStartPause.setImageResource(R.drawable.ic_baseline_play_circle_outline_24)
        binding.ibTimerStartPause.isVisible = true

        val layoutParams = ConstraintLayout.LayoutParams(
            widthAndHeightSetNewTime,
            widthAndHeightSetNewTime
        )
        layoutParams.rightToRight = binding.ibTimerStartPause.id
        layoutParams.leftToRight = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        layoutParams.topToTop = binding.ibTimerStartPause.id
        binding.ibSetNewTime.layoutParams = layoutParams

        binding.ibTimerStartPause.setOnClickListener {
            requireActivity().startForegroundService(
                Intent(
                    requireActivity(),
                    ForegroundTimerService::class.java
                )
            )
        }
    }

    private fun timerFinishes() {
        if (RINGTONE_TIMER?.isPlaying == true) {
            binding.ibSetNewTime.isVisible = false
            binding.ibCancelAlarm.isVisible = true

            binding.ibCancelAlarm.setImageResource(R.drawable.ic_baseline_timer_off_24)
            binding.ibCancelAlarm.setOnClickListener {
                RINGTONE_TIMER!!.stop()
                requireActivity().stopService(
                    Intent(
                        requireActivity(),
                        ForegroundTimerService::class.java
                    )
                )

                NotificationManagerCompat.from(requireActivity()).cancelAll()
            }
        }
    }

    private fun refreshFragment() {
        val navController = findNavController()
        navController.run {
            popBackStack()
            navigate(R.id.timerFragment)
        }
    }

    private var lockScreen = false

    override fun onPause() {
        super.onPause()
        lockScreen = true
    }

    override fun onResume() {
        super.onResume()

        lockScreen = false
    }

}
