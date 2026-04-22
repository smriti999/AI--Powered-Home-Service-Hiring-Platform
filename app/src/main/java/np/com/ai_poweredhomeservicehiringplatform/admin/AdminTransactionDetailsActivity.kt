package np.com.ai_poweredhomeservicehiringplatform.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val EXTRA_PAYMENT_ID = "extra_payment_id"

class AdminTransactionDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isAdminLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val paymentId = intent.getIntExtra(EXTRA_PAYMENT_ID, 0)
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                AdminTransactionDetailsScreen(paymentId = paymentId, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTransactionDetailsScreen(paymentId: Int, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val payment = remember(paymentId) { AppStorage.loadPayments(context).firstOrNull { it.id == paymentId } }
    val work = remember(paymentId) {
        val p = payment ?: return@remember null
        AppStorage.loadWorks(context).firstOrNull { it.id == p.workId }
    }
    val workers = remember { AppStorage.loadWorkers(context) }

    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Transaction Details",
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
            if (payment == null) {
                Text(text = "Transaction not found", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            val statusText = if (payment.status == PaymentStatus.Paid) "Paid" else "Pending"
            val methodText = payment.method?.name ?: "-"

            Text(text = "Payment #${payment.id}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            InfoRow(label = "Status", value = statusText)
            InfoRow(label = "Amount", value = "Rs. ${payment.amountNpr}")
            InfoRow(label = "Method", value = methodText)
            InfoRow(label = "User", value = payment.userEmail)
            InfoRow(label = "Work ID", value = payment.workId.toString())
            InfoRow(label = "Time", value = dateFmt.format(Date(payment.timestampMillis)))

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Work Details", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            if (work == null) {
                Text(text = "Work not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val workerName = work.workerEmail
                    ?.let { email -> workers.firstOrNull { it.email.equals(email, ignoreCase = true) }?.name }
                InfoRow(label = "Service", value = work.workName)
                InfoRow(label = "Worker", value = workerName ?: "-")
                Text(
                    text = work.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
