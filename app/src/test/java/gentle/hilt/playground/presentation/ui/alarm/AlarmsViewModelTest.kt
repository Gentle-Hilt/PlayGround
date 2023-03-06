package gentle.hilt.playground.presentation.ui.alarm

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import gentle.hilt.playground.data.datastore.DataStoreManager
import gentle.hilt.playground.data.room.AlarmDataBase
import gentle.hilt.playground.data.room.dao.DaoAlarm
import gentle.hilt.playground.data.room.entity.AlarmEntity
import gentle.hilt.playground.data.room.repository.AlarmEntityRepository
import gentle.hilt.playground.unitTestUtils.TestCoroutineRule
import gentle.hilt.playground.unitTestUtils.getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AlarmsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val rule = TestCoroutineRule() // UnconfinedTestDispatcher right now

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var viewModel: AlarmsViewModel
    private lateinit var dataBase: AlarmDataBase
    private lateinit var daoAlarm: DaoAlarm

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        dataBase = Room.inMemoryDatabaseBuilder(context, AlarmDataBase::class.java)
            .allowMainThreadQueries().build()
        daoAlarm = dataBase.alarmDao()

        viewModel = AlarmsViewModel(rxDataStore, AlarmEntityRepository(daoAlarm), rule.testCoroutineDispatcher)
    }

    @After
    fun tearDown() {
        unmockkAll()
        dataBase.close()
    }

    @RelaxedMockK
    lateinit var rxDataStore: DataStoreManager

    @Test
    fun `read from dataStore Preferences`() = rule.runTest {
        val makesMeSad = "LiveData value was never set"
        // returning fake data
        every { rxDataStore.readUserAlarmTittle } returns flow {
            emit(makesMeSad)
        }
        // Why test fails if i place viewModel in @Before or above every{} ?
        val viewModel = AlarmsViewModel(rxDataStore, AlarmEntityRepository(daoAlarm), rule.testCoroutineDispatcher)
        assertEquals(viewModel.readUserAlarmTittle.getOrAwaitValue(), makesMeSad)
    }

    @Test
    fun `save to dataStore Preferences`() = rule.runTest {
        val makesMeSad = "Expected at least one element"
        val dataStore = DataStoreManager(context)
        // Testing viewModelScope https://gist.github.com/manuelvicnt/751f4b77e91b7be2bc5f7981b25ee75e
        // Why i can't call viewModel.saveUserAlarmTittle when
        // it has the same test dispatcher that i pass through constructor
        val viewModel = AlarmsViewModel(dataStore, AlarmEntityRepository(daoAlarm), rule.testCoroutineDispatcher)
        viewModel.dataStore.saveUserAlarmTittle(makesMeSad)
        // Then
        assertEquals(dataStore.readUserAlarmTittle.first(), makesMeSad)
    }

    private var alarmEntity = AlarmEntity(
        1,
        "Nice",
        "",
        1,
        1,
        "",
        false,
        Calendar.getInstance().time
    )

    @Test
    fun `inserting alarm populates list, status success`() = rule.runTest {
        // When
        viewModel.insertEntityAlarmIntoDb(alarmEntity)
        // Then
        assertEquals(viewModel.uiState.value, UiState.Success(alarmEntity))
        assertThat(viewModel.observeAlarmEntities.getOrAwaitValue()).isEqualTo(listOf(alarmEntity))
    }

    @Test
    fun `delete alarm from list`() = rule.runTest {
        // Given
        daoAlarm.insertEntityAlarm(alarmEntity)
        // When
        assertThat(viewModel.observeAlarmEntities.getOrAwaitValue()).isNotEmpty()
        viewModel.deleteEntityAlarmFromDb(alarmEntity)
        // Then
        assertThat(viewModel.observeAlarmEntities.getOrAwaitValue()).isEmpty()
    }

    @Test
    fun `update alarm in list, turn off, turn turn on`() {
        // Given
        assertThat(alarmEntity.isEnabled).isFalse()
        viewModel.insertEntityAlarmIntoDb(alarmEntity)
        // When
        val alarmTrue = alarmEntity.copy(isEnabled = true)
        viewModel.updateAlarmInDb(alarmTrue)
        // Then
        val value = viewModel.observeAlarmEntities.getOrAwaitValue()
        assertEquals(value[0].isEnabled, alarmTrue.isEnabled)
    }
}
