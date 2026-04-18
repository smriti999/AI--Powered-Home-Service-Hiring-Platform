package np.com.ai_poweredhomeservicehiringplatform.user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class UserSignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                UserSignUpScreen(onBackToLogin = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserSignUpScreen(
    onBackToLogin: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var streetHomeNumber by rememberSaveable { mutableStateOf("") }
    var alternativeLocation by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var isLocationMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val locationOptions = listOf("Kathmandu", "Bhaktapur", "Lalitpur")

    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showThankYouDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "User Sign Up",
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

                        val users = AppStorage.loadUsers(context)
                        if (users.any { it.email.equals(trimmedEmail, ignoreCase = true) }) {
                            errorMessage = "Email already registered"
                            return@Button
                        }

                        val nextId = (users.maxOfOrNull { it.id } ?: 0) + 1
                        val updatedUsers = users + UserUiModel(
                            id = nextId,
                            name = trimmedName,
                            status = "Active",
                            email = trimmedEmail,
                            phoneNumber = trimmedPhone,
                            location = location,
                            streetHomeNumber = trimmedStreet,
                            alternativeLocation = trimmedAlt,
                            passwordHash = sha256Hex(password)
                        )
                        AppStorage.saveUsers(context, updatedUsers)

                        errorMessage = null
                        showThankYouDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .height(46.dp)
                ) {
                    Text(text = "SIGN UP")
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

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                placeholder = { Text(text = "Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
            )
        }
    }

    if (showThankYouDialog) {
        AlertDialog(
            onDismissRequest = { showThankYouDialog = false },
            title = { Text(text = "Thank you") },
            text = { Text(text = "Thank you for signing up.") },
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

