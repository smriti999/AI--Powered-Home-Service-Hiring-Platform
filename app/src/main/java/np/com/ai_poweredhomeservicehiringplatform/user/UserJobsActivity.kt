package np.com.ai_poweredhomeservicehiringplatform.user

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

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
                    onPayClick = { workId, amount ->
                        val intent = Intent(this, UserPaymentActivity::class.java)
                        intent.putExtra(EXTRA_WORK_ID, workId)
                        intent.putExtra(EXTRA_AMOUNT_NPR, amount)
                        startActivity(intent)
                    },
                    onRateClick = { workId, workerName, profession ->
                        val intent = Intent(this, UserRateServiceActivity::class.java)
                        intent.putExtra(EXTRA_RATE_WORK_ID, workId)
                        intent.putExtra(EXTRA_RATE_WORKER_NAME, workerName)
                        intent.putExtra(EXTRA_RATE_PROFESSION, profession)
                        startActivity(intent)
                    },
                    onBackClick = { finish() }
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
    onPayClick: (workId: Int, amountNpr: Int) -> Unit,
    onRateClick: (workId: Int, workerName: String, profession: String) -> Unit,
    onBackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var works by remember { mutableStateOf(AppStorage.loadWorks(context)) }
    var payments by remember { mutableStateOf(AppStorage.loadPayments(context)) }
    var ratings by remember { mutableStateOf(AppStorage.loadRatings(context)) }

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
    val visibleWorks = if (selectedTab == 0) {
        myWorks.filter { it.status != WorkStatus.Completed }
    } else {
        myWorks.filter { it.status == WorkStatus.Completed }
    }.sortedByDescending { it.id }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "My Bookings") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(
                        onClick = {
                            works = AppStorage.loadWorks(context)
                            payments = AppStorage.loadPayments(context)
                            ratings = AppStorage.loadRatings(context)
                        }
                    ) {
                        Text(text = "Refresh", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    TextButton(onClick = onBackClick) {
                        Text(text = "Back", color = MaterialTheme.colorScheme.onPrimary)
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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val activeSelected = selectedTab == 0
                val completedSelected = selectedTab == 1

                if (activeSelected) {
                    Button(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) { Text(text = "Active") }
                } else {
                    OutlinedButton(onClick = { selectedTab = 0 }, modifier = Modifier.weight(1f)) { Text(text = "Active") }
                }

                if (completedSelected) {
                    Button(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) { Text(text = "Completed") }
                } else {
                    OutlinedButton(onClick = { selectedTab = 1 }, modifier = Modifier.weight(1f)) { Text(text = "Completed") }
                }
            }

            if (visibleWorks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = if (selectedTab == 0) "No active bookings" else "No completed bookings")
                }
            } else {
                visibleWorks.forEach { work ->
                    val timeText = extractTime(work.detail) ?: "-"
                    val title = "${work.workName.substringBefore(" Services")} - $timeText"
                    val provider = work.workerName ?: "Not assigned"
                    val status = statusLabel(work.status)
                    val payment = payments.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                    val isPaid = payment?.status == PaymentStatus.Paid

                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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

                            if (selectedTab == 0) {
                                Spacer(modifier = Modifier.padding(top = 10.dp))

                                Button(
                                    onClick = {
                                        val updatedWorks = works.filterNot { it.id == work.id }
                                        works = updatedWorks
                                        AppStorage.saveWorks(context, updatedWorks)

                                        val innerDesc = work.detail.substringAfter("\n\n", work.detail)
                                        val updatedJobs = AppStorage.loadUserJobs(context).filterNot { job: UserJobUiModel ->
                                            job.userEmail.equals(userEmail, ignoreCase = true) &&
                                                job.service.equals(work.workName, ignoreCase = true) &&
                                                job.description.contains(innerDesc, ignoreCase = true)
                                        }
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

                            if (selectedTab == 1) {
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
                                    } else {
                                        Button(
                                            onClick = { onPayClick(work.id, payment?.amountNpr ?: 0) },
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
                                    }

                                    val rating = ratings.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                                     OutlinedButton(
                                         onClick = { onRateClick(work.id, work.workerName ?: "", work.workName.substringBefore(" Services")) },
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
