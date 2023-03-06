package gentle.hilt.playground.data.room.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import gentle.hilt.playground.data.room.entity.AlarmEntity

@Dao
interface DaoAlarm {

    @Insert
    suspend fun insertEntityAlarm(alarmEntity: AlarmEntity)

    @Delete
    suspend fun deleteEntityAlarm(alarmEntity: AlarmEntity)

    @Query("SELECT * FROM alarm_table")
    fun observeAllAlarmEntities(): LiveData<List<AlarmEntity>>

    @Update
    suspend fun update(alarmEntity: AlarmEntity)
}
