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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkerNotificationsActivity : ComponentActivity() {
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
                WorkerNotificationsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerNotificationsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val workerEmail = AppStorage.currentWorkerEmail(context).orEmpty()
    val workers = AppStorage.loadWorkers(context)
    val worker = workers.find { it.email.equals(workerEmail, ignoreCase = true) }
    val workerName = worker?.name.orEmpty()

    LaunchedEffect(workerEmail) {
        AppStorage.markNotificationsSeen(context, workerEmail)
    }

    val notifications = remember { AppStorage.loadNotifications(context) }
        .filter { it.userEmail.equals(workerEmail, ignoreCase = true) }
        .sortedByDescending { it.timestampMillis }

    val works = remember { AppStorage.loadWorks(context) }
        .filter { (it.workerEmail ?: "").equals(workerEmail, ignoreCase = true) }
        .sortedByDescending { it.id }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Notifications", "Work History")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Worker Updates",
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text = title) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTab == 0) {
                    if (notifications.isEmpty()) {
                        EmptyState("No notifications yet")
                    } else {
                        notifications.forEach { n ->
                            NotificationCard(n.title, n.message, n.timestampMillis)
                        }
                    }
                } else {
                    if (works.isEmpty()) {
                        EmptyState("No work history found")
                    } else {
                        works.forEach { w ->
                            WorkHistoryCard(w.workName, w.status, w.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(title: String, message: String, timestamp: Long) {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(date)

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun WorkHistoryCard(workName: String, status: WorkStatus, id: Int) {
    val description = when (status) {
        WorkStatus.Pending -> "This job is currently available for you to accept."
        WorkStatus.Booked -> "You have booked and accepted this job."
        WorkStatus.Completed -> "You have successfully completed this work."
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when (status) {
                        WorkStatus.Completed -> Color(0xFF2E7D32)
                        WorkStatus.Booked -> Color(0xFF1565C0)
                        WorkStatus.Pending -> Color(0xFFF9A825)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Job ID: #$id",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
    }
}
