package np.com.ai_poweredhomeservicehiringplatform.user

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import np.com.ai_poweredhomeservicehiringplatform.R
import np.com.ai_poweredhomeservicehiringplatform.auth.LoginActivity
import np.com.ai_poweredhomeservicehiringplatform.common.storage.AppStorage
import np.com.ai_poweredhomeservicehiringplatform.ui.components.LogoTopAppBar
import np.com.ai_poweredhomeservicehiringplatform.ui.components.NotificationBell
import np.com.ai_poweredhomeservicehiringplatform.ui.components.rememberUnreadNotificationCount
import np.com.ai_poweredhomeservicehiringplatform.ui.theme.AIPoweredHomeServiceHiringPlatformTheme

private enum class UserHomeBottomTab { Home, NeedWorker, Works }

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
                        AppStorage.logoutAll(this)
                        startActivity(
                            Intent(this, LoginActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationCount = rememberUnreadNotificationCount(userEmail)
    val categories = listOf(
        "Plumbing Services",
        "Cleaning Services",
        "Electrical Services",
        "Others"
    )
    var selectedTab by remember { mutableStateOf(UserHomeBottomTab.Home) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                selectedTab = UserHomeBottomTab.Home
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LogoTopAppBar(
                title = "Home",
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(32.dp)
                    )
                },
                actions = {
                    NotificationBell(count = notificationCount, onClick = onNotificationsClick)
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                NavigationBarItem(
                    selected = selectedTab == UserHomeBottomTab.Home,
                    onClick = { selectedTab = UserHomeBottomTab.Home },
                    icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(text = "Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == UserHomeBottomTab.NeedWorker,
                    onClick = {
                        selectedTab = UserHomeBottomTab.NeedWorker
                        onCreateJobClick()
                    },
                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Need Worker") },
                    label = { Text(text = "Need Worker") }
                )
                NavigationBarItem(
                    selected = selectedTab == UserHomeBottomTab.Works,
                    onClick = {
                        selectedTab = UserHomeBottomTab.Works
                        onMyJobsClick()
                    },
                    icon = { Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "Works") },
                    label = { Text(text = "Works") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        context.startActivity(Intent(context, UserMenuActivity::class.java))
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            val onSearch = {
                val q = searchQuery.trim()
                val intent = Intent(context, UserWorkersActivity::class.java)
                if (q.isNotBlank()) {
                    intent.putExtra(EXTRA_INITIAL_WORKER_SEARCH, q)
                }
                context.startActivity(intent)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = "Search for services...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(text = "Categories", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(10.dp))

            val visibleCategories = categories.filter { c ->
                val q = searchQuery.trim()
                q.isBlank() || c.contains(q, ignoreCase = true)
            }

            if (visibleCategories.isEmpty()) {
                Text(
                    text = "No categories found. Tap Search to find workers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                visibleCategories.chunked(2).forEach { row ->
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
                                    Text(
                                        text = category.replace(" Services", ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (row.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

        }
    }
}
