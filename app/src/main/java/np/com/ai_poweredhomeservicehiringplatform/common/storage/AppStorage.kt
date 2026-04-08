package np.com.ai_poweredhomeservicehiringplatform.common.storage

import android.content.Context
import np.com.ai_poweredhomeservicehiringplatform.common.model.NotificationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentMethod
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.PaymentUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerApplicationStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerApplicationUiModel
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

    private const val KEY_ADMIN_LOGGED_IN = "admin_logged_in"
    private const val KEY_USER_LOGGED_IN = "user_logged_in"
    private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
    private const val KEY_WORKER_LOGGED_IN = "worker_logged_in"
    private const val KEY_CURRENT_WORKER_EMAIL = "current_worker_email"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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
        val raw = prefs(context).getString(KEY_USERS_JSON, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = ArrayList<UserUiModel>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            result.add(
                UserUiModel(
                    id = obj.optInt("id", 0),
                    name = obj.optString("name", ""),
                    status = obj.optString("status", "Active"),
                    email = obj.optString("email", ""),
                    phoneNumber = obj.optString("phoneNumber", ""),
                    location = obj.optString("location", ""),
                    streetHomeNumber = obj.optString("streetHomeNumber", ""),
                    alternativeLocation = obj.optString("alternativeLocation", ""),
                    passwordHash = obj.optString("passwordHash", "")
                )
            )
        }
        return result
    }

    fun saveUsers(context: Context, users: List<UserUiModel>) {
        val arr = JSONArray()
        users.forEach { user ->
            val obj = JSONObject()
            obj.put("id", user.id)
            obj.put("name", user.name)
            obj.put("status", user.status)
            obj.put("email", user.email)
            obj.put("phoneNumber", user.phoneNumber)
            obj.put("location", user.location)
            obj.put("streetHomeNumber", user.streetHomeNumber)
            obj.put("alternativeLocation", user.alternativeLocation)
            obj.put("passwordHash", user.passwordHash)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_USERS_JSON, arr.toString()).apply()
    }

    fun loadWorks(context: Context): List<WorkUiModel> {
        val raw = prefs(context).getString(KEY_WORKS_JSON, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = ArrayList<WorkUiModel>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val status = runCatching { WorkStatus.valueOf(obj.optString("status", WorkStatus.Pending.name)) }
                .getOrElse { WorkStatus.Pending }
            result.add(
                WorkUiModel(
                    id = obj.optInt("id", 0),
                    workName = obj.optString("workName", ""),
                    detail = obj.optString("detail", ""),
                    workerName = if (obj.has("workerName") && !obj.isNull("workerName")) obj.optString("workerName") else null,
                    status = status
                )
            )
        }
        return result
    }

    fun saveWorks(context: Context, works: List<WorkUiModel>) {
        val arr = JSONArray()
        works.forEach { work ->
            val obj = JSONObject()
            obj.put("id", work.id)
            obj.put("workName", work.workName)
            obj.put("detail", work.detail)
            obj.put("workerName", work.workerName)
            obj.put("status", work.status.name)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_WORKS_JSON, arr.toString()).apply()
    }

    fun loadUserJobs(context: Context): List<UserJobUiModel> {
        val raw = prefs(context).getString(KEY_USER_JOBS_JSON, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = ArrayList<UserJobUiModel>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val status = runCatching { WorkStatus.valueOf(obj.optString("status", WorkStatus.Pending.name)) }
                .getOrElse { WorkStatus.Pending }
            result.add(
                UserJobUiModel(
                    id = obj.optInt("id", 0),
                    userEmail = obj.optString("userEmail", ""),
                    service = obj.optString("service", ""),
                    description = obj.optString("description", ""),
                    location = obj.optString("location", ""),
                    streetHomeNumber = obj.optString("streetHomeNumber", ""),
                    alternativeLocation = obj.optString("alternativeLocation", ""),
                    status = status
                )
            )
        }
        return result
    }

    fun saveUserJobs(context: Context, jobs: List<UserJobUiModel>) {
        val arr = JSONArray()
        jobs.forEach { job ->
            val obj = JSONObject()
            obj.put("id", job.id)
            obj.put("userEmail", job.userEmail)
            obj.put("service", job.service)
            obj.put("description", job.description)
            obj.put("location", job.location)
            obj.put("streetHomeNumber", job.streetHomeNumber)
            obj.put("alternativeLocation", job.alternativeLocation)
            obj.put("status", job.status.name)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_USER_JOBS_JSON, arr.toString()).apply()
    }

    fun loadNotifications(context: Context): List<NotificationUiModel> {
        val raw = prefs(context).getString(KEY_NOTIFICATIONS_JSON, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = ArrayList<NotificationUiModel>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            result.add(
                NotificationUiModel(
                    id = obj.optInt("id", 0),
                    userEmail = obj.optString("userEmail", ""),
                    title = obj.optString("title", ""),
                    message = obj.optString("message", ""),
                    timestampMillis = obj.optLong("timestampMillis", 0L)
                )
            )
        }
        return result
    }

    fun saveNotifications(context: Context, notifications: List<NotificationUiModel>) {
        val arr = JSONArray()
        notifications.forEach { n ->
            val obj = JSONObject()
            obj.put("id", n.id)
            obj.put("userEmail", n.userEmail)
            obj.put("title", n.title)
            obj.put("message", n.message)
            obj.put("timestampMillis", n.timestampMillis)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_NOTIFICATIONS_JSON, arr.toString()).apply()
    }

    fun loadPayments(context: Context): List<PaymentUiModel> {
        val raw = prefs(context).getString(KEY_PAYMENTS_JSON, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = ArrayList<PaymentUiModel>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val status = runCatching { PaymentStatus.valueOf(obj.optString("status", PaymentStatus.Pending.name)) }
                .getOrElse { PaymentStatus.Pending }
            val method = runCatching {
                val rawMethod = obj.optString("method", "")
                if (rawMethod.isBlank()) null else PaymentMethod.valueOf(rawMethod)
            }.getOrNull()
            result.add(
                PaymentUiModel(
                    id = obj.optInt("id", 0),
                    workId = obj.optInt("workId", 0),
                    userEmail = obj.optString("userEmail", ""),
                    amountNpr = obj.optInt("amountNpr", 0),
                    method = method,
                    status = status,
                    timestampMillis = obj.optLong("timestampMillis", 0L)
                )
            )
        }
        return result
    }

    fun savePayments(context: Context, payments: List<PaymentUiModel>) {
        val arr = JSONArray()
        payments.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("workId", p.workId)
            obj.put("userEmail", p.userEmail)
            obj.put("amountNpr", p.amountNpr)
            obj.put("method", p.method?.name ?: "")
            obj.put("status", p.status.name)
            obj.put("timestampMillis", p.timestampMillis)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_PAYMENTS_JSON, arr.toString()).apply()
    }

    fun loadWorkerApplications(context: Context): List<WorkerApplicationUiModel> {
        val raw = prefs(context).getString(KEY_WORKER_APPLICATIONS_JSON, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = ArrayList<WorkerApplicationUiModel>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val status = runCatching { WorkerApplicationStatus.valueOf(obj.optString("status", WorkerApplicationStatus.Pending.name)) }
                .getOrElse { WorkerApplicationStatus.Pending }
            result.add(
                WorkerApplicationUiModel(
                    id = obj.optInt("id", 0),
                    name = obj.optString("name", ""),
                    email = obj.optString("email", ""),
                    phoneNumber = obj.optString("phoneNumber", ""),
                    location = obj.optString("location", ""),
                    streetHomeNumber = obj.optString("streetHomeNumber", ""),
                    alternativeLocation = obj.optString("alternativeLocation", ""),
                    gender = obj.optString("gender", ""),
                    profession = obj.optString("profession", ""),
                    experienceYears = obj.optString("experienceYears", ""),
                    passwordHash = obj.optString("passwordHash", ""),
                    cvUri = obj.optString("cvUri", ""),
                    cvFileName = obj.optString("cvFileName", ""),
                    cvSizeBytes = obj.optLong("cvSizeBytes", 0L),
                    status = status
                )
            )
        }
        return result
    }

    fun saveWorkerApplications(context: Context, applications: List<WorkerApplicationUiModel>) {
        val arr = JSONArray()
        applications.forEach { app ->
            val obj = JSONObject()
            obj.put("id", app.id)
            obj.put("name", app.name)
            obj.put("email", app.email)
            obj.put("phoneNumber", app.phoneNumber)
            obj.put("location", app.location)
            obj.put("streetHomeNumber", app.streetHomeNumber)
            obj.put("alternativeLocation", app.alternativeLocation)
            obj.put("gender", app.gender)
            obj.put("profession", app.profession)
            obj.put("experienceYears", app.experienceYears)
            obj.put("passwordHash", app.passwordHash)
            obj.put("cvUri", app.cvUri)
            obj.put("cvFileName", app.cvFileName)
            obj.put("cvSizeBytes", app.cvSizeBytes)
            obj.put("status", app.status.name)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_WORKER_APPLICATIONS_JSON, arr.toString()).apply()
    }

    fun loadWorkers(context: Context): List<WorkerUiModel> {
        val raw = prefs(context).getString(KEY_WORKERS_JSON, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val result = ArrayList<WorkerUiModel>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            result.add(
                WorkerUiModel(
                    id = obj.optInt("id", 0),
                    name = obj.optString("name", ""),
                    status = obj.optString("status", "Active"),
                    email = obj.optString("email", ""),
                    phoneNumber = obj.optString("phoneNumber", ""),
                    location = obj.optString("location", ""),
                    streetHomeNumber = obj.optString("streetHomeNumber", ""),
                    alternativeLocation = obj.optString("alternativeLocation", ""),
                    gender = obj.optString("gender", ""),
                    profession = obj.optString("profession", ""),
                    experienceYears = obj.optString("experienceYears", ""),
                    passwordHash = obj.optString("passwordHash", "")
                )
            )
        }
        return result
    }

    fun saveWorkers(context: Context, workers: List<WorkerUiModel>) {
        val arr = JSONArray()
        workers.forEach { worker ->
            val obj = JSONObject()
            obj.put("id", worker.id)
            obj.put("name", worker.name)
            obj.put("status", worker.status)
            obj.put("email", worker.email)
            obj.put("phoneNumber", worker.phoneNumber)
            obj.put("location", worker.location)
            obj.put("streetHomeNumber", worker.streetHomeNumber)
            obj.put("alternativeLocation", worker.alternativeLocation)
            obj.put("gender", worker.gender)
            obj.put("profession", worker.profession)
            obj.put("experienceYears", worker.experienceYears)
            obj.put("passwordHash", worker.passwordHash)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_WORKERS_JSON, arr.toString()).apply()
    }
}
