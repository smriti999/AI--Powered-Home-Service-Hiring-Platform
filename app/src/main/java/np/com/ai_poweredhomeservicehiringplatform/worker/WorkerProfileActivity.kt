package np.com.ai_poweredhomeservicehiringplatform.worker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.sha256Hex
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.StarRating
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class WorkerProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (!AppStorage.isWorkerLoggedIn(this)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            AIPoweredHomeServiceHiringPlatformTheme {
                WorkerProfileScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val email = AppStorage.currentWorkerEmail(context).orEmpty()
    val workers = AppStorage.loadWorkers(context)
    val worker = workers.find { it.email.equals(email, ignoreCase = true) }
    
    val ratings = AppStorage.loadRatings(context)
    val workerRatings = ratings.filter { it.workerEmail.equals(email, ignoreCase = true) }
    val avgRating = if (workerRatings.isNotEmpty()) workerRatings.map { it.stars }.average().toInt() else 0

    var isEditMode by remember { mutableStateOf(false) }

    var name by rememberSaveable { mutableStateOf(worker?.name.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var workerEmail by rememberSaveable { mutableStateOf(worker?.email.orEmpty()) }
    var number by rememberSaveable { mutableStateOf(worker?.phoneNumber.orEmpty()) }
    var location by rememberSaveable { mutableStateOf(worker?.location.orEmpty()) }
    var profession by rememberSaveable { mutableStateOf(worker?.profession.orEmpty()) }
    var experience by rememberSaveable { mutableStateOf(worker?.experienceYears.orEmpty()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = if (isEditMode) "Edit Profile" else "Worker Profile",
                navigationIcon = {
                    IconButton(onClick = { if (isEditMode) isEditMode = false else onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (!isEditMode) {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                if (isEditMode) {
                    Button(
                        onClick = {
                            if (name.isBlank() || workerEmail.isBlank() || number.isBlank() || location.isBlank() || profession.isBlank() || experience.isBlank()) {
                                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val currentWorkers = AppStorage.loadWorkers(context)
                            val currentWorker = currentWorkers.find { it.email.equals(email, ignoreCase = true) }

                            if (currentWorker != null) {
                                val updatedWorker = currentWorker.copy(
                                    name = name,
                                    email = workerEmail,
                                    phoneNumber = number,
                                    location = location,
                                    profession = profession,
                                    experienceYears = experience,
                                    passwordHash = if (password.isNotBlank()) sha256Hex(password) else currentWorker.passwordHash
                                )

                                val updatedList = currentWorkers.map {
                                    if (it.id == currentWorker.id) updatedWorker else it
                                }

                                AppStorage.saveWorkers(context, updatedList)
                                AppStorage.setWorkerLoggedIn(context, true, workerEmail)
                                
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                                isEditMode = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "Save Changes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { isEditMode = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = { isEditMode = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color(0xFFCCCCCC), CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = name.ifBlank { "Worker" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            if (!isEditMode) {
                Text(
                    text = profession.ifBlank { "Professional" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRating(rating = avgRating, starSize = 20.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "($avgRating/5)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isEditMode) {
                ProfileField(value = name, label = "Full name", onValueChange = { name = it })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField(value = password, label = "New Password", onValueChange = { password = it })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField(value = workerEmail, label = "Email", onValueChange = { workerEmail = it })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField(value = number, label = "Number", onValueChange = { number = it })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField(value = location, label = "Location", onValueChange = { location = it })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField(value = profession, label = "Profession", onValueChange = { profession = it })
                Spacer(modifier = Modifier.height(16.dp))
                ProfileField(value = experience, label = "Experience (Years)", onValueChange = { experience = it })
            } else {
                // View Mode
                InfoRow(label = "Email", value = workerEmail)
                InfoRow(label = "Number", value = number)
                InfoRow(label = "Location", value = location)
                InfoRow(label = "Experience", value = "$experience Years")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
private fun ProfileField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    )
}
