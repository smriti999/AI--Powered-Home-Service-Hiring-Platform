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
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.StarRating
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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
    val payments = remember { AppStorage.loadPayments(context) }
    val workerSettings = remember { AppStorage.loadAllWorkerSettings(context) }
    val userEmail = remember { AppStorage.currentUserEmail(context).orEmpty() }
    val userLocation = remember(userEmail) {
        AppStorage.loadUsers(context).firstOrNull { it.email.equals(userEmail, ignoreCase = true) }?.location.orEmpty()
    }
    val notificationCount = rememberUnreadNotificationCount(userEmail)

    var search by rememberSaveable { mutableStateOf("") }

    val globalMean = remember(ratings) {
        if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average()
    }

    val availabilityByWorkerEmail = remember(workerSettings) {
        workerSettings.associate { it.workerEmail.lowercase() to it.available }
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
                    workerEmail = worker.email,
                    workerProfession = worker.profession,
                    workerLocation = worker.location,
                    workerExperienceYears = worker.experienceYears,
                    globalMeanStars = globalMean,
                    allRatings = ratings,
                    allWorks = works,
                    allPayments = payments,
                    isAvailable = availabilityByWorkerEmail[worker.email.lowercase()] ?: true,
                    userLocation = userLocation,
                    searchQuery = search
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
                },
                actions = {
                    NotificationBell(
                        count = notificationCount,
                        onClick = { context.startActivity(Intent(context, UserNotificationsActivity::class.java)) }
                    )
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
    workerEmail: String,
    workerProfession: String,
    workerLocation: String,
    workerExperienceYears: String,
    globalMeanStars: Double,
    allRatings: List<np.com.ai_poweredhomeservicehiringplatform.common.model.RatingUiModel>,
    allWorks: List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>,
    allPayments: List<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>,
    isAvailable: Boolean,
    userLocation: String,
    searchQuery: String
): Double {
    val workerRatings = allRatings.filter { it.workerName.equals(workerName, ignoreCase = true) }
    val reviewCount = workerRatings.size
    val avgStars = if (reviewCount == 0) 0.0 else workerRatings.map { it.stars }.average()
    val bayes = bayesianAverageStars(avgStars, reviewCount, globalMeanStars, m = 8)
    val wilson = wilsonLowerBound(
        avgStars = if (reviewCount == 0) globalMeanStars else avgStars,
        reviewCount = reviewCount,
        maxStars = 5.0,
        z = 1.96
    )
    val ratingQuality = (0.6 * (bayes / 5.0)) + (0.4 * wilson)

    val nowMillis = System.currentTimeMillis()
    val recencyScore = if (workerRatings.isEmpty()) 0.0 else {
        val halfLifeDays = 45.0
        val weights = workerRatings.map { r ->
            val ageDays = ((nowMillis - r.timestampMillis).coerceAtLeast(0L)).toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
            expDecay(ageDays, halfLifeDays).coerceIn(0.0, 1.0)
        }
        weights.average().coerceIn(0.0, 1.0)
    }

    val experience = parseIntOrZero(workerExperienceYears)
    val expScore = min(experience / 10.0, 1.0)

    val demand = allWorks.count {
        it.workerName?.equals(workerName, ignoreCase = true) == true &&
            (it.status == WorkStatus.Booked || it.status == WorkStatus.Completed)
    }
    val demandScore = min(ln(1.0 + demand.toDouble()) / ln(1.0 + 20.0), 1.0)

    val locationScore = locationSimilarity(userLocation, workerLocation)

    val reviewScore = min(ln(1.0 + reviewCount.toDouble()) / ln(1.0 + 50.0), 1.0)

    val professionScore = professionMatchScore(searchQuery, workerProfession)

    val valueScore = workerValueScore(
        workerName = workerName,
        allWorks = allWorks,
        allPayments = allPayments
    )

    val availabilityScore = if (isAvailable) 1.0 else 0.0

    val emailBonus = if (workerEmail.isNotBlank()) 1.0 else 0.0

    return (0.40 * ratingQuality) +
        (0.14 * reviewScore) +
        (0.12 * expScore) +
        (0.10 * recencyScore) +
        (0.08 * locationScore) +
        (0.08 * valueScore) +
        (0.05 * professionScore) +
        (0.02 * demandScore) +
        (0.01 * availabilityScore) +
        (0.00 * emailBonus)
}

private fun wilsonLowerBound(
    avgStars: Double,
    reviewCount: Int,
    maxStars: Double,
    z: Double
): Double {
    val n = max(0, reviewCount)
    if (n == 0) return (avgStars / maxStars).coerceIn(0.0, 1.0)
    val p = (avgStars / maxStars).coerceIn(0.0, 1.0)
    val z2 = z * z
    val denom = 1.0 + (z2 / n.toDouble())
    val centre = p + (z2 / (2.0 * n.toDouble()))
    val margin = z * sqrt((p * (1.0 - p) + (z2 / (4.0 * n.toDouble()))) / n.toDouble())
    return ((centre - margin) / denom).coerceIn(0.0, 1.0)
}

private fun expDecay(ageDays: Double, halfLifeDays: Double): Double {
    if (halfLifeDays <= 0.0) return 0.0
    val lambda = ln(2.0) / halfLifeDays
    return kotlin.math.exp(-lambda * max(0.0, ageDays))
}

private fun locationSimilarity(userLocation: String, workerLocation: String): Double {
    val u = tokenize(userLocation)
    val w = tokenize(workerLocation)
    if (u.isEmpty() || w.isEmpty()) return 0.0
    val intersection = u.intersect(w).size.toDouble()
    val union = u.union(w).size.toDouble().coerceAtLeast(1.0)
    return (intersection / union).coerceIn(0.0, 1.0)
}

private fun professionMatchScore(searchQuery: String, workerProfession: String): Double {
    val q = searchQuery.trim()
    if (q.isBlank()) return 0.0
    val qTokens = tokenize(q)
    val pTokens = tokenize(workerProfession)
    if (qTokens.isEmpty() || pTokens.isEmpty()) return 0.0
    val overlap = qTokens.intersect(pTokens).size.toDouble()
    val denom = min(qTokens.size.toDouble(), pTokens.size.toDouble()).coerceAtLeast(1.0)
    return (overlap / denom).coerceIn(0.0, 1.0)
}

private fun tokenize(raw: String): Set<String> {
    return raw.lowercase()
        .split(' ', ',', '-', '/', '|', '.', ':', ';')
        .map { it.trim() }
        .filter { it.length >= 2 }
        .toSet()
}

private fun workerValueScore(
    workerName: String,
    allWorks: List<np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel>,
    allPayments: List<np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel>
): Double {
    if (allPayments.isEmpty()) return 0.5
    val workIdToWorker = allWorks
        .filter { it.workerName != null }
        .associate { it.id to (it.workerName ?: "") }

    val paidAmounts = allPayments
        .asSequence()
        .filter { it.status == PaymentStatus.Paid }
        .mapNotNull { p ->
            val wName = workIdToWorker[p.workId] ?: return@mapNotNull null
            if (!wName.equals(workerName, ignoreCase = true)) return@mapNotNull null
            p.amountNpr
        }
        .toList()

    if (paidAmounts.isEmpty()) return 0.5

    val median = paidAmounts.sorted().let { list ->
        val mid = list.size / 2
        if (list.size % 2 == 1) list[mid].toDouble() else (list[mid - 1] + list[mid]).toDouble() / 2.0
    }

    val allPaidMedians = allPayments
        .asSequence()
        .filter { it.status == PaymentStatus.Paid }
        .map { it.amountNpr.toDouble() }
        .toList()

    if (allPaidMedians.isEmpty()) return 0.5
    val minPrice = allPaidMedians.minOrNull() ?: return 0.5
    val maxPrice = allPaidMedians.maxOrNull() ?: return 0.5
    if (maxPrice <= minPrice) return 0.5

    val normalized = ((median - minPrice) / (maxPrice - minPrice)).coerceIn(0.0, 1.0)
    return (1.0 - normalized).coerceIn(0.0, 1.0)
}
