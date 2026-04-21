package np.com.ai_poweredhomeservicehiringplatform.user

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.AppDrawer
import np.com.ai_poweredhomeservicehiringplatform.ui.components.BurgerMenuIcon
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NavigationItem
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

class UserHomeActivity : ComponentActivity() {
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
                UserHomeScreen(
                    userEmail = email,
                    onCreateJobClick = { startActivity(Intent(this, UserCreateJobActivity::class.java)) },
                    onMyJobsClick = { startActivity(Intent(this, UserJobsActivity::class.java)) },
                    onNotificationsClick = { startActivity(Intent(this, UserNotificationsActivity::class.java)) },
                    onProfileClick = { startActivity(Intent(this, UserProfileActivity::class.java)) },
                    onFindWorkersClick = { startActivity(Intent(this, UserWorkersActivity::class.java)) },
                    onSupportClick = { startActivity(Intent(this, UserSupportActivity::class.java)) },
                    onCategoryClick = { category ->
                        if (category == "Others") {
                            startActivity(Intent(this, UserCategoriesActivity::class.java))
                        } else {
                            val intent = Intent(this, UserCreateJobActivity::class.java)
                            intent.putExtra(EXTRA_PRESET_SERVICE, category)
                            startActivity(intent)
                        }
                    },
                    onLogoutClick = {
                        AppStorage.setUserLoggedIn(this, false, null)
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserHomeScreen(
    userEmail: String,
    onCreateJobClick: () -> Unit,
    onMyJobsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFindWorkersClick: () -> Unit,
    onSupportClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onLogoutClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationCount = rememberUnreadNotificationCount(userEmail)
    val categories = listOf(
        "Plumbing Services",
        "Cleaning Services",
        "Electrical Services",
        "Others"
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val navItems = listOf(
        NavigationItem("Home", Icons.Default.Home, { }),
        NavigationItem("Find Workers", Icons.Default.Search, onFindWorkersClick),
        NavigationItem("Profile", Icons.Default.AccountCircle, onProfileClick),
        NavigationItem("My Jobs", Icons.AutoMirrored.Filled.List, onMyJobsClick),
        NavigationItem("Notifications", Icons.Default.Notifications, onNotificationsClick),
        NavigationItem("Support", Icons.AutoMirrored.Filled.Help, onSupportClick),
        NavigationItem("Logout", Icons.AutoMirrored.Filled.ExitToApp, onLogoutClick)
    )

    AppDrawer(drawerState = drawerState, items = navItems) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LogoTopAppBar(
                    title = "Home",
                    navigationIcon = {
                        BurgerMenuIcon(drawerState = drawerState)
                    },
                    actions = {
                        NotificationBell(count = notificationCount, onClick = onNotificationsClick)
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
            OutlinedTextField(
                value = "",
                onValueChange = { },
                placeholder = { Text(text = "Search for services...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Categories", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(10.dp))

            categories.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { category ->
                        OutlinedCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            onClick = { onCategoryClick(category) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = category.replace(" Services", ""), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCreateJobClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(text = "NEED WORKER")
                }
                OutlinedButton(
                    onClick = onMyJobsClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text(text = "WORKS")
                }
            }

        }
    }
}
}
