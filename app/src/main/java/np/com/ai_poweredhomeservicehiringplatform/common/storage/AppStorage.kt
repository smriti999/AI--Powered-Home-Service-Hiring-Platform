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
