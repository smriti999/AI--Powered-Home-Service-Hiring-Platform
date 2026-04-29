package np.com.ai_poweredhomeservicehiringplatform.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import np.com.ai_poweredhomeservicehiringplatform.R
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.FullScreenLoading
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.RequestBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberPendingRequestCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class AdminDashboardActivity : ComponentActivity() {
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
                AdminDashboardScreen(
                    onRequestsClick = { startActivity(Intent(this, AdminRequestsActivity::class.java)) },
                    onWorkersClick = { startActivity(Intent(this, AdminWorkerManagementActivity::class.java)) },
                    onUsersClick = { startActivity(Intent(this, AdminUserManagementActivity::class.java)) },
                    onWorksClick = { startActivity(Intent(this, AdminWorkManagementActivity::class.java)) },
                    onRevenueClick = { startActivity(Intent(this, AdminRevenueActivity::class.java)) },
                    onBroadcastClick = { startActivity(Intent(this, AdminBroadcastActivity::class.java)) },
                    onLogoutClick = {
                        AppStorage.logoutAll(this)
                        startActivity(
                            Intent(this, LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminDashboardScreen(
    onRequestsClick: () -> Unit,
    onWorkersClick: () -> Unit,
    onUsersClick: () -> Unit,
    onWorksClick: () -> Unit,
    onRevenueClick: () -> Unit,
    onBroadcastClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var users by remember { mutableStateOf(emptyList<np.com.ai_poweredhomeservicehiringplatform.common.model.UserUiModel>()) }
    var workers by remember { mutableStateOf(emptyList<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel>()) }
    var works by remember { mutableStateOf(emptyList<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>()) }
    var payments by remember { mutableStateOf(emptyList<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>()) }
    val pendingRequestCount = rememberPendingRequestCount()

    LaunchedEffect(Unit) {
        isLoading = true
        val loadedUsers = withContext(Dispatchers.IO) { AppStorage.loadUsers(context) }
        val loadedWorkers = withContext(Dispatchers.IO) { AppStorage.loadWorkers(context) }
        val loadedWorks = withContext(Dispatchers.IO) { AppStorage.loadWorks(context) }
        val loadedPayments = withContext(Dispatchers.IO) { AppStorage.loadPayments(context) }
        users = loadedUsers
        workers = loadedWorkers
        works = loadedWorks
        payments = loadedPayments
        isLoading = false
    }

    val totalUsers = users.size
    val activeWorkers = workers.count { it.status.equals("Active", ignoreCase = true) }
    val pendingJobs = works.count { it.status == np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus.Pending }
    val revenue = payments.filter { it.status == np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus.Paid }.sumOf { it.amountNpr }
    var selectedTab by remember { mutableStateOf(AdminBottomTab.Home) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Admin Dashboard",
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(32.dp)
                    )
                },
                actions = {
                    RequestBell(count = pendingRequestCount, onClick = onRequestsClick)
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = selectedTab == AdminBottomTab.Home,
                    onClick = { selectedTab = AdminBottomTab.Home },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(text = "Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AdminWorkerManagementActivity::class.java))
                    },
                    icon = { Icon(imageVector = Icons.Default.Group, contentDescription = "Workers") },
                    label = { Text(text = "Workers") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AdminUserManagementActivity::class.java))
                    },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Users") },
                    label = { Text(text = "Users") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AdminMenuActivity::class.java))
                    },
                    icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text(text = "Menu") }
                )
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                FullScreenLoading()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = onUsersClick
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Total Users", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = totalUsers.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = onWorkersClick
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Active Workers", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activeWorkers.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = onWorksClick
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Pending Jobs", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pendingJobs.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    onClick = onRevenueClick
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Revenue", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rs. $revenue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(text = "Recent Activity", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No recent activity",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
