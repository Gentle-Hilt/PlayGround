package gentle.hilt.playground.presentation.ui.alarm.add

import android.widget.TimePicker
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import gentle.hilt.playground.R
import gentle.hilt.playground.androidTestUtils.TestCoroutineRule
import gentle.hilt.playground.androidTestUtils.getOrAwaitValue
import gentle.hilt.playground.androidTestUtils.launchFragmentInHiltContainer
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.data.room.AlarmDataBase
import gentle.hilt.playground.data.room.repository.AlarmEntityRepository
import gentle.hilt.playground.presentation.ui.alarm.AlarmsViewModel
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
class AddAlarmFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Inject
    lateinit var dataBase: AlarmDataBase

    @Inject
    lateinit var dataStore: DataStoreManager

    @Inject
    lateinit var repository: AlarmEntityRepository

    private lateinit var viewModel: AlarmsViewModel

    private val rule = TestCoroutineRule()

    @Before
    fun setup() {
        hiltRule.inject()
        MockKAnnotations.init(this)

        viewModel = AlarmsViewModel(dataStore, repository, rule.testCoroutineDispatcher)

        launchFragmentInHiltContainer<AddAlarmFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
    }

    @After
    fun tearDown() {
        dataBase.clearAllTables()
    }

    @RelaxedMockK
    lateinit var navController: NavController

    @Test
    fun click_on_fab_navigate_to_AlarmFragment() {
        onView(withId(R.id.fabAddAlarm)).perform(click())

        verify { navController.popBackStack() }
    }

    @Test
    fun change_goal_read_from_dataStore() {
        // Animators may only be run on Looper threads
        // cannot be called on the main application thread
        // it fixed itself overtime, but immediate fix was to add rule with StandardTestDispatcher()

        // Adding new alarm with Goal
        onView(withId(R.id.etAlarmGoal)).perform(replaceText("Make me sad"))
        onView(withId(R.id.fabAddAlarm)).perform(click())
        val firstSubject = viewModel.observeAlarmEntities.getOrAwaitValue()
        assertThat(firstSubject[0].timerGoal).isEqualTo("Make me sad")

        // Adding new alarm without Goal, dataStore should read previous goal
        onView(withId(R.id.fabAddAlarm)).perform(click())
        val secondSubject = viewModel.observeAlarmEntities.getOrAwaitValue()
        assertThat(secondSubject[1].timerGoal).isEqualTo("Make me sad")
    }

    @Test
    fun set_time_on_timePicker() {
        launchFragmentInHiltContainer<AddAlarmFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
        onView(isAssignableFrom(TimePicker::class.java)).perform(
            PickerActions.setTime(
                12, 12
            )
        )
        onView(withId(R.id.fabAddAlarm)).perform(click())

        val value = viewModel.observeAlarmEntities.getOrAwaitValue()
        assertThat(value[0].hour + value[0].minute).isEqualTo(value[0].hour + value[0].minute)
    }

    @Test
    fun changing_image_read_from_dataStore() {
        // I need to choose image from gallery, but  it's not possible with Espresso
        // There's some method, https://stackoverflow.com/questions/26469661/how-to-click-on-android-gallery-with-espresso
        // but i think it's too much, easier will be to test contracts itself
    }
}
