package com.example.quickstage

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.quickstage.data.Ticket
import com.example.quickstage.ui.ScannerScreen
import com.example.quickstage.ui.theme.QuickStageTheme
import com.example.quickstage.ui.viewmodel.AppViewModel
import com.example.quickstage.ui.viewmodel.AppViewModelFactory
import com.example.quickstage.utils.QRCodeUtils

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission required for scanning", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        val app = application as QuickStageApplication
        val viewModel: AppViewModel by viewModels {
            AppViewModelFactory(app.database.ticketDao(), app.database.scanLogDao())
        }

        setContent {
            QuickStageTheme {
                MainApp(viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val adminPassword by viewModel.adminPassword.collectAsStateWithLifecycle()

    if (adminPassword == null) {
        LoginScreen { pwd -> viewModel.setAdminPassword(pwd) }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("tickets") },
                        icon = { Icon(Icons.Default.Add, contentDescription = "Tickets") },
                        label = { Text("Tickets") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("logs") },
                        icon = { Icon(Icons.Default.List, contentDescription = "Logs") },
                        label = { Text("Logs") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("scan") },
                        icon = { Text("📷") },
                        label = { Text("Scan") }
                    )
                }
            }
        ) { padding ->
            NavHost(navController = navController, startDestination = "tickets", modifier = Modifier.padding(padding)) {
                composable("tickets") { TicketsScreen(viewModel) }
                composable("logs") { LogsScreen(viewModel) }
                composable("scan") { ScannerScreen(viewModel) { navController.popBackStack() } }
            }
        }
    }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Admin Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { if (password.isNotEmpty()) onLogin(password) }) {
                Text("Login")
            }
        }
    }
}

@Composable
fun TicketsScreen(viewModel: AppViewModel) {
    val tickets by viewModel.allTickets.collectAsStateWithLifecycle(initialValue = emptyList())
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.generateTicket() }) {
                Icon(Icons.Default.Add, contentDescription = "Generate")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(tickets) { ticket ->
                TicketItem(ticket, onShare = { viewModel.shareTicket(context, ticket) })
            }
        }
    }
}

@Composable
fun TicketItem(ticket: Ticket, onShare: () -> Unit) {
    var showQr by remember { mutableStateOf(false) }
    
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), onClick = { showQr = !showQr }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ticket #${ticket.id}", style = MaterialTheme.typography.titleMedium)
                    Text("Hash: ${ticket.hash.take(8)}...", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
            
            if (showQr && ticket.hash != "PENDING") {
                Spacer(modifier = Modifier.height(8.dp))
                val content = "${ticket.id}.${ticket.hash}"
                val bitmap = remember(content) { QRCodeUtils.generateQRCode(content) }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun LogsScreen(viewModel: AppViewModel) {
    val logs by viewModel.allLogs.collectAsStateWithLifecycle(initialValue = emptyList())
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logs) { log ->
            ListItem(
                headlineContent = { Text("Ticket #${log.ticketId}") },
                supportingContent = { Text("${log.scannedAt}\n${log.message}") },
                trailingContent = { 
                    Text(
                        if (log.isValid) "VALID" else "INVALID",
                        color = if (log.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            )
            HorizontalDivider()
        }
    }
}
