package np.com.ai_poweredhomeservicehiringplatform.user

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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.FullScreenLoading
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

private enum class UserJobsBottomTab { Home, NeedWorker, Works }

class UserJobsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isUserLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                val email = AppStorage.currentUserEmail(this) ?: ""
                UserJobsScreen(
                    userEmail = email,
                    onOpenDetails = { workId ->
                        val intent = Intent(this, UserJobDetailsActivity::class.java)
                        intent.putExtra(EXTRA_JOB_DETAIL_WORK_ID, workId)
                        startActivity(intent)
                        finish()
                    },
                    onPayClick = { workId, amount ->
                        val intent = Intent(this, UserPaymentActivity::class.java)
                        intent.putExtra(EXTRA_WORK_ID, workId)
                        intent.putExtra(EXTRA_AMOUNT_NPR, amount)
                        startActivity(intent)
                        finish()
                    },
                    onRateClick = { workId, workerName, profession ->
                        val intent = Intent(this, UserRateServiceActivity::class.java)
                        intent.putExtra(EXTRA_RATE_WORK_ID, workId)
                        intent.putExtra(EXTRA_RATE_WORKER_EMAIL, workerName)
                        intent.putExtra(EXTRA_RATE_PROFESSION, profession)
                        startActivity(intent)
                        finish()
                    },
                )
            }
        }
    }
}

private fun extractUserEmail(detail: String): String? {
    val userLine = detail.lineSequence()
        .firstOrNull { it.trim().startsWith("User:", ignoreCase = true) }
        ?: return null
    return userLine.substringAfter(":").trim().takeIf { it.isNotBlank() }
}

private fun extractTime(detail: String): String? {
    val timeLine = detail.lineSequence()
        .firstOrNull { it.trim().startsWith("Time:", ignoreCase = true) }
        ?: return null
    return timeLine.substringAfter(":").trim().takeIf { it.isNotBlank() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserJobsScreen(
    userEmail: String,
    onOpenDetails: (workId: Int) -> Unit,
    onPayClick: (workId: Int, amountNpr: Int) -> Unit,
    onRateClick: (workId: Int, workerEmail: String, profession: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val selectedBottomTab = UserJobsBottomTab.Works
    val scope = rememberCoroutineScope()
    var isLoading by rememberSaveable { mutableStateOf(true) }

    var selectedWorkTab by remember { mutableIntStateOf(0) }
    var works by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>>(emptyList()) }
    var payments by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>>(emptyList()) }
    var ratings by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.RatingUiModel>>(emptyList()) }
    var workers by remember { mutableStateOf<List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel>>(emptyList()) }
    var userJobs by remember { mutableStateOf<List<UserJobUiModel>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = withContext(Dispatchers.IO) {
            listOf(
                AppStorage.loadWorks(context),
                AppStorage.loadPayments(context),
                AppStorage.loadRatings(context),
                AppStorage.loadWorkers(context),
                AppStorage.loadUserJobs(context)
            )
        }
        @Suppress("UNCHECKED_CAST")
        works = loaded[0] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>
        @Suppress("UNCHECKED_CAST")
        payments = loaded[1] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>
        @Suppress("UNCHECKED_CAST")
        ratings = loaded[2] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.RatingUiModel>
        @Suppress("UNCHECKED_CAST")
        workers = loaded[3] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel>
        @Suppress("UNCHECKED_CAST")
        userJobs = loaded[4] as List<UserJobUiModel>
        isLoading = false
    }

    fun statusColor(status: WorkStatus): Color {
        return when (status) {
            WorkStatus.Pending -> Color(0xFFF9A825)
            WorkStatus.Booked -> Color(0xFF1565C0)
            WorkStatus.Completed -> Color(0xFF2E7D32)
        }
    }

    fun statusLabel(status: WorkStatus): String {
        return when (status) {
            WorkStatus.Pending -> "Pending"
            WorkStatus.Booked -> "Confirmed"
            WorkStatus.Completed -> "Completed"
        }
    }

    val myWorks = works.filter { w -> extractUserEmail(w.detail)?.equals(userEmail, ignoreCase = true) == true }
    val notificationCount = rememberUnreadNotificationCount(userEmail)
    val visibleWorks = if (selectedWorkTab == 0) {
        myWorks.filter { it.status != WorkStatus.Completed }
    } else {
        myWorks.filter { it.status == WorkStatus.Completed }
    }.sortedByDescending { it.id }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "My Bookings",
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                val loaded = withContext(Dispatchers.IO) {
                                    listOf(
                                        AppStorage.loadWorks(context),
                                        AppStorage.loadPayments(context),
                                        AppStorage.loadRatings(context),
                                        AppStorage.loadWorkers(context),
                                        AppStorage.loadUserJobs(context)
                                    )
                                }
                                @Suppress("UNCHECKED_CAST")
                                works = loaded[0] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>
                                @Suppress("UNCHECKED_CAST")
                                payments = loaded[1] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>
                                @Suppress("UNCHECKED_CAST")
                                ratings = loaded[2] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.RatingUiModel>
                                @Suppress("UNCHECKED_CAST")
                                workers = loaded[3] as List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel>
                                @Suppress("UNCHECKED_CAST")
                                userJobs = loaded[4] as List<UserJobUiModel>
                                isLoading = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                    NotificationBell(
                        count = notificationCount,
                        onClick = {
                            context.startActivity(Intent(context, UserNotificationsActivity::class.java))
                            activity?.finish()
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = selectedBottomTab == UserJobsBottomTab.Home,
                    onClick = {
                        context.startActivity(
                            Intent(context, UserHomeActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(text = "Home") }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == UserJobsBottomTab.NeedWorker,
                    onClick = {
                        context.startActivity(Intent(context, UserCreateJobActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Need Worker") },
                    label = { Text(text = "Need Worker") }
                )
                NavigationBarItem(
                    selected = selectedBottomTab == UserJobsBottomTab.Works,
                    onClick = { },
                    icon = { Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Works") },
                    label = { Text(text = "Works") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, UserMenuActivity::class.java))
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeSelected = selectedWorkTab == 0
                val completedSelected = selectedWorkTab == 1

                if (activeSelected) {
                    Button(onClick = { selectedWorkTab = 0 }, modifier = Modifier.weight(1f)) { Text(text = "Active") }
                } else {
                    OutlinedButton(onClick = { selectedWorkTab = 0 }, modifier = Modifier.weight(1f)) { Text(text = "Active") }
                }

                if (completedSelected) {
                    Button(onClick = { selectedWorkTab = 1 }, modifier = Modifier.weight(1f)) { Text(text = "Completed") }
                } else {
                    OutlinedButton(onClick = { selectedWorkTab = 1 }, modifier = Modifier.weight(1f)) { Text(text = "Completed") }
                }
            }

            if (visibleWorks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = if (selectedWorkTab == 0) "No active bookings" else "No completed bookings")
                }
            } else {
                visibleWorks.forEach { work ->
                    val timeText = extractTime(work.detail) ?: "-"
                    val title = "${work.workName.substringBefore(" Services")} - $timeText"
                    val provider = work.workerEmail
                        ?.let { email -> workers.firstOrNull { it.email.equals(email, ignoreCase = true) }?.name }
                        ?: "Not assigned"
                    val status = statusLabel(work.status)
                    val payment = payments.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                    val isPaid = payment?.status == PaymentStatus.Paid
                    val amount = payment?.amountNpr ?: 0

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onOpenDetails(work.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = status,
                                    color = statusColor(work.status),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.padding(top = 6.dp))

                            Text(
                                text = "Provider: $provider  Status: $status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (selectedWorkTab == 0) {
                                Spacer(modifier = Modifier.padding(top = 10.dp))

                                Button(
                                    onClick = {
                                        val updatedWorks = works.filterNot { it.id == work.id }
                                        works = updatedWorks
                                        AppStorage.saveWorks(context, updatedWorks)

                                        val innerDesc = work.detail.substringAfter("\n\n", work.detail)
                                        val updatedJobs = userJobs.filterNot { job: UserJobUiModel ->
                                            job.userEmail.equals(userEmail, ignoreCase = true) &&
                                                job.service.equals(work.workName, ignoreCase = true) &&
                                                job.description.contains(innerDesc, ignoreCase = true)
                                        }
                                        userJobs = updatedJobs
                                        AppStorage.saveUserJobs(context, updatedJobs)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD32F2F),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(text = "Delete")
                                }
                            }

                            if (selectedWorkTab == 1) {
                                Spacer(modifier = Modifier.padding(top = 10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (isPaid) {
                                        Text(
                                            text = "Payment: Paid",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else if (amount > 0) {
                                        Button(
                                            onClick = { onPayClick(work.id, amount) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF1565C0),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .weight(1f)
                                        ) {
                                            Text(text = "Pay Now", style = MaterialTheme.typography.bodySmall)
                                        }
                                    } else {
                                        Text(
                                            text = "Payment: Amount not decided",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    val rating = ratings.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                                     OutlinedButton(
                                         onClick = { onRateClick(work.id, work.workerEmail.orEmpty(), work.workName.substringBefore(" Services")) },
                                         modifier = Modifier
                                             .height(36.dp)
                                             .weight(1f)
                                     ) {
                                        Text(
                                            text = if (rating != null) "Rated ${rating.stars}★" else "Rate Service",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
}
}
