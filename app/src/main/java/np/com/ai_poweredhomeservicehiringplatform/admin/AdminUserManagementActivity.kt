package np.com.ai_poweredhomeservicehiringplatform.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.model.UserUiModel
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.FullScreenLoading
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.RequestBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberPendingRequestCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class AdminUserManagementActivity : ComponentActivity() {
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
                AdminUserManagementScreen(
                    onDashboardClick = {
                        startActivity(
                            Intent(this, AdminDashboardActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        finish()
                    },
                    onRequestsClick = {
                        startActivity(Intent(this, AdminRequestsActivity::class.java))
                        finish()
                    },
                    onWorkersClick = {
                        startActivity(Intent(this, AdminWorkerManagementActivity::class.java))
                        finish()
                    },
                    onWorksClick = {
                        startActivity(Intent(this, AdminWorkManagementActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminUserManagementScreen(
    onDashboardClick: () -> Unit,
    onRequestsClick: () -> Unit,
    onWorkersClick: () -> Unit,
    onWorksClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var users by remember { mutableStateOf(emptyList<UserUiModel>()) }
    var selectedUser by remember { mutableStateOf<UserUiModel?>(null) }
    var selectedTab by remember { mutableStateOf(AdminBottomTab.Users) }
    val pendingRequestCount = rememberPendingRequestCount()

    LaunchedEffect(Unit) {
        isLoading = true
        val loadedUsers = withContext(Dispatchers.IO) { AppStorage.loadUsers(context) }
        users = loadedUsers
        isLoading = false
    }

    val filteredUsers = users.filter { user ->
        val query = searchQuery.trim()
        query.isBlank() || user.name.contains(query, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "User Management",
                actions = {
                    RequestBell(count = pendingRequestCount, onClick = onRequestsClick)
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        selectedTab = AdminBottomTab.Home
                        context.startActivity(
                            Intent(context, AdminDashboardActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        )
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(text = "Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        selectedTab = AdminBottomTab.Workers
                        context.startActivity(Intent(context, AdminWorkerManagementActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Group, contentDescription = "Workers") },
                    label = { Text(text = "Workers") }
                )
                NavigationBarItem(
                    selected = selectedTab == AdminBottomTab.Users,
                    onClick = { selectedTab = AdminBottomTab.Users },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Users") },
                    label = { Text(text = "Users") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        selectedTab = AdminBottomTab.Menu
                        context.startActivity(Intent(context, AdminMenuActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text(text = "Menu") }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            if (isLoading) {
                FullScreenLoading()
                return@Column
            }
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
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { selectedUser = user }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Status: ${user.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val updated = users.filterNot { it.id == user.id }
                                            users = updated
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    AppStorage.saveUsers(context, updated)
                                                }
                                            }
                                        },
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

    selectedUser?.let { user ->
        AlertDialog(
            onDismissRequest = { selectedUser = null },
            title = { Text(text = user.name) },
            text = {
                Column {
                    Text(text = "Email: ${user.email.ifBlank { "-" }}")
                    Text(text = "Phone: ${user.phoneNumber.ifBlank { "-" }}")
                    Text(text = "Status: ${user.status.ifBlank { "-" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Location: ${user.location.ifBlank { "-" }}")
                    Text(text = "Street/Home: ${user.streetHomeNumber.ifBlank { "-" }}")
                    Text(text = "Alternative: ${user.alternativeLocation.ifBlank { "-" }}")
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedUser = null }) {
                    Text(text = "Close")
                }
            }
        )
    }
}
