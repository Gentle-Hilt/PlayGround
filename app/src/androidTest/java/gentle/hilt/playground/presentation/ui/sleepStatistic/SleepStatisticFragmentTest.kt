package gentle.hilt.playground.presentation.ui.sleepStatistic

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
import gentle.hilt.playground.androidTestUtils.getOrAwaitValue
import gentle.hilt.playground.androidTestUtils.launchFragmentInHiltContainer
import gentle.hilt.playground.data.room.SleepTrackingDataBase
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity
import gentle.hilt.playground.data.room.repository.SleepTrackingRepository
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*
import javax.inject.Inject

@HiltAndroidTest
@ExperimentalCoroutinesApi
class SleepStatisticFragmentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var navController: NavController

    @Inject
    lateinit var repository: SleepTrackingRepository

    @Inject
    lateinit var dataBase: SleepTrackingDataBase

    private lateinit var viewModel: SleepStatisticViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        MockKAnnotations.init(this)

        viewModel = SleepStatisticViewModel(repository)
    }

    @After
    fun tearDown() {
        dataBase.clearAllTables()
    }

    private var entity: SleepTrackingEntity = SleepTrackingEntity(
        Calendar.getInstance().time.time.toInt(),
        1,
        1,
        Calendar.getInstance().time,
        ""
    )


    @Test
    fun adding_statistic_populates_list() {
        launchFragmentInHiltContainer<SleepStatisticFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }

        onView(withId(R.id.tvYouNeedStatistic)).check(matches(isDisplayed()))
        // Message that you never set alarm
        Truth.assertThat(viewModel.sleepTrackingEntities.getOrAwaitValue()).isEmpty()
        // Inserting sleep data
        viewModel.insertSleepingTrackEntityIntoDB(entity)
        // Message should be gone
        Truth.assertThat(withId(R.id.tvYouNeedStatistic).matches(isDisplayed())).isFalse()
    }

    @Test
    fun delete_all_sleep_statistic() = runTest {
        // Inserting initial statistic for delete btn to appear in the first place
        viewModel.insertSleepingTrackEntityIntoDB(entity)

        launchFragmentInHiltContainer<SleepStatisticFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
        // The message about empty statistic
        Truth.assertThat(withId(R.id.tvYouNeedStatistic).matches(isDisplayed())).isFalse()

        onView(withId(R.id.btnDelete)).perform(click())
        onView(withText(R.string.delete_sleep_positive)).perform(click())
        // Data should be deleted
        Truth.assertThat(viewModel.sleepTrackingEntities.getOrAwaitValue()).isEmpty()
    }
}
