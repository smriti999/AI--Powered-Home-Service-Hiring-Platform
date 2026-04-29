package np.com.ai_poweredhomeservicehiringplatform.admin

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
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
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.FullScreenLoading
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.RequestBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberPendingRequestCount
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
                AdminMlModelReportScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminMlModelReportScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    var isLoading by remember { mutableStateOf(true) }
    var report by remember { mutableStateOf(AppStorage.getPriceModelReport(context)) }
    var stats by remember { mutableStateOf(AppStorage.getDatasetStats(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pendingRequestCount = rememberPendingRequestCount()

    LaunchedEffect(Unit) {
        isLoading = true
        val loadedReport = withContext(Dispatchers.IO) { AppStorage.getPriceModelReport(context) }
        val loadedStats = withContext(Dispatchers.IO) { AppStorage.getDatasetStats(context) }
        report = loadedReport
        stats = loadedStats
        isLoading = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "ML Model Report",
                navigationIcon = {
                    IconButton(
                        onClick = {
                            (context as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    RequestBell(
                        count = pendingRequestCount,
                        onClick = {
                            context.startActivity(Intent(context, AdminRequestsActivity::class.java))
                            activity?.finish()
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(
                            Intent(context, AdminDashboardActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(text = "Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AdminWorkerManagementActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Group, contentDescription = "Workers") },
                    label = { Text(text = "Workers") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AdminUserManagementActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Users") },
                    label = { Text(text = "Users") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AdminMenuActivity::class.java))
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

                        Text(
                            text = "DB: workers=${stats.workers}, works=${stats.works}, payments=${stats.payments}, paid=${stats.paidPayments}, ratings=${stats.ratings}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (!stats.lastError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stats.lastError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
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
                                if (isBusy) return@Button
                                isBusy = true
                                message = "Training..."
                                scope.launch {
                                    val result = runCatching {
                                        withContext(Dispatchers.Default) {
                                            AppStorage.trainPriceModel(context)
                                        }
                                    }
                                    val newReport = result.getOrNull()
                                    report = newReport
                                    stats = AppStorage.getDatasetStats(context)
                                    message = if (result.isFailure) {
                                        val e = result.exceptionOrNull()
                                        "Training error: ${e?.javaClass?.simpleName}: ${e?.message}"
                                    } else if (newReport == null) {
                                        "Training failed: not enough paid data."
                                    } else {
                                        "Training completed."
                                    }
                                    isBusy = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBusy
                        ) {
                            Text(text = "Train / Retrain")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isBusy
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

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(text = "Reset Dataset?") },
            text = { Text(text = "This will delete workers, works, ratings, payments, and the trained model, then re-import from dataset.csv.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isBusy) return@TextButton
                        showResetConfirm = false
                        isBusy = true
                        message = "Resetting and importing..."
                        scope.launch {
                            val result = runCatching {
                                withContext(Dispatchers.IO) {
                                    AppStorage.resetAndReimportDatasetFromAssets(context)
                                }
                            }
                            report = AppStorage.getPriceModelReport(context)
                            stats = AppStorage.getDatasetStats(context)
                            message = if (result.isFailure) {
                                val e = result.exceptionOrNull()
                                "Reset error: ${e?.javaClass?.simpleName}: ${e?.message}"
                            } else {
                                "Dataset re-imported. Train the model again."
                            }
                            isBusy = false
                        }
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
