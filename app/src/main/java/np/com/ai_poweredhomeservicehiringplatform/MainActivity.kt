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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    AdminLogin,
    AdminRequests,
    AdminWorkerManagement,
    WorkerApply
}

private enum class WorkerRequestDecision {
    Pending,
    Approved,
    Rejected
}

private data class WorkerRequestUiModel(
    val id: Int,
    val name: String,
    val role: String,
    val experienceYears: Int,
    val decision: WorkerRequestDecision = WorkerRequestDecision.Pending
)

private data class WorkerUiModel(
    val id: Int,
    val name: String,
    val status: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedAdminIfNeeded(this)
        enableEdgeToEdge()
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                var screen by rememberSaveable { mutableStateOf(AppScreen.AdminLogin) }
                when (screen) {
                    AppScreen.AdminLogin -> {
                        AdminLoginScreen(
                            onLoginClick = { _, _ -> screen = AppScreen.AdminRequests }
                        )
                    }

                    AppScreen.AdminRequests -> {
                        AdminRequestWorkerScreen(
                            onWorkersClick = { screen = AppScreen.AdminWorkerManagement }
                        )
                    }

                    AppScreen.AdminWorkerManagement -> {
                        AdminWorkerManagementScreen(
                            onRequestsClick = { screen = AppScreen.AdminRequests }
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
fun AdminLoginScreen(
    modifier: Modifier = Modifier,
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

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(28.dp))

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
fun AdminRequestWorkerScreen(
    modifier: Modifier = Modifier,
    onWorkersClick: () -> Unit = { }
) {
    val initialRequests = remember {
        emptyList<WorkerRequestUiModel>()
    }
    var requests by remember { mutableStateOf(initialRequests) }

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
                    TextButton(onClick = onWorkersClick) {
                        Text(text = "Workers", color = MaterialTheme.colorScheme.onPrimary)
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
                                            requests = requests.map {
                                                if (it.id == item.id) it.copy(decision = WorkerRequestDecision.Approved) else it
                                            }
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
                                            requests = requests.map {
                                                if (it.id == item.id) it.copy(decision = WorkerRequestDecision.Rejected) else it
                                            }
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
        AdminRequestWorkerScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminWorkerManagementScreen(
    modifier: Modifier = Modifier,
    onRequestsClick: () -> Unit = { }
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val initialWorkers = remember {
        listOf(
            WorkerUiModel(id = 1, name = "Smriti", status = "Active")
        )
    }
    var workers by remember { mutableStateOf(initialWorkers) }

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
                    TextButton(onClick = onRequestsClick) {
                        Text(text = "Requests", color = MaterialTheme.colorScheme.onPrimary)
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

            Text(text = "Total workers: ${filteredWorkers.size}")

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredWorkers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No workers available")
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
                                    onClick = { workers = workers.filterNot { it.id == worker.id } },
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
        AdminWorkerManagementScreen()
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
