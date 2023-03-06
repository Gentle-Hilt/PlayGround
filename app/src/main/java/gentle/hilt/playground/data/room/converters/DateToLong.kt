package gentle.hilt.playground.data.room.converters

import androidx.room.TypeConverter
import java.util.*

object DateToLong {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
