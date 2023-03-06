package gentle.hilt.playground.presentation.ui.alarm

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import gentle.hilt.playground.R
import gentle.hilt.playground.androidTestUtils.launchFragmentInHiltContainer
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@ExperimentalCoroutinesApi
class AlarmsFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        hiltRule.inject()
        MockKAnnotations.init(this)
    }

    @RelaxedMockK
    lateinit var navController: NavController

    @Test
    fun click_on_fab_navigate_to_AddAlarmFragment() {
        launchFragmentInHiltContainer<AlarmsFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
        onView(withId(R.id.fabGoToAddTimerFragment)).perform(click())

        verify {
            navController.navigate(
                AlarmsFragmentDirections.actionAlarmsFragmentToAddAlarmFragment()
            )
        }
    }

    @Test
    fun click_on_threeDots_navigate_to_OptionsFragment() {
        launchFragmentInHiltContainer<AlarmsFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
        onView(withId(R.id.btnSettings)).perform(click())

        verify {
            navController.navigate(
                AlarmsFragmentDirections.actionAlarmsFragmentToOptionsFragment()
            )
        }
    }

    @Test
    fun click_on_appImage_navigate_to_SleepStatistic() {
        launchFragmentInHiltContainer<AlarmsFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }

        onView(withId(R.id.ivStatistics)).perform(click())

        verify {
            navController.navigate(
                AlarmsFragmentDirections.actionAlarmsFragmentToSleepStatisticFragment()
            )
        }
    }
}
