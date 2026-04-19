package np.com.ai_poweredhomeservicehiringplatform.common.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WorkStatus {
    Pending,
    Booked,
    Completed
}

enum class WorkerApplicationStatus {
    Pending,
    Approved,
    Rejected
}

enum class PaymentStatus {
    Pending,
    Paid
}

enum class PaymentMethod {
    Esewa,
    Khalti,
    CashOnDelivery
}

@Entity(tableName = "users")
data class UserUiModel(
    @PrimaryKey
    val id: Int,
    val name: String,
    val status: String,
    val email: String,
    val phoneNumber: String,
    val location: String,
    val streetHomeNumber: String,
    val alternativeLocation: String,
    val passwordHash: String
)

@Entity(tableName = "workers")
data class WorkerUiModel(
    @PrimaryKey
    val id: Int,
    val name: String,
    val status: String,
    val email: String,
    val phoneNumber: String,
    val location: String,
    val streetHomeNumber: String,
    val alternativeLocation: String,
    val gender: String,
    val profession: String,
    val experienceYears: String,
    val passwordHash: String
)

@Entity(tableName = "worker_applications")
data class WorkerApplicationUiModel(
    @PrimaryKey
    val id: Int,
    val name: String,
    val email: String,
    val phoneNumber: String,
    val location: String,
    val streetHomeNumber: String,
    val alternativeLocation: String,
    val gender: String,
    val profession: String,
    val experienceYears: String,
    val passwordHash: String,
    val cvUri: String?,
    val cvFileName: String?,
    val cvSizeBytes: Long?,
    val status: WorkerApplicationStatus
)

@Entity(tableName = "works")
data class WorkUiModel(
    @PrimaryKey
    val id: Int,
    val workName: String,
    val detail: String,
    val workerName: String?,
    val status: WorkStatus
)

@Entity(tableName = "user_jobs")
data class UserJobUiModel(
    @PrimaryKey
    val id: Int,
    val userEmail: String,
    val service: String,
    val description: String,
    val location: String,
    val streetHomeNumber: String,
    val alternativeLocation: String,
    val status: WorkStatus
)

@Entity(tableName = "notifications")
data class NotificationUiModel(
    @PrimaryKey
    val id: Int,
    val userEmail: String,
    val title: String,
    val message: String,
    val timestampMillis: Long
)

@Entity(tableName = "payments")
data class PaymentUiModel(
    @PrimaryKey
    val id: Int,
    val workId: Int,
    val userEmail: String,
    val amountNpr: Int,
    val method: PaymentMethod?,
    val status: PaymentStatus,
    val timestampMillis: Long
)

@Entity(tableName = "ratings")
data class RatingUiModel(
    @PrimaryKey
    val id: Int,
    val workId: Int,
    val userEmail: String,
    val workerName: String,
    val profession: String,
    val stars: Int,
    val review: String,
    val timestampMillis: Long
)

@Entity(tableName = "worker_settings")
data class WorkerSettingsUiModel(
    @PrimaryKey
    val workerEmail: String,
    val available: Boolean,
    val serviceAreasJson: String,
    val payoutMethod: PaymentMethod?,
    val payoutAccount: String
)
