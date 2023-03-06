package gentle.hilt.playground.data.room.repository

import androidx.lifecycle.LiveData
import gentle.hilt.playground.data.room.dao.DaoAlarm
import gentle.hilt.playground.data.room.entity.AlarmEntity
import javax.inject.Inject

class AlarmEntityRepository @Inject constructor(
    private val daoAlarm: DaoAlarm
) {

    suspend fun update(alarmEntity: AlarmEntity) = daoAlarm.update(alarmEntity)

    suspend fun insertEntityAlarm(alarmEntity: AlarmEntity) {
        daoAlarm.insertEntityAlarm(alarmEntity)
    }

    suspend fun deleteEntityAlarm(alarmEntity: AlarmEntity) {
        daoAlarm.deleteEntityAlarm(alarmEntity)
    }

    fun observeAllAlarmEntities(): LiveData<List<AlarmEntity>> {
        return daoAlarm.observeAllAlarmEntities()
    }
}
