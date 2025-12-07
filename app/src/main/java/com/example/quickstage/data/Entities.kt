package com.example.quickstage.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hash: String,
    val maxUsage: Int = 1,
    val createdAt: Date = Date()
)

@Entity(tableName = "scan_logs")
data class ScanLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ticketId: Int,
    val scannedAt: Date = Date(),
    val isValid: Boolean,
    val message: String
)
