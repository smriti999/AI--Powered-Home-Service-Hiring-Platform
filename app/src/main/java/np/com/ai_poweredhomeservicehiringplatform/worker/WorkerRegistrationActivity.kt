package np.com.ai_poweredhomeservicehiringplatform.worker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.common.normalizeGmailEmail
import np.com.ai_poweredhomeservicehiringplatform.common.normalizePhoneNumber
import np.com.ai_poweredhomeservicehiringplatform.common.sha256Hex
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerApplicationStatus
import np.com.ai_poweredhomeservicehiringplatform.common.model.WorkerApplicationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class WorkerRegistrationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                WorkerRegistrationScreen(onBackToLogin = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerRegistrationScreen(
    onBackToLogin: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    var location by rememberSaveable { mutableStateOf("") }
    var streetHomeNumber by rememberSaveable { mutableStateOf("") }
    var alternativeLocation by rememberSaveable { mutableStateOf("") }

    var gender by rememberSaveable { mutableStateOf("") }
    var profession by rememberSaveable { mutableStateOf("") }
    var experienceYears by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showThankYouDialog by rememberSaveable { mutableStateOf(false) }

    val locationOptions = listOf("Kathmandu", "Bhaktapur", "Lalitpur")
    var isLocationMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")
    var isGenderMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val professionOptions = listOf(
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
    var isProfessionMenuExpanded by rememberSaveable { mutableStateOf(false) }

    var cvUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var cvFileName by rememberSaveable { mutableStateOf<String?>(null) }
    var cvSizeBytes by rememberSaveable { mutableStateOf<Long?>(null) }

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
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        val (name, sizeBytes) = getFileNameAndSize(uri)
        val minBytes = 2L * 1024L * 1024L
        val maxBytes = 5L * 1024L * 1024L
        val isValidSize = sizeBytes != null && sizeBytes in minBytes..maxBytes

        if (!isValidSize) {
            cvUriString = null
            cvFileName = null
            cvSizeBytes = null
            errorMessage = "CV must be a PDF between 2MB and 5MB"
            return@rememberLauncherForActivityResult
        }

        cvUriString = uri.toString()
        cvFileName = name ?: uri.lastPathSegment
        cvSizeBytes = sizeBytes
        errorMessage = null
    }

    val minBytes = 2L * 1024L * 1024L
    val maxBytes = 5L * 1024L * 1024L
    val isCvValid = cvUriString == null || (cvSizeBytes != null && cvSizeBytes in minBytes..maxBytes)
    val isFormFilled =
        fullName.trim().isNotBlank() &&
            email.trim().isNotBlank() &&
            phoneNumber.trim().length == 10 &&
            location.isNotBlank() &&
            streetHomeNumber.trim().isNotBlank() &&
            gender.isNotBlank() &&
            profession.isNotBlank() &&
            experienceYears.trim().isNotBlank() &&
            password.isNotBlank()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Worker Registration",
                navigationIcon = {
                    IconButton(onClick = onBackToLogin) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
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
                    enabled = isFormFilled && isCvValid,
                    onClick = {
                        val trimmedName = fullName.trim()
                        val trimmedEmail = email.trim()
                        val trimmedPhone = phoneNumber.trim()
                        val trimmedStreet = streetHomeNumber.trim()
                        val trimmedAlt = alternativeLocation.trim()
                        val trimmedExperience = experienceYears.trim()
                        val cvUri = cvUriString?.let { Uri.parse(it) }
                        val cvSize = cvSizeBytes
                        val cvName = cvFileName

                        if (trimmedName.isBlank() ||
                            trimmedEmail.isBlank() ||
                            trimmedPhone.isBlank() ||
                            location.isBlank() ||
                            trimmedStreet.isBlank() ||
                            gender.isBlank() ||
                            profession.isBlank() ||
                            trimmedExperience.isBlank() ||
                            password.isBlank()
                        ) {
                            errorMessage = "All fields are required"
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

                        if (cvUri != null && (cvSize == null || cvSize !in minBytes..maxBytes)) {
                            errorMessage = "CV must be a PDF between 2MB and 5MB"
                            return@Button
                        }

                        val apps = AppStorage.loadWorkerApplications(context)
                        val nextId = (apps.maxOfOrNull { it.id } ?: 0) + 1
                        val updated = apps + WorkerApplicationUiModel(
                            id = nextId,
                            name = trimmedName,
                            email = trimmedEmail,
                            phoneNumber = trimmedPhone,
                            location = location,
                            streetHomeNumber = trimmedStreet,
                            alternativeLocation = trimmedAlt,
                            gender = gender,
                            profession = profession,
                            experienceYears = trimmedExperience,
                            passwordHash = sha256Hex(password),
                            cvUri = cvUri?.toString(),
                            cvFileName = cvName,
                            cvSizeBytes = cvSize,
                            status = WorkerApplicationStatus.Pending
                        )
                        AppStorage.saveWorkerApplications(context, updated)

                        errorMessage = null
                        showThankYouDialog = true
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Full Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        TextButton(onClick = { isLocationMenuExpanded = true }) { Text(text = "▼") }
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
                        .widthIn(max = 360.dp)
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
                        .widthIn(max = 360.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            ) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = { },
                    readOnly = true,
                    placeholder = { Text(text = "Gender") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isGenderMenuExpanded = true },
                    trailingIcon = {
                        TextButton(onClick = { isGenderMenuExpanded = true }) { Text(text = "▼") }
                    }
                )

                DropdownMenu(
                    expanded = isGenderMenuExpanded,
                    onDismissRequest = { isGenderMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option) },
                            onClick = {
                                gender = option
                                isGenderMenuExpanded = false
                                errorMessage = null
                            }
                        )
                    }
                }
            }

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
                        TextButton(onClick = { isProfessionMenuExpanded = true }) { Text(text = "▼") }
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
                                errorMessage = null
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
                    errorMessage = null
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
                    errorMessage = null
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

            val sizeMb = cvSizeBytes?.let { it.toDouble() / (1024.0 * 1024.0) }
            Text(
                text = if (cvUriString != null) {
                    val name = cvFileName ?: "CV selected"
                    val mb = sizeMb?.let { String.format("%.2f", it) } ?: "-"
                    "Selected: $name ($mb MB)"
                } else {
                    "No CV selected"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = { showThankYouDialog = false },
            title = { Text(text = "Thank you") },
            text = { Text(text = "Thank you for submitting. Please wait for 24 hours.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showThankYouDialog = false
                        onBackToLogin()
                    }
                ) {
                    Text(text = "OK")
                }
            }
        )
    }
}

