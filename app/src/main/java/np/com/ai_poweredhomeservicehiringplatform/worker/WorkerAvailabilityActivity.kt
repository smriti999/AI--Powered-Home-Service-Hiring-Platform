package np.com.ai_poweredhomeservicehiringplatform.worker

import android.app.Activity
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class WorkerAvailabilityActivity : ComponentActivity() {
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
                WorkerAvailabilityScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkerAvailabilityScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val workerEmail = AppStorage.currentWorkerEmail(context).orEmpty()
    val notificationCount = rememberUnreadNotificationCount(workerEmail)

    val locationOptions = listOf("Kathmandu", "Bhaktapur", "Lalitpur")
    var available by rememberSaveable { mutableStateOf(AppStorage.loadWorkerAvailability(context, workerEmail)) }
    var selectedAreas by rememberSaveable {
        mutableStateOf(AppStorage.loadWorkerServiceAreas(context, workerEmail).toSet())
    }
    var selectedTab by remember { mutableStateOf(WorkerBottomTab.Availability) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Availability",
                actions = {
                    NotificationBell(
                        count = notificationCount,
                        onClick = {
                            context.startActivity(Intent(context, WorkerNotificationsActivity::class.java))
                            activity?.finish()
                        }
                    )
                }
            )
        },
        bottomBar = {
            Column {
                Button(
                    onClick = {
                        AppStorage.saveWorkerAvailability(context, workerEmail, available)
                        AppStorage.saveWorkerServiceAreas(context, workerEmail, selectedAreas.toList())
                        Toast.makeText(context, "Availability saved", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(46.dp)
                ) {
                    Text(text = "Save")
                }
                NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                    NavigationBarItem(
                        selected = selectedTab == WorkerBottomTab.Home,
                        onClick = {
                            selectedTab = WorkerBottomTab.Home
                            context.startActivity(
                                Intent(context, WorkerDashboardActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            )
                            activity?.finish()
                        },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text(text = "Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == WorkerBottomTab.Earnings,
                        onClick = {
                            selectedTab = WorkerBottomTab.Earnings
                            context.startActivity(Intent(context, WorkerEarningsActivity::class.java))
                            activity?.finish()
                        },
                        icon = { Icon(imageVector = Icons.Default.Payments, contentDescription = "Earnings") },
                        label = { Text(text = "Earnings") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == WorkerBottomTab.Availability,
                        onClick = { selectedTab = WorkerBottomTab.Availability },
                        icon = { Icon(imageVector = Icons.Default.ToggleOn, contentDescription = "Availability") },
                        label = { Text(text = "Availability") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == WorkerBottomTab.Menu,
                        onClick = {
                            selectedTab = WorkerBottomTab.Menu
                            context.startActivity(Intent(context, WorkerMenuActivity::class.java))
                            activity?.finish()
                        },
                        icon = { Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu") },
                        label = { Text(text = "Menu") }
                    )
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Available for new jobs", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (available) "You will appear in searches and can accept jobs." else "You will not accept new jobs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = available, onCheckedChange = { available = it })
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(text = "Service Areas", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))

            locationOptions.forEach { area ->
                val checked = selectedAreas.contains(area)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            selectedAreas = if (it) selectedAreas + area else selectedAreas - area
                        }
                    )
                    Text(text = area)
                }
            }
        }
    }
}
