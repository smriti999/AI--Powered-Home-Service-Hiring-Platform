package np.com.ai_poweredhomeservicehiringplatform.common.storage

import android.content.Context
import androidx.room.Room
import np.com.ai_poweredhomeservicehiringplatform.common.model.NotificationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentMethod
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.RatingUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerApplicationStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerApplicationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerSettingsUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.sha256Hex
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import org.json.JSONArray
import org.json.JSONObject

object AppStorage {
    private const val PREFS = "auth_prefs"

    private const val KEY_SEEDED_ADMIN_USERNAME = "seeded_admin_username"
    private const val KEY_SEEDED_ADMIN_PASSWORD_HASH = "seeded_admin_password_hash"

    private const val KEY_USERS_JSON = "users_json"
    private const val KEY_WORKS_JSON = "works_json"
    private const val KEY_USER_JOBS_JSON = "user_jobs_json"
    private const val KEY_WORKER_APPLICATIONS_JSON = "worker_applications_json"
    private const val KEY_WORKERS_JSON = "workers_json"
    private const val KEY_NOTIFICATIONS_JSON = "notifications_json"
    private const val KEY_PAYMENTS_JSON = "payments_json"
    private const val KEY_RATINGS_JSON = "ratings_json"
    private const val KEY_WORKER_SETTINGS_JSON = "worker_settings_json"

    private const val KEY_ADMIN_LOGGED_IN = "admin_logged_in"
    private const val KEY_USER_LOGGED_IN = "user_logged_in"
    private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
    private const val KEY_WORKER_LOGGED_IN = "worker_logged_in"
    private const val KEY_CURRENT_WORKER_EMAIL = "current_worker_email"

    private const val KEY_SEEDED_DATASET_CSV = "seeded_dataset_csv_v1"
    private const val KEY_PRICE_MODEL_JSON = "price_model_json_v1"
    private const val KEY_WORK_TIMER_PREFIX = "work_timer_start_"
    private const val KEY_SEEDED_MARKET_HISTORY = "seeded_market_history_v1"
    private const val KEY_SEEDED_PRICE_TRAINING_EXTRA = "seeded_price_training_extra_v1"
    private const val KEY_DATASET_LAST_ERROR = "dataset_last_error_v1"
    private const val KEY_NOTIFICATION_LAST_SEEN_PREFIX = "notif_last_seen_"

    private const val MAX_DATASET_WORKERS = 250
    private const val MAX_WORKS_PER_WORKER = 8
    private const val MAX_REVIEWS_PER_WORKER = 60
    private const val MAX_TRAIN_SAMPLES = 4000

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile
    private var dbInstance: AppDatabase? = null

    private fun db(context: Context): AppDatabase {
        val existing = dbInstance
        if (existing != null) return existing
        val created = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "aiphs.db"
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
        dbInstance = created
        return created
    }

    private fun dao(context: Context): AppDao = db(context).dao()

    fun seedAdminIfNeeded(context: Context) {
        val p = prefs(context)
        val hasUsername = p.contains(KEY_SEEDED_ADMIN_USERNAME)
        val hasPasswordHash = p.contains(KEY_SEEDED_ADMIN_PASSWORD_HASH)
        if (hasUsername && hasPasswordHash) return

        p.edit()
            .putString(KEY_SEEDED_ADMIN_USERNAME, "admin")
            .putString(KEY_SEEDED_ADMIN_PASSWORD_HASH, sha256Hex("admin123"))
            .apply()
    }

    private data class DatasetCsvRow(
        val profileName: String,
        val basicPrice: Double,
        val reviewCount: Int,
        val avgRating: Double,
        val location: String,
        val demand: String,
        val experienceYears: Int,
        val timeSlot: String
    )

    data class DatasetStats(
        val workers: Int,
        val works: Int,
        val payments: Int,
        val paidPayments: Int,
        val ratings: Int,
        val lastError: String?
    )

    fun getDatasetStats(context: Context): DatasetStats {
        val d = dao(context)
        val payments = d.getPayments()
        return DatasetStats(
            workers = d.getWorkers().size,
            works = d.getWorks().size,
            payments = payments.size,
            paidPayments = payments.count { it.status == PaymentStatus.Paid && it.amountNpr > 0 },
            ratings = d.getRatings().size,
            lastError = prefs(context).getString(KEY_DATASET_LAST_ERROR, null)
        )
    }

    private fun setDatasetLastError(context: Context, message: String?) {
        val editor = prefs(context).edit()
        if (message.isNullOrBlank()) editor.remove(KEY_DATASET_LAST_ERROR) else editor.putString(KEY_DATASET_LAST_ERROR, message)
        editor.apply()
    }

    data class PriceModelReport(
        val sampleCount: Int,
        val featureNames: List<String>,
        val weights: List<Double>,
        val featureMeans: List<Double>,
        val featureStds: List<Double>,
        val targetMean: Double,
        val targetStd: Double,
        val rmse: Double,
        val mae: Double,
        val trainedAtMillis: Long
    )

    private data class PriceTrainRow(
        val amountNpr: Double,
        val experienceYears: Int,
        val avgRating: Double,
        val reviewCount: Int,
        val demandCount: Int,
        val location: String,
        val timeSlot: String
    )

    fun seedDatasetFromAssetsIfNeeded(
        context: Context,
        assetFileName: String = "dataset.csv"
    ) {
        val p = prefs(context)
        if (p.getBoolean(KEY_SEEDED_DATASET_CSV, false)) return

        val existingWorkers = dao(context).getWorkers()
        if (existingWorkers.isNotEmpty()) {
            p.edit().putBoolean(KEY_SEEDED_DATASET_CSV, true).apply()
            return
        }

        val rows = try {
            setDatasetLastError(context, null)
            readDatasetCsv(context, assetFileName)
        } catch (e: Exception) {
            setDatasetLastError(context, "Dataset read failed: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
        if (rows.isEmpty()) return
        val limitedRows = rows.take(MAX_DATASET_WORKERS)

        val serviceOptions = listOf(
            "Cleaning Services",
            "Plumbing Services",
            "Electrical Services",
            "Carpentry Services",
            "AC & Appliance Repair",
            "Painting Services",
            "Pest Control",
            "Handyman Services",
            "Relocation Services",
            "Maid & Cooking Services"
        )

        val workerPasswordHash = sha256Hex("worker123")
        val nameCounts = HashMap<String, Int>()
        val usedEmailPrefixes = HashSet<String>()

        val workers = ArrayList<WorkerUiModel>(limitedRows.size)
        val works = ArrayList<WorkUiModel>()
        val payments = ArrayList<PaymentUiModel>()
        val ratings = ArrayList<RatingUiModel>()
        val workerSettings = ArrayList<WorkerSettingsUiModel>(limitedRows.size)

        var nextWorkerId = 1
        var nextWorkId = 1
        var nextPaymentId = 1
        var nextRatingId = 1

        val now = System.currentTimeMillis()
        val globalMeanRating = limitedRows.map { it.avgRating }.filter { it > 0.0 }.average().let { if (it.isNaN()) 4.7 else it }

        limitedRows.forEachIndexed { idx, row ->
            val baseName = row.profileName.trim()
            if (baseName.isBlank()) return@forEachIndexed

            val seen = (nameCounts[baseName.lowercase()] ?: 0) + 1
            nameCounts[baseName.lowercase()] = seen
            val uniqueName = if (seen == 1) baseName else "$baseName ($seen)"

            val profession = serviceOptions[(idx % serviceOptions.size)]
            val email = generateUniqueGmail(uniqueName, nextWorkerId, usedEmailPrefixes)

            val worker = WorkerUiModel(
                id = nextWorkerId++,
                name = uniqueName,
                status = "Active",
                email = email,
                phoneNumber = "",
                location = row.location.ifBlank { "Kathmandu" },
                streetHomeNumber = "",
                alternativeLocation = "",
                gender = "",
                profession = profession,
                experienceYears = row.experienceYears.toString(),
                passwordHash = workerPasswordHash
            )
            workers.add(worker)

            val settings = WorkerSettingsUiModel(
                workerEmail = email.lowercase(),
                available = true,
                serviceAreasJson = JSONArray().put(worker.location).toString(),
                payoutMethod = null,
                payoutAccount = ""
            )
            workerSettings.add(settings)

            val demandWorksCount = min(MAX_WORKS_PER_WORKER, demandToWorkCount(row.demand, row.reviewCount))
            val timeSlot = normalizeTimeSlot(row.timeSlot)

            val seededAvgStars = if (row.avgRating <= 0.0) globalMeanRating else row.avgRating.coerceIn(1.0, 5.0)
            val perReviewWorkIds = ArrayList<Int>(max(1, demandWorksCount))

            repeat(max(1, demandWorksCount)) { w ->
                val workId = nextWorkId++
                perReviewWorkIds.add(workId)

                val userEmail = "user${(workId % 500) + 1}@gmail.com"
                val location = worker.location
                val detail = buildString {
                    append("User: ")
                    append(userEmail)
                    append("\nTime: ")
                    append(timeSlot)
                    append("\nLocation: ")
                    append(location)
                    append("\nStreet/Home: -\n\nImported dataset job")
                }
                works.add(
                    WorkUiModel(
                        id = workId,
                        workName = profession,
                        detail = detail,
                        workerEmail = email.lowercase(),
                        status = WorkStatus.Completed
                    )
                )

                val amount = generateSeededPriceAmount(
                    basePrice = row.basicPrice,
                    avgRating = seededAvgStars,
                    experienceYears = row.experienceYears,
                    demand = row.demand,
                    timeSlot = timeSlot,
                    rng = seededRandom(uniqueName, workId)
                )

                payments.add(
                    PaymentUiModel(
                        id = nextPaymentId++,
                        workId = workId,
                        userEmail = userEmail,
                        amountNpr = amount,
                        method = PaymentMethod.CashOnDelivery,
                        status = PaymentStatus.Paid,
                        timestampMillis = now - (seededRandom(uniqueName, amount).nextInt(0, 365) * 24L * 60L * 60L * 1000L)
                    )
                )
            }

            val reviewCount = max(0, row.reviewCount)
            val generatedReviewCount = min(MAX_REVIEWS_PER_WORKER, reviewCount)
            repeat(generatedReviewCount) { r ->
                val workIdForReview = perReviewWorkIds[r % perReviewWorkIds.size]
                val stars = generateSeededStars(
                    avgStars = seededAvgStars,
                    rng = seededRandom(uniqueName, r + 17)
                )
                ratings.add(
                    RatingUiModel(
                        id = nextRatingId++,
                        workId = workIdForReview,
                        userEmail = "user${((workIdForReview + r) % 500) + 1}@gmail.com",
                        workerEmail = email.lowercase(),
                        profession = profession,
                        stars = stars,
                        review = "Review ${r + 1}",
                        timestampMillis = now - (seededRandom(uniqueName, workIdForReview + r).nextInt(0, 540) * 24L * 60L * 60L * 1000L)
                    )
                )
            }
        }

        dao(context).replaceWorkers(workers)
        dao(context).replaceWorks(works)
        dao(context).replacePayments(payments)
        dao(context).replaceRatings(ratings)
        workerSettings.forEach { dao(context).upsertWorkerSettings(it) }

        p.edit().putBoolean(KEY_SEEDED_DATASET_CSV, true).apply()
    }

    fun resetAndReimportDatasetFromAssets(
        context: Context,
        assetFileName: String = "dataset.csv"
    ) {
        setDatasetLastError(context, null)

        runCatching {
            db(context).runInTransaction {
                dao(context).clearRatings()
                dao(context).clearPayments()
                dao(context).clearWorks()
                dao(context).clearWorkers()
                dao(context).clearWorkerSettings()
            }
        }.onFailure { e ->
            setDatasetLastError(context, "Reset failed: ${e.javaClass.simpleName}: ${e.message}")
            return
        }

        prefs(context).edit()
            .remove(KEY_PRICE_MODEL_JSON)
            .putBoolean(KEY_SEEDED_MARKET_HISTORY, false)
            .putBoolean(KEY_SEEDED_PRICE_TRAINING_EXTRA, false)
            .putBoolean(KEY_SEEDED_DATASET_CSV, false)
            .apply()

        seedDatasetFromAssetsIfNeeded(context, assetFileName)
        seedMarketHistoryFromAssetsIfNeeded(context, assetFileName)

        val paidCount = dao(context).getPayments().count { it.status == PaymentStatus.Paid && it.amountNpr > 0 }
        if (paidCount == 0) {
            setDatasetLastError(context, "Import completed but no paid payments were created. Check CSV header/values.")
        }
    }

    fun startWorkTimer(context: Context, workId: Int) {
        if (workId <= 0) return
        prefs(context).edit().putLong("$KEY_WORK_TIMER_PREFIX$workId", System.currentTimeMillis()).apply()
    }

    fun getWorkTimerStartMillis(context: Context, workId: Int): Long? {
        if (workId <= 0) return null
        val key = "$KEY_WORK_TIMER_PREFIX$workId"
        val prefs = prefs(context)
        if (!prefs.contains(key)) return null
        return prefs.getLong(key, 0L).takeIf { it > 0L }
    }

    fun clearWorkTimer(context: Context, workId: Int) {
        if (workId <= 0) return
        prefs(context).edit().remove("$KEY_WORK_TIMER_PREFIX$workId").apply()
    }

    fun finalizeWorkPriceNpr(
        context: Context,
        workId: Int,
        service: String,
        location: String,
        timeText: String,
        workerEmail: String,
        workerExperienceYears: String,
        durationMinutes: Int
    ): Int {
        val base = recommendPriceNpr(
            context = context,
            service = service,
            location = location,
            timeText = timeText,
            workerEmail = workerEmail,
            workerExperienceYears = workerExperienceYears
        )
        val minutes = max(1, durationMinutes)
        val factor = (minutes.toDouble() / 60.0).coerceIn(0.10, 2.0)
        return (base.toDouble() * factor).roundToInt().coerceIn(200, 40000)
    }

    fun recommendPriceNpr(
        context: Context,
        service: String,
        location: String,
        timeText: String,
        workerEmail: String,
        workerExperienceYears: String
    ): Int {
        val works = dao(context).getWorks()
        var payments = dao(context).getPayments().filter { it.status == PaymentStatus.Paid }
        if (payments.isEmpty()) {
            seedMarketHistoryFromAssetsIfNeeded(context)
            payments = dao(context).getPayments().filter { it.status == PaymentStatus.Paid }
            if (payments.isEmpty()) return 0
        }

        val timeSlot = normalizeTimeSlot(timeText)

        val worksById = works.associateBy { it.id }
        val candidateAmounts = payments.mapNotNull { payment ->
            val work = worksById[payment.workId] ?: return@mapNotNull null
            if (!work.workName.equals(service, ignoreCase = true)) return@mapNotNull null
            val workLoc = extractDetailField(work.detail, "Location")
            if (workLoc.isNullOrBlank() || !workLoc.equals(location, ignoreCase = true)) return@mapNotNull null
            val workTime = extractDetailField(work.detail, "Time")
            if (workTime.isNullOrBlank() || normalizeTimeSlot(workTime) != timeSlot) return@mapNotNull null
            payment.amountNpr.toDouble().takeIf { it in 200.0..25000.0 }
        }

        val allPaidAmounts = payments.mapNotNull { it.amountNpr.toDouble().takeIf { a -> a > 0.0 } }
        val fallbackAmounts = allPaidAmounts.filter { it in 200.0..25000.0 }.ifEmpty { allPaidAmounts }
        val base = median(candidateAmounts).takeIf { it > 0.0 } ?: median(fallbackAmounts)
        if (base <= 0.0) return 0

        val workerRatings = dao(context).getRatings().filter { it.workerEmail.equals(workerEmail, ignoreCase = true) }
        val globalMean = dao(context).getRatings().takeIf { it.isNotEmpty() }?.map { it.stars }?.average() ?: 4.7
        val workerAvg = if (workerRatings.isEmpty()) globalMean else workerRatings.map { it.stars }.average()

        val exp = parseIntOrZero(workerExperienceYears)
        val expAdj = (1.0 + 0.03 * ((exp - 5).toDouble() / 10.0)).coerceIn(0.90, 1.15)
        val ratingAdj = (1.0 + 0.06 * ((workerAvg - globalMean) / 1.0)).coerceIn(0.90, 1.15)

        val demandCount = works.count {
            it.status != WorkStatus.Pending &&
                it.workName.equals(service, ignoreCase = true) &&
                extractDetailField(it.detail, "Location")?.equals(location, ignoreCase = true) == true
        }
        val demandAdj = (1.0 + 0.05 * (ln(1.0 + demandCount.toDouble()) / ln(21.0))).coerceIn(1.0, 1.12)
        val timeAdj = when (timeSlot.lowercase()) {
            "morning" -> 0.97
            "evening" -> 1.05
            else -> 1.0
        }

        val heuristicPrice = (base * expAdj * ratingAdj * demandAdj * timeAdj)

        val trainedModel = getPriceModelReport(context)
        val modelPrice = trainedModel?.let { model ->
            predictPriceFromModel(
                model = model,
                expYears = exp,
                avgRating = workerAvg,
                reviewCount = workerRatings.size,
                demandCount = demandCount,
                location = location,
                timeSlot = timeSlot
            )
        }

        val finalPrice = if (modelPrice != null && modelPrice > 0.0) {
            (0.55 * modelPrice) + (0.45 * heuristicPrice)
        } else {
            heuristicPrice
        }

        return finalPrice.roundToInt().coerceIn(200, 25000)
    }

    private fun seedMarketHistoryFromAssetsIfNeeded(
        context: Context,
        assetFileName: String = "dataset.csv"
    ) {
        val p = prefs(context)
        if (p.getBoolean(KEY_SEEDED_MARKET_HISTORY, false)) return

        val paid = dao(context).getPayments().any { it.status == PaymentStatus.Paid && it.amountNpr > 0 }
        if (paid) {
            p.edit().putBoolean(KEY_SEEDED_MARKET_HISTORY, true).apply()
            return
        }

        val rows = try {
            setDatasetLastError(context, null)
            readDatasetCsv(context, assetFileName)
        } catch (e: Exception) {
            setDatasetLastError(context, "Dataset read failed: ${e.javaClass.simpleName}: ${e.message}")
            emptyList()
        }
        if (rows.isEmpty()) return

        val existingWorks = dao(context).getWorks()
        val existingPayments = dao(context).getPayments()
        var nextWorkId = (existingWorks.maxOfOrNull { it.id } ?: 0) + 1
        var nextPaymentId = (existingPayments.maxOfOrNull { it.id } ?: 0) + 1

        val now = System.currentTimeMillis()
        val serviceOptions = listOf(
            "Cleaning Services",
            "Plumbing Services",
            "Electrical Services",
            "Carpentry Services",
            "AC & Appliance Repair",
            "Painting Services",
            "Pest Control",
            "Handyman Services",
            "Relocation Services",
            "Maid & Cooking Services"
        )

        val works = ArrayList<WorkUiModel>()
        val payments = ArrayList<PaymentUiModel>()

        rows.take(250).forEachIndexed { idx, row ->
            val service = serviceOptions[idx % serviceOptions.size]
            val loc = row.location.ifBlank { "Kathmandu" }
            val timeSlot = normalizeTimeSlot(row.timeSlot)
            val amount = generateSeededPriceAmount(
                basePrice = row.basicPrice,
                avgRating = row.avgRating.coerceIn(1.0, 5.0).takeIf { it > 0.0 } ?: 4.7,
                experienceYears = row.experienceYears,
                demand = row.demand,
                timeSlot = timeSlot,
                rng = seededRandom(loc, idx + 31)
            )
            val workId = nextWorkId++
            val userEmail = "market@gmail.com"
            val detail = buildString {
                append("User: ")
                append(userEmail)
                append("\nTime: ")
                append(timeSlot)
                append("\nLocation: ")
                append(loc)
                append("\nStreet/Home: -\n\nMarket history")
            }
            works.add(
                WorkUiModel(
                    id = workId,
                    workName = service,
                    detail = detail,
                    workerEmail = null,
                    status = WorkStatus.Completed
                )
            )
            payments.add(
                PaymentUiModel(
                    id = nextPaymentId++,
                    workId = workId,
                    userEmail = userEmail,
                    amountNpr = amount,
                    method = PaymentMethod.CashOnDelivery,
                    status = PaymentStatus.Paid,
                    timestampMillis = now - (seededRandom(service, amount).nextInt(0, 365) * 24L * 60L * 60L * 1000L)
                )
            )
        }

        if (works.isNotEmpty()) dao(context).replaceWorks(existingWorks + works)
        if (payments.isNotEmpty()) dao(context).replacePayments(existingPayments + payments)

        p.edit().putBoolean(KEY_SEEDED_MARKET_HISTORY, true).apply()
    }

    fun trainPriceModel(context: Context): PriceModelReport? {
        var rows = collectPriceTrainRows(context)
        if (rows.size < 12) {
            seedPaidHistoryForTrainingIfNeeded(context)
            rows = collectPriceTrainRows(context)
        }
        if (rows.size < 12) return null
        if (rows.size > MAX_TRAIN_SAMPLES) {
            rows = rows.take(MAX_TRAIN_SAMPLES)
        }

        val featureNames = listOf(
            "exp_norm",
            "rating_norm",
            "reviews_log_norm",
            "demand_log_norm",
            "time_morning",
            "time_afternoon",
            "time_evening",
            "loc_kathmandu",
            "loc_bhaktapur",
            "loc_lalitpur"
        )

        val xs = rows.map { row ->
            buildPriceFeatureRaw(
                expYears = row.experienceYears,
                avgRating = row.avgRating,
                reviewCount = row.reviewCount,
                demandCount = row.demandCount,
                location = row.location,
                timeSlot = row.timeSlot
            )
        }
        val ys = rows.map { it.amountNpr }

        val featureCount = featureNames.size
        val means = DoubleArray(featureCount) { j -> xs.map { it[j] }.average() }
        val stds = DoubleArray(featureCount) { j ->
            val m = means[j]
            val variance = xs.map { v -> val d = v[j] - m; d * d }.average()
            val s = kotlin.math.sqrt(variance)
            if (s < 1e-9) 1.0 else s
        }
        val yMean = ys.average()
        val yVar = ys.map { y -> val d = y - yMean; d * d }.average()
        val yStd = kotlin.math.sqrt(yVar).let { if (it < 1e-9) 1.0 else it }

        val xNorm = xs.map { v ->
            DoubleArray(featureCount) { j -> (v[j] - means[j]) / stds[j] }
        }
        val yNorm = ys.map { y -> (y - yMean) / yStd }

        val w = DoubleArray(featureCount + 1) { 0.0 }
        val learningRate = 0.05
        val epochs = 500
        val n = xNorm.size.toDouble()

        repeat(epochs) {
            val grad = DoubleArray(featureCount + 1) { 0.0 }
            for (i in xNorm.indices) {
                val x = xNorm[i]
                val pred = w[0] + (0 until featureCount).sumOf { j -> w[j + 1] * x[j] }
                val err = pred - yNorm[i]
                grad[0] += err
                for (j in 0 until featureCount) grad[j + 1] += err * x[j]
            }
            for (j in grad.indices) {
                grad[j] /= n
                w[j] -= learningRate * grad[j]
            }
        }

        var sq = 0.0
        var absErr = 0.0
        for (i in xNorm.indices) {
            val x = xNorm[i]
            val predNorm = w[0] + (0 until featureCount).sumOf { j -> w[j + 1] * x[j] }
            val pred = yMean + (yStd * predNorm)
            val err = pred - ys[i]
            sq += err * err
            absErr += abs(err)
        }
        val rmse = kotlin.math.sqrt(sq / xNorm.size.toDouble())
        val mae = absErr / xNorm.size.toDouble()

        val report = PriceModelReport(
            sampleCount = rows.size,
            featureNames = featureNames,
            weights = w.toList(),
            featureMeans = means.toList(),
            featureStds = stds.toList(),
            targetMean = yMean,
            targetStd = yStd,
            rmse = rmse,
            mae = mae,
            trainedAtMillis = System.currentTimeMillis()
        )

        val json = JSONObject()
            .put("sampleCount", report.sampleCount)
            .put("featureNames", JSONArray(report.featureNames))
            .put("weights", JSONArray(report.weights))
            .put("featureMeans", JSONArray(report.featureMeans))
            .put("featureStds", JSONArray(report.featureStds))
            .put("targetMean", report.targetMean)
            .put("targetStd", report.targetStd)
            .put("rmse", report.rmse)
            .put("mae", report.mae)
            .put("trainedAtMillis", report.trainedAtMillis)

        prefs(context).edit().putString(KEY_PRICE_MODEL_JSON, json.toString()).apply()
        return report
    }

    private fun seedPaidHistoryForTrainingIfNeeded(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_SEEDED_PRICE_TRAINING_EXTRA, false)) return

        val existingTrainRows = collectPriceTrainRows(context)
        if (existingTrainRows.size >= 20) {
            p.edit().putBoolean(KEY_SEEDED_PRICE_TRAINING_EXTRA, true).apply()
            return
        }

        if (dao(context).getWorkers().isEmpty()) {
            seedDatasetFromAssetsIfNeeded(context)
        }

        val workers = dao(context).getWorkers()
        if (workers.isEmpty()) return

        val worksExisting = dao(context).getWorks()
        val paymentsExisting = dao(context).getPayments()

        var nextWorkId = (worksExisting.maxOfOrNull { it.id } ?: 0) + 1
        var nextPaymentId = (paymentsExisting.maxOfOrNull { it.id } ?: 0) + 1

        val now = System.currentTimeMillis()
        val newWorks = ArrayList<WorkUiModel>()
        val newPayments = ArrayList<PaymentUiModel>()

        val ratings = dao(context).getRatings()
        val avgRatingsByWorker = ratings
            .groupBy { it.workerEmail.lowercase() }
            .mapValues { (_, rs) -> rs.map { it.stars }.average() }

        val globalMean = if (ratings.isEmpty()) 4.7 else ratings.map { it.stars }.average()

        workers.take(60).forEachIndexed { idx, worker ->
            val service = worker.profession.ifBlank { "Cleaning Services" }
            val loc = worker.location.ifBlank { "Kathmandu" }
            val timeSlot = if (idx % 3 == 0) "Morning" else if (idx % 3 == 1) "Afternoon" else "Evening"
            val avgStars = avgRatingsByWorker[worker.email.lowercase()] ?: globalMean
            val expYears = parseIntOrZero(worker.experienceYears)

            val basePrice = 1800.0 + (expYears * 120.0) + ((avgStars - 3.0) * 220.0)
            val amount = generateSeededPriceAmount(
                basePrice = basePrice,
                avgRating = avgStars,
                experienceYears = expYears,
                demand = "Medium",
                timeSlot = timeSlot,
                rng = seededRandom(worker.name, idx + 77)
            )

            val workId = nextWorkId++
            val userEmail = "train@gmail.com"
            val detail = buildString {
                append("User: ")
                append(userEmail)
                append("\nTime: ")
                append(timeSlot)
                append("\nLocation: ")
                append(loc)
                append("\nStreet/Home: -\n\nTraining history")
            }

            newWorks.add(
                WorkUiModel(
                    id = workId,
                    workName = service,
                    detail = detail,
                    workerEmail = worker.email.lowercase(),
                    status = WorkStatus.Completed
                )
            )

            newPayments.add(
                PaymentUiModel(
                    id = nextPaymentId++,
                    workId = workId,
                    userEmail = userEmail,
                    amountNpr = amount,
                    method = PaymentMethod.CashOnDelivery,
                    status = PaymentStatus.Paid,
                    timestampMillis = now - (seededRandom(service, amount).nextInt(0, 180) * 24L * 60L * 60L * 1000L)
                )
            )
        }

        if (newWorks.isNotEmpty()) dao(context).replaceWorks(worksExisting + newWorks)
        if (newPayments.isNotEmpty()) dao(context).replacePayments(paymentsExisting + newPayments)

        p.edit().putBoolean(KEY_SEEDED_PRICE_TRAINING_EXTRA, true).apply()
    }

    fun getPriceModelReport(context: Context): PriceModelReport? {
        val raw = prefs(context).getString(KEY_PRICE_MODEL_JSON, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val featureNames = obj.getJSONArray("featureNames").toStringList()
            val weights = obj.getJSONArray("weights").toDoubleList()
            val means = obj.getJSONArray("featureMeans").toDoubleList()
            val stds = obj.getJSONArray("featureStds").toDoubleList()
            PriceModelReport(
                sampleCount = obj.optInt("sampleCount", 0),
                featureNames = featureNames,
                weights = weights,
                featureMeans = means,
                featureStds = stds,
                targetMean = obj.optDouble("targetMean", 0.0),
                targetStd = obj.optDouble("targetStd", 1.0).let { if (it == 0.0) 1.0 else it },
                rmse = obj.optDouble("rmse", 0.0),
                mae = obj.optDouble("mae", 0.0),
                trainedAtMillis = obj.optLong("trainedAtMillis", 0L)
            )
        }.getOrNull()
    }

    private fun parseIntOrZero(raw: String): Int {
        val cleaned = raw.trim().filter { it.isDigit() }
        return cleaned.toIntOrNull() ?: 0
    }

    private fun collectPriceTrainRows(context: Context): List<PriceTrainRow> {
        val works = dao(context).getWorks()
        val workers = dao(context).getWorkers()
        val ratings = dao(context).getRatings()
        val payments = dao(context).getPayments().filter { it.status == PaymentStatus.Paid && it.amountNpr > 0 }
        if (payments.isEmpty()) return emptyList()

        val worksById = works.associateBy { it.id }
        val workersByEmail = workers.associateBy { it.email.lowercase() }
        val reviewCountsByWorker = ratings.groupingBy { it.workerEmail.lowercase() }.eachCount()
        val avgRatingsByWorker = ratings
            .groupBy { it.workerEmail.lowercase() }
            .mapValues { (_, rs) -> rs.map { it.stars }.average() }

        return payments.mapNotNull { payment ->
            val work = worksById[payment.workId] ?: return@mapNotNull null
            val workerEmail = work.workerEmail?.trim().orEmpty()
            if (workerEmail.isBlank()) return@mapNotNull null
            val worker = workersByEmail[workerEmail.lowercase()] ?: return@mapNotNull null

            val location = extractDetailField(work.detail, "Location") ?: worker.location
            val timeSlot = normalizeTimeSlot(extractDetailField(work.detail, "Time").orEmpty())
            val demandCount = works.count {
                it.status != WorkStatus.Pending &&
                    it.workName.equals(work.workName, ignoreCase = true) &&
                    extractDetailField(it.detail, "Location")?.equals(location, ignoreCase = true) == true
            }

            PriceTrainRow(
                amountNpr = payment.amountNpr.toDouble(),
                experienceYears = parseIntOrZero(worker.experienceYears),
                avgRating = avgRatingsByWorker[workerEmail.lowercase()] ?: 4.7,
                reviewCount = reviewCountsByWorker[workerEmail.lowercase()] ?: 0,
                demandCount = demandCount,
                location = location,
                timeSlot = timeSlot
            )
        }
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    private fun extractDetailField(detail: String, key: String): String? {
        val prefix = "$key:"
        val line = detail.lineSequence().firstOrNull { it.trim().startsWith(prefix, ignoreCase = true) } ?: return null
        return line.substringAfter(":").trim().takeIf { it.isNotBlank() }
    }

    private fun demandToWorkCount(demand: String, reviewCount: Int): Int {
        val base = when (demand.trim().lowercase()) {
            "high" -> 10
            "medium" -> 5
            else -> 2
        }
        val extra = min(6, max(0, reviewCount / 80))
        return base + extra
    }

    private fun normalizeTimeSlot(raw: String): String {
        val t = raw.trim()
        if (t.equals("morning", ignoreCase = true)) return "Morning"
        if (t.equals("afternoon", ignoreCase = true)) return "Afternoon"
        if (t.equals("evening", ignoreCase = true)) return "Evening"

        val lower = t.lowercase()
        if (lower.contains("am") || lower.contains("a.m")) return "Morning"
        if (lower.contains("pm") || lower.contains("p.m")) {
            val hour = lower.filter { it.isDigit() }.take(2).toIntOrNull()
            if (hour != null && hour in 1..4) return "Afternoon"
            return "Evening"
        }

        return "Afternoon"
    }

    private fun readDatasetCsv(context: Context, assetFileName: String): List<DatasetCsvRow> {
        val input = context.assets.open(assetFileName)
        BufferedReader(InputStreamReader(input)).use { reader ->
            val headerLine = reader.readLine() ?: return emptyList()
            val header = parseCsvLine(headerLine)
                .map { it.trim().trimStart('\uFEFF') }
            if (header.isEmpty()) return emptyList()

            val idx = header
                .mapIndexed { i, name -> name.trim().lowercase() to i }
                .toMap()

            fun col(name: String): Int? = idx[name.trim().lowercase()]

            val nameIdx = col("profile name") ?: return emptyList()
            val priceIdx = col("basic price") ?: return emptyList()
            val reviewIdx = col("total number of reviews") ?: return emptyList()
            val ratingIdx = col("ratings") ?: return emptyList()
            val locationIdx = col("location") ?: return emptyList()
            val demandIdx = col("demand") ?: return emptyList()
            val expIdx = col("experience") ?: return emptyList()
            val timeIdx = col("time") ?: return emptyList()

            val rows = ArrayList<DatasetCsvRow>()
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                if (line.isNullOrBlank()) continue
                val parts = parseCsvLine(line)
                if (parts.size <= maxOf(nameIdx, priceIdx, reviewIdx, ratingIdx, locationIdx, demandIdx, expIdx, timeIdx)) continue

                val profileName = parts[nameIdx].trim()
                if (profileName.isBlank()) continue

                val basicPrice = parts[priceIdx].trim().toDoubleOrNull() ?: 0.0
                val reviewCount = parts[reviewIdx].trim().toIntOrNull() ?: 0
                val avgRating = parts[ratingIdx].trim().toDoubleOrNull() ?: 0.0
                val location = parts[locationIdx].trim()
                val demand = parts[demandIdx].trim()
                val experienceYears = parts[expIdx].trim().toIntOrNull() ?: 0
                val timeSlot = parts[timeIdx].trim()

                rows.add(
                    DatasetCsvRow(
                        profileName = profileName,
                        basicPrice = basicPrice,
                        reviewCount = reviewCount,
                        avgRating = avgRating,
                        location = location,
                        demand = demand,
                        experienceYears = experienceYears,
                        timeSlot = timeSlot
                    )
                )
            }
            return rows
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var i = 0
        var inQuotes = false
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                val nextIsQuote = i + 1 < line.length && line[i + 1] == '"'
                if (nextIsQuote) {
                    sb.append('"')
                    i += 2
                    continue
                }
                inQuotes = !inQuotes
                i++
                continue
            }
            if (c == ',' && !inQuotes) {
                out.add(sb.toString())
                sb.setLength(0)
                i++
                continue
            }
            sb.append(c)
            i++
        }
        out.add(sb.toString())
        return out
    }

    private fun generateUniqueGmail(name: String, workerId: Int, usedPrefixes: MutableSet<String>): String {
        val base = name.lowercase()
            .replace("[^a-z0-9]".toRegex(), "")
            .take(20)
            .ifBlank { "worker$workerId" }
        var prefix = base
        var counter = 2
        while (usedPrefixes.contains(prefix)) {
            prefix = (base.take(18) + counter.toString()).take(20)
            counter++
        }
        usedPrefixes.add(prefix)
        return "$prefix@gmail.com"
    }

    private fun seededRandom(key: String, salt: Int): Random {
        val h = key.fold(0) { acc, c -> (acc * 31) + c.code }
        return Random(h xor (salt * 2654435761L).toInt())
    }

    private fun generateSeededStars(avgStars: Double, rng: Random): Int {
        val noise = (rng.nextDouble() - 0.5) * 0.8
        val raw = (avgStars + noise).coerceIn(1.0, 5.0)
        return raw.roundToInt().coerceIn(1, 5)
    }

    private fun generateSeededPriceAmount(
        basePrice: Double,
        avgRating: Double,
        experienceYears: Int,
        demand: String,
        timeSlot: String,
        rng: Random
    ): Int {
        val safeBase = if (basePrice > 0.0) {
            var v = basePrice
            while (v > 12000.0) v /= 10.0
            v.coerceIn(600.0, 9000.0)
        } else {
            2500.0
        }
        val demandFactor = when (demand.trim().lowercase()) {
            "high" -> 1.18
            "medium" -> 1.08
            else -> 1.00
        }
        val timeFactor = when (timeSlot.lowercase()) {
            "morning" -> 0.97
            "evening" -> 1.06
            else -> 1.00
        }
        val expFactor = (1.0 + 0.015 * max(0, experienceYears - 5)).coerceIn(0.95, 1.20)
        val ratingFactor = (1.0 + 0.05 * (avgRating - 4.5)).coerceIn(0.90, 1.15)
        val noise = 0.90 + (rng.nextDouble() * 0.22)
        val amount = safeBase * demandFactor * timeFactor * expFactor * ratingFactor * noise
        return amount.roundToInt().coerceAtLeast(200)
    }

    private fun buildPriceFeatureRaw(
        expYears: Int,
        avgRating: Double,
        reviewCount: Int,
        demandCount: Int,
        location: String,
        timeSlot: String
    ): DoubleArray {
        val t = normalizeTimeSlot(timeSlot).lowercase()
        val loc = location.trim().lowercase()
        return doubleArrayOf(
            (expYears.coerceIn(0, 30) / 10.0),
            (avgRating.coerceIn(1.0, 5.0) / 5.0),
            (ln(1.0 + reviewCount.coerceAtLeast(0).toDouble()) / ln(101.0)).coerceIn(0.0, 1.0),
            (ln(1.0 + demandCount.coerceAtLeast(0).toDouble()) / ln(101.0)).coerceIn(0.0, 1.0),
            if (t == "morning") 1.0 else 0.0,
            if (t == "afternoon") 1.0 else 0.0,
            if (t == "evening") 1.0 else 0.0,
            if (loc == "kathmandu") 1.0 else 0.0,
            if (loc == "bhaktapur") 1.0 else 0.0,
            if (loc == "lalitpur") 1.0 else 0.0
        )
    }

    private fun predictPriceFromModel(
        model: PriceModelReport,
        expYears: Int,
        avgRating: Double,
        reviewCount: Int,
        demandCount: Int,
        location: String,
        timeSlot: String
    ): Double? {
        if (model.weights.size != model.featureNames.size + 1) return null
        if (model.featureMeans.size != model.featureNames.size || model.featureStds.size != model.featureNames.size) return null

        val raw = buildPriceFeatureRaw(
            expYears = expYears,
            avgRating = avgRating,
            reviewCount = reviewCount,
            demandCount = demandCount,
            location = location,
            timeSlot = timeSlot
        )
        if (raw.size != model.featureNames.size) return null

        val normalized = DoubleArray(raw.size) { i ->
            val std = if (model.featureStds[i] == 0.0) 1.0 else model.featureStds[i]
            (raw[i] - model.featureMeans[i]) / std
        }
        val predNorm = model.weights[0] + normalized.indices.sumOf { i -> model.weights[i + 1] * normalized[i] }
        val pred = model.targetMean + (model.targetStd * predNorm)
        return pred.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun JSONArray.toDoubleList(): List<Double> {
        val result = ArrayList<Double>(length())
        for (i in 0 until length()) result.add(optDouble(i, 0.0))
        return result
    }

    private fun JSONArray.toStringList(): List<String> {
        val result = ArrayList<String>(length())
        for (i in 0 until length()) result.add(optString(i, ""))
        return result
    }

    fun isAdminCredentialValid(context: Context, username: String, password: String): Boolean {
        val p = prefs(context)
        val seededUsername = p.getString(KEY_SEEDED_ADMIN_USERNAME, null) ?: return false
        val seededPasswordHash = p.getString(KEY_SEEDED_ADMIN_PASSWORD_HASH, null) ?: return false
        return username == seededUsername && sha256Hex(password) == seededPasswordHash
    }

    fun setAdminLoggedIn(context: Context, loggedIn: Boolean) {
        prefs(context).edit().putBoolean(KEY_ADMIN_LOGGED_IN, loggedIn).apply()
    }

    fun isAdminLoggedIn(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ADMIN_LOGGED_IN, false)
    }

    fun setUserLoggedIn(context: Context, loggedIn: Boolean, email: String?) {
        prefs(context).edit()
            .putBoolean(KEY_USER_LOGGED_IN, loggedIn)
            .putString(KEY_CURRENT_USER_EMAIL, email)
            .apply()
    }

    fun isUserLoggedIn(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_USER_LOGGED_IN, false)
    }

    fun currentUserEmail(context: Context): String? {
        return prefs(context).getString(KEY_CURRENT_USER_EMAIL, null)
    }

    fun setWorkerLoggedIn(context: Context, loggedIn: Boolean, email: String?) {
        prefs(context).edit()
            .putBoolean(KEY_WORKER_LOGGED_IN, loggedIn)
            .putString(KEY_CURRENT_WORKER_EMAIL, email)
            .apply()
    }

    fun isWorkerLoggedIn(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_WORKER_LOGGED_IN, false)
    }

    fun currentWorkerEmail(context: Context): String? {
        return prefs(context).getString(KEY_CURRENT_WORKER_EMAIL, null)
    }

    fun logoutAll(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ADMIN_LOGGED_IN, false)
            .putBoolean(KEY_USER_LOGGED_IN, false)
            .putString(KEY_CURRENT_USER_EMAIL, null)
            .putBoolean(KEY_WORKER_LOGGED_IN, false)
            .putString(KEY_CURRENT_WORKER_EMAIL, null)
            .apply()
    }

    fun loginAsAdmin(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_ADMIN_LOGGED_IN, true)
            .putBoolean(KEY_USER_LOGGED_IN, false)
            .putString(KEY_CURRENT_USER_EMAIL, null)
            .putBoolean(KEY_WORKER_LOGGED_IN, false)
            .putString(KEY_CURRENT_WORKER_EMAIL, null)
            .apply()
    }

    fun loginAsUser(context: Context, email: String) {
        prefs(context).edit()
            .putBoolean(KEY_ADMIN_LOGGED_IN, false)
            .putBoolean(KEY_USER_LOGGED_IN, true)
            .putString(KEY_CURRENT_USER_EMAIL, email)
            .putBoolean(KEY_WORKER_LOGGED_IN, false)
            .putString(KEY_CURRENT_WORKER_EMAIL, null)
            .apply()
    }

    fun loginAsWorker(context: Context, email: String) {
        prefs(context).edit()
            .putBoolean(KEY_ADMIN_LOGGED_IN, false)
            .putBoolean(KEY_USER_LOGGED_IN, false)
            .putString(KEY_CURRENT_USER_EMAIL, null)
            .putBoolean(KEY_WORKER_LOGGED_IN, true)
            .putString(KEY_CURRENT_WORKER_EMAIL, email)
            .apply()
    }

    fun loadUsers(context: Context): List<UserUiModel> {
        return dao(context).getUsers()
    }

    fun saveUsers(context: Context, users: List<UserUiModel>) {
        dao(context).replaceUsers(users)
    }

    fun loadWorks(context: Context): List<WorkUiModel> {
        return dao(context).getWorks()
    }

    fun saveWorks(context: Context, works: List<WorkUiModel>) {
        dao(context).replaceWorks(works)
    }

    fun loadUserJobs(context: Context): List<UserJobUiModel> {
        return dao(context).getUserJobs()
    }

    fun saveUserJobs(context: Context, jobs: List<UserJobUiModel>) {
        dao(context).replaceUserJobs(jobs)
    }

    fun loadNotifications(context: Context): List<NotificationUiModel> {
        return dao(context).getNotifications()
    }

    fun saveNotifications(context: Context, notifications: List<NotificationUiModel>) {
        dao(context).replaceNotifications(notifications)
    }

    fun getUnreadNotificationCount(context: Context, userEmail: String): Int {
        val email = userEmail.trim()
        if (email.isBlank()) return 0
        val lastSeen = prefs(context).getLong(KEY_NOTIFICATION_LAST_SEEN_PREFIX + email.lowercase(), 0L)
        return dao(context).getNotifications().count {
            it.userEmail.equals(email, ignoreCase = true) && it.timestampMillis > lastSeen
        }
    }

    fun markNotificationsSeen(context: Context, userEmail: String) {
        val email = userEmail.trim()
        if (email.isBlank()) return
        val maxTs = dao(context).getNotifications()
            .asSequence()
            .filter { it.userEmail.equals(email, ignoreCase = true) }
            .maxOfOrNull { it.timestampMillis }
            ?: System.currentTimeMillis()
        prefs(context).edit().putLong(KEY_NOTIFICATION_LAST_SEEN_PREFIX + email.lowercase(), maxTs).apply()
    }

    fun loadPayments(context: Context): List<PaymentUiModel> {
        return dao(context).getPayments()
    }

    fun savePayments(context: Context, payments: List<PaymentUiModel>) {
        dao(context).replacePayments(payments)
    }

    fun loadRatings(context: Context): List<RatingUiModel> {
        return dao(context).getRatings()
    }

    fun saveRatings(context: Context, ratings: List<RatingUiModel>) {
        dao(context).replaceRatings(ratings)
    }

    fun loadWorkerApplications(context: Context): List<WorkerApplicationUiModel> {
        return dao(context).getWorkerApplications()
    }

    fun saveWorkerApplications(context: Context, applications: List<WorkerApplicationUiModel>) {
        dao(context).replaceWorkerApplications(applications)
    }

    fun loadWorkers(context: Context): List<WorkerUiModel> {
        return dao(context).getWorkers()
    }

    fun saveWorkers(context: Context, workers: List<WorkerUiModel>) {
        dao(context).replaceWorkers(workers)
    }

    fun loadWorkerAvailability(context: Context, workerEmail: String): Boolean {
        if (workerEmail.isBlank()) return true
        return dao(context).getWorkerSettings(workerEmail.lowercase())?.available ?: true
    }

    fun loadAllWorkerSettings(context: Context): List<WorkerSettingsUiModel> {
        return dao(context).getAllWorkerSettings()
    }

    fun saveWorkerAvailability(context: Context, workerEmail: String, available: Boolean) {
        if (workerEmail.isBlank()) return
        val key = workerEmail.lowercase()
        val existing = dao(context).getWorkerSettings(key)
        val updated = if (existing == null) {
            WorkerSettingsUiModel(
                workerEmail = key,
                available = available,
                serviceAreasJson = "[]",
                payoutMethod = null,
                payoutAccount = ""
            )
        } else {
            existing.copy(available = available)
        }
        dao(context).upsertWorkerSettings(updated)
    }

    fun loadWorkerServiceAreas(context: Context, workerEmail: String): List<String> {
        if (workerEmail.isBlank()) return emptyList()
        val settings = dao(context).getWorkerSettings(workerEmail.lowercase()) ?: return emptyList()
        val arr = runCatching { JSONArray(settings.serviceAreasJson) }.getOrElse { JSONArray() }
        val result = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val v = arr.optString(i, "").trim()
            if (v.isNotBlank()) result.add(v)
        }
        return result
    }

    fun saveWorkerServiceAreas(context: Context, workerEmail: String, areas: List<String>) {
        val arr = JSONArray()
        areas.distinct().forEach { a ->
            val v = a.trim()
            if (v.isNotBlank()) arr.put(v)
        }
        val key = workerEmail.lowercase()
        val existing = dao(context).getWorkerSettings(key)
        val updated = if (existing == null) {
            WorkerSettingsUiModel(
                workerEmail = key,
                available = true,
                serviceAreasJson = arr.toString(),
                payoutMethod = null,
                payoutAccount = ""
            )
        } else {
            existing.copy(serviceAreasJson = arr.toString())
        }
        dao(context).upsertWorkerSettings(updated)
    }

    fun loadWorkerPayoutMethod(context: Context, workerEmail: String): PaymentMethod? {
        if (workerEmail.isBlank()) return null
        return dao(context).getWorkerSettings(workerEmail.lowercase())?.payoutMethod
    }

    fun loadWorkerPayoutAccount(context: Context, workerEmail: String): String {
        if (workerEmail.isBlank()) return ""
        return dao(context).getWorkerSettings(workerEmail.lowercase())?.payoutAccount.orEmpty()
    }

    fun saveWorkerPayoutSettings(context: Context, workerEmail: String, method: PaymentMethod?, account: String) {
        if (workerEmail.isBlank()) return
        val key = workerEmail.lowercase()
        val existing = dao(context).getWorkerSettings(key)
        val updated = if (existing == null) {
            WorkerSettingsUiModel(
                workerEmail = key,
                available = true,
                serviceAreasJson = "[]",
                payoutMethod = method,
                payoutAccount = account.trim()
            )
        } else {
            existing.copy(payoutMethod = method, payoutAccount = account.trim())
        }
        dao(context).upsertWorkerSettings(updated)
    }
}
