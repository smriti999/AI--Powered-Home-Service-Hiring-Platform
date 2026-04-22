package np.com.ai_poweredhomeservicehiringplatform.user

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.NotificationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.RatingUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

const val EXTRA_RATE_WORK_ID = "extra_rate_work_id"
const val EXTRA_RATE_WORKER_EMAIL = "extra_rate_worker_email"
const val EXTRA_RATE_PROFESSION = "extra_rate_profession"

class UserRateServiceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isUserLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val workId = intent.getIntExtra(EXTRA_RATE_WORK_ID, 0)
        val workerEmail = intent.getStringExtra(EXTRA_RATE_WORKER_EMAIL).orEmpty()
        val profession = intent.getStringExtra(EXTRA_RATE_PROFESSION).orEmpty()

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                RateServiceScreen(
                    workId = workId,
                    workerEmail = workerEmail,
                    profession = profession,
                    onBack = { finish() },
                    onSubmitted = {
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
private fun RateServiceScreen(
    workId: Int,
    workerEmail: String,
    profession: String,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val userEmail = AppStorage.currentUserEmail(context).orEmpty()
    val workers = remember { AppStorage.loadWorkers(context) }
    val workerName = remember(workerEmail, workers) {
        workers.firstOrNull { it.email.equals(workerEmail, ignoreCase = true) }?.name.orEmpty()
    }

    var stars by rememberSaveable { mutableIntStateOf(5) }
    var review by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showThanksDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Rate Service",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        if (workId <= 0) {
                            errorMessage = "Invalid booking"
                            return@Button
                        }
                        if (stars !in 1..5) {
                            errorMessage = "Please select rating"
                            return@Button
                        }

                        val ratings = AppStorage.loadRatings(context)
                        val existing = ratings.firstOrNull { it.workId == workId && it.userEmail.equals(userEmail, ignoreCase = true) }
                        val updatedRatings = if (existing != null) {
                            ratings.map {
                                if (it.id == existing.id) {
                                    it.copy(
                                        stars = stars,
                                        review = review.trim(),
                                        timestampMillis = System.currentTimeMillis()
                                    )
                                } else it
                            }
                        } else {
                            val nextId = (ratings.maxOfOrNull { it.id } ?: 0) + 1
                            ratings + RatingUiModel(
                                id = nextId,
                                workId = workId,
                                userEmail = userEmail,
                                workerEmail = workerEmail.ifBlank { "worker@gmail.com" },
                                profession = profession,
                                stars = stars,
                                review = review.trim(),
                                timestampMillis = System.currentTimeMillis()
                            )
                        }
                        AppStorage.saveRatings(context, updatedRatings)

                        val notifications = AppStorage.loadNotifications(context)
                        val nextNId = (notifications.maxOfOrNull { it.id } ?: 0) + 1
                        val updatedNotifications = notifications + NotificationUiModel(
                            id = nextNId,
                            userEmail = userEmail,
                            title = "Review Submitted",
                            message = "Thanks for rating $stars/5.",
                            timestampMillis = System.currentTimeMillis()
                        )
                        AppStorage.saveNotifications(context, updatedNotifications)

                        showThanksDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RectangleShape
                ) {
                    Text(
                        text = "SUBMIT REVIEW",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color(0xFFF2F2F2), CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = workerName.ifBlank { "Worker" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = profession.ifBlank { "Plumber" },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "How was your service?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                (1..5).forEach { index ->
                    val isSelected = index <= stars
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $index",
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                stars = index
                                errorMessage = null
                            },
                        tint = if (isSelected) Color(0xFFFFC107) else Color(0xFFEEEEEE)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = review,
                onValueChange = {
                    review = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Write a review...", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = MaterialTheme.shapes.medium
            )
        }
    }

    if (showThanksDialog) {
        AlertDialog(
            onDismissRequest = { showThanksDialog = false },
            title = { Text(text = "Thank you") },
            text = { Text(text = "Your review has been submitted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showThanksDialog = false
                        onSubmitted()
                    }
                ) {
                    Text(text = "OK")
                }
            }
        )
    }
}
