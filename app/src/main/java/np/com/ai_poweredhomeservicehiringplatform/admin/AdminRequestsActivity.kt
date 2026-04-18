package np.com.ai_poweredhomeservicehiringplatform.admin

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerApplicationStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class AdminRequestsActivity : ComponentActivity() {
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
                AdminRequestsScreen(
                    onDashboardClick = { startActivity(Intent(this, AdminDashboardActivity::class.java)) },
                    onWorkersClick = { startActivity(Intent(this, AdminWorkerManagementActivity::class.java)) },
                    onUsersClick = { startActivity(Intent(this, AdminUserManagementActivity::class.java)) },
                    onWorksClick = { startActivity(Intent(this, AdminWorkManagementActivity::class.java)) },
                    onOpenCv = { uri ->
                        val intent = Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "application/pdf")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivity(Intent.createChooser(intent, "Open CV"))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminRequestsScreen(
    onDashboardClick: () -> Unit,
    onWorkersClick: () -> Unit,
    onUsersClick: () -> Unit,
    onWorksClick: () -> Unit,
    onOpenCv: (uri: Uri) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var applications by remember { mutableStateOf(AppStorage.loadWorkerApplications(context)) }

    val pendingApplications = applications.filter { it.status == WorkerApplicationStatus.Pending }

    fun updateApplicationStatus(appId: Int, status: WorkerApplicationStatus) {
        val updated = applications.map { existing ->
            if (existing.id == appId) existing.copy(status = status) else existing
        }
        applications = updated
        AppStorage.saveWorkerApplications(context, updated)
    }

    fun approve(appId: Int) {
        val app = applications.firstOrNull { it.id == appId } ?: return
        updateApplicationStatus(appId, WorkerApplicationStatus.Approved)

        val workers = AppStorage.loadWorkers(context)
        val nextId = (workers.maxOfOrNull { it.id } ?: 0) + 1
        val newWorker = WorkerUiModel(
            id = nextId,
            name = app.name,
            status = "Active",
            email = app.email,
            phoneNumber = app.phoneNumber,
            location = app.location,
            streetHomeNumber = app.streetHomeNumber,
            alternativeLocation = app.alternativeLocation,
            gender = app.gender,
            profession = app.profession,
            experienceYears = app.experienceYears,
            passwordHash = app.passwordHash
        )
        AppStorage.saveWorkers(context, workers + newWorker)
    }

    fun reject(appId: Int) {
        updateApplicationStatus(appId, WorkerApplicationStatus.Rejected)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Worker Requests",
                actions = {
                    Row {
                        TextButton(onClick = onDashboardClick) { Text(text = "Dashboard", color = Color.White) }
                        TextButton(onClick = onWorkersClick) { Text(text = "Workers", color = Color.White) }
                        TextButton(onClick = onUsersClick) { Text(text = "Users", color = Color.White) }
                        TextButton(onClick = onWorksClick) { Text(text = "Works", color = Color.White) }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (pendingApplications.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No applied applicant available")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pendingApplications.forEach { app ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${app.name} (${app.profession})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.padding(top = 4.dp))
                            Text(
                                text = "Experience: ${app.experienceYears} years",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Email: ${app.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Phone: ${app.phoneNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Location: ${app.location}, ${app.streetHomeNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gender: ${app.gender}",
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
                                    onClick = { approve(app.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(text = "Approve")
                                }

                                Button(
                                    onClick = { reject(app.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD32F2F),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(text = "Reject")
                                }

                                Button(
                                    onClick = { onOpenCv(Uri.parse(app.cvUri)) }
                                ) {
                                    Text(text = "View CV")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

