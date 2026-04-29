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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.RequestBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberPendingRequestCount
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class AdminMenuActivity : ComponentActivity() {
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
                AdminMenuScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminMenuScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val pendingRequestCount = rememberPendingRequestCount()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Menu",
                actions = {
                    RequestBell(
                        count = pendingRequestCount,
                        onClick = {
                            context.startActivity(Intent(context, AdminRequestsActivity::class.java))
                            activity?.finish()
                        }
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
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
                        context.startActivity(Intent(context, AdminWorkerManagementActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Group, contentDescription = "Workers") },
                    label = { Text(text = "Workers") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, AdminUserManagementActivity::class.java))
                        activity?.finish()
                    },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Users") },
                    label = { Text(text = "Users") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text(text = "Menu") }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuCard(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Works",
                onClick = {
                    context.startActivity(Intent(context, AdminWorkManagementActivity::class.java))
                    activity?.finish()
                }
            )
            MenuCard(
                icon = Icons.AutoMirrored.Filled.List,
                label = "Completed Works",
                onClick = {
                    context.startActivity(Intent(context, AdminCompletedWorksActivity::class.java))
                    activity?.finish()
                }
            )
            MenuCard(
                icon = Icons.Default.MonetizationOn,
                label = "Revenue",
                onClick = {
                    context.startActivity(Intent(context, AdminRevenueActivity::class.java))
                    activity?.finish()
                }
            )
            MenuCard(
                icon = Icons.Default.Psychology,
                label = "ML Model",
                onClick = {
                    context.startActivity(Intent(context, AdminMlModelReportActivity::class.java))
                    activity?.finish()
                }
            )
            MenuCard(
                icon = Icons.Default.Campaign,
                label = "Broadcast",
                onClick = {
                    context.startActivity(Intent(context, AdminBroadcastActivity::class.java))
                    activity?.finish()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuCard(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Logout",
                onClick = {
                    showLogoutConfirm = true
                }
            )
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(text = "Logout") },
            text = { Text(text = "Are you sure want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        AppStorage.logoutAll(context)
                        context.startActivity(
                            Intent(context, LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        (context as? Activity)?.finish()
                    }
                ) {
                    Text(text = "Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(text = "No")
                }
            }
        )
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label)
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

enum class AdminBottomTab {
    Home,
    Workers,
    Users,
    Menu
}
