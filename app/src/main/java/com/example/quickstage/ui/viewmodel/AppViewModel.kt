package com.example.quickstage.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.quickstage.data.ScanLog
import com.example.quickstage.data.ScanLogDao
import com.example.quickstage.data.Ticket
import com.example.quickstage.data.TicketDao
import com.example.quickstage.utils.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.example.quickstage.utils.QRCodeUtils
import java.io.File
import java.io.FileOutputStream

sealed class ScanStatus {
    object Idle : ScanStatus()
    data class Success(val message: String) : ScanStatus()
    data class Error(val message: String) : ScanStatus()
}

class AppViewModel(
    private val ticketDao: TicketDao,
    private val scanLogDao: ScanLogDao
) : ViewModel() {

    private val _adminPassword = MutableStateFlow<String?>(null)
    val adminPassword = _adminPassword.asStateFlow()
    
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.Idle)
    val scanStatus = _scanStatus.asStateFlow()

    val allTickets = ticketDao.getAllTickets()
    val allLogs = scanLogDao.getAllLogs()

    fun setAdminPassword(password: String) {
        _adminPassword.value = password
    }
    
    fun resetScanStatus() {
        _scanStatus.value = ScanStatus.Idle
    }
    
    fun generateTicket(maxUsage: Int = 1) {
        val password = _adminPassword.value ?: return
        viewModelScope.launch {
            // 1. Insert with temporary hash
            val tempTicket = Ticket(hash = "PENDING", maxUsage = maxUsage)
            val id = ticketDao.insert(tempTicket).toInt()
            
            // 2. Compute real hash
            val hash = CryptoUtils.generateHash(id, password)
            
            // 3. Update ticket
            val updatedTicket = tempTicket.copy(id = id, hash = hash)
            ticketDao.update(updatedTicket)
        }
    }

    fun shareTicket(context: Context, ticket: Ticket) {
        if (ticket.hash == "PENDING") return

        val content = "${ticket.id}.${ticket.hash}"
        val bitmap = QRCodeUtils.generateQRCode(content) ?: return
        
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            val stream = FileOutputStream("$cachePath/ticket_${ticket.id}.png") // overwrites this image every time
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(context.cacheDir, "images")
            val newFile = File(imagePath, "ticket_${ticket.id}.png")
            val contentUri = FileProvider.getUriForFile(context, "com.example.quickstage.fileprovider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
                shareIntent.setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                context.startActivity(Intent.createChooser(shareIntent, "Share Ticket"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun processScan(content: String) {
        if (_scanStatus.value !is ScanStatus.Idle) return

        val password = _adminPassword.value ?: return
        viewModelScope.launch {
            try {
                // Format: <id>.<hash>
                val parts = content.split(".")
                if (parts.size != 2) {
                    handleScanResult(-1, false, "Invalid format")
                    return@launch
                }

                val ticketId = parts[0].toIntOrNull()
                val providedHash = parts[1]

                if (ticketId == null) {
                    handleScanResult(-1, false, "Invalid ID")
                    return@launch
                }

                // 1. Validate Hash
                val expectedHash = CryptoUtils.generateHash(ticketId, password)
                if (providedHash != expectedHash) {
                    handleScanResult(ticketId, false, "Invalid Hash")
                    return@launch
                }

                // 2. Check Database existence (optional but good)
                val ticket = ticketDao.getTicketById(ticketId)
                if (ticket == null) {
                    handleScanResult(ticketId, false, "Ticket not found in DB")
                    return@launch
                }

                // 3. Check Usage
                val usageCount = scanLogDao.getUsageCount(ticketId)
                if (usageCount >= ticket.maxUsage) {
                    handleScanResult(ticketId, false, "Usage limit exceeded")
                    return@launch
                }

                // Valid
                handleScanResult(ticketId, true, "Success")
                
            } catch (e: Exception) {
                handleScanResult(-1, false, "Error processing: ${e.message}")
            }
        }
    }
    
    private suspend fun handleScanResult(ticketId: Int, isValid: Boolean, message: String) {
        scanLogDao.insert(ScanLog(ticketId = ticketId, isValid = isValid, message = message))
        if (isValid) {
            _scanStatus.value = ScanStatus.Success(message)
        } else {
            _scanStatus.value = ScanStatus.Error(message)
        }
    }
}

class AppViewModelFactory(
    private val ticketDao: TicketDao,
    private val scanLogDao: ScanLogDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppViewModel(ticketDao, scanLogDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
