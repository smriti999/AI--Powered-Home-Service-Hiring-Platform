package np.com.ai_poweredhomeservicehiringplatform.user

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

const val EXTRA_PRESET_SERVICE = "extra_preset_service"

class UserCreateJobActivity : ComponentActivity() {
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
                val email = AppStorage.currentUserEmail(this) ?: ""
                val presetService = intent.getStringExtra(EXTRA_PRESET_SERVICE).orEmpty()
                UserCreateJobScreen(
                    userEmail = email,
                    presetService = presetService,
                    onBackClick = { finish() },
                    onSubmit = { service, description, location, streetHomeNumber, alternativeLocation ->
                        val jobs = AppStorage.loadUserJobs(this)
                        val works = AppStorage.loadWorks(this)

                        val nextJobId = (jobs.maxOfOrNull { it.id } ?: 0) + 1
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
                        AppStorage.saveUserJobs(this, jobs + newJob)

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
                        AppStorage.saveWorks(
                            this,
                            works + WorkUiModel(
                                id = nextWorkId,
                                workName = service,
                                detail = detailText,
                                workerName = null,
                                status = WorkStatus.Pending
                            )
                        )

                        startActivity(Intent(this, UserJobsActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserCreateJobScreen(
    userEmail: String,
    presetService: String,
    onBackClick: () -> Unit,
    onSubmit: (
        service: String,
        description: String,
        location: String,
        streetHomeNumber: String,
        alternativeLocation: String
    ) -> Unit
) {
    var service by rememberSaveable { mutableStateOf(presetService) }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var streetHomeNumber by rememberSaveable { mutableStateOf("") }
    var alternativeLocation by rememberSaveable { mutableStateOf("") }
    var isLocationMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val locationOptions = listOf("Kathmandu", "Bhaktapur", "Lalitpur")
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
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
