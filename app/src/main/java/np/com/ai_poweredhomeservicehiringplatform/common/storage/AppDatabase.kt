package np.com.ai_poweredhomeservicehiringplatform.common.storage

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Transaction
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

class RoomConverters {
    @TypeConverter
    fun workStatusToString(value: WorkStatus): String = value.name

    @TypeConverter
    fun stringToWorkStatus(value: String): WorkStatus = runCatching { WorkStatus.valueOf(value) }.getOrElse { WorkStatus.Pending }

    @TypeConverter
    fun workerApplicationStatusToString(value: WorkerApplicationStatus): String = value.name

    @TypeConverter
    fun stringToWorkerApplicationStatus(value: String): WorkerApplicationStatus =
        runCatching { WorkerApplicationStatus.valueOf(value) }.getOrElse { WorkerApplicationStatus.Pending }

    @TypeConverter
    fun paymentStatusToString(value: PaymentStatus): String = value.name

    @TypeConverter
    fun stringToPaymentStatus(value: String): PaymentStatus = runCatching { PaymentStatus.valueOf(value) }.getOrElse { PaymentStatus.Pending }

    @TypeConverter
    fun paymentMethodToString(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun stringToPaymentMethod(value: String?): PaymentMethod? {
        val v = value?.trim().orEmpty()
        if (v.isBlank()) return null
        return runCatching { PaymentMethod.valueOf(v) }.getOrNull()
    }
}

@Dao
interface AppDao {
    @Query("SELECT * FROM users")
    fun getUsers(): List<UserUiModel>

    @Query("DELETE FROM users")
    fun clearUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(users: List<UserUiModel>)

    @Transaction
    fun replaceUsers(users: List<UserUiModel>) {
        clearUsers()
        insertUsers(users)
    }

    @Query("SELECT * FROM workers")
    fun getWorkers(): List<WorkerUiModel>

    @Query("DELETE FROM workers")
    fun clearWorkers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkers(workers: List<WorkerUiModel>)

    @Transaction
    fun replaceWorkers(workers: List<WorkerUiModel>) {
        clearWorkers()
        insertWorkers(workers)
    }

    @Query("SELECT * FROM worker_applications")
    fun getWorkerApplications(): List<WorkerApplicationUiModel>

    @Query("DELETE FROM worker_applications")
    fun clearWorkerApplications()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorkerApplications(applications: List<WorkerApplicationUiModel>)

    @Transaction
    fun replaceWorkerApplications(applications: List<WorkerApplicationUiModel>) {
        clearWorkerApplications()
        insertWorkerApplications(applications)
    }

    @Query("SELECT * FROM works")
    fun getWorks(): List<WorkUiModel>

    @Query("DELETE FROM works")
    fun clearWorks()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertWorks(works: List<WorkUiModel>)

    @Transaction
    fun replaceWorks(works: List<WorkUiModel>) {
        clearWorks()
        insertWorks(works)
    }

    @Query("SELECT * FROM user_jobs")
    fun getUserJobs(): List<UserJobUiModel>

    @Query("DELETE FROM user_jobs")
    fun clearUserJobs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserJobs(jobs: List<UserJobUiModel>)

    @Transaction
    fun replaceUserJobs(jobs: List<UserJobUiModel>) {
        clearUserJobs()
        insertUserJobs(jobs)
    }

    @Query("SELECT * FROM notifications")
    fun getNotifications(): List<NotificationUiModel>

    @Query("DELETE FROM notifications")
    fun clearNotifications()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotifications(notifications: List<NotificationUiModel>)

    @Transaction
    fun replaceNotifications(notifications: List<NotificationUiModel>) {
        clearNotifications()
        insertNotifications(notifications)
    }

    @Query("SELECT * FROM payments")
    fun getPayments(): List<PaymentUiModel>

    @Query("DELETE FROM payments")
    fun clearPayments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPayments(payments: List<PaymentUiModel>)

    @Transaction
    fun replacePayments(payments: List<PaymentUiModel>) {
        clearPayments()
        insertPayments(payments)
    }

    @Query("SELECT * FROM ratings")
    fun getRatings(): List<RatingUiModel>

    @Query("DELETE FROM ratings")
    fun clearRatings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRatings(ratings: List<RatingUiModel>)

    @Transaction
    fun replaceRatings(ratings: List<RatingUiModel>) {
        clearRatings()
        insertRatings(ratings)
    }

    @Query("SELECT * FROM worker_settings WHERE workerEmail = :workerEmail LIMIT 1")
    fun getWorkerSettings(workerEmail: String): WorkerSettingsUiModel?

    @Query("SELECT * FROM worker_settings")
    fun getAllWorkerSettings(): List<WorkerSettingsUiModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertWorkerSettings(settings: WorkerSettingsUiModel)
}

@Database(
    entities = [
        UserUiModel::class,
        WorkerUiModel::class,
        WorkerApplicationUiModel::class,
        WorkUiModel::class,
        UserJobUiModel::class,
        NotificationUiModel::class,
        PaymentUiModel::class,
        RatingUiModel::class,
        WorkerSettingsUiModel::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
