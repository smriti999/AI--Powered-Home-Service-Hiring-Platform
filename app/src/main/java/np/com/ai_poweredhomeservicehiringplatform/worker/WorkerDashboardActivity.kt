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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import np.com.ai_poweredhomeservicehiringplatform.common.model.NotificationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerDashboardScreen(
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val workerEmail = AppStorage.currentWorkerEmail(context).orEmpty()
    val workers = remember { AppStorage.loadWorkers(context) }
    val currentWorker = workers.firstOrNull { it.email.equals(workerEmail, ignoreCase = true) }
    val profession = currentWorker?.profession.orEmpty()
    val workerName = currentWorker?.name ?: "Worker"

    var works by remember { mutableStateOf(AppStorage.loadWorks(context)) }

    val availableWorks = works.filter { work ->
        work.status == WorkStatus.Pending &&
            matchesProfession(work.workName, profession) &&
            (searchQuery.trim().isBlank() ||
                work.workName.contains(searchQuery.trim(), ignoreCase = true) ||
                work.detail.contains(searchQuery.trim(), ignoreCase = true))
    }

    val activeWorks = works.filter { work ->
        work.status == WorkStatus.Booked &&
            (work.workerName ?: "").equals(workerName, ignoreCase = true) &&
            (searchQuery.trim().isBlank() ||
                work.workName.contains(searchQuery.trim(), ignoreCase = true) ||
                work.detail.contains(searchQuery.trim(), ignoreCase = true))
    }

    fun statusColor(status: WorkStatus): Color {
        return when (status) {
            WorkStatus.Pending -> Color(0xFFF9A825)
            WorkStatus.Booked -> Color(0xFF1565C0)
            WorkStatus.Completed -> Color(0xFF2E7D32)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Worker Dashboard") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onLogout) {
                        Text(text = "Logout", color = MaterialTheme.colorScheme.onPrimary)
                    }
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
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                                                    val notifications = AppStorage.loadNotifications(context)
                                                    val nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                                    val updatedNotifications = notifications + NotificationUiModel(
                                                        id = nextId,
                                                        userEmail = userEmail,
                                                        title = "Worker Arrived",
                                                        message = "$workerName has arrived at your location.",
                                                        timestampMillis = System.currentTimeMillis()
                                                    )
                                                    AppStorage.saveNotifications(context, updatedNotifications)
                                                }
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
                                                    val nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                                    val updatedNotifications = notifications + NotificationUiModel(
                                                        id = nextId,
                                                        userEmail = userEmail,
                                                        title = "Service Completed",
                                                        message = "$workerName completed the service.",
                                                        timestampMillis = System.currentTimeMillis()
                                                    )
                                                    AppStorage.saveNotifications(context, updatedNotifications)
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
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                                                    existing.copy(status = WorkStatus.Booked, workerName = workerName)
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
                                                val nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                                val updatedNotifications = notifications + NotificationUiModel(
                                                    id = nextId,
                                                    userEmail = userEmail,
                                                    title = "Booking Confirmed",
                                                    message = "$workerName accepted your request.",
                                                    timestampMillis = System.currentTimeMillis()
                                                )
                                                AppStorage.saveNotifications(context, updatedNotifications)
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
