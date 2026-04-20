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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.AppDrawer
import np.com.ai_poweredhomeservicehiringplatform.ui.components.BurgerMenuIcon
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NavigationItem
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
                        AppStorage.setAdminLoggedIn(this, false)
                        startActivity(Intent(this, LoginActivity::class.java))
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
    val users = AppStorage.loadUsers(context)
    val workers = AppStorage.loadWorkers(context)
    val works = AppStorage.loadWorks(context)
    val payments = AppStorage.loadPayments(context)

    val totalUsers = users.size
    val activeWorkers = workers.count { it.status.equals("Active", ignoreCase = true) }
    val pendingJobs = works.count { it.status == np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus.Pending }
    val revenue = payments.filter { it.status == np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus.Paid }.sumOf { it.amountNpr }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navItems = listOf(
        NavigationItem("Dashboard", Icons.Default.Dashboard, { }),
        NavigationItem("Requests", Icons.Default.PendingActions, onRequestsClick),
        NavigationItem("Workers", Icons.Default.Group, onWorkersClick),
        NavigationItem("Users", Icons.Default.Group, onUsersClick),
        NavigationItem("Works", Icons.AutoMirrored.Filled.List, onWorksClick),
        NavigationItem("Revenue", Icons.Default.MonetizationOn, onRevenueClick),
        NavigationItem("ML Model", Icons.Default.Psychology, {
            context.startActivity(Intent(context, AdminMlModelReportActivity::class.java))
        }),
        NavigationItem("Broadcast", Icons.Default.Campaign, onBroadcastClick),
        NavigationItem("Logout", Icons.AutoMirrored.Filled.ExitToApp, onLogoutClick)
    )

    AppDrawer(drawerState = drawerState, items = navItems) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LogoTopAppBar(
                    title = "Admin Dashboard",
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onRequestsClick, modifier = Modifier.weight(1f)) { Text(text = "Requests") }
                Button(onClick = onWorkersClick, modifier = Modifier.weight(1f)) { Text(text = "Workers") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onUsersClick, modifier = Modifier.weight(1f)) { Text(text = "Users") }
                Button(onClick = onWorksClick, modifier = Modifier.weight(1f)) { Text(text = "Works") }
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
}
