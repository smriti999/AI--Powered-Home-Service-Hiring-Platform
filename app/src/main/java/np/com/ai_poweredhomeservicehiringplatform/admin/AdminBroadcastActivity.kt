package np.com.ai_poweredhomeservicehiringplatform.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.NotificationUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class AdminBroadcastActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isAdminLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                AdminBroadcastScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminBroadcastScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var target by remember { mutableIntStateOf(0) } // 0=All,1=Users,2=Workers

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Broadcast",
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Button(
                onClick = {
                    val t = title.trim()
                    val m = message.trim()
                    if (t.isBlank() || m.isBlank()) {
                        Toast.makeText(context, "Title and message required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val users = AppStorage.loadUsers(context).map { it.email }.filter { it.isNotBlank() }
                    val workers = AppStorage.loadWorkers(context).map { it.email }.filter { it.isNotBlank() }
                    val recipients = when (target) {
                        1 -> users
                        2 -> workers
                        else -> (users + workers).distinct()
                    }

                    val existing = AppStorage.loadNotifications(context)
                    var nextId = (existing.maxOfOrNull { it.id } ?: 0) + 1
                    val now = System.currentTimeMillis()
                    val added = recipients.map { email ->
                        NotificationUiModel(
                            id = nextId++,
                            userEmail = email,
                            title = t,
                            message = m,
                            timestampMillis = now
                        )
                    }
                    AppStorage.saveNotifications(context, existing + added)
                    Toast.makeText(context, "Broadcast sent to ${recipients.size}", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(46.dp)
            ) {
                Text(text = "Send")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(text = "Target", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (target == 0) Button(onClick = { target = 0 }, modifier = Modifier.weight(1f)) { Text("All") }
                else OutlinedButton(onClick = { target = 0 }, modifier = Modifier.weight(1f)) { Text("All") }

                if (target == 1) Button(onClick = { target = 1 }, modifier = Modifier.weight(1f)) { Text("Users") }
                else OutlinedButton(onClick = { target = 1 }, modifier = Modifier.weight(1f)) { Text("Users") }

                if (target == 2) Button(onClick = { target = 2 }, modifier = Modifier.weight(1f)) { Text("Workers") }
                else OutlinedButton(onClick = { target = 2 }, modifier = Modifier.weight(1f)) { Text("Workers") }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                label = { Text(text = "Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text(text = "Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "This will appear in the notifications page.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

