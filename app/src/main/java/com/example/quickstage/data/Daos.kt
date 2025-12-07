package com.example.quickstage.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets ORDER BY createdAt DESC")
    fun getAllTickets(): Flow<List<Ticket>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ticket: Ticket): Long
    
    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getTicketById(id: Int): Ticket?
    
    @Update
    suspend fun update(ticket: Ticket)
}

@Dao
interface ScanLogDao {
    @Query("SELECT * FROM scan_logs ORDER BY scannedAt DESC")
    fun getAllLogs(): Flow<List<ScanLog>>
    
    @Query("SELECT COUNT(*) FROM scan_logs WHERE ticketId = :ticketId AND isValid = 1")
    suspend fun getUsageCount(ticketId: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(scanLog: ScanLog)
}
