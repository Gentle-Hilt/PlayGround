package gentle.hilt.playground.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "alarm_table")
data class AlarmEntity(
    @PrimaryKey val id: Int,
    val timerGoal: String,
    @ColumnInfo(name = "alarm_image")
    val image: String,
    val hour: Int,
    val minute: Int,
    val requestCode: String,
    @ColumnInfo(name = "is_enable") val isEnabled: Boolean? = null,
    @ColumnInfo(name = "alarm_creation_date") val creationDate: Date = Date()
)
