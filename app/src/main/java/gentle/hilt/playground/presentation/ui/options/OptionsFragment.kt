package gentle.hilt.playground.presentation.ui.options

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R
import gentle.hilt.playground.databinding.FragmentOptionsBinding
import gentle.hilt.playground.databinding.SnoozeDialogBinding
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.ALARM_TONE_URI
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.RINGTONE_TONE_URI
import io.github.stack07142.discreteseekbar.DiscreteSeekBar
import kotlinx.coroutines.launch
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

@AndroidEntryPoint
class OptionsFragment : Fragment() {
    private lateinit var binding: FragmentOptionsBinding
    private val viewModel: OptionsViewModel by viewModels()

    private val emptyResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val defaultAlarmSoundLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                activityResult ->
            when (activityResult.resultCode) {
                Activity.RESULT_OK -> {
                    val data: Intent? = activityResult.data
                    ALARM_TONE_URI = data?.parcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    RingtoneManager.setActualDefaultRingtoneUri(
                        context,
                        RingtoneManager.TYPE_ALARM,
                        ALARM_TONE_URI
                    )
                }
                else -> {
                    Timber.tag("Result").e("defaultAlarmSoundLauncher error")
                }
            }
        }

    private val defaultRingtoneSoundLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                activityResult ->
            when (activityResult.resultCode) {
                Activity.RESULT_OK -> {
                    val data: Intent? = activityResult.data
                    RINGTONE_TONE_URI =
                        data?.parcelable(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                    RingtoneManager.setActualDefaultRingtoneUri(
                        context,
                        RingtoneManager.TYPE_RINGTONE,
                        RINGTONE_TONE_URI
                    )
                }
                else -> {
                    Timber.tag("Result").e("defaultRingtoneSoundLauncher error")
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        turnDarkMode()

        switchNightMode(binding.switchNightMode)
        setDefaultAlarmSound(binding.llDefaultAlarmSound)
        setDefaultRingtoneSound(binding.llDefaultRingtoneSound)
        changeSnoozeSettings(binding.llAlarmSnooze)

        setupNavigationViews()
    }

    private fun turnDarkMode() {
        lifecycleScope.launch {
            viewModel.dataStore.darkModeEnabled.collect { enabled ->
                binding.switchNightMode.isChecked = enabled
                enableNightMode(enabled)
            }
        }
    }

    private fun switchNightMode(switchNightMode: SwitchMaterial) {
        switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setDarkMode(isChecked)
        }
    }

    companion object {
        const val FIVE_MINUTES = -200
        const val TEN_MINUTES = -100
        const val FIFTEEN_MINUTES = 0
        const val TWENTY_MINUTES = 100
        const val TWENTY_FIVE_MINUTES = 200
        const val THIRTY_MINUTES = 300

        const val THUMB_SIZE = 24
        const val PRESSED_SIZE = 30
    }

    private fun changeSnoozeSettings(llAlarmSnooze: LinearLayout) {
        llAlarmSnooze.setOnClickListener {
            val snoozeDialog = Dialog(requireContext())
            val binding: SnoozeDialogBinding = SnoozeDialogBinding.inflate(layoutInflater)
            snoozeDialog.setContentView(binding.root)

            lifecycleScope.launch {
                viewModel.readDiscreteSliderValue.observe(viewLifecycleOwner) { sliderValue ->
                    binding.discreteSeekBar.setValue(sliderValue)
                }
            }

            val tick = SparseArray<String>()
            tick.append(FIVE_MINUTES, "5")
            tick.append(TEN_MINUTES, "10")
            tick.append(FIFTEEN_MINUTES, "15")
            tick.append(TWENTY_MINUTES, "20")
            tick.append(TWENTY_FIVE_MINUTES, "25")
            tick.append(THIRTY_MINUTES, "30")

            binding.discreteSeekBar.getConfigBuilder().setTickMarkTextColor(R.color.black)
                .setThumbDefaultSize(THUMB_SIZE).setThumbPressedSize(PRESSED_SIZE).setTickMarkTextArray(tick)
                .setOnValueChangedListener(object : DiscreteSeekBar.OnValueChangedListener {
                    override fun onValueChanged(value: Int) {
                        viewModel.saveDiscreteSlider(value)
                    }
                }).build()

            binding.btnOk.setOnClickListener {
                snoozeDialog.dismiss().apply {
                    Snackbar.make(requireView(), R.string.snooze_message_made, Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            snoozeDialog.show()
        }
    }

    private fun setupNavigationViews() {
        binding.layoutDateAndTime.setOnClickListener {
            val dateSettings = Intent(Settings.ACTION_DATE_SETTINGS)
            emptyResultLauncher.launch(dateSettings)
        }
        binding.optionsBtnBack.setOnClickListener {
            val action = OptionsFragmentDirections.actionOptionsFragmentToAlarmsFragment()
            findNavController().navigate(action)
        }
    }

    private fun setDefaultAlarmSound(llDefaultAlarmSound: LinearLayout) {
        llDefaultAlarmSound.setOnClickListener {
            if (Settings.System.canWrite(requireContext())) {
                openDefaultAlarmSound()
            } else {
                MaterialDialog(requireContext()).show {
                    title(R.string.tittle_write)
                    message(R.string.message_write)

                    positiveButton(R.string.agree_option) {
                        openManagerWriteSettings()
                    }

                    negativeButton(R.string.disagree_option) {
                        dismiss()
                    }
                }
            }
        }
    }

    private fun setDefaultRingtoneSound(llDefaultRingtoneSound: LinearLayout) {
        llDefaultRingtoneSound.setOnClickListener {
            if (Settings.System.canWrite(requireContext())) {
                openDefaultRingtoneSound()
            } else {
                MaterialDialog(requireContext()).show {
                    title(R.string.tittle_write)
                    message(R.string.message_write)

                    positiveButton(R.string.agree_option) {
                        openManagerWriteSettings()
                    }

                    negativeButton(R.string.disagree_option) {
                        dismiss()
                    }
                }
            }
        }
    }

    private fun openDefaultAlarmSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Default Alarm Sound")
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ALARM_TONE_URI)

        defaultAlarmSoundLauncher.launch(intent)
    }

    private fun openDefaultRingtoneSound() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Default Ringtone Sound")
            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, RINGTONE_TONE_URI)

        defaultRingtoneSoundLauncher.launch(intent)
    }

    private fun openManagerWriteSettings() {
        val intent = Intent().setAction(ACTION_MANAGE_WRITE_SETTINGS)
        emptyResultLauncher.launch(intent)
    }

    private fun enableNightMode(enable: Boolean) {
        if (enable) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOptionsBinding.inflate(layoutInflater)
        return binding.root
    }
}
