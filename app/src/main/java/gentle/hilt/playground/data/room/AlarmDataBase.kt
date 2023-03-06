package gentle.hilt.playground.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import gentle.hilt.playground.data.room.AlarmDataBase.Companion.DB_ALARM_VERSION
import gentle.hilt.playground.data.room.converters.DateToLong
import gentle.hilt.playground.data.room.dao.DaoAlarm
import gentle.hilt.playground.data.room.entity.AlarmEntity

@Database(
    entities = [AlarmEntity::class],
    version = DB_ALARM_VERSION
)
@TypeConverters(DateToLong::class)
abstract class AlarmDataBase : RoomDatabase() {

    abstract fun alarmDao(): DaoAlarm

    companion object {
        const val DB_ALARM_VERSION = 1
    }
}
