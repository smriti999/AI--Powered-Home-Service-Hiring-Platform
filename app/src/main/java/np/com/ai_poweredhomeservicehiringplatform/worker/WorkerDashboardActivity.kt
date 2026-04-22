package np.com.ai_poweredhomeservicehiringplatform.worker

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
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
import kotlinx.coroutines.delay
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.NotificationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.AppDrawer
import np.com.ai_poweredhomeservicehiringplatform.ui.components.BurgerMenuIcon
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NavigationItem
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class WorkerDashboardActivity : ComponentActivity() {
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
                WorkerDashboardScreen(
                    onProfileClick = { startActivity(Intent(this, WorkerProfileActivity::class.java)) },
                    onEarningsClick = { startActivity(Intent(this, WorkerEarningsActivity::class.java)) },
                    onNotificationsClick = { startActivity(Intent(this, WorkerNotificationsActivity::class.java)) },
                    onAvailabilityClick = { startActivity(Intent(this, WorkerAvailabilityActivity::class.java)) },
                    onPayoutClick = { startActivity(Intent(this, WorkerPayoutSettingsActivity::class.java)) },
                    onOpenWork = { workId ->
                        val intent = Intent(this, WorkerJobDetailsActivity::class.java)
                        intent.putExtra(EXTRA_WORK_DETAIL_ID, workId)
                        startActivity(intent)
                    },
                    onLogout = {
                        AppStorage.setWorkerLoggedIn(this, false, null)
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

private fun matchesProfession(workName: String, profession: String): Boolean {
    val name = workName.lowercase()
    val prof = profession.lowercase()

    val keywords = when {
        prof.contains("plumbing") -> listOf("plumb")
        prof.contains("clean") -> listOf("clean")
        prof.contains("electric") -> listOf("electric")
        prof.contains("carp") -> listOf("carp", "wood")
        prof.contains("ac") || prof.contains("appliance") -> listOf("ac", "appliance", "repair")
        prof.contains("paint") -> listOf("paint")
        prof.contains("pest") -> listOf("pest")
        prof.contains("handyman") -> listOf("handyman")
        prof.contains("relocation") || prof.contains("moving") -> listOf("relocation", "moving", "shift")
        prof.contains("maid") || prof.contains("cook") -> listOf("maid", "cook", "cooking")
        else -> prof.split(" ").filter { it.isNotBlank() }
    }

    return keywords.any { keyword -> keyword.isNotBlank() && name.contains(keyword) }
}

private fun extractUserEmail(detail: String): String? {
    val userLine = detail.lineSequence()
        .firstOrNull { it.trim().startsWith("User:", ignoreCase = true) }
        ?: return null
    return userLine.substringAfter(":").trim().takeIf { it.isNotBlank() }
}

private fun formatDuration(durationMillis: Long): String {
    val totalSeconds = (durationMillis.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerDashboardScreen(
    onProfileClick: () -> Unit,
    onEarningsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAvailabilityClick: () -> Unit,
    onPayoutClick: () -> Unit,
    onOpenWork: (workId: Int) -> Unit,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val workerEmail = AppStorage.currentWorkerEmail(context).orEmpty()
    val workers = remember { AppStorage.loadWorkers(context) }
    val currentWorker = workers.firstOrNull { it.email.equals(workerEmail, ignoreCase = true) }
    val profession = currentWorker?.profession.orEmpty()
    val workerName = currentWorker?.name ?: "Worker"
    val notificationCount = rememberUnreadNotificationCount(workerEmail)

    var works by remember { mutableStateOf(AppStorage.loadWorks(context)) }
    var tickerNow by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            tickerNow = System.currentTimeMillis()
            delay(1000)
        }
    }

    val availableWorks = works.filter { work ->
        work.status == WorkStatus.Pending &&
            matchesProfession(work.workName, profession) &&
            (searchQuery.trim().isBlank() ||
                work.workName.contains(searchQuery.trim(), ignoreCase = true) ||
                work.detail.contains(searchQuery.trim(), ignoreCase = true))
    }

    val activeWorks = works.filter { work ->
        work.status == WorkStatus.Booked &&
            (work.workerEmail ?: "").equals(workerEmail, ignoreCase = true) &&
            (searchQuery.trim().isBlank() ||
                work.workName.contains(searchQuery.trim(), ignoreCase = true) ||
                work.detail.contains(searchQuery.trim(), ignoreCase = true))
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navItems = listOf(
        NavigationItem("Dashboard", Icons.Default.Dashboard, { }),
        NavigationItem("Profile", Icons.Default.AccountCircle, onProfileClick),
        NavigationItem("Earnings", Icons.Default.Payments, onEarningsClick),
        NavigationItem("Notifications", Icons.Default.Notifications, onNotificationsClick),
        NavigationItem("Availability", Icons.Default.ToggleOn, onAvailabilityClick),
        NavigationItem("Payout", Icons.Default.AccountBalanceWallet, onPayoutClick),
        NavigationItem("Logout", Icons.AutoMirrored.Filled.ExitToApp, onLogout)
    )

    fun statusColor(status: WorkStatus): Color {
        return when (status) {
            WorkStatus.Pending -> Color(0xFFF9A825)
            WorkStatus.Booked -> Color(0xFF1565C0)
            WorkStatus.Completed -> Color(0xFF2E7D32)
        }
    }

    AppDrawer(drawerState = drawerState, items = navItems) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LogoTopAppBar(
                    title = "Worker Dashboard",
                    navigationIcon = {
                        BurgerMenuIcon(drawerState = drawerState)
                    },
                    actions = {
                        NotificationBell(count = notificationCount, onClick = onNotificationsClick)
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
                Text(
                    text = profession.ifBlank { "Profession: -" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.padding(top = 10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(text = "Search jobs...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.padding(top = 12.dp))

                if (profession.isBlank()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No profession found for this worker")
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Active", fontWeight = FontWeight.SemiBold)

                        if (activeWorks.isEmpty()) {
                            Text(
                                text = "No active jobs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            activeWorks.forEach { work: WorkUiModel ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenWork(work.id) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = work.workName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Confirmed",
                                                color = statusColor(work.status),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.padding(top = 6.dp))

                                        Text(
                                            text = work.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val startedAt = AppStorage.getWorkTimerStartMillis(context, work.id)
                                        if (startedAt != null) {
                                            Spacer(modifier = Modifier.padding(top = 8.dp))
                                            Text(
                                                text = "Elapsed: ${formatDuration(tickerNow - startedAt)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.padding(top = 10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    val userEmail = extractUserEmail(work.detail)
                                                    if (userEmail != null) {
                                                        if (AppStorage.getWorkTimerStartMillis(context, work.id) == null) {
                                                            AppStorage.startWorkTimer(context, work.id)
                                                        }
                                                        val notifications = AppStorage.loadNotifications(context)
                                                        var nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                                        
                                                        // Notify User
                                                        val n1 = NotificationUiModel(
                                                            id = nextId++,
                                                            userEmail = userEmail,
                                                            title = "Worker Arrived",
                                                            message = "$workerName has arrived and started the work.",
                                                            timestampMillis = System.currentTimeMillis()
                                                        )
                                                        
                                                        // Notify Worker
                                                        val n2 = NotificationUiModel(
                                                            id = nextId,
                                                            userEmail = workerEmail,
                                                            title = "Arrival Notified",
                                                            message = "You notified the user that you arrived for: ${work.workName}",
                                                            timestampMillis = System.currentTimeMillis()
                                                        )
                                                        
                                                        AppStorage.saveNotifications(context, notifications + n1 + n2)
                                                    }

                                                    val intent = Intent(context, WorkerJobDetailsActivity::class.java)
                                                    intent.putExtra(EXTRA_WORK_DETAIL_ID, work.id)
                                                    intent.putExtra(EXTRA_OPEN_TIMER, true)
                                                    context.startActivity(intent)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2E7D32),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(text = "Arrived")
                                            }

                                            Button(
                                                onClick = {
                                                    val updatedWorks = works.map { existing ->
                                                        if (existing.id == work.id) {
                                                            existing.copy(status = WorkStatus.Completed)
                                                        } else {
                                                            existing
                                                        }
                                                    }
                                                    works = updatedWorks
                                                    AppStorage.saveWorks(context, updatedWorks)

                                                    val userEmail = extractUserEmail(work.detail)
                                                    if (userEmail != null) {
                                                        val notifications = AppStorage.loadNotifications(context)
                                                        var nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                                        
                                                        // Notify User
                                                        val n1 = NotificationUiModel(
                                                            id = nextId++,
                                                            userEmail = userEmail,
                                                            title = "Service Completed",
                                                            message = "$workerName completed the service.",
                                                            timestampMillis = System.currentTimeMillis()
                                                        )
                                                        
                                                        // Notify Worker
                                                        val n2 = NotificationUiModel(
                                                            id = nextId,
                                                            userEmail = workerEmail,
                                                            title = "Job Completed",
                                                            message = "You completed the work: ${work.workName}",
                                                            timestampMillis = System.currentTimeMillis()
                                                        )
                                                        
                                                        AppStorage.saveNotifications(context, notifications + n1 + n2)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1565C0),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(text = "Complete")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.padding(top = 10.dp))

                        Text(text = "Available", fontWeight = FontWeight.SemiBold)

                        if (availableWorks.isEmpty()) {
                            Text(
                                text = "No jobs available for $profession",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            availableWorks.forEach { work: WorkUiModel ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onOpenWork(work.id) }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = work.workName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = work.status.name,
                                                color = statusColor(work.status),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.padding(top = 6.dp))

                                        Text(
                                            text = work.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.padding(top = 10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    val updatedWorks = works.map { existing ->
                                                        if (existing.id == work.id) {
                                                            existing.copy(status = WorkStatus.Booked, workerEmail = workerEmail)
                                                        } else {
                                                            existing
                                                        }
                                                    }
                                                    works = updatedWorks
                                                    AppStorage.saveWorks(context, updatedWorks)

                                                    val userEmail = extractUserEmail(work.detail)
                                                    if (userEmail != null) {
                                                        val payments = AppStorage.loadPayments(context)
                                                        val existingPayment = payments.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                                                        if (existingPayment == null) {
                                                            val nextPaymentId = (payments.maxOfOrNull { it.id } ?: 0) + 1
                                                            val updatedPayments = payments + PaymentUiModel(
                                                                id = nextPaymentId,
                                                                workId = work.id,
                                                                userEmail = userEmail,
                                                                amountNpr = 0,
                                                                method = null,
                                                                status = PaymentStatus.Pending,
                                                                timestampMillis = System.currentTimeMillis()
                                                            )
                                                            AppStorage.savePayments(context, updatedPayments)
                                                        }

                                                        val notifications = AppStorage.loadNotifications(context)
                                                        var nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                                        
                                                        // Notify User
                                                        val n1 = NotificationUiModel(
                                                            id = nextId++,
                                                            userEmail = userEmail,
                                                            title = "Booking Confirmed",
                                                            message = "$workerName accepted your request.",
                                                            timestampMillis = System.currentTimeMillis()
                                                        )
                                                        
                                                        // Notify Worker
                                                        val n2 = NotificationUiModel(
                                                            id = nextId,
                                                            userEmail = workerEmail,
                                                            title = "Job Accepted",
                                                            message = "You accepted the job: ${work.workName}",
                                                            timestampMillis = System.currentTimeMillis()
                                                        )
                                                        
                                                        AppStorage.saveNotifications(context, notifications + n1 + n2)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF1565C0),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text(text = "Accept")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
