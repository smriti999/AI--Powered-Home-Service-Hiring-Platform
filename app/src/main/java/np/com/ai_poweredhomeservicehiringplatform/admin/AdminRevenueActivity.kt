package np.com.ai_poweredhomeservicehiringplatform.admin

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.AppDrawer
import np.com.ai_poweredhomeservicehiringplatform.ui.components.BurgerMenuIcon
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NavigationItem
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminRevenueActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isAdminLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                AdminRevenueScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminRevenueScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val payments = remember { AppStorage.loadPayments(context) }
    val paidPayments = payments.filter { it.status == PaymentStatus.Paid }
    val totalRevenue = paidPayments.sumOf { it.amountNpr }
    var trainMessage by remember { mutableStateOf<String?>(null) }
    val modelReport = remember(trainMessage) { AppStorage.getPriceModelReport(context) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navItems = listOf(
        NavigationItem("Dashboard", Icons.Default.Dashboard, {
            context.startActivity(Intent(context, AdminDashboardActivity::class.java))
        }),
        NavigationItem("Requests", Icons.Default.PendingActions, {
            context.startActivity(Intent(context, AdminRequestsActivity::class.java))
        }),
        NavigationItem("Workers", Icons.Default.Group, {
            context.startActivity(Intent(context, AdminWorkerManagementActivity::class.java))
        }),
        NavigationItem("Users", Icons.Default.Group, {
            context.startActivity(Intent(context, AdminUserManagementActivity::class.java))
        }),
        NavigationItem("Works", Icons.AutoMirrored.Filled.List, {
            context.startActivity(Intent(context, AdminWorkManagementActivity::class.java))
        }),
        NavigationItem("Revenue", Icons.Default.MonetizationOn, { }),
        NavigationItem("Broadcast", Icons.Default.Campaign, {
            context.startActivity(Intent(context, AdminBroadcastActivity::class.java))
        }),
        NavigationItem("ML Model", Icons.Default.Psychology, {
            context.startActivity(Intent(context, AdminMlModelReportActivity::class.java))
        }),
        NavigationItem("Logout", Icons.AutoMirrored.Filled.ExitToApp, {
            AppStorage.setAdminLoggedIn(context, false)
            context.startActivity(Intent(context, LoginActivity::class.java))
            (context as? ComponentActivity)?.finish()
        })
    )

    AppDrawer(drawerState = drawerState, items = navItems) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LogoTopAppBar(
                    title = "Revenue Management",
                    navigationIcon = {
                        BurgerMenuIcon(drawerState = drawerState)
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
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Revenue Collected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rs. $totalRevenue",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Total Transactions: ${paidPayments.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ML Training Proof (Price Model)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (modelReport == null) {
                                "Model not trained yet."
                            } else {
                                "Samples: ${modelReport.sampleCount} | RMSE: ${"%.2f".format(modelReport.rmse)} | MAE: ${"%.2f".format(modelReport.mae)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (modelReport != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap ML Model to see full weights report.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        if (!trainMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = trainMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = {
                                val report = AppStorage.trainPriceModel(context)
                                trainMessage = if (report == null) {
                                    "Training failed: not enough paid data."
                                } else {
                                    "Training completed. Samples=${report.sampleCount}, RMSE=${"%.2f".format(report.rmse)}, MAE=${"%.2f".format(report.mae)}"
                                }
                            }
                        ) {
                            Text(text = "Train / Retrain Model")
                        }
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(context, AdminMlModelReportActivity::class.java))
                            }
                        ) {
                            Text(text = "Open Full Model Report")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (paidPayments.isEmpty()) {
                    Text(
                        text = "No revenue recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                } else {
                    paidPayments.sortedByDescending { it.timestampMillis }.forEach { payment ->
                        TransactionItem(
                            paymentId = payment.id,
                            userEmail = payment.userEmail,
                            amount = payment.amountNpr,
                            method = payment.method?.name ?: "Unknown",
                            timestamp = payment.timestampMillis
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(paymentId: Int, userEmail: String, amount: Int, method: String, timestamp: Long) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(date)

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            val intent = Intent(context, AdminTransactionDetailsActivity::class.java)
            intent.putExtra(EXTRA_PAYMENT_ID, paymentId)
            context.startActivity(intent)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "Via: $method",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "+ Rs. $amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF2E7D32)
            )
        }
    }
}
