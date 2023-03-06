package gentle.hilt.playground.presentation.ui.timer

import android.view.InputDevice
import android.view.MotionEvent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.actionWithAssertions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import gentle.hilt.playground.R
import gentle.hilt.playground.androidTestUtils.advanceTimeByAndRun
import gentle.hilt.playground.androidTestUtils.launchFragmentInHiltContainer
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@ExperimentalCoroutinesApi
class TimerFragmentTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var navController: NavController

    @Before
    fun setup() {
        hiltRule.inject()
        MockKAnnotations.init(this)

        launchFragmentInHiltContainer<TimerFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
    }

    private val scrollUp =
        actionWithAssertions(
            GeneralClickAction(
                Tap.SINGLE,
                GeneralLocation.BOTTOM_CENTER,
                Press.FINGER,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY
            )
        )

    @Test
    fun start_service_timer_count_time() = runTest {
        // Open timer
        onView(withId(R.id.ibSetNewTime)).perform(click())
        // Scroll each to 1
        onView(withId(R.id.npHours)).perform(scrollUp)
        onView(withId(R.id.npMinutes)).perform(scrollUp)
        onView(withId(R.id.npSeconds)).perform(scrollUp)
        // set time
        onView(withId(R.id.btnSetTime)).perform(click())
        // Assert the time was saved
        onView(withId(R.id.tvCountDown)).check(matches(withText("1h 1m 1s")))
        // turn on
        onView(withId(R.id.ibTimerStartPause)).perform(click())
        // turn off
        // Delay can be different on another machine
        advanceTimeByAndRun(3000)
        onView(withId(R.id.ibTimerStartPause)).perform(click())
        // Assert that the Service counted the time
        advanceUntilIdle()
        onView(withId(R.id.tvCountDown)).check(matches(withText("1h 1m")))
    }
}
