package com.example.quickstage

import com.example.quickstage.data.ScanLogDao
import com.example.quickstage.data.Ticket
import com.example.quickstage.data.TicketDao
import com.example.quickstage.ui.viewmodel.AppViewModel
import com.example.quickstage.ui.viewmodel.ScanStatus
import com.example.quickstage.utils.CryptoUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private lateinit var viewModel: AppViewModel
    private lateinit var ticketDao: TicketDao
    private lateinit var scanLogDao: ScanLogDao
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ticketDao = mockk(relaxed = true)
        scanLogDao = mockk(relaxed = true)
        viewModel = AppViewModel(ticketDao, scanLogDao)
        
        // Mock static utils if needed, but CryptoUtils is pure logic so we can rely on real implementation or mock it.
        // For integration-like unit test, using real CryptoUtils is fine if it's stateless.
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `processScan with invalid format returns error`() = runTest {
        viewModel.setAdminPassword("admin123")
        viewModel.processScan("invalid_qr_code")
        advanceUntilIdle()

        val status = viewModel.scanStatus.value
        assertTrue(status is ScanStatus.Error)
        assertEquals("Invalid format", (status as ScanStatus.Error).message)
    }

    @Test
    fun `processScan with valid hash and usage limit not exceeded returns success`() = runTest {
        val password = "admin123"
        val ticketId = 1
        val hash = CryptoUtils.generateHash(ticketId, password)
        val qrContent = "$ticketId.$hash"
        
        viewModel.setAdminPassword(password)
        
        coEvery { ticketDao.getTicketById(ticketId) } returns Ticket(id = ticketId, hash = hash, maxUsage = 1)
        coEvery { scanLogDao.getUsageCount(ticketId) } returns 0
        
        viewModel.processScan(qrContent)
        advanceUntilIdle()

        val status = viewModel.scanStatus.value
        assertTrue(status is ScanStatus.Success)
        assertEquals("Success", (status as ScanStatus.Success).message)
        
        coVerify { scanLogDao.insert(any()) }
    }

    @Test
    fun `processScan with wrong hash returns error`() = runTest {
        val password = "admin123"
        val ticketId = 1
        val wrongHash = "wrongHash"
        val qrContent = "$ticketId.$wrongHash"
        
        viewModel.setAdminPassword(password)
        
        viewModel.processScan(qrContent)
        advanceUntilIdle()

        val status = viewModel.scanStatus.value
        assertTrue(status is ScanStatus.Error)
        assertEquals("Invalid Hash", (status as ScanStatus.Error).message)
    }

    @Test
    fun `processScan with usage limit exceeded returns error`() = runTest {
        val password = "admin123"
        val ticketId = 1
        val hash = CryptoUtils.generateHash(ticketId, password)
        val qrContent = "$ticketId.$hash"
        
        viewModel.setAdminPassword(password)
        
        coEvery { ticketDao.getTicketById(ticketId) } returns Ticket(id = ticketId, hash = hash, maxUsage = 1)
        coEvery { scanLogDao.getUsageCount(ticketId) } returns 1 // Already used once
        
        viewModel.processScan(qrContent)
        advanceUntilIdle()

        val status = viewModel.scanStatus.value
        assertTrue(status is ScanStatus.Error)
        assertEquals("Usage limit exceeded", (status as ScanStatus.Error).message)
    }
}
