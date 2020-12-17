package com.example.androidDeviceDetails.models

import androidx.room.*

@Entity
data class AppDataUsage(
    @PrimaryKey val timeStamp: Long,
    @ColumnInfo(name = "packageName ") val packageName: String,
    @ColumnInfo(name = "transferredDataWifi") val transferredDataWifi: Long,
    @ColumnInfo(name = "transferredDataMobile") val transferredDataMobile: Long,
    @ColumnInfo(name = "receivedDataWifi ") val receivedDataWifi: Long,
    @ColumnInfo(name = "receivedDataMobile ") val receivedDataMobile: Long
)

@Dao
interface AppDataUsageDao {
    @Query("SELECT * FROM AppDataUsage")
    fun getAll(): List<AppDataUsage>

    @Query("SELECT * FROM AppDataUsage WHERE timeStamp BETWEEN (:startTime) AND (:endTime)")
    fun getAllBetween(startTime: Long, endTime: Long): List<AppDataUsage>

    @Query("DELETE FROM AppDataUsage")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg appDataUsage: AppDataUsage)

    @Delete
    fun delete(appDataUsage: AppDataUsage)
}
