package gentle.hilt.playground.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity

@Dao
interface SleepTrackingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackSleepingEntity(sleepTrackingEntity: SleepTrackingEntity)

    @Query("DELETE FROM sleep_table")
    suspend fun deleteTrackSleepingStatistic()

    @Query("SELECT * FROM sleep_table")
    fun observeAllSleepingTracking(): LiveData<List<SleepTrackingEntity>>
}
