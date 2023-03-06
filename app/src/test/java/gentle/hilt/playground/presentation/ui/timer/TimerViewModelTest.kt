package gentle.hilt.playground.presentation.ui.timer

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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TimerViewModelTest {
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
        val makesMeSad = 1L
        // returning fake data
        every { rxDataStore.readFutureTimeInSeconds } returns flow {
            emit(makesMeSad)
        }
        // Why test fails if i place viewModel in @Before or above every{} ?
        val viewModel = TimerViewModel(rxDataStore, rule.testCoroutineDispatcher)
        assertEquals(viewModel.futureTime.getOrAwaitValue(), makesMeSad)
    }

    @Test
    fun `save to dataStore Preferences`() = rule.runTest {
        val makesMeSad = 1L
        val dataStore = DataStoreManager(context)
        // Testing viewModelScope https://gist.github.com/manuelvicnt/751f4b77e91b7be2bc5f7981b25ee75e
        // Why i can't call viewModel.saveFutureTimeInSeconds when
        // it has the same test dispatcher that i pass through constructor
        val viewModel = TimerViewModel(dataStore, rule.testCoroutineDispatcher)
        viewModel.dataStore.saveFutureTimeInSeconds(makesMeSad)
        // Then
        assertEquals(dataStore.readFutureTimeInSeconds.first(), makesMeSad)
    }
}
