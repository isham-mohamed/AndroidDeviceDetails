package com.example.androidDeviceDetails.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.androidDeviceDetails.collectors.SignalChangeCollector
import com.example.androidDeviceDetails.collectors.WifiCollector
import com.example.androidDeviceDetails.cooker.SignalCooker
import com.example.androidDeviceDetails.viewModel.SignalViewModel

/**
 * A data class used by the [WifiCollector] and [SignalChangeCollector] to record wifi and cellular
 * signal values and save to the [RoomDB.signalDao] and also used by the [SignalCooker]
 * @param timeStamp Time of the record.
 * @param signal To denote if the input signal row belongs to CELLULAR or WIFI
 *
 * 0 for CELLULAR
 *
 * 1 for WIFI.
 * @param strength Signal strength in dBm.
 * @param linkSpeed LinkSpeed in Mbps for WIFI
 * @param level Signal level.
 *
 *  @see [RoomDB]
 *  @see[SignalDao]
 */
@Entity
data class SignalRaw(
    @PrimaryKey val timeStamp: Long,
    @ColumnInfo(name = "signal") val signal: Int,
    @ColumnInfo(name = "strength") val strength: Int,
    @ColumnInfo(name = "linkSpeed") val linkSpeed: Int?,
    @ColumnInfo(name = "level") val level: String,
    @ColumnInfo(name = "operatorName") val operatorName: String,
    @ColumnInfo(name = "isRoaming") val isRoaming: Boolean,
    @ColumnInfo(name = "band") val band: String?,
    @ColumnInfo(name = "percentage") val strengthPercentage: Float
)

/**
 * An interface that contains functions to handle database operations.
 */
@Dao
interface SignalDao {

    /**
     * Insert the [signalRaw] into the table.
     * @param signalRaw record to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg signalRaw: SignalRaw)

    /**
     * Delete the [signalRaw] record from the [SignalRaw] table.
     * @param signalRaw record to be deleted.
     */
    @Delete
    fun delete(signalRaw: SignalRaw)

    /**
     * Returns the last [SignalRaw] from the table,
     * also observed by [SignalViewModel] to update UI on realtime basis.
     * @return [SignalRaw] which is used to update UI in [SignalViewModel].
     */
    @Query("SELECT * FROM SignalRaw ORDER BY timeStamp DESC LIMIT 1")
    fun getLastRaw(): LiveData<SignalRaw>

    /**
     * Returns all the [SignalRaw] in the given time frame.
     * @param startTime Start Time.
     * @param endTime End Time.
     * @return List of [SignalRaw].
     */
    @Query("SELECT * FROM SignalRaw WHERE (timeStamp BETWEEN (:startTime) AND (:endTime))")
    fun getAll(startTime: Long, endTime: Long): List<SignalRaw>

    /**
     * Delete all the records in the [SignalRaw] table.
     */
    @Query("DELETE FROM SignalRaw")
    fun deleteAll()

    @Insert
    fun insertAll(signalRawList: List<SignalRaw>)
}