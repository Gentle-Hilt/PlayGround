package gentle.hilt.playground.presentation.ui.alarm

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI
import de.coldtea.smplr.smplralarm.smplrAlarmCancel
import de.coldtea.smplr.smplralarm.smplrAlarmSet
import de.coldtea.smplr.smplralarm.smplrAlarmUpdate
import gentle.hilt.playground.databinding.FragmentAlarmsBinding
import javax.inject.Inject

@AndroidEntryPoint
class AlarmsFragment : Fragment() {

    private lateinit var binding: FragmentAlarmsBinding

    val viewModel: AlarmsViewModel by viewModels()

    @Inject
    lateinit var glide: RequestManager

    private lateinit var alarmAdapter: AlarmAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeUi()
        alarmAdapter(binding.rvSetupTimer)
        implementTouchHelper()

        setupNavigationViews()
    }

    private fun alarmAdapter(rvSetupTimer: RecyclerView) {
        alarmAdapter = AlarmAdapter(
            onSwitchClicked = { alarmEntity, isChecked ->
                if (isChecked) {
                    smplrAlarmUpdate(requireContext()) {
                        requestCode { alarmEntity.requestCode.toInt() }
                        hour { alarmEntity.hour }
                        min { alarmEntity.minute }
                        isActive { isChecked }
                    }
                    viewModel.updateAlarmInDb(alarmEntity)
                } else {
                    smplrAlarmUpdate(requireContext()) {
                        requestCode { alarmEntity.requestCode.toInt() }
                        hour { alarmEntity.hour }
                        min { alarmEntity.minute }
                        isActive { isChecked }
                    }
                }
                viewModel.updateAlarmInDb(alarmEntity)
            },
            glide
        )

        rvSetupTimer.apply {
            adapter = alarmAdapter
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
                reverseLayout = false
            }
        }
    }

    private fun implementTouchHelper() {
        val itemTouchCallBack = object : ItemTouchHelper.SimpleCallback(
            0,
            LEFT or RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.layoutPosition
                val item = alarmAdapter.currentList[position]

                viewModel.deleteEntityAlarmFromDb(item)

                if (item.isEnabled == false) {
                    smplrAlarmCancel(requireContext()) {
                        requestCode { item.requestCode.toInt() }
                        hour { item.hour }
                        min { item.minute }
                        isActive { false }
                    }
                } else {
                    smplrAlarmUpdate(requireContext()) {
                        requestCode { item.requestCode.toInt() }
                        hour { item.hour }
                        min { item.minute }
                        isActive { false }
                    }

                    Snackbar.make(requireView(), "Successfully deleted alarm", Snackbar.LENGTH_LONG)
                        .apply {
                            setActionTextColor(Color.parseColor("#03FFEA"))
                            setAction("Undo") {
                                viewModel.insertEntityAlarmIntoDb(item)
                                smplrAlarmUpdate(requireContext()) {
                                    requestCode { item.requestCode.toInt() }
                                    hour { item.hour }
                                    min { item.minute }
                                    isActive { true }
                                }
                            }
                            show()
                        }
                }
            }
        }

        ItemTouchHelper(itemTouchCallBack).attachToRecyclerView(binding.rvSetupTimer)
    }

    private fun setupNavigationViews() {
        binding.fabGoToAddTimerFragment.setOnClickListener {
            val action = AlarmsFragmentDirections.actionAlarmsFragmentToAddAlarmFragment()
            findNavController().navigate(action)
        }
        binding.btnSettings.setOnClickListener {
            val action = AlarmsFragmentDirections.actionAlarmsFragmentToOptionsFragment()
            findNavController().navigate(action)
        }
        binding.ivStatistics.setOnClickListener {
            val action = AlarmsFragmentDirections.actionAlarmsFragmentToSleepStatisticFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeUi() {
        viewModel.observeAlarmEntities.observe(viewLifecycleOwner) {
            alarmAdapter.submitList(it)

            dealWithViews()
        }
    }

    private fun dealWithViews() {
        if (alarmAdapter.currentList.isNotEmpty()) {
            binding.tvSetupAlarmCaption.isVisible = false
            binding.ivSetupAlarm.isVisible = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlarmsBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        alarmAdapter.notifyDataSetChanged()
    }
}
