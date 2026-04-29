package np.com.ai_poweredhomeservicehiringplatform.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme
import np.com.ai_poweredhomeservicehiringplatform.user.UserSignUpActivity
import np.com.ai_poweredhomeservicehiringplatform.worker.WorkerRegistrationActivity

private enum class SignUpAccountType { User, Worker }

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                SignUpChooser(
                    onBackToLogin = {
                        finish()
                    },
                    onUserSignUp = {
                        startActivity(Intent(this, UserSignUpActivity::class.java))
                    },
                    onWorkerSignUp = {
                        startActivity(Intent(this, WorkerRegistrationActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpChooser(
    onBackToLogin: () -> Unit,
    onUserSignUp: () -> Unit,
    onWorkerSignUp: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedType by rememberSaveable { mutableStateOf<SignUpAccountType?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                selectedType = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedType) {
        when (selectedType) {
            SignUpAccountType.User -> {
                delay(120)
                onUserSignUp()
            }
            SignUpAccountType.Worker -> {
                delay(120)
                onWorkerSignUp()
            }
            null -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Sign Up",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Choose account type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedType == SignUpAccountType.User) {
                    Button(
                        onClick = { selectedType = SignUpAccountType.User },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "User")
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedType = SignUpAccountType.User },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "User")
                    }
                }

                if (selectedType == SignUpAccountType.Worker) {
                    Button(
                        onClick = { selectedType = SignUpAccountType.Worker },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Worker")
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedType = SignUpAccountType.Worker },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Worker")
                    }
                }
            }
        }
    }
}
