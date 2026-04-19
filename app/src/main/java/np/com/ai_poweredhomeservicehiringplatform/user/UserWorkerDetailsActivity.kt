package np.com.ai_poweredhomeservicehiringplatform.user

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.StarRating
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

const val EXTRA_PREFERRED_WORKER_NAME = "extra_preferred_worker_name"

class UserWorkerDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isUserLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val workerEmail = intent.getStringExtra(EXTRA_WORKER_EMAIL).orEmpty()
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                UserWorkerDetailsScreen(
                    workerEmail = workerEmail,
                    onBack = { finish() },
                    onRequestService = { profession, workerName ->
                        val intent = Intent(this, UserCreateJobActivity::class.java)
                        intent.putExtra(EXTRA_PRESET_SERVICE, profession)
                        intent.putExtra(EXTRA_PREFERRED_WORKER_NAME, workerName)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserWorkerDetailsScreen(
    workerEmail: String,
    onBack: () -> Unit,
    onRequestService: (profession: String, workerName: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val worker = remember(workerEmail) {
        AppStorage.loadWorkers(context).firstOrNull { it.email.equals(workerEmail, ignoreCase = true) }
    }
    val ratings = remember { AppStorage.loadRatings(context) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Worker Profile",
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
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (worker == null) {
                Text(text = "Worker not found", color = MaterialTheme.colorScheme.error)
                return@Column
            }

            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(Color(0xFFCCCCCC), CircleShape)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = worker.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = worker.profession,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            val workerRatings = ratings.filter { it.workerName.equals(worker.name, ignoreCase = true) }
            val avgRating = if (workerRatings.isNotEmpty()) workerRatings.map { it.stars }.average().toInt() else 0

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRating(rating = avgRating, starSize = 20.dp)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (workerRatings.isEmpty()) "(No ratings)" else "($avgRating/5)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onRequestService(worker.profession, worker.name) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(text = "Request Service")
            }

            Spacer(modifier = Modifier.height(18.dp))

            InfoRow(label = "Location", value = worker.location)
            InfoRow(label = "Experience", value = "${worker.experienceYears} Years")
            InfoRow(label = "Phone", value = worker.phoneNumber)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Reviews",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (workerRatings.isEmpty()) {
                Text(
                    text = "No reviews yet",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                workerRatings
                    .sortedByDescending { it.timestampMillis }
                    .take(12)
                    .forEach { r ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StarRating(rating = r.stars, starSize = 18.dp)
                                    Text(
                                        text = "Work #${r.workId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (r.review.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = r.review, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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

