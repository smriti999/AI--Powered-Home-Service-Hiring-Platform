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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.AppDrawer
import np.com.ai_poweredhomeservicehiringplatform.ui.components.BurgerMenuIcon
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NavigationItem
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminMlModelReportActivity : ComponentActivity() {
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
                AdminMlModelReportScreen(
                    onBackToDashboard = {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminMlModelReportScreen(
    onBackToDashboard: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var report by remember { mutableStateOf(AppStorage.getPriceModelReport(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navItems = listOf(
        NavigationItem("Dashboard", Icons.Default.Dashboard, onBackToDashboard),
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
        NavigationItem("Revenue", Icons.Default.MonetizationOn, {
            context.startActivity(Intent(context, AdminRevenueActivity::class.java))
        }),
        NavigationItem("ML Model", Icons.Default.Psychology, { }),
        NavigationItem("Broadcast", Icons.Default.Campaign, {
            context.startActivity(Intent(context, AdminBroadcastActivity::class.java))
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
                    title = "ML Model Report",
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
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Price Model (Regression)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (report == null) {
                            Text(
                                text = "Model not trained yet. Tap Train to generate weights and metrics from your dataset data.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                            Text(
                                text = "Samples: ${report?.sampleCount}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Trained at: ${sdf.format(Date(report?.trainedAtMillis ?: 0L))}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "RMSE: ${"%.2f".format(report?.rmse ?: 0.0)} | MAE: ${"%.2f".format(report?.mae ?: 0.0)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (!message.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = message.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val newReport = AppStorage.trainPriceModel(context)
                                report = newReport
                                message = if (newReport == null) {
                                    "Training failed: not enough paid data."
                                } else {
                                    "Training completed."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Train / Retrain")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Reset & Re-import Dataset")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Weights (Learned Parameters)",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (report == null) {
                            Text(
                                text = "No weights yet. Train the model first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val r = report!!
                            val weights = r.weights
                            Text(
                                text = "Intercept: ${"%.4f".format(weights.firstOrNull() ?: 0.0)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            r.featureNames.forEachIndexed { i, name ->
                                val w = weights.getOrNull(i + 1) ?: 0.0
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = name, style = MaterialTheme.typography.bodySmall)
                                    Text(text = "%.4f".format(w), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "How to Explain", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "This model learns how price changes based on experience, rating, reviews, demand, time slot, and location, using your dataset-generated history stored in Room.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(text = "Reset Dataset?") },
            text = { Text(text = "This will delete workers, works, ratings, payments, and the trained model, then re-import from dataset.csv.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        AppStorage.resetAndReimportDatasetFromAssets(context)
                        report = AppStorage.getPriceModelReport(context)
                        message = "Dataset re-imported. Train the model again."
                    }
                ) {
                    Text(text = "Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}
