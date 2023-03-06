package gentle.hilt.playground.data.room.repository

import androidx.lifecycle.LiveData
import gentle.hilt.playground.data.room.dao.SleepTrackingDao
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity

class SleepTrackingRepository(
    private val daoSleep: SleepTrackingDao
) {

    suspend fun insertSleepTracking(trackingEntity: SleepTrackingEntity) {
        daoSleep.insertTrackSleepingEntity(trackingEntity)
    }

    suspend fun deleteSleepTrackingStatistic() {
        daoSleep.deleteTrackSleepingStatistic()
    }

    fun observeAllSleepingTrackingEntities(): LiveData<List<SleepTrackingEntity>> {
        return daoSleep.observeAllSleepingTracking()
    }
}
