package gentle.hilt.playground.presentation.ui.alarm.add

import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R
import gentle.hilt.playground.databinding.FragmentAddAlarmBinding
import gentle.hilt.playground.presentation.app.PlayGroundActivity.Companion.ALARM_TONE_URI
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel
import gentle.hilt.playground.presentation.ui.alarm.UiState
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class AddAlarmFragment : Fragment() {
    private lateinit var binding: FragmentAddAlarmBinding
    private val viewModel: AlarmsViewModel by viewModels()

    private var previouslyUsedImage: Uri? = null
    private var fingerVisibilityState: Boolean = true
    private val currentTime: Date = Calendar.getInstance().time

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                Timber.tag("permissions").e("Success, granted")
            } else {
                Timber.tag("permissions").e("Fail to get permission")
            }
        }

    private val selectImageFromGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { Uri ->
            if (Uri != null) {
                previouslyUsedImage = Uri
                fingerVisibilityState = false

                requireActivity().contentResolver.takePersistableUriPermission(
                    Uri,
                    FLAG_GRANT_READ_URI_PERMISSION
                )

                preloadUserData()
            }
        }

    private fun selectImageFromGallery() {
        selectImageFromGalleryLauncher.launch(arrayOf("image/*"))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAlarmState()
        observeUser()

        updateTimePickerDependingOnTimeZone()
        resizeAddAlarmFragmentInLandscapeMode()

        addNewAlarm(binding.fabAddAlarm)
        selectImage(binding.ivChooseImage)
    }

    private fun addNewAlarm(view: FloatingActionButton) {
        binding.apply {
            view.setOnClickListener {
                preloadUserData()
                val alarmId = viewModel.setFullScreenIntentAlarm(
                    hour = timePicker.hour,
                    minute = timePicker.minute,
                    applicationContext = requireContext().applicationContext,
                    alarmTone = ALARM_TONE_URI.toString(),
                    goal = etAlarmGoal.text.toString(),
                    creationDate = currentTime.time,
                    image = previouslyUsedImage.toString()
                )
                viewModel.createAlarm(
                    id = alarmId,
                    timerGoal = etAlarmGoal.text.toString(),
                    image = previouslyUsedImage.toString(),
                    hour = timePicker.hour,
                    minute = timePicker.minute,
                    requestCode = alarmId.toString(),
                    isEnabled = true,
                    creationDate = currentTime
                )
            }
        }
    }

    private fun selectImage(view: ImageView) {
        view.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                selectImageFromGallery()
            } else {
                requestPermission.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun preloadUserData() {
        lifecycleScope.launch {
            launch {
                binding.etAlarmGoal.text.toString().let { tittleString ->
                    viewModel.saveUserAlarmTittle(tittleString)
                }
            }
            launch {
                fingerVisibilityState.let { visibilityBoolean ->
                    viewModel.saveFingerState(visibilityBoolean)
                }
            }
            launch {
                previouslyUsedImage?.let { imageUri ->
                    viewModel.savePreviousAlarmUserImage(imageUri)
                }
            }
        }
    }

    private fun observeAlarmState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                when (uiState) {
                    is UiState.Success -> {
                        Snackbar.make(requireView(), R.string.added_alarm, Snackbar.LENGTH_SHORT)
                            .show()
                        findNavController().popBackStack()
                    }

                    is UiState.Error -> {
                        Snackbar.make(requireView(), uiState.message, Snackbar.LENGTH_LONG)
                            .show()
                    }

                    is UiState.Loading -> {
                    }
                }
            }
        }
    }

    private fun observeUser() {
        viewModel.userPreviousImage.observe(viewLifecycleOwner) { imageString ->
            val uri = imageString.toUri()

            binding.ivChooseImage.setImageURI(uri)
            previouslyUsedImage = uri
        }
        viewModel.readUserAlarmTittle.observe(viewLifecycleOwner) { tittleString ->
            binding.etAlarmGoal.setText(tittleString)
        }
        viewModel.readFingerState.observe(viewLifecycleOwner) { visibleBoolean ->
            binding.ivFingerClick.isVisible = visibleBoolean
            fingerVisibilityState = visibleBoolean
        }
    }

    private fun updateTimePickerDependingOnTimeZone() {
        if (DateFormat.is24HourFormat(requireContext())) {
            binding.timePicker.setIs24HourView(true)
        } else {
            binding.timePicker.setIs24HourView(false)
        }
    }

    private fun resizeAddAlarmFragmentInLandscapeMode() {
        val dpMarginRight = requireContext().resources
            .getDimensionPixelSize(R.dimen.margin_end_for_add_fragment)
        val dpMarginBottom = requireContext().resources
            .getDimensionPixelSize(R.dimen.margin_bottom_for_add_fragment)

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val constrainLayout: ConstraintLayout = binding.clAddAlarm
            val set = ConstraintSet()
            set.clone(constrainLayout)

            set.clear(R.id.timePicker, ConstraintSet.TOP)
            set.clear(R.id.timePicker, ConstraintSet.END)
            set.clear(R.id.timePicker, ConstraintSet.START)
            set.clear(R.id.timePicker, ConstraintSet.BOTTOM)

            set.clear(R.id.cvRoundedImageAddAlarm, ConstraintSet.START)
            set.clear(R.id.cvRoundedImageAddAlarm, ConstraintSet.BOTTOM)
            set.clear(R.id.cvRoundedImageAddAlarm, ConstraintSet.END)

            set.connect(
                R.id.cvRoundedImageAddAlarm, ConstraintSet.BOTTOM,
                R.id.clAddAlarm, ConstraintSet.BOTTOM, dpMarginBottom
            )

            set.connect(
                R.id.timePicker, ConstraintSet.BOTTOM,
                R.id.clAddAlarm, ConstraintSet.BOTTOM, dpMarginBottom
            )

            set.connect(
                R.id.cvRoundedImageAddAlarm, ConstraintSet.END,
                R.id.clAddAlarm, ConstraintSet.END, dpMarginRight
            )

            set.connect(
                R.id.timePicker, ConstraintSet.END,
                R.id.cvRoundedImageAddAlarm, ConstraintSet.START, dpMarginRight
            )

            set.applyTo(constrainLayout)

            binding.etAlarmGoal.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddAlarmBinding.inflate(layoutInflater)
        return binding.root
    }
}
