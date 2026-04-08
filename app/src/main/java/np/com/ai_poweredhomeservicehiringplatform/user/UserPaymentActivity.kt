package np.com.ai_poweredhomeservicehiringplatform.user

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
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
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentMethod
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

const val EXTRA_WORK_ID = "extra_work_id"
const val EXTRA_AMOUNT_NPR = "extra_amount_npr"

class UserPaymentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isUserLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val workId = intent.getIntExtra(EXTRA_WORK_ID, 0)
        val amount = intent.getIntExtra(EXTRA_AMOUNT_NPR, 0)

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                PaymentScreen(
                    workId = workId,
                    amountNpr = amount,
                    onBack = { finish() },
                    onPaid = {
                        startActivity(Intent(this, UserJobsActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentScreen(
    workId: Int,
    amountNpr: Int,
    onBack: () -> Unit,
    onPaid: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userEmail = AppStorage.currentUserEmail(context).orEmpty()

    var selectedMethod by rememberSaveable { mutableStateOf(PaymentMethod.Esewa) }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Payment") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onBack) {
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Text(
                    text = "Total Amount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "To be decided",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Select Payment Method",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                PaymentOption(
                    label = "eSewa Wallet",
                    selected = selectedMethod == PaymentMethod.Esewa,
                    onClick = { selectedMethod = PaymentMethod.Esewa }
                )
                Spacer(modifier = Modifier.height(10.dp))
                PaymentOption(
                    label = "Khalti Wallet",
                    selected = selectedMethod == PaymentMethod.Khalti,
                    onClick = { selectedMethod = PaymentMethod.Khalti }
                )
                Spacer(modifier = Modifier.height(10.dp))
                PaymentOption(
                    label = "Cash on Delivery",
                    selected = selectedMethod == PaymentMethod.CashOnDelivery,
                    onClick = { selectedMethod = PaymentMethod.CashOnDelivery }
                )
            }

            Button(
                onClick = {
                    val payments = AppStorage.loadPayments(context)
                    val existing = payments.firstOrNull { it.workId == workId && it.userEmail.equals(userEmail, ignoreCase = true) }
                    val updated = if (existing != null) {
                        payments.map {
                            if (it.id == existing.id) {
                                it.copy(
                                    status = PaymentStatus.Paid,
                                    method = selectedMethod,
                                    timestampMillis = System.currentTimeMillis()
                                )
                            } else it
                        }
                    } else {
                        val nextId = (payments.maxOfOrNull { it.id } ?: 0) + 1
                        payments + PaymentUiModel(
                            id = nextId,
                            workId = workId,
                            userEmail = userEmail,
                            amountNpr = amountNpr,
                            method = selectedMethod,
                            status = PaymentStatus.Paid,
                            timestampMillis = System.currentTimeMillis()
                        )
                    }
                    AppStorage.savePayments(context, updated)

                    val notifications = AppStorage.loadNotifications(context)
                    val nextNId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                    val updatedNotifications = notifications + NotificationUiModel(
                        id = nextNId,
                        userEmail = userEmail,
                        title = "Payment Successful",
                        message = "Payment completed via ${when (selectedMethod) {
                            PaymentMethod.Esewa -> "eSewa"
                            PaymentMethod.Khalti -> "Khalti"
                            PaymentMethod.CashOnDelivery -> "Cash on Delivery"
                        }}.",
                        timestampMillis = System.currentTimeMillis()
                    )
                    AppStorage.saveNotifications(context, updatedNotifications)

                    showSuccessDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .height(48.dp)
            ) {
                Text(text = "PAY NOW")
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text(text = "Success") },
            text = { Text(text = "Payment completed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessDialog = false
                        onPaid()
                    }
                ) {
                    Text(text = "OK")
                }
            }
        )
    }
}

@Composable
private fun PaymentOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color(0xFF1565C0) else MaterialTheme.colorScheme.outline
    val contentColor = if (selected) Color(0xFF1565C0) else MaterialTheme.colorScheme.onSurface

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
        ) {
            Text(text = label)
        }
    }
}
