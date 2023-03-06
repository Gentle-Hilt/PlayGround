package gentle.hilt.playground.presentation.ui.sleepStatistic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import gentle.hilt.playground.R
import gentle.hilt.playground.databinding.FragmentSleepStatisticBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SleepStatisticFragment : Fragment() {
    private lateinit var binding: FragmentSleepStatisticBinding
    private val viewModel: SleepStatisticViewModel by viewModels()
    private lateinit var sleepAdapter: SleepTrackingAdapter

    @Inject
    lateinit var glide: RequestManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sleepTrackingAdapter(binding.rvSleepTracking)
        deleteSleepStatistic(binding.btnDelete)

        collectSleepTrackingEntities()
    }

    private fun collectSleepTrackingEntities() {
        lifecycleScope.launch {
            viewModel.sleepTrackingEntities.observe(viewLifecycleOwner) { entitiesList ->
                sleepAdapter.submitList(entitiesList)

                dealWithViewsVisibility()
            }
        }
    }

    private fun sleepTrackingAdapter(rvSleepTracking: RecyclerView) {
        sleepAdapter = SleepTrackingAdapter(glide)
        rvSleepTracking.apply {
            adapter = sleepAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
                reverseLayout = true
            }
        }
    }

    private fun dealWithViewsVisibility() {
        if (sleepAdapter.currentList.isNotEmpty()) {
            binding.ivSetupAlarm.isVisible = false
            binding.tvYouNeedStatistic.isVisible = false
            binding.btnDelete.isVisible = true
        } else {
            binding.btnDelete.isVisible = false
        }
    }

    private fun deleteSleepStatistic(view: ImageButton) {
        view.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.delete_sleep_statistic_tittle)
                message(R.string.delete_sleep_statistic_message)

                positiveButton(R.string.delete_sleep_positive) {
                    viewModel.deleteSleepingStatistic()
                }
                negativeButton {
                    cancel()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSleepStatisticBinding.inflate(layoutInflater)
        return binding.root
    }
}
