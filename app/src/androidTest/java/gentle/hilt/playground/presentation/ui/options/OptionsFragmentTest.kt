package gentle.hilt.playground.presentation.ui.options

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth
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
class OptionsFragmentTest {
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

        launchFragmentInHiltContainer<OptionsFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
    }

    @Test
    fun set_dark_theme() {
        onView(withId(R.id.switchNightMode)).check(matches(isNotChecked()))
        onView(withId(R.id.switchNightMode)).perform(click())
        onView(withId(R.id.switchNightMode)).check(matches(isChecked()))
    }

    @Test
    fun back_to_alarmsFragment() {
        onView(withId(R.id.optionsBtnBack)).perform(click())

        verify {
            navController.navigate(
                OptionsFragmentDirections.actionOptionsFragmentToAlarmsFragment()
            )
        }
    }

    @Test
    fun click_on_date_and_time_settings() {
        onView(withId(R.id.optionsBtnBack)).check(matches(isDisplayed()))
        onView(withId(R.id.layoutDateAndTime)).perform(click())
        Truth.assertThat(withId(R.id.optionsBtnBack).matches(isDisplayed())).isFalse()
    }

    @Test
    fun click_on_dialog_to_change_default_alarm_sound() {
        onView(withId(R.id.optionsBtnBack)).check(matches(isDisplayed()))
        onView(withId(R.id.llDefaultAlarmSound)).perform(click())
        onView(withText(R.string.agree_option)).perform(click())
        Truth.assertThat(withId(R.id.optionsBtnBack).matches(isDisplayed())).isFalse()
    }

    @Test
    fun click_on_snooze_to_change_snooze_time() {
        onView(withId(R.id.llAlarmSnooze)).perform(click())
        onView(withText("OK")).perform(click())
        onView(withText(R.string.snooze_message_made)).check(matches(isDisplayed()))
    }

    @Test
    fun click_to_change_default_ringtone_sound() {
        onView(withId(R.id.optionsBtnBack)).check(matches(isDisplayed()))
        onView(withId(R.id.llDefaultRingtoneSound)).perform(click())
        onView(withText(R.string.agree_option)).perform(click())
        Truth.assertThat(withId(R.id.optionsBtnBack).matches(isDisplayed())).isFalse()
    }
}
