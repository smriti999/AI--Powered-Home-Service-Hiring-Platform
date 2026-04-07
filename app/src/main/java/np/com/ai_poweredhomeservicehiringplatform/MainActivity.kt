package np.com.ai_poweredhomeservicehiringplatform

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

private const val AUTH_PREFS = "auth_prefs"
private const val KEY_SEEDED_ADMIN_USERNAME = "seeded_admin_username"
private const val KEY_SEEDED_ADMIN_PASSWORD_HASH = "seeded_admin_password_hash"
private const val KEY_USERS_JSON = "users_json"

private fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun normalizePhoneNumber(value: String): String {
    return value.filter { it.isDigit() }.take(10)
}

private fun normalizeGmailEmail(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return ""
    val prefix = trimmed.substringBefore("@").replace(" ", "")
    if (prefix.isBlank()) return ""
    return "$prefix@gmail.com"
}

private fun seedAdminIfNeeded(context: Context) {
    val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
    val hasUsername = prefs.contains(KEY_SEEDED_ADMIN_USERNAME)
    val hasPasswordHash = prefs.contains(KEY_SEEDED_ADMIN_PASSWORD_HASH)
    if (hasUsername && hasPasswordHash) return

    prefs.edit()
        .putString(KEY_SEEDED_ADMIN_USERNAME, "admin")
        .putString(KEY_SEEDED_ADMIN_PASSWORD_HASH, sha256Hex("admin123"))
        .apply()
}

private fun isSeededAdminCredentialValid(
    context: Context,
    username: String,
    password: String
): Boolean {
    val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
    val seededUsername = prefs.getString(KEY_SEEDED_ADMIN_USERNAME, null) ?: return false
    val seededPasswordHash = prefs.getString(KEY_SEEDED_ADMIN_PASSWORD_HASH, null) ?: return false
    return username == seededUsername && sha256Hex(password) == seededPasswordHash
}

private fun loadUsers(context: Context): List<UserUiModel> {
    val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_USERS_JSON, "[]") ?: "[]"
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

private fun saveUsers(context: Context, users: List<UserUiModel>) {
    val prefs = context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
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
    prefs.edit().putString(KEY_USERS_JSON, arr.toString()).apply()
}

private enum class AppScreen {
    AuthLogin,
    AuthSignUp,
    UserHome,
    UserCreateJob,
    UserJobs,
    AdminLogin,
    AdminDashboard,
    AdminRequests,
    AdminWorkerManagement,
    AdminUserManagement,
    AdminWorkManagement,
    WorkerApply
}

private enum class SignUpRole {
    User,
    Worker
}

enum class WorkerRequestDecision {
    Pending,
    Approved,
    Rejected
}

enum class WorkStatus {
    Pending,
    Booked,
    Completed
}

data class WorkerRequestUiModel(
    val id: Int,
    val name: String,
    val role: String,
    val experienceYears: Int,
    val decision: WorkerRequestDecision = WorkerRequestDecision.Pending
)

data class WorkerUiModel(
    val id: Int,
    val name: String,
    val status: String
)

data class UserUiModel(
    val id: Int,
    val name: String,
    val status: String,
    val email: String = "",
    val phoneNumber: String = "",
    val location: String = "",
    val streetHomeNumber: String = "",
    val alternativeLocation: String = "",
    val passwordHash: String = ""
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedAdminIfNeeded(this)
        enableEdgeToEdge()
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                val context = LocalContext.current
                var requests by remember { mutableStateOf(listOf<WorkerRequestUiModel>()) }
                var workers by remember { mutableStateOf(listOf<WorkerUiModel>()) }
                val initialUsers = remember { loadUsers(context) }
                var users by remember { mutableStateOf(initialUsers) }
                var works by remember { mutableStateOf(listOf<WorkUiModel>()) }
                var userJobs by remember { mutableStateOf(listOf<UserJobUiModel>()) }

                var isAdminLoggedIn by rememberSaveable { mutableStateOf(false) }
                var isUserLoggedIn by rememberSaveable { mutableStateOf(false) }
                var currentUserEmail by rememberSaveable { mutableStateOf<String?>(null) }
                var screen by rememberSaveable { mutableStateOf(AppScreen.AuthLogin) }
                var pendingAdminDestination by rememberSaveable { mutableStateOf<AppScreen?>(null) }

                fun openAdmin(destination: AppScreen) {
                    if (isAdminLoggedIn) {
                        screen = destination
                    } else {
                        pendingAdminDestination = destination
                        screen = AppScreen.AdminLogin
                    }
                }

                val adminOnlyScreens = setOf(
                    AppScreen.AdminDashboard,
                    AppScreen.AdminRequests,
                    AppScreen.AdminWorkerManagement,
                    AppScreen.AdminUserManagement,
                    AppScreen.AdminWorkManagement
                )
                val userOnlyScreens = setOf(
                    AppScreen.UserHome,
                    AppScreen.UserCreateJob,
                    AppScreen.UserJobs
                )
                val effectiveScreen = when {
                    !isAdminLoggedIn && screen in adminOnlyScreens -> {
                        pendingAdminDestination = screen
                        AppScreen.AdminLogin
                    }

                    !isUserLoggedIn && screen in userOnlyScreens -> AppScreen.AuthLogin
                    else -> screen
                }

                when (effectiveScreen) {
                    AppScreen.AuthLogin -> {
                        AuthLoginScreen(
                            onAdminLogoClick = {
                                pendingAdminDestination = AppScreen.AdminDashboard
                                screen = AppScreen.AdminLogin
                            },
                            onLogin = { email, password ->
                                val trimmedEmail = email.trim()
                                val hashedPassword = sha256Hex(password)
                                users.any { it.email.equals(trimmedEmail, ignoreCase = true) && it.passwordHash == hashedPassword }
                            },
                            onLoginSuccess = { email ->
                                isUserLoggedIn = true
                                currentUserEmail = email
                                screen = AppScreen.UserHome
                            },
                            onSignUpClick = {
                                screen = AppScreen.AuthSignUp
                            }
                        )
                    }

                    AppScreen.AuthSignUp -> {
                        AuthSignUpScreen(
                            existingUserEmails = users.map { it.email.lowercase() }.toSet(),
                            onUserSignUp = { fullName, email, phoneNumber, location, streetHomeNumber, alternativeLocation, password ->
                                val nextId = (users.maxOfOrNull { it.id } ?: 0) + 1
                                val newUser = UserUiModel(
                                    id = nextId,
                                    name = fullName,
                                    status = "Active",
                                    email = email.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    location = location,
                                    streetHomeNumber = streetHomeNumber.trim(),
                                    alternativeLocation = alternativeLocation.trim(),
                                    passwordHash = sha256Hex(password)
                                )
                                val updatedUsers = users + newUser
                                users = updatedUsers
                                saveUsers(context, updatedUsers)
                                screen = AppScreen.AuthLogin
                            },
                            onBackToLoginClick = { screen = AppScreen.AuthLogin }
                        )
                    }

                    AppScreen.UserHome -> {
                        val email = currentUserEmail ?: ""
                        UserHomeScreen(
                            userEmail = email,
                            onCreateJobClick = { screen = AppScreen.UserCreateJob },
                            onMyJobsClick = { screen = AppScreen.UserJobs },
                            onLogoutClick = {
                                isUserLoggedIn = false
                                currentUserEmail = null
                                screen = AppScreen.AuthLogin
                            }
                        )
                    }

                    AppScreen.UserCreateJob -> {
                        val email = currentUserEmail ?: ""
                        UserCreateJobScreen(
                            userEmail = email,
                            onBackClick = { screen = AppScreen.UserHome },
                            onSubmit = { service, description, location, streetHomeNumber, alternativeLocation ->
                                val nextJobId = (userJobs.maxOfOrNull { it.id } ?: 0) + 1
                                val nextWorkId = (works.maxOfOrNull { it.id } ?: 0) + 1

                                val newJob = UserJobUiModel(
                                    id = nextJobId,
                                    userEmail = email,
                                    service = service,
                                    description = description,
                                    location = location,
                                    streetHomeNumber = streetHomeNumber,
                                    alternativeLocation = alternativeLocation,
                                    status = WorkStatus.Pending
                                )
                                userJobs = userJobs + newJob

                                val detailText = buildString {
                                    append("User: ")
                                    append(email)
                                    append("\nLocation: ")
                                    append(location)
                                    append("\nStreet/Home: ")
                                    append(streetHomeNumber)
                                    if (alternativeLocation.isNotBlank()) {
                                        append("\nAlt: ")
                                        append(alternativeLocation)
                                    }
                                    append("\n\n")
                                    append(description)
                                }
                                works = works + WorkUiModel(
                                    id = nextWorkId,
                                    workName = service,
                                    detail = detailText,
                                    workerName = null,
                                    status = WorkStatus.Pending
                                )

                                screen = AppScreen.UserJobs
                            }
                        )
                    }

                    AppScreen.UserJobs -> {
                        val email = currentUserEmail ?: ""
                        UserJobsScreen(
                            userEmail = email,
                            jobs = userJobs.filter { it.userEmail.equals(email, ignoreCase = true) },
                            onBackClick = { screen = AppScreen.UserHome }
                        )
                    }

                    AppScreen.AdminLogin -> {
                        AdminLoginScreen(
                            onLogoClick = {
                                pendingAdminDestination = null
                                screen = AppScreen.AuthLogin
                            },
                            onLoginClick = { _, _ ->
                                isAdminLoggedIn = true
                                screen = pendingAdminDestination ?: AppScreen.AdminDashboard
                                pendingAdminDestination = null
                            }
                        )
                    }

                    AppScreen.AdminDashboard -> {
                        AdminDashboardScreen(
                            users = users,
                            workers = workers,
                            works = works,
                            onRequestsClick = { openAdmin(AppScreen.AdminRequests) },
                            onWorkersClick = { openAdmin(AppScreen.AdminWorkerManagement) },
                            onUsersClick = { openAdmin(AppScreen.AdminUserManagement) },
                            onWorksClick = { openAdmin(AppScreen.AdminWorkManagement) },
                            onLogoutClick = {
                                isAdminLoggedIn = false
                                pendingAdminDestination = null
                                screen = AppScreen.AuthLogin
                            }
                        )
                    }

                    AppScreen.AdminRequests -> {
                        AdminRequestWorkerScreen(
                            requests = requests,
                            onUpdateDecision = { requestId, decision ->
                                requests = requests.map { existing ->
                                    if (existing.id == requestId) existing.copy(decision = decision) else existing
                                }
                            },
                            onDashboardClick = { openAdmin(AppScreen.AdminDashboard) },
                            onWorkersClick = { openAdmin(AppScreen.AdminWorkerManagement) },
                            onUsersClick = { openAdmin(AppScreen.AdminUserManagement) },
                            onWorksClick = { openAdmin(AppScreen.AdminWorkManagement) }
                        )
                    }

                    AppScreen.AdminWorkerManagement -> {
                        AdminWorkerManagementScreen(
                            workers = workers,
                            onDeleteWorker = { workerId ->
                                workers = workers.filterNot { it.id == workerId }
                            },
                            onDashboardClick = { openAdmin(AppScreen.AdminDashboard) },
                            onRequestsClick = { openAdmin(AppScreen.AdminRequests) },
                            onUsersClick = { openAdmin(AppScreen.AdminUserManagement) },
                            onWorksClick = { openAdmin(AppScreen.AdminWorkManagement) }
                        )
                    }

                    AppScreen.AdminUserManagement -> {
                        AdminUserManagementScreen(
                            users = users,
                            onDeleteUser = { userId ->
                                val updatedUsers = users.filterNot { it.id == userId }
                                users = updatedUsers
                                saveUsers(context, updatedUsers)
                            },
                            onDashboardClick = { openAdmin(AppScreen.AdminDashboard) },
                            onRequestsClick = { openAdmin(AppScreen.AdminRequests) },
                            onWorkersClick = { openAdmin(AppScreen.AdminWorkerManagement) },
                            onWorksClick = { openAdmin(AppScreen.AdminWorkManagement) }
                        )
                    }

                    AppScreen.AdminWorkManagement -> {
                        AdminWorkManagementScreen(
                            works = works,
                            onDashboardClick = { openAdmin(AppScreen.AdminDashboard) },
                            onRequestsClick = { openAdmin(AppScreen.AdminRequests) },
                            onWorkersClick = { openAdmin(AppScreen.AdminWorkerManagement) },
                            onUsersClick = { openAdmin(AppScreen.AdminUserManagement) }
                        )
                    }

                    AppScreen.WorkerApply -> {
                        WorkerApplyScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthLoginScreen(
    modifier: Modifier = Modifier,
    onAdminLogoClick: () -> Unit = { },
    onLogin: (email: String, password: String) -> Boolean = { _, _ -> false },
    onLoginSuccess: (email: String) -> Unit = { },
    onSignUpClick: () -> Unit = { }
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoginSuccessful by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clickable(onClick = onAdminLogoClick)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = normalizeGmailEmail(it)
                    errorMessage = null
                    isLoginSuccessful = false
                },
                placeholder = { Text(text = "Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                    isLoginSuccessful = false
                },
                placeholder = { Text(text = "Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(10.dp))
            } else if (isLoginSuccessful) {
                Text(text = "Login successful")
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password required"
                        isLoginSuccessful = false
                        return@Button
                    }

                    val ok = onLogin(trimmedEmail, password)
                    if (!ok) {
                        errorMessage = "Invalid credentials"
                        isLoginSuccessful = false
                        return@Button
                    }

                    errorMessage = null
                    isLoginSuccessful = true
                    onLoginSuccess(trimmedEmail)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(44.dp)
            ) {
                Text(text = "LOGIN")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Or sign up with Google",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onSignUpClick) {
                Text(text = "Sign Up")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthLoginPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AuthLoginScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    onCreateJobClick: () -> Unit = { },
    onMyJobsClick: () -> Unit = { },
    onLogoutClick: () -> Unit = { }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Home") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Text(text = "Logout", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onCreateJobClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(46.dp)
            ) {
                Text(text = "CREATE JOB")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onMyJobsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(46.dp)
            ) {
                Text(text = "MY JOBS")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCreateJobScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    onBackClick: () -> Unit = { },
    onSubmit: (
        service: String,
        description: String,
        location: String,
        streetHomeNumber: String,
        alternativeLocation: String
    ) -> Unit = { _, _, _, _, _ -> }
) {
    var service by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var streetHomeNumber by rememberSaveable { mutableStateOf("") }
    var alternativeLocation by rememberSaveable { mutableStateOf("") }
    var isLocationMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val locationOptions = remember { listOf("Kathmandu", "Bhaktapur", "Lalitpur") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Create Job") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "Back", color = MaterialTheme.colorScheme.onPrimary)
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
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = service,
                onValueChange = {
                    service = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Service (e.g. Plumber)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Job Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text(text = "Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLocationMenuExpanded = true },
                    trailingIcon = {
                        TextButton(onClick = { isLocationMenuExpanded = true }) {
                            Text(text = "▼")
                        }
                    }
                )

                DropdownMenu(
                    expanded = isLocationMenuExpanded,
                    onDismissRequest = { isLocationMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    locationOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            onClick = {
                                location = option
                                isLocationMenuExpanded = false
                                errorMessage = null
                            }
                        )
                    }
                }
            }

            if (location.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = streetHomeNumber,
                    onValueChange = {
                        streetHomeNumber = it
                        errorMessage = null
                    },
                    placeholder = { Text(text = "Street, Home Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = alternativeLocation,
                    onValueChange = {
                        alternativeLocation = it
                        errorMessage = null
                    },
                    placeholder = { Text(text = "Alternative Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val s = service.trim()
                    val d = description.trim()
                    val st = streetHomeNumber.trim()
                    val alt = alternativeLocation.trim()

                    if (s.isBlank() || d.isBlank() || location.isBlank() || st.isBlank()) {
                        errorMessage = "All fields are required"
                        return@Button
                    }

                    errorMessage = null
                    onSubmit(s, d, location, st, alt)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(46.dp)
            ) {
                Text(text = "SUBMIT")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserJobsScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    jobs: List<UserJobUiModel>,
    onBackClick: () -> Unit = { }
) {
    fun statusColor(status: WorkStatus): Color {
        return when (status) {
            WorkStatus.Pending -> Color(0xFFF9A825)
            WorkStatus.Booked -> Color(0xFF1565C0)
            WorkStatus.Completed -> Color(0xFF2E7D32)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "My Jobs") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onBackClick) {
                        Text(text = "Back", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (jobs.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No jobs yet")
                }
            } else {
                jobs.forEach { job ->
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = job.service,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = job.status.name,
                                    color = statusColor(job.status),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = job.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Location: ${job.location}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Street/Home: ${job.streetHomeNumber}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (job.alternativeLocation.isNotBlank()) {
                                Text(
                                    text = "Alt: ${job.alternativeLocation}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSignUpScreen(
    modifier: Modifier = Modifier,
    existingUserEmails: Set<String> = emptySet(),
    onUserSignUp: (
        fullName: String,
        email: String,
        phoneNumber: String,
        location: String,
        streetHomeNumber: String,
        alternativeLocation: String,
        password: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onBackToLoginClick: () -> Unit = { }
) {
    val context = LocalContext.current
    var role by rememberSaveable { mutableStateOf(SignUpRole.User) }

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var streetHomeNumber by rememberSaveable { mutableStateOf("") }
    var alternativeLocation by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var profession by rememberSaveable { mutableStateOf("") }
    var experienceYears by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var workerErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showWorkerThankYouDialog by rememberSaveable { mutableStateOf(false) }
    var workerCvUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var workerCvFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var workerCvSizeBytes by rememberSaveable { mutableStateOf<Long?>(null) }

    val titleText = if (role == SignUpRole.User) "User Sign Up" else "Worker Registration"
    val locationOptions = remember { listOf("Kathmandu", "Bhaktapur", "Lalitpur") }
    val professionOptions = remember {
        listOf(
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
    }
    var isLocationMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isProfessionMenuExpanded by rememberSaveable { mutableStateOf(false) }

    fun getFileNameAndSize(uri: Uri): Pair<String?, Long?> {
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val moved = cursor.moveToFirst()
                val name = if (moved && nameIndex >= 0) cursor.getString(nameIndex) else null
                val size = if (moved && sizeIndex >= 0) cursor.getLong(sizeIndex) else null
                name to size
            }
        }.getOrNull() ?: (null to null)
    }

    val cvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val (name, sizeBytes) = getFileNameAndSize(uri)
        val minBytes = 2L * 1024L * 1024L
        val maxBytes = 5L * 1024L * 1024L
        val isValidSize = sizeBytes != null && sizeBytes in minBytes..maxBytes

        if (!isValidSize) {
            workerCvUriString = null
            workerCvFileName = null
            workerCvSizeBytes = null
            workerErrorMessage = "CV must be a PDF between 2MB and 5MB"
            return@rememberLauncherForActivityResult
        }

        workerCvUriString = uri.toString()
        workerCvFileName = name ?: uri.lastPathSegment
        workerCvSizeBytes = sizeBytes
        workerErrorMessage = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = titleText) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    TextButton(onClick = onBackToLoginClick) {
                        Text(text = "Login", color = MaterialTheme.colorScheme.onPrimary)
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
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val selectedColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                val unselectedColors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )

                if (role == SignUpRole.User) {
                    Button(
                        onClick = { role = SignUpRole.User },
                        colors = selectedColors,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "User")
                    }
                } else {
                    OutlinedButton(
                        onClick = { role = SignUpRole.User },
                        colors = unselectedColors,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "User")
                    }
                }

                if (role == SignUpRole.Worker) {
                    Button(
                        onClick = { role = SignUpRole.Worker },
                        colors = selectedColors,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Worker")
                    }
                } else {
                    OutlinedButton(
                        onClick = { role = SignUpRole.Worker },
                        colors = unselectedColors,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Worker")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = fullName,
            onValueChange = {
                fullName = it
                errorMessage = null
                workerErrorMessage = null
            },
                placeholder = { Text(text = "Full Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (role == SignUpRole.User) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = normalizeGmailEmail(it)
                        errorMessage = null
                    },
                    placeholder = { Text(text = "Email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = normalizePhoneNumber(it)
                        errorMessage = null
                    },
                    placeholder = { Text(text = "Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text(text = "Location") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLocationMenuExpanded = true },
                        trailingIcon = {
                            TextButton(onClick = { isLocationMenuExpanded = true }) {
                                Text(text = "▼")
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = isLocationMenuExpanded,
                        onDismissRequest = { isLocationMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        locationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    location = option
                                    isLocationMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = streetHomeNumber,
                        onValueChange = { streetHomeNumber = it },
                        placeholder = { Text(text = "Street, Home Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = alternativeLocation,
                        onValueChange = { alternativeLocation = it },
                        placeholder = { Text(text = "Alternative Location") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text(text = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = { Text(text = "Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(26.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        val trimmedName = fullName.trim()
                        val trimmedEmail = email.trim()
                        val trimmedPhone = phoneNumber.trim()
                        val trimmedStreet = streetHomeNumber.trim()
                        val trimmedAlt = alternativeLocation.trim()

                        if (trimmedName.isBlank() ||
                            trimmedEmail.isBlank() ||
                            trimmedPhone.isBlank() ||
                            location.isBlank() ||
                            trimmedStreet.isBlank() ||
                            password.isBlank() ||
                            confirmPassword.isBlank()
                        ) {
                            errorMessage = "All fields are required"
                            return@Button
                        }

                        if (existingUserEmails.contains(trimmedEmail.lowercase())) {
                            errorMessage = "Email already registered"
                            return@Button
                        }

                        if (!trimmedEmail.lowercase().endsWith("@gmail.com")) {
                            errorMessage = "Email must end with @gmail.com"
                            return@Button
                        }

                        if (trimmedPhone.length != 10) {
                            errorMessage = "Phone number must be 10 digits"
                            return@Button
                        }

                        if (password != confirmPassword) {
                            errorMessage = "Password does not match"
                            return@Button
                        }

                        errorMessage = null
                        onUserSignUp(trimmedName, trimmedEmail, trimmedPhone, location, trimmedStreet, trimmedAlt, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .height(46.dp)
                ) {
                    Text(text = "SIGN UP")
                }
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = normalizeGmailEmail(it)
                        workerErrorMessage = null
                    },
                    placeholder = { Text(text = "Email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        phoneNumber = normalizePhoneNumber(it)
                        workerErrorMessage = null
                    },
                    placeholder = { Text(text = "Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                ) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text(text = "Location") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isLocationMenuExpanded = true },
                        trailingIcon = {
                            TextButton(onClick = { isLocationMenuExpanded = true }) {
                                Text(text = "▼")
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = isLocationMenuExpanded,
                        onDismissRequest = { isLocationMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        locationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    location = option
                                    isLocationMenuExpanded = false
                                    workerErrorMessage = null
                                }
                            )
                        }
                    }
                }

                if (location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = streetHomeNumber,
                    onValueChange = {
                        streetHomeNumber = it
                        workerErrorMessage = null
                    },
                        placeholder = { Text(text = "Street, Home Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = alternativeLocation,
                    onValueChange = {
                        alternativeLocation = it
                        workerErrorMessage = null
                    },
                        placeholder = { Text(text = "Alternative Location") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 360.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gender,
                    onValueChange = {
                        gender = it
                        workerErrorMessage = null
                    },
                    placeholder = { Text(text = "Gender") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                ) {
                    OutlinedTextField(
                        value = profession,
                        onValueChange = { },
                        readOnly = true,
                        placeholder = { Text(text = "Profession") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProfessionMenuExpanded = true },
                        trailingIcon = {
                            TextButton(onClick = { isProfessionMenuExpanded = true }) {
                                Text(text = "▼")
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = isProfessionMenuExpanded,
                        onDismissRequest = { isProfessionMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        professionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    profession = option
                                    isProfessionMenuExpanded = false
                                    workerErrorMessage = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = experienceYears,
                    onValueChange = {
                        experienceYears = it
                        workerErrorMessage = null
                    },
                    placeholder = { Text(text = "Experience (Years)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        workerErrorMessage = null
                    },
                    placeholder = { Text(text = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { cvPickerLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .height(44.dp)
                ) {
                    Text(text = "UPLOAD CV (PDF)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                val sizeMb = workerCvSizeBytes?.let { (it.toDouble() / (1024.0 * 1024.0)) }
                Text(
                    text = if (workerCvUriString != null) {
                        val name = workerCvFileName ?: "CV selected"
                        val mb = sizeMb?.let { String.format("%.2f", it) } ?: "-"
                        "Selected: $name ($mb MB)"
                    } else {
                        "No CV selected"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (workerErrorMessage != null) {
                    Text(
                        text = workerErrorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        val trimmedName = fullName.trim()
                        val trimmedEmail = email.trim()
                        val trimmedPhone = phoneNumber.trim()
                        val trimmedStreet = streetHomeNumber.trim()
                        val trimmedAlt = alternativeLocation.trim()
                        val trimmedGender = gender.trim()
                        val trimmedExperience = experienceYears.trim()

                        val cvUri = workerCvUriString?.let { Uri.parse(it) }
                        val cvSize = workerCvSizeBytes

                        if (
                            trimmedName.isBlank() ||
                            trimmedEmail.isBlank() ||
                            trimmedPhone.isBlank() ||
                            location.isBlank() ||
                            trimmedStreet.isBlank() ||
                            trimmedGender.isBlank() ||
                            profession.isBlank() ||
                            trimmedExperience.isBlank() ||
                            password.isBlank() ||
                            cvUri == null ||
                            cvSize == null
                        ) {
                            workerErrorMessage = "All fields are required"
                            return@Button
                        }

                        if (!trimmedEmail.lowercase().endsWith("@gmail.com")) {
                            workerErrorMessage = "Email must end with @gmail.com"
                            return@Button
                        }

                        if (trimmedPhone.length != 10) {
                            workerErrorMessage = "Phone number must be 10 digits"
                            return@Button
                        }

                        val minBytes = 2L * 1024L * 1024L
                        val maxBytes = 5L * 1024L * 1024L
                        if (cvSize !in minBytes..maxBytes) {
                            workerErrorMessage = "CV must be a PDF between 2MB and 5MB"
                            return@Button
                        }

                        workerErrorMessage = null
                        showWorkerThankYouDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .height(46.dp)
                ) {
                    Text(text = "REGISTER")
                }
            }
        }
    }

    if (showWorkerThankYouDialog) {
        AlertDialog(
            onDismissRequest = { showWorkerThankYouDialog = false },
            title = { Text(text = "Thank you") },
            text = { Text(text = "Thank you for submitting. Please wait for 24 hours.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWorkerThankYouDialog = false
                        onBackToLoginClick()
                    }
                ) {
                    Text(text = "OK")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AuthSignUpPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AuthSignUpScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    modifier: Modifier = Modifier,
    onLogoClick: () -> Unit = { },
    onLoginClick: (username: String, password: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Admin Login") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "Welcome Admin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clickable(onClick = onLogoClick)
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Username") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .align(Alignment.CenterHorizontally)
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .align(Alignment.CenterHorizontally)
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(18.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val trimmedUsername = username.trim()
                    if (trimmedUsername.isBlank() || password.isBlank()) {
                        errorMessage = "Username and password required"
                        return@Button
                    }

                    if (!isSeededAdminCredentialValid(context, trimmedUsername, password)) {
                        errorMessage = "Invalid admin credentials"
                        return@Button
                    }

                    onLoginClick(trimmedUsername, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(44.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(text = "LOGIN")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminLoginPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AdminLoginScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    modifier: Modifier = Modifier,
    users: List<UserUiModel>,
    workers: List<WorkerUiModel>,
    works: List<WorkUiModel>,
    onRequestsClick: () -> Unit = { },
    onWorkersClick: () -> Unit = { },
    onUsersClick: () -> Unit = { },
    onWorksClick: () -> Unit = { },
    onLogoutClick: () -> Unit = { }
) {
    val totalUsers = users.size
    val activeWorkers = workers.count { it.status.equals("Active", ignoreCase = true) }
    val pendingJobs = works.count { it.status == WorkStatus.Pending }
    val revenue = 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Admin Dashboard") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Total Users", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = totalUsers.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Active Workers", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = activeWorkers.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Pending Jobs", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = pendingJobs.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Revenue", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "$$revenue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRequestsClick,
                    modifier = Modifier.weight(1f)
                ) { Text(text = "Requests") }
                Button(
                    onClick = onWorkersClick,
                    modifier = Modifier.weight(1f)
                ) { Text(text = "Workers") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onUsersClick,
                    modifier = Modifier.weight(1f)
                ) { Text(text = "Users") }
                Button(
                    onClick = onWorksClick,
                    modifier = Modifier.weight(1f)
                ) { Text(text = "Works") }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(text = "Recent Activity", fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No recent activity",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(text = "LOGOUT")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminDashboardPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AdminDashboardScreen(
            users = emptyList(),
            workers = emptyList(),
            works = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRequestWorkerScreen(
    modifier: Modifier = Modifier,
    requests: List<WorkerRequestUiModel>,
    onUpdateDecision: (requestId: Int, decision: WorkerRequestDecision) -> Unit = { _, _ -> },
    onDashboardClick: () -> Unit = { },
    onWorkersClick: () -> Unit = { },
    onUsersClick: () -> Unit = { },
    onWorksClick: () -> Unit = { }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Request Worker") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Row {
                        TextButton(onClick = onDashboardClick) {
                            Text(text = "Dashboard", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onWorkersClick) {
                            Text(text = "Workers", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onUsersClick) {
                            Text(text = "Users", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onWorksClick) {
                            Text(text = "Works", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (requests.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No applied applicant available")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                requests.forEach { item ->
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${item.name} (${item.role})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Experience: ${item.experienceYears} years",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val isDecisionMade = item.decision != WorkerRequestDecision.Pending

                            Column(horizontalAlignment = Alignment.Start) {
                                androidx.compose.foundation.layout.Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        enabled = !isDecisionMade,
                                        onClick = {
                                            onUpdateDecision(item.id, WorkerRequestDecision.Approved)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2E7D32),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color(0xFF2E7D32).copy(alpha = 0.35f),
                                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                                        )
                                    ) {
                                        Text(text = "Approve")
                                    }

                                    Button(
                                        enabled = !isDecisionMade,
                                        onClick = {
                                            onUpdateDecision(item.id, WorkerRequestDecision.Rejected)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD32F2F),
                                            contentColor = Color.White,
                                            disabledContainerColor = Color(0xFFD32F2F).copy(alpha = 0.35f),
                                            disabledContentColor = Color.White.copy(alpha = 0.8f)
                                        )
                                    ) {
                                        Text(text = "Reject")
                                    }
                                }

                                if (isDecisionMade) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Status: ${item.decision.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminRequestWorkerPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AdminRequestWorkerScreen(
            requests = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWorkerManagementScreen(
    modifier: Modifier = Modifier,
    workers: List<WorkerUiModel>,
    onDeleteWorker: (workerId: Int) -> Unit = { },
    onDashboardClick: () -> Unit = { },
    onRequestsClick: () -> Unit = { },
    onUsersClick: () -> Unit = { },
    onWorksClick: () -> Unit = { }
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredWorkers = workers.filter { worker ->
        val query = searchQuery.trim()
        query.isBlank() || worker.name.contains(query, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Worker Management") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Row {
                        TextButton(onClick = onDashboardClick) {
                            Text(text = "Dashboard", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onRequestsClick) {
                            Text(text = "Requests", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onUsersClick) {
                            Text(text = "Users", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onWorksClick) {
                            Text(text = "Works", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = "Search worker.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Total workers: ${workers.size}")

            Spacer(modifier = Modifier.height(12.dp))

            if (workers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No workers available")
                }
            } else if (filteredWorkers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No workers found")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredWorkers.forEach { worker ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = worker.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${worker.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onDeleteWorker(worker.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD32F2F),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .widthIn(min = 84.dp)
                                ) {
                                    Text(text = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminWorkerManagementPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AdminWorkerManagementScreen(
            workers = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    modifier: Modifier = Modifier,
    users: List<UserUiModel>,
    onDeleteUser: (userId: Int) -> Unit = { },
    onDashboardClick: () -> Unit = { },
    onRequestsClick: () -> Unit = { },
    onWorkersClick: () -> Unit = { },
    onWorksClick: () -> Unit = { }
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedUser by remember { mutableStateOf<UserUiModel?>(null) }

    val filteredUsers = users.filter { user ->
        val query = searchQuery.trim()
        query.isBlank() || user.name.contains(query, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "User Management") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Row {
                        TextButton(onClick = onDashboardClick) {
                            Text(text = "Dashboard", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onRequestsClick) {
                            Text(text = "Requests", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onWorkersClick) {
                            Text(text = "Workers", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onWorksClick) {
                            Text(text = "Works", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = "Search user.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Total users: ${users.size}")

            Spacer(modifier = Modifier.height(12.dp))

            if (users.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No users available")
                }
            } else if (filteredUsers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No users found")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredUsers.forEach { user ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable { selectedUser = user }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${user.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { onDeleteUser(user.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFD32F2F),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .widthIn(min = 84.dp)
                                ) {
                                    Text(text = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedUser != null) {
        val user = selectedUser ?: return
        AlertDialog(
            onDismissRequest = { selectedUser = null },
            title = { Text(text = user.name) },
            text = {
                Column {
                    Text(text = "Email: ${user.email.ifBlank { "-" }}")
                    Text(text = "Phone: ${user.phoneNumber.ifBlank { "-" }}")
                    Text(text = "Status: ${user.status.ifBlank { "-" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Location: ${user.location.ifBlank { "-" }}")
                    Text(text = "Street/Home: ${user.streetHomeNumber.ifBlank { "-" }}")
                    Text(text = "Alternative: ${user.alternativeLocation.ifBlank { "-" }}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedUser = null }) {
                    Text(text = "Close")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AdminUserManagementPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AdminUserManagementScreen(
            users = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWorkManagementScreen(
    modifier: Modifier = Modifier,
    works: List<WorkUiModel>,
    onDashboardClick: () -> Unit = { },
    onRequestsClick: () -> Unit = { },
    onWorkersClick: () -> Unit = { },
    onUsersClick: () -> Unit = { }
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredWorks = works.filter { work ->
        val query = searchQuery.trim()
        query.isBlank() ||
            work.workName.contains(query, ignoreCase = true) ||
            work.detail.contains(query, ignoreCase = true) ||
            (work.workerName?.contains(query, ignoreCase = true) == true)
    }

    fun statusColor(status: WorkStatus): Color {
        return when (status) {
            WorkStatus.Pending -> Color(0xFFF9A825)
            WorkStatus.Booked -> Color(0xFF1565C0)
            WorkStatus.Completed -> Color(0xFF2E7D32)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Work Management") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Row {
                        TextButton(onClick = onDashboardClick) {
                            Text(text = "Dashboard", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onRequestsClick) {
                            Text(text = "Requests", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onWorkersClick) {
                            Text(text = "Workers", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        TextButton(onClick = onUsersClick) {
                            Text(text = "Users", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = "Search work.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(text = "Total works: ${works.size}")

            Spacer(modifier = Modifier.height(12.dp))

            if (works.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No works available")
                }
            } else if (filteredWorks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No works found")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    filteredWorks.forEach { work ->
                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = work.workName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = work.status.name,
                                        color = statusColor(work.status),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = work.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                val workerLabel = when (work.status) {
                                    WorkStatus.Pending -> "Worker: Not assigned"
                                    WorkStatus.Booked -> "Worker: ${work.workerName ?: "Not assigned"}"
                                    WorkStatus.Completed -> "Completed by: ${work.workerName ?: "Unknown"}"
                                }

                                Text(
                                    text = workerLabel,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminWorkManagementPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        AdminWorkManagementScreen(
            works = emptyList()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerApplyScreen(
    modifier: Modifier = Modifier,
    onSubmit: (fullName: String, phone: String, email: String, address: String, service: String, experience: String, cvUri: Uri) -> Unit = { _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var service by rememberSaveable { mutableStateOf("") }
    var experience by rememberSaveable { mutableStateOf("") }

    var cvUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var cvFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val cvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        cvUri = uri
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val name = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        }.getOrNull()

        cvFileName = name ?: uri.lastPathSegment
    }

    val isSubmitEnabled =
        fullName.isNotBlank() &&
            phone.isNotBlank() &&
            email.isNotBlank() &&
            address.isNotBlank() &&
            service.isNotBlank() &&
            cvUri != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Worker Apply") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Apply as Worker",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                placeholder = { Text(text = "Full Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text(text = "Phone") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text(text = "Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                placeholder = { Text(text = "Address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = service,
                onValueChange = { service = it },
                placeholder = { Text(text = "Service (e.g. Plumber, Electrician)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = experience,
                onValueChange = { experience = it },
                placeholder = { Text(text = "Experience (e.g. 2 years)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = { cvPickerLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(44.dp)
            ) {
                Text(text = "UPLOAD CV")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = cvFileName?.let { "Selected: $it" } ?: "No CV selected",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                enabled = isSubmitEnabled,
                onClick = {
                    val uri = cvUri ?: return@Button
                    submitted = true
                    onSubmit(fullName, phone, email, address, service, experience, uri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(46.dp)
            ) {
                Text(text = "SUBMIT")
            }

            if (submitted) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Application submitted", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WorkerApplyPreview() {
    AIPoweredHomeServiceHiringPlatformTheme {
        WorkerApplyScreen()
    }
}
