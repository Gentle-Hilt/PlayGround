package gentle.hilt.playground.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "sleep_table")
data class SleepTrackingEntity(
    @PrimaryKey
    val id: Int,
    val hour: Int,
    val minute: Int,
    @ColumnInfo(name = "sleep_creation_date")
    val creationDate: Date = Date(),

    @ColumnInfo(name = "image_sleep")
    val image: String?

)
