package gentle.hilt.playground.presentation.ui.maps

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.unitTestUtils.TestCoroutineRule
import gentle.hilt.playground.unitTestUtils.getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MapScreenViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val rule = TestCoroutineRule() // UnconfinedTestDispatcher right now

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @RelaxedMockK
    lateinit var rxDataStore: DataStoreManager

    @Test
    fun `read from dataStore Preferences`() = rule.runTest {
        val makesMeSad = "LiveData value was never set"
        // returning fake data
        every { rxDataStore.readFirstPoint } returns flow {
            emit(makesMeSad)
        }
        // Why test fails if i place viewModel in @Before or above every{} ?
        val viewModel = MapScreenViewModel(rxDataStore, rule.testCoroutineDispatcher)
        assertEquals(viewModel.readFirstPoint.getOrAwaitValue(), makesMeSad)
    }

    @Test
    fun `save to dataStore Preferences`() = rule.runTest {
        val makesMeSad = "Expected at least one element"
        val dataStore = DataStoreManager(context)
        // Testing viewModelScope https://gist.github.com/manuelvicnt/751f4b77e91b7be2bc5f7981b25ee75e
        // Why i can't call viewModel.saveFirstPoint when
        // it has the same test dispatcher that i pass through constructor
        val viewModel = MapScreenViewModel(dataStore, rule.testCoroutineDispatcher)
        viewModel.dataStore.saveFirstPoint(makesMeSad)
        // Then
        assertEquals(dataStore.readFirstPoint.first(), makesMeSad)
    }
}
