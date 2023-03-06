package gentle.hilt.playground.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import gentle.hilt.playground.data.room.SleepTrackingDataBase.Companion.DB_SLEEP_TRACKING_VERSION
import gentle.hilt.playground.data.room.converters.DateToLong
import gentle.hilt.playground.data.room.dao.SleepTrackingDao
import gentle.hilt.playground.data.room.entity.SleepTrackingEntity

@Database(
    entities = [SleepTrackingEntity::class],
    version = DB_SLEEP_TRACKING_VERSION
)
@TypeConverters(DateToLong::class)
abstract class SleepTrackingDataBase : RoomDatabase() {

    abstract fun sleepTrackingDao(): SleepTrackingDao

    companion object {
        const val DB_SLEEP_TRACKING_VERSION = 1
    }
}
