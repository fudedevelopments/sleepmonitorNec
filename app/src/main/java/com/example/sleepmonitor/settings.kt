package com.example.sleepmonitor
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,  // We'll use a single entry, so we set a fixed ID
    var threshold: Double,
    var mobileNumber: String,
    var alarmDuration: Int
)

