package np.com.ai_poweredhomeservicehiringplatform.common.model

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

data class UserUiModel(
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

data class WorkerUiModel(
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

data class WorkerApplicationUiModel(
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
    val cvUri: String,
    val cvFileName: String,
    val cvSizeBytes: Long,
    val status: WorkerApplicationStatus
)

data class WorkUiModel(
    val id: Int,
    val workName: String,
    val detail: String,
    val workerName: String?,
    val status: WorkStatus
)

data class UserJobUiModel(
    val id: Int,
    val userEmail: String,
    val service: String,
    val description: String,
    val location: String,
    val streetHomeNumber: String,
    val alternativeLocation: String,
    val status: WorkStatus
)

data class NotificationUiModel(
    val id: Int,
    val userEmail: String,
    val title: String,
    val message: String,
    val timestampMillis: Long
)

data class PaymentUiModel(
    val id: Int,
    val workId: Int,
    val userEmail: String,
    val amountNpr: Int,
    val method: PaymentMethod?,
    val status: PaymentStatus,
    val timestampMillis: Long
)
