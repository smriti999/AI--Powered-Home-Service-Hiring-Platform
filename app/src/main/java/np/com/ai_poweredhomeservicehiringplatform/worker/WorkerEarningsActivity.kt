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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
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
                WorkerEarningsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerEarningsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val workerEmail = AppStorage.currentWorkerEmail(context).orEmpty()
    val workers = AppStorage.loadWorkers(context)
    val worker = workers.find { it.email.equals(workerEmail, ignoreCase = true) }
    val workerName = worker?.name.orEmpty()

    val works = AppStorage.loadWorks(context)
    val userJobs = AppStorage.loadUserJobs(context)
    val payments = AppStorage.loadPayments(context)

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "My Earnings",
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

            if (earnings.isEmpty()) {
                Text(
                    text = "No earnings recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 20.dp)
                )
            } else {
                earnings.forEach { item ->
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
