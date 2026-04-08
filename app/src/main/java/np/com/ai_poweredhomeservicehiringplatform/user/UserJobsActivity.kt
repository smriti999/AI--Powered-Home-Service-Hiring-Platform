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
                val jobs = AppStorage.loadUserJobs(this).filter { it.userEmail.equals(email, ignoreCase = true) }
                UserJobsScreen(
                    userEmail = email,
                    jobs = jobs,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserJobsScreen(
    userEmail: String,
    jobs: List<UserJobUiModel>,
    onBackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var visibleJobs by remember { mutableStateOf(jobs) }

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
            CenterAlignedTopAppBar(
                title = { Text(text = "Works") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
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
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.padding(top = 12.dp))

            if (visibleJobs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No jobs yet")
                }
            } else {
                visibleJobs.forEach { job ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = job.service,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = job.status.name,
                                    color = statusColor(job.status),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.padding(top = 6.dp))

                            Text(
                                text = job.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.padding(top = 8.dp))

                            Text(text = "Location: ${job.location}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Street/Home: ${job.streetHomeNumber}", style = MaterialTheme.typography.bodySmall)
                            if (job.alternativeLocation.isNotBlank()) {
                                Text(text = "Alt: ${job.alternativeLocation}", style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.padding(top = 10.dp))

                            Button(
                                onClick = {
                                    val updatedJobs = AppStorage.loadUserJobs(context).filterNot { it.id == job.id }
                                    AppStorage.saveUserJobs(context, updatedJobs)
                                    visibleJobs = updatedJobs.filter { it.userEmail.equals(userEmail, ignoreCase = true) }

                                    val works = AppStorage.loadWorks(context)
                                    val updatedWorks = works.filterNot { work ->
                                        work.workName.equals(job.service, ignoreCase = true) &&
                                            work.detail.contains("User: ${job.userEmail}", ignoreCase = true) &&
                                            work.detail.contains(job.description, ignoreCase = true)
                                    }
                                    AppStorage.saveWorks(context, updatedWorks)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD32F2F),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = "Delete")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
