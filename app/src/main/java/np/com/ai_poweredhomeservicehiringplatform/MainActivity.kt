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

private const val AUTH_PREFS = "auth_prefs"
private const val KEY_SEEDED_ADMIN_USERNAME = "seeded_admin_username"
private const val KEY_SEEDED_ADMIN_PASSWORD_HASH = "seeded_admin_password_hash"

private fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
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

private enum class AppScreen {
    AuthLogin,
    AuthSignUp,
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
    val status: String
)

data class WorkUiModel(
    val id: Int,
    val workName: String,
    val detail: String,
    val workerName: String?,
    val status: WorkStatus
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedAdminIfNeeded(this)
        enableEdgeToEdge()
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                var requests by remember { mutableStateOf(listOf<WorkerRequestUiModel>()) }
                var workers by remember { mutableStateOf(listOf<WorkerUiModel>()) }
                var users by remember { mutableStateOf(listOf<UserUiModel>()) }
                var works by remember { mutableStateOf(listOf<WorkUiModel>()) }

                var isAdminLoggedIn by rememberSaveable { mutableStateOf(false) }
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
                val effectiveScreen = if (!isAdminLoggedIn && screen in adminOnlyScreens) {
                    pendingAdminDestination = screen
                    AppScreen.AdminLogin
                } else {
                    screen
                }

                when (effectiveScreen) {
                    AppScreen.AuthLogin -> {
                        AuthLoginScreen(
                            onAdminLogoClick = {
                                pendingAdminDestination = AppScreen.AdminDashboard
                                screen = AppScreen.AdminLogin
                            },
                            onSignUpClick = {
                                screen = AppScreen.AuthSignUp
                            }
                        )
                    }

                    AppScreen.AuthSignUp -> {
                        AuthSignUpScreen(
                            onBackToLoginClick = { screen = AppScreen.AuthLogin }
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
                            onWorksClick = { openAdmin(AppScreen.AdminWorkManagement) }
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
                                users = users.filterNot { it.id == userId }
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
    onSignUpClick: () -> Unit = { }
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

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
                .padding(horizontal = 24.dp),
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
                onValueChange = { email = it },
                placeholder = { Text(text = "Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )

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

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { },
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
fun AuthSignUpScreen(
    modifier: Modifier = Modifier,
    onBackToLoginClick: () -> Unit = { }
) {
    var role by rememberSaveable { mutableStateOf(SignUpRole.User) }

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var profession by rememberSaveable { mutableStateOf("") }
    var experienceYears by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }

    val titleText = if (role == SignUpRole.User) "User Sign Up" else "Worker Registration"

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
                onValueChange = { fullName = it },
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
                    onValueChange = { email = it },
                    placeholder = { Text(text = "Email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    placeholder = { Text(text = "Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text(text = "Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

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

                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .height(46.dp)
                ) {
                    Text(text = "CREATE ACCOUNT")
                }
            } else {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    placeholder = { Text(text = "Phone Number") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = profession,
                    onValueChange = { profession = it },
                    placeholder = { Text(text = "Profession (e.g. Plumber)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = experienceYears,
                    onValueChange = { experienceYears = it },
                    placeholder = { Text(text = "Experience (Years)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

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
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text(text = "Email") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text(text = "Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = gender,
                    onValueChange = { gender = it },
                    placeholder = { Text(text = "Gender") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    placeholder = { Text(text = "Bio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                )

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = { },
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
                .padding(horizontal = 24.dp),
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
    onWorksClick: () -> Unit = { }
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
                                        fontWeight = FontWeight.SemiBold
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
