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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

const val EXTRA_WORK_DETAIL_ID = "extra_work_detail_id"

class WorkerJobDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isWorkerLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val workId = intent.getIntExtra(EXTRA_WORK_DETAIL_ID, 0)
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                WorkerJobDetailsScreen(
                    workId = workId,
                    onBack = { finish() }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerJobDetailsScreen(
    workId: Int,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val workerEmail = AppStorage.currentWorkerEmail(context).orEmpty()
    val workers = remember { AppStorage.loadWorkers(context) }
    val currentWorker = workers.firstOrNull { it.email.equals(workerEmail, ignoreCase = true) }
    val workerName = currentWorker?.name ?: "Worker"

    var works by remember { mutableStateOf(AppStorage.loadWorks(context)) }
    val work = works.firstOrNull { it.id == workId }

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
            LogoTopAppBar(
                title = "Job Details",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (work == null) {
                Text(text = "Job not found", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Text(text = work.workName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Status: ${work.status.name}", color = statusColor(work.status), fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(label = "Assigned To", value = work.workerName ?: "Not assigned")
            InfoRow(label = "User", value = extractUserEmail(work.detail) ?: "-")

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Details", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = work.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(18.dp))

            if (work.status == WorkStatus.Pending) {
                Button(
                    onClick = {
                        val updatedWorks = works.map { existing ->
                            if (existing.id == work.id) existing.copy(status = WorkStatus.Booked, workerName = workerName) else existing
                        }
                        works = updatedWorks
                        AppStorage.saveWorks(context, updatedWorks)

                        val userEmail = extractUserEmail(work.detail)
                        if (userEmail != null) {
                            val payments = AppStorage.loadPayments(context)
                            val existingPayment = payments.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                            if (existingPayment == null) {
                                val nextPaymentId = (payments.maxOfOrNull { it.id } ?: 0) + 1
                                AppStorage.savePayments(
                                    context,
                                    payments + PaymentUiModel(
                                        id = nextPaymentId,
                                        workId = work.id,
                                        userEmail = userEmail,
                                        amountNpr = 0,
                                        method = null,
                                        status = PaymentStatus.Pending,
                                        timestampMillis = System.currentTimeMillis()
                                    )
                                )
                            }

                            val notifications = AppStorage.loadNotifications(context)
                            var nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                            val n1 = NotificationUiModel(
                                id = nextId++,
                                userEmail = userEmail,
                                title = "Booking Confirmed",
                                message = "$workerName accepted your request.",
                                timestampMillis = System.currentTimeMillis()
                            )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1565C0),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Accept Job")
                }
            }

            if (work.status == WorkStatus.Booked && work.workerName?.equals(workerName, ignoreCase = true) == true) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val userEmail = extractUserEmail(work.detail)
                            if (userEmail != null) {
                                val notifications = AppStorage.loadNotifications(context)
                                var nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                val n1 = NotificationUiModel(
                                    id = nextId++,
                                    userEmail = userEmail,
                                    title = "Worker Arrived",
                                    message = "$workerName has arrived at your location.",
                                    timestampMillis = System.currentTimeMillis()
                                )
                                val n2 = NotificationUiModel(
                                    id = nextId,
                                    userEmail = workerEmail,
                                    title = "Arrival Notified",
                                    message = "You notified the user that you arrived for: ${work.workName}",
                                    timestampMillis = System.currentTimeMillis()
                                )
                                AppStorage.saveNotifications(context, notifications + n1 + n2)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "Arrived")
                    }

                    Button(
                        onClick = {
                            val updatedWorks = works.map { existing ->
                                if (existing.id == work.id) existing.copy(status = WorkStatus.Completed) else existing
                            }
                            works = updatedWorks
                            AppStorage.saveWorks(context, updatedWorks)

                            val userEmail = extractUserEmail(work.detail)
                            if (userEmail != null) {
                                val notifications = AppStorage.loadNotifications(context)
                                var nextId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                                val n1 = NotificationUiModel(
                                    id = nextId++,
                                    userEmail = userEmail,
                                    title = "Service Completed",
                                    message = "$workerName completed the service.",
                                    timestampMillis = System.currentTimeMillis()
                                )
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
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1565C0),
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "Complete")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(10.dp))
    }
}

