package gentle.hilt.playground.presentation.ui.maps

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.common.truth.Truth
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import gentle.hilt.playground.R
import gentle.hilt.playground.androidTestUtils.TestCoroutineRule
import gentle.hilt.playground.androidTestUtils.getOrAwaitValue
import gentle.hilt.playground.androidTestUtils.launchFragmentInHiltContainer
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.presentation.app.PlayGroundActivity
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.hamcrest.CoreMatchers.anything
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

// Launch each test individually because MapKitFactory.setApiKey
// should be called only once, when application started
// yandex just didn't create self check initialization mechanism
@HiltAndroidTest
@ExperimentalCoroutinesApi
class MapScreenFragmentTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var navController: NavController

    private var rule = TestCoroutineRule() // StandardTestDispatcher()

    @Inject
    lateinit var dataStore: DataStoreManager

    private lateinit var viewModel: MapScreenViewModel

    companion object {
        const val PRELOAD_START_LOCATION = 1
        const val BUS_ROUTE = 1
    }

    @Before
    fun setup() {
        hiltRule.inject()
        MockKAnnotations.init(this)

        MapKitFactory.setApiKey(PlayGroundActivity.MAP_API_KEY)
        viewModel = MapScreenViewModel(dataStore, rule.testCoroutineDispatcher)

        launchFragmentInHiltContainer<MapScreenFragment> {
            Navigation.setViewNavController(requireView(), navController)
        }
    }

    @Test
    fun search_start_location() = runTest {
        withContext(Dispatchers.IO) {
            // Wait for api to load before searching
            Thread.sleep(7000)
            // DataStore should not have any value
            Truth.assertThat(viewModel.dataStore.readFirstPointLatitude.first()).isEqualTo(0.0)
            // Start searching
            onView(withId(R.id.searchFirstPoint)).perform(click())
            onView(withId(R.id.searchFirstPoint)).perform(replaceText("Komsomolskaya 138"))
            // Wait for search results to appear and click
            Thread.sleep(3000)
            onData(anything()).inAdapterView(withId(R.id.lvSuggestResult)).atPosition(0).perform(click())
            // Wait for navigation, new coordinates should be saved to dataStore
            Thread.sleep(3000)
            Truth.assertThat(viewModel.readFirstPointLatitude.getOrAwaitValue()).isNotEqualTo(0.0)
        }
    }

    @Test
    fun search_destination() = runTest {
        withContext(Dispatchers.IO) {
            // Wait for api to load before searching
            Thread.sleep(7000)
            // DataStore should not have any value
            Truth.assertThat(viewModel.dataStore.readSecondPointLatitude.first()).isEqualTo(0.0)
            // Start searching
            onView(withId(R.id.searchSecondPoint)).perform(click())
            onView(withId(R.id.searchSecondPoint)).perform(replaceText("Komsomolskaya 145"))
            // Wait for search results to appear and click
            Thread.sleep(3000)
            onData(anything()).inAdapterView(withId(R.id.lvSuggestResult)).atPosition(2).perform(click())
            // Wait for navigation, new coordinates should be saved to dataStore
            Thread.sleep(3000)
            Truth.assertThat(viewModel.readSecondPointLatitude.getOrAwaitValue()).isNotEqualTo(0.0)
        }
    }

    @Test
    fun map_option_change() {
        onView(withId(R.id.mapOptions)).perform(click())

        onView(withText(R.string.start_menu)).check(matches(isDisplayed()))
        onView(withText(R.string.start_menu)).perform(click())

        // Datastore should update its new value
        val value = viewModel.readUserPreloadLocationChoice.getOrAwaitValue()
        Truth.assertThat(value).isEqualTo(PRELOAD_START_LOCATION)
    }

    @Test
    fun route_option_change() {
        onView(withId(R.id.routeOptions)).perform(click())

        onView(withText(R.string.bus_menu)).check(matches(isDisplayed()))
        onView(withText(R.string.bus_menu)).perform(click())

        val value = viewModel.readRouteType.getOrAwaitValue()
        Truth.assertThat(value).isEqualTo(BUS_ROUTE)
    }

    @Test
    fun zoom_in_change() {
        val initialZoom = viewModel.readUserZoomChange.getOrAwaitValue()
        onView(withId(R.id.zoomIn)).perform(click())
        val changedZoom = viewModel.readUserZoomChange.getOrAwaitValue()
        Truth.assertThat(initialZoom).isNotEqualTo(changedZoom)
    }

    @Test
    fun zoom_out_change() {
        val initialZoom = viewModel.readUserZoomChange.getOrAwaitValue()
        onView(withId(R.id.zoomOut)).perform(click())
        val changedZoom = viewModel.readUserZoomChange.getOrAwaitValue()
        Truth.assertThat(initialZoom).isNotEqualTo(changedZoom)
    }

    @Test
    fun tracking_mode_turn_on_off() {
        // Initially visible views
        onView(withId(R.id.searchFirstPoint)).check(matches(isDisplayed()))
        onView(withId(R.id.searchSecondPoint)).check(matches(isDisplayed()))
        onView(withId(R.id.mapOptions)).check(matches(isDisplayed()))
        onView(withId(R.id.routeOptions)).check(matches(isDisplayed()))
        onView(withId(R.id.ibMakeRoute)).check(matches(isDisplayed()))
        // On
        onView(withId(R.id.turnUserTracking)).perform(click())
        // Views should be hidden
        Truth.assertThat(withId(R.id.searchFirstPoint).matches(isDisplayed())).isFalse()
        Truth.assertThat(withId(R.id.searchSecondPoint).matches(isDisplayed())).isFalse()
        Truth.assertThat(withId(R.id.mapOptions).matches(isDisplayed())).isFalse()
        Truth.assertThat(withId(R.id.routeOptions).matches(isDisplayed())).isFalse()
        Truth.assertThat(withId(R.id.ibMakeRoute).matches(isDisplayed())).isFalse()
        // Datastore saved tracking choice
        val on = viewModel.readUserTrackingChoice.getOrAwaitValue()
        Truth.assertThat(on).isTrue()
        // Off
        onView(withId(R.id.turnUserTracking)).perform(click())
        // Views should appear again
        onView(withId(R.id.searchFirstPoint)).check(matches(isDisplayed()))
        onView(withId(R.id.searchSecondPoint)).check(matches(isDisplayed()))
        onView(withId(R.id.mapOptions)).check(matches(isDisplayed()))
        onView(withId(R.id.routeOptions)).check(matches(isDisplayed()))
        onView(withId(R.id.ibMakeRoute)).check(matches(isDisplayed()))

        val off = viewModel.readUserTrackingChoice.getOrAwaitValue()
        Truth.assertThat(off).isFalse()
    }
}
