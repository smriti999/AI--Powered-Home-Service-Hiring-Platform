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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.StarRating
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

const val EXTRA_WORKER_EMAIL = "extra_worker_email"

class UserWorkersActivity : ComponentActivity() {
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
                UserWorkersScreen(
                    onBack = { finish() },
                    onWorkerClick = { workerEmail ->
                        val intent = Intent(this, UserWorkerDetailsActivity::class.java)
                        intent.putExtra(EXTRA_WORKER_EMAIL, workerEmail)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserWorkersScreen(
    onBack: () -> Unit,
    onWorkerClick: (workerEmail: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val workers = remember { AppStorage.loadWorkers(context) }
    val ratings = remember { AppStorage.loadRatings(context) }
    val works = remember { AppStorage.loadWorks(context) }
    val userEmail = remember { AppStorage.currentUserEmail(context).orEmpty() }
    val userLocation = remember(userEmail) {
        AppStorage.loadUsers(context).firstOrNull { it.email.equals(userEmail, ignoreCase = true) }?.location.orEmpty()
    }

    var search by rememberSaveable { mutableStateOf("") }

    val globalMean = remember(ratings) {
        if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average()
    }

    val visibleWorkers = workers
        .filter { it.status.equals("Active", ignoreCase = true) }
        .filter { w ->
            val q = search.trim()
            q.isBlank() ||
                w.name.contains(q, ignoreCase = true) ||
                w.profession.contains(q, ignoreCase = true) ||
                w.location.contains(q, ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel> { worker ->
                workerRecommendationScore(
                    workerName = worker.name,
                    workerProfession = worker.profession,
                    workerLocation = worker.location,
                    workerExperienceYears = worker.experienceYears,
                    globalMeanStars = globalMean,
                    allRatings = ratings,
                    allWorks = works,
                    userLocation = userLocation
                )
            }.thenByDescending { worker ->
                val workerRatings = ratings.filter { it.workerName.equals(worker.name, ignoreCase = true) }
                workerRatings.size
            }.thenByDescending { worker ->
                parseIntOrZero(worker.experienceYears)
            }.thenBy { it.name.lowercase() }
        )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Find Workers",
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
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                placeholder = { Text(text = "Search by name, profession, or location") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (visibleWorkers.isEmpty()) {
                Text(
                    text = "No workers found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                visibleWorkers.forEach { worker ->
                    val workerRatings = ratings.filter { it.workerName.equals(worker.name, ignoreCase = true) }
                    val reviewCount = workerRatings.size
                    val avgRating = if (workerRatings.isNotEmpty()) workerRatings.map { it.stars }.average() else 0.0
                    val bayesRating = bayesianAverageStars(
                        avgStars = avgRating,
                        reviewCount = reviewCount,
                        globalMeanStars = globalMean,
                        m = 8
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onWorkerClick(worker.email) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = worker.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = worker.profession,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    StarRating(rating = bayesRating.toInt().coerceIn(0, 5), starSize = 18.dp)
                                    Text(
                                        text = if (reviewCount == 0) "No reviews" else "${String.format("%.1f", avgRating)} ($reviewCount)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = worker.location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private fun parseIntOrZero(raw: String): Int {
    val cleaned = raw.trim().filter { it.isDigit() }
    return cleaned.toIntOrNull() ?: 0
}

private fun bayesianAverageStars(
    avgStars: Double,
    reviewCount: Int,
    globalMeanStars: Double,
    m: Int
): Double {
    val v = max(0, reviewCount)
    val mm = max(1, m)
    if (v == 0) return globalMeanStars
    return (v.toDouble() / (v + mm)) * avgStars + (mm.toDouble() / (v + mm)) * globalMeanStars
}

private fun workerRecommendationScore(
    workerName: String,
    workerProfession: String,
    workerLocation: String,
    workerExperienceYears: String,
    globalMeanStars: Double,
    allRatings: List<np.com.ai_poweredhomeservicehiringplatform.common.model.RatingUiModel>,
    allWorks: List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>,
    userLocation: String
): Double {
    val workerRatings = allRatings.filter { it.workerName.equals(workerName, ignoreCase = true) }
    val reviewCount = workerRatings.size
    val avgStars = if (reviewCount == 0) 0.0 else workerRatings.map { it.stars }.average()
    val bayes = bayesianAverageStars(avgStars, reviewCount, globalMeanStars, m = 8)

    val experience = parseIntOrZero(workerExperienceYears)
    val expScore = min(experience / 10.0, 1.0)

    val demand = allWorks.count {
        it.workerName?.equals(workerName, ignoreCase = true) == true &&
            (it.status == WorkStatus.Booked || it.status == WorkStatus.Completed)
    }
    val demandScore = min(ln(1.0 + demand.toDouble()) / ln(1.0 + 20.0), 1.0)

    val locationScore = if (userLocation.isNotBlank() && workerLocation.equals(userLocation, ignoreCase = true)) 1.0 else 0.0

    val reviewScore = min(ln(1.0 + reviewCount.toDouble()) / ln(1.0 + 50.0), 1.0)

    val professionScore = if (workerProfession.isNotBlank()) 1.0 else 0.0

    return (0.55 * (bayes / 5.0)) +
        (0.20 * reviewScore) +
        (0.15 * expScore) +
        (0.07 * locationScore) +
        (0.03 * demandScore) +
        (0.00 * professionScore)
}
