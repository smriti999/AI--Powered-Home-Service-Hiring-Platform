package np.com.ai_poweredhomeservicehiringplatform.worker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.FullScreenLoading
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkerEarningsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isWorkerLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                WorkerEarningsScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerEarningsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val workerEmail = AppStorage.currentWorkerEmail(context).orEmpty()
    val notificationCount = rememberUnreadNotificationCount(workerEmail)
    var isLoading by rememberSaveable { mutableStateOf(true) }
    var workers by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel>>(emptyList()) }
    var works by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>>(emptyList()) }
    var userJobs by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel>>(emptyList()) }
    var payments by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            listOf(
                AppStorage.loadWorkers(context),
                AppStorage.loadWorks(context),
                AppStorage.loadUserJobs(context),
                AppStorage.loadPayments(context)
            )
        }
        @Suppress("UNCHECKED_CAST")
        workers = loaded[0] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel>
        @Suppress("UNCHECKED_CAST")
        works = loaded[1] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>
        @Suppress("UNCHECKED_CAST")
        userJobs = loaded[2] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel>
        @Suppress("UNCHECKED_CAST")
        payments = loaded[3] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>
        isLoading = false
    }

    val worker = workers.find { it.email.equals(workerEmail, ignoreCase = true) }
    val workerName = worker?.name.orEmpty()

    // Filter works completed by this worker
    val completedWorks = works.filter { 
        it.status == WorkStatus.Completed && it.workerEmail.equals(workerEmail, ignoreCase = true)
    }

    // Link works with payments and user job details (for location)
    val earnings = completedWorks.mapNotNull { work ->
        val payment = payments.find { it.workId == work.id && it.status == PaymentStatus.Paid }
        val userJob = userJobs.find { it.id == work.id }
        if (payment != null) {
            EarningItemData(
                workName = work.workName,
                location = userJob?.location ?: "Unknown",
                amount = payment.amountNpr,
                method = payment.method?.name ?: "Unknown",
                timestamp = payment.timestampMillis
            )
        } else null
    }.sortedByDescending { it.timestamp }

    val totalEarned = earnings.sumOf { it.amount }
    var selectedTab by remember { mutableStateOf(WorkerBottomTab.Earnings) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val visibleEarnings = earnings.filter { item ->
        val q = searchQuery.trim()
        q.isBlank() ||
            item.workName.contains(q, ignoreCase = true) ||
            item.location.contains(q, ignoreCase = true) ||
            item.method.contains(q, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "My Earnings",
                actions = {
                    NotificationBell(
                        count = notificationCount,
                        onClick = {
                            context.startActivity(Intent(context, WorkerNotificationsActivity::class.java))
                            activity?.finish()
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = selectedTab == WorkerBottomTab.Home,
                    onClick = {
                        selectedTab = WorkerBottomTab.Home
                        context.startActivity(
                            Intent(context, WorkerDashboardActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(text = "Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == WorkerBottomTab.Earnings,
                    onClick = { selectedTab = WorkerBottomTab.Earnings },
                    icon = { Icon(imageVector = Icons.Default.Payments, contentDescription = "Earnings") },
                    label = { Text(text = "Earnings") }
                )
                NavigationBarItem(
                    selected = selectedTab == WorkerBottomTab.Availability,
                    onClick = {
                        selectedTab = WorkerBottomTab.Availability
                        context.startActivity(Intent(context, WorkerAvailabilityActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.ToggleOn, contentDescription = "Availability") },
                    label = { Text(text = "Availability") }
                )
                NavigationBarItem(
                    selected = selectedTab == WorkerBottomTab.Menu,
                    onClick = {
                        selectedTab = WorkerBottomTab.Menu
                        context.startActivity(Intent(context, WorkerMenuActivity::class.java))
                        activity?.finish()
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Earnings",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rs. $totalEarned",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Jobs Paid: ${earnings.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Earning History",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = { Text(text = "Search earnings...") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (visibleEarnings.isEmpty()) {
                Text(
                    text = "No earnings recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                visibleEarnings.forEach { item ->
                    EarningCard(item)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

data class EarningItemData(
    val workName: String,
    val location: String,
    val amount: Int,
    val method: String,
    val timestamp: Long
)

@Composable
private fun EarningCard(item: EarningItemData) {
    val date = Date(item.timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(date)

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.workName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Location: ${item.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Via: ${item.method}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "+ Rs. ${item.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2E7D32)
            )
        }
    }
}
