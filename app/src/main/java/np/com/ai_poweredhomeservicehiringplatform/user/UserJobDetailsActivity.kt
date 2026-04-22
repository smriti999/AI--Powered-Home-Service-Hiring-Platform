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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

const val EXTRA_JOB_DETAIL_WORK_ID = "extra_job_detail_work_id"

class UserJobDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isUserLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val workId = intent.getIntExtra(EXTRA_JOB_DETAIL_WORK_ID, 0)
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                val email = AppStorage.currentUserEmail(this) ?: ""
                UserJobDetailsScreen(
                    userEmail = email,
                    workId = workId,
                    onBack = { finish() },
                    onPay = { id, amount ->
                        val i = Intent(this, UserPaymentActivity::class.java)
                        i.putExtra(EXTRA_WORK_ID, id)
                        i.putExtra(EXTRA_AMOUNT_NPR, amount)
                        startActivity(i)
                    },
                    onRate = { id, workerEmail, profession ->
                        val i = Intent(this, UserRateServiceActivity::class.java)
                        i.putExtra(EXTRA_RATE_WORK_ID, id)
                        i.putExtra(EXTRA_RATE_WORKER_EMAIL, workerEmail)
                        i.putExtra(EXTRA_RATE_PROFESSION, profession)
                        startActivity(i)
                    },
                    onOpenWorker = { workerEmail ->
                        val i = Intent(this, UserWorkerDetailsActivity::class.java)
                        i.putExtra(EXTRA_WORKER_EMAIL, workerEmail)
                        startActivity(i)
                    }
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

private fun extractLocation(detail: String): String? {
    val locationLine = detail.lineSequence()
        .firstOrNull { it.trim().startsWith("Location:", ignoreCase = true) }
        ?: return null
    return locationLine.substringAfter(":").trim().takeIf { it.isNotBlank() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserJobDetailsScreen(
    userEmail: String,
    workId: Int,
    onBack: () -> Unit,
    onPay: (workId: Int, amountNpr: Int) -> Unit,
    onRate: (workId: Int, workerEmail: String, profession: String) -> Unit,
    onOpenWorker: (workerEmail: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var works by remember { mutableStateOf(AppStorage.loadWorks(context)) }
    var payments by remember { mutableStateOf(AppStorage.loadPayments(context)) }
    val ratings = remember { AppStorage.loadRatings(context) }
    val workers = remember { AppStorage.loadWorkers(context) }

    val work = works.firstOrNull { it.id == workId }
    val isMyWork = work != null && extractUserEmail(work.detail)?.equals(userEmail, ignoreCase = true) == true

    val providerEmail = work?.workerEmail
    val providerName = providerEmail?.let { email ->
        workers.firstOrNull { it.email.equals(email, ignoreCase = true) }?.name
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
            LogoTopAppBar(
                title = "Booking Details",
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
            if (work == null || !isMyWork) {
                Text(text = "Booking not found", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            val timeText = extractTime(work.detail) ?: "-"
            val locationText = extractLocation(work.detail) ?: "-"
            val statusText = work.status.name

            Text(text = work.workName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Status: $statusText", color = statusColor(work.status), fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(14.dp))

            InfoRow(label = "Time", value = timeText)
            InfoRow(label = "Location", value = locationText)
            InfoRow(label = "Provider", value = providerName ?: "Not assigned")

            if (providerEmail != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onOpenWorker(providerEmail) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(text = "View Worker Profile")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Details", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = work.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(18.dp))

            if (work.status == WorkStatus.Completed) {
                val payment = payments.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                val isPaid = payment?.status == PaymentStatus.Paid
                val amount = payment?.amountNpr ?: 0

                Spacer(modifier = Modifier.height(10.dp))
                InfoRow(
                    label = "Final Amount",
                    value = if (amount > 0) "Rs. $amount" else "To be decided"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isPaid) {
                        Text(
                            text = "Payment: Paid",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Button(
                            onClick = { onPay(work.id, payment?.amountNpr ?: 0) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1565C0),
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "Pay Now")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onRate(work.id, work.workerEmail.orEmpty(), work.workName.substringBefore(" Services"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        val rating = ratings.firstOrNull { it.workId == work.id && it.userEmail.equals(userEmail, ignoreCase = true) }
                        Text(text = if (rating != null) "Rated ${rating.stars}★" else "Rate Service")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
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
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Delete Booking")
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
