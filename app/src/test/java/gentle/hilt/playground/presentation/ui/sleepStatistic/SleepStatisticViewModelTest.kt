package gentle.hilt.playground.presentation.ui.sleepStatistic

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import gentle.hilt.playground.data.room.SleepTrackingDataBase
import gentle.hilt.playground.data.room.dao.SleepTrackingDao
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity
import gentle.hilt.playground.data.room.repository.SleepTrackingRepository
import gentle.hilt.playground.unitTestUtils.TestCoroutineRule
import gentle.hilt.playground.unitTestUtils.getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SleepStatisticViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val rule = TestCoroutineRule() // UnconfinedTestDispatcher right now

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var dataBase: SleepTrackingDataBase
    private lateinit var daoSleepTracking: SleepTrackingDao
    private lateinit var viewModel: SleepStatisticViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        dataBase = Room.inMemoryDatabaseBuilder(context, SleepTrackingDataBase::class.java)
            .allowMainThreadQueries()
            .build()
        daoSleepTracking = dataBase.sleepTrackingDao()

        viewModel = SleepStatisticViewModel(SleepTrackingRepository(daoSleepTracking))
    }

    @After
    fun tearDown() {
        unmockkAll()
        dataBase.close()
    }

    private val sleepTrackingEntity = SleepTrackingEntity(
        1,
        1,
        1,
        Calendar.getInstance().time,
        ""
    )

    @Test
    fun `inserted statistic populates list`() = rule.runTest {
        // When
        assertThat(viewModel.sleepTrackingEntities.getOrAwaitValue()).isEmpty()
        viewModel.insertSleepingTrackEntityIntoDB(sleepTrackingEntity)
        // Then
        assertThat(viewModel.sleepTrackingEntities.getOrAwaitValue()).isEqualTo(listOf(sleepTrackingEntity))
    }

    @Test
    fun `delete ALL sleep statistic, clear list`() = rule.runTest {
        // Given
        daoSleepTracking.insertTrackSleepingEntity(sleepTrackingEntity)
        // When
        assertThat(viewModel.sleepTrackingEntities.getOrAwaitValue()).isNotEmpty()
        viewModel.deleteSleepingStatistic()
        // Then
        assertThat(viewModel.sleepTrackingEntities.getOrAwaitValue()).isEmpty()
    }
}
