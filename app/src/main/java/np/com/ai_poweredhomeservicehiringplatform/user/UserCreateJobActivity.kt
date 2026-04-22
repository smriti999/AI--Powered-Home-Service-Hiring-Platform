package np.com.ai_poweredhomeservicehiringplatform.user

import android.content.Intent
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserJobUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import java.util.Calendar

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
                val preferredWorkerName = intent.getStringExtra(EXTRA_PREFERRED_WORKER_NAME).orEmpty()
                UserCreateJobScreen(
                    userEmail = email,
                    presetService = presetService,
                    preferredWorkerName = preferredWorkerName,
                    onBackClick = { finish() },
                    onSubmit = { service, description, _, location ->
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
                            streetHomeNumber = "",
                            alternativeLocation = "",
                            status = WorkStatus.Pending
                        )
                        AppStorage.saveUserJobs(this, jobs + newJob)

                        val detailText = buildString {
                            append("User: ")
                            append(email)
                            append("\nLocation: ")
                            append(location)
                            append("\n\n")
                            append(description)
                        }
                        AppStorage.saveWorks(
                            this,
                            works + WorkUiModel(
                                id = nextWorkId,
                                workName = service,
                                detail = detailText,
                                workerEmail = null,
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
    preferredWorkerName: String,
    onBackClick: () -> Unit,
    onSubmit: (
        service: String,
        description: String,
        time: String,
        location: String
    ) -> Unit
) {
    val context = LocalContext.current
    val notificationCount = rememberUnreadNotificationCount(userEmail)
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

    var service by rememberSaveable { mutableStateOf(presetService.ifBlank { "" }) }
    var isServiceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var description by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var location by rememberSaveable { mutableStateOf("") }
    var streetHomeNumber by rememberSaveable { mutableStateOf("") }
    var alternativeLocation by rememberSaveable { mutableStateOf("") }
    var isLocationMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val locationOptions = listOf("Kathmandu", "Bhaktapur", "Lalitpur")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Worker needed",
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    NotificationBell(
                        count = notificationCount,
                        onClick = { context.startActivity(Intent(context, UserNotificationsActivity::class.java)) }
                    )
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        val s = service.trim()
                        val d = description.trim()
                        val t = time.trim()
                        val loc = location.trim()
                        val street = streetHomeNumber.trim()
                        val alt = alternativeLocation.trim()

                        if (s.isBlank() || d.isBlank() || t.isBlank() || loc.isBlank() || (loc.isNotBlank() && street.isBlank())) {
                            errorMessage = "All fields are required"
                            return@Button
                        }

                        errorMessage = null
                        val fullDescription = buildString {
                            if (preferredWorkerName.isNotBlank()) {
                                append("Preferred Worker: ")
                                append(preferredWorkerName.trim())
                                append("\n")
                            }
                            append("Time: ")
                            append(t)
                            append("\nLocation: ")
                            append(loc)
                            append("\nStreet/Home: ")
                            append(street)
                            if (alt.isNotBlank()) {
                                append("\nOptional: ")
                                append(alt)
                            }
                            append("\n\n")
                            append(d)
                        }
                        onSubmit(s, fullDescription, t, loc)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .height(46.dp)
                ) {
                    Text(text = "Post Job")
                }
            }
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
                text = "Job Details",
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (preferredWorkerName.isNotBlank()) {
                OutlinedTextField(
                    value = preferredWorkerName,
                    onValueChange = { },
                    readOnly = true,
                    singleLine = true,
                    label = { Text(text = "Preferred Worker") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
            ) {
                OutlinedTextField(
                    value = service,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text(text = "Service category") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isServiceMenuExpanded = true },
                    trailingIcon = {
                        TextButton(onClick = { isServiceMenuExpanded = true }) {
                            Text(text = "▼")
                        }
                    }
                )

                DropdownMenu(
                    expanded = isServiceMenuExpanded,
                    onDismissRequest = { isServiceMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    serviceOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            onClick = {
                                service = option
                                isServiceMenuExpanded = false
                                errorMessage = null
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .height(120.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = time,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text(text = "Time") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker = true },
                    trailingIcon = {
                        TextButton(onClick = { showTimePicker = true }) {
                            Text(text = "▼")
                        }
                    }
                )

                Box(modifier = Modifier.weight(1f)) {
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
                                    streetHomeNumber = ""
                                    alternativeLocation = ""
                                    errorMessage = null
                                }
                            )
                        }
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
                    placeholder = { Text(text = "Street, House Number") },
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
                    placeholder = { Text(text = "Optional Location") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                )
            }
        }
    }

    LaunchedEffect(showTimePicker) {
        if (!showTimePicker) return@LaunchedEffect
        showTimePicker = false

        val now = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour12 = run {
                    val raw = hourOfDay % 12
                    if (raw == 0) 12 else raw
                }
                time = String.format("%02d:%02d %s", hour12, minute, amPm)
                errorMessage = null
            },
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE),
            false
        ).show()
    }
}
