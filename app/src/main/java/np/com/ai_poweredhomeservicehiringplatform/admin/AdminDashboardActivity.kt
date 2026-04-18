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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(title = "Admin Dashboard")
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(text = "LOGOUT")
            }
        }
    }
}

