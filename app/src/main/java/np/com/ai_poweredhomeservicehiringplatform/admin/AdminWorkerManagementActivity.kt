package np.com.ai_poweredhomeservicehiringplatform.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.AppDrawer
import np.com.ai_poweredhomeservicehiringplatform.ui.components.BurgerMenuIcon
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NavigationItem
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class AdminWorkerManagementActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isAdminLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                AdminWorkerManagementScreen(
                    onDashboardClick = { startActivity(Intent(this, AdminDashboardActivity::class.java)) },
                    onRequestsClick = { startActivity(Intent(this, AdminRequestsActivity::class.java)) },
                    onUsersClick = { startActivity(Intent(this, AdminUserManagementActivity::class.java)) },
                    onWorksClick = { startActivity(Intent(this, AdminWorkManagementActivity::class.java)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminWorkerManagementScreen(
    onDashboardClick: () -> Unit,
    onRequestsClick: () -> Unit,
    onUsersClick: () -> Unit,
    onWorksClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var workers by remember { mutableStateOf(AppStorage.loadWorkers(context)) }
    var selectedWorker by remember { mutableStateOf<WorkerUiModel?>(null) }

    val filteredWorkers = workers.filter { worker ->
        val query = searchQuery.trim()
        query.isBlank() || worker.name.contains(query, ignoreCase = true)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navItems = listOf(
        NavigationItem("Dashboard", Icons.Default.Dashboard, onDashboardClick),
        NavigationItem("Requests", Icons.Default.PendingActions, onRequestsClick),
        NavigationItem("Workers", Icons.Default.Group, { }),
        NavigationItem("Users", Icons.Default.Group, onUsersClick),
        NavigationItem("Works", Icons.Default.List, onWorksClick),
        NavigationItem("Revenue", Icons.Default.MonetizationOn, {
            context.startActivity(Intent(context, AdminRevenueActivity::class.java))
        }),
        NavigationItem("Logout", Icons.Default.ExitToApp, {
            AppStorage.setAdminLoggedIn(context, false)
            context.startActivity(Intent(context, LoginActivity::class.java))
            (context as? ComponentActivity)?.finish()
        })
    )

    AppDrawer(drawerState = drawerState, items = navItems) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LogoTopAppBar(
                    title = "Worker Management",
                    navigationIcon = {
                        BurgerMenuIcon(drawerState = drawerState)
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(text = "Search worker.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Total workers: ${workers.size}")

                Spacer(modifier = Modifier.height(12.dp))

                if (workers.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No workers available")
                    }
                } else if (filteredWorkers.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No workers found")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filteredWorkers.forEach { worker: WorkerUiModel ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedWorker = worker }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = worker.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Status: ${worker.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val updated = workers.filterNot { it.id == worker.id }
                                            workers = updated
                                            AppStorage.saveWorkers(context, updated)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD32F2F),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .height(32.dp)
                                            .widthIn(min = 84.dp)
                                    ) {
                                        Text(text = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedWorker?.let { w ->
        AlertDialog(
            onDismissRequest = { selectedWorker = null },
            title = { Text(text = "Worker Details") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailItem(label = "Full Name", value = w.name)
                    DetailItem(label = "Profession", value = w.profession)
                    DetailItem(label = "Experience", value = "${w.experienceYears} Years")
                    DetailItem(label = "Email", value = w.email)
                    DetailItem(label = "Phone", value = w.phoneNumber)
                    DetailItem(label = "Gender", value = w.gender)
                    DetailItem(label = "Location", value = w.location)
                    DetailItem(label = "Street/Home", value = w.streetHomeNumber)
                    if (w.alternativeLocation.isNotBlank()) {
                        DetailItem(label = "Alt Location", value = w.alternativeLocation)
                    }
                    DetailItem(label = "Status", value = w.status)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWorker = null }) {
                    Text(text = "Close")
                }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.widthIn(min = 100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
