package np.com.ai_poweredhomeservicehiringplatform.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.R
import np.com.ai_poweredhomeservicehiringplatform.admin.AdminDashboardActivity
import np.com.ai_poweredhomeservicehiringplatform.admin.AdminLoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.normalizeGmailEmail
import np.com.ai_poweredhomeservicehiringplatform.common.sha256Hex
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import np.com.ai_poweredhomeservicehiringplatform.user.UserHomeActivity
import np.com.ai_poweredhomeservicehiringplatform.worker.WorkerDashboardActivity

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppStorage.seedAdminIfNeeded(this)
        enableEdgeToEdge()

        if (AppStorage.isAdminLoggedIn(this)) {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            finish()
            return
        }

        if (AppStorage.isUserLoggedIn(this)) {
            startActivity(Intent(this, UserHomeActivity::class.java))
            finish()
            return
        }

        if (AppStorage.isWorkerLoggedIn(this)) {
            startActivity(Intent(this, WorkerDashboardActivity::class.java))
            finish()
            return
        }

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                LoginScreen(
                    onAdminLogoClick = {
                        startActivity(Intent(this, AdminLoginActivity::class.java))
                    },
                    onUserLoginSuccess = { email ->
                        AppStorage.setUserLoggedIn(this, true, email)
                        startActivity(Intent(this, UserHomeActivity::class.java))
                        finish()
                    },
                    onWorkerLoginSuccess = { email ->
                        AppStorage.setWorkerLoggedIn(this, true, email)
                        startActivity(Intent(this, WorkerDashboardActivity::class.java))
                        finish()
                    },
                    onSignUpClick = {
                        startActivity(Intent(this, SignUpActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
    onAdminLogoClick: () -> Unit,
    onUserLoginSuccess: (email: String) -> Unit,
    onWorkerLoginSuccess: (email: String) -> Unit,
    onSignUpClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Login",
                actions = {
                    TextButton(onClick = onSignUpClick) {
                        Text(text = "Sign Up", color = Color.White)
                    }
                }
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
                Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isBlank() || password.isBlank()) {
                        errorMessage = "Email and password required"
                        return@Button
                    }

                    val users = AppStorage.loadUsers(context)
                    val isUserOk = users.any {
                        it.email.equals(trimmedEmail, ignoreCase = true) && it.passwordHash == sha256Hex(password)
                    }

                    if (isUserOk) {
                        errorMessage = null
                        onUserLoginSuccess(trimmedEmail)
                        return@Button
                    }

                    val workers = AppStorage.loadWorkers(context)
                    val isWorkerOk = workers.any {
                        it.email.equals(trimmedEmail, ignoreCase = true) &&
                            it.passwordHash == sha256Hex(password) &&
                            it.status.equals("Active", ignoreCase = true)
                    }

                    if (isWorkerOk) {
                        errorMessage = null
                        onWorkerLoginSuccess(trimmedEmail)
                        return@Button
                    }

                    errorMessage = "Invalid credentials"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(44.dp)
            ) {
                Text(text = "LOGIN")
            }
        }
    }
}
