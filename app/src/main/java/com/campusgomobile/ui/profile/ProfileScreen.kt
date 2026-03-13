package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusgomobile.data.model.UserStudent
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.theme.Blue50
import com.campusgomobile.ui.theme.Blue500
import com.campusgomobile.ui.theme.Blue600
import com.campusgomobile.util.userInitials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    navController: NavController,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState(initial = null)
    val scrollState = rememberScrollState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(isVisible) {
        if (isVisible) viewModel.refreshUser()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.refreshUser()
                delay(1200)
                isRefreshing = false
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
        // Header: avatar, name, email, stats — blue accent
        val headerBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else Blue50
        val headerOnBg = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Blue600
        val avatarBg = if (isDark) MaterialTheme.colorScheme.primary else Blue500
        val avatarOn = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = headerBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user?.profileImage != null) {
                    AsyncImage(
                        model = user!!.profileImage,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials(user),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = avatarOn
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user?.name ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = headerOnBg
                )
                Text(
                    text = user?.email ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else Blue600.copy(alpha = 0.85f)
                )
                user?.student?.let { student ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildStudentInfo(student),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f) else Blue600.copy(alpha = 0.75f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat("Points", (user?.pointsBalance ?: 0).toString(), headerOnBg)
                    ProfileStat("Level", (user?.level ?: 1).toString(), headerOnBg)
                    ProfileStat("XP", (user?.totalXpEarned ?: 0).toString(), headerOnBg)
                    ProfileStat("Quests", (user?.totalCompletedQuests ?: 0).toString(), headerOnBg)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Game",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        val menuIconBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else null
        val menuIconFg = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else null
        ProfileMenuItem(
            icon = Icons.Default.Leaderboard,
            title = "Leaderboard",
            onClick = { navController.navigate(NavRoutes.PROFILE_LEADERBOARD) },
            iconContainerColor = menuIconBg ?: Blue50,
            iconTint = menuIconFg ?: Blue600
        )
        ProfileMenuItem(
            icon = Icons.Default.EmojiEvents,
            title = "Achievements",
            onClick = { navController.navigate(NavRoutes.PROFILE_ACHIEVEMENTS) },
            iconContainerColor = menuIconBg ?: Blue50,
            iconTint = menuIconFg ?: Blue600
        )
        ProfileMenuItem(
            icon = Icons.Default.Inventory,
            title = "Inventory",
            onClick = { navController.navigate(NavRoutes.PROFILE_INVENTORY) },
            iconContainerColor = menuIconBg ?: Blue50,
            iconTint = menuIconFg ?: Blue600
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Account",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )
        ProfileMenuItem(
            icon = Icons.Default.Settings,
            title = "Edit profile",
            onClick = { navController.navigate(NavRoutes.PROFILE_EDIT) },
            iconContainerColor = menuIconBg ?: Blue50,
            iconTint = menuIconFg ?: Blue600
        )
        ProfileMenuItem(
            icon = Icons.AutoMirrored.Filled.Send,
            title = "Transfer points",
            onClick = { navController.navigate(NavRoutes.PROFILE_TRANSFER_POINTS) },
            iconContainerColor = menuIconBg ?: Blue50,
            iconTint = menuIconFg ?: Blue600
        )
        ProfileMenuItem(
            icon = Icons.Default.History,
            title = "Transaction history",
            onClick = { navController.navigate(NavRoutes.PROFILE_TRANSACTIONS) },
            iconContainerColor = menuIconBg ?: Blue50,
            iconTint = menuIconFg ?: Blue600
        )
        ProfileMenuItem(
            icon = Icons.Default.History,
            title = "Activity log",
            onClick = { navController.navigate(NavRoutes.PROFILE_ACTIVITY) },
            iconContainerColor = menuIconBg ?: Blue50,
            iconTint = menuIconFg ?: Blue600
        )

        Spacer(modifier = Modifier.height(24.dp))
        SignOutButton(onClick = { viewModel.signOut() })
        Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun buildStudentInfo(student: UserStudent): String {
    val id = student.studentNumber?.takeIf { it.isNotBlank() }
    val course = student.course?.takeIf { it.isNotBlank() } ?: ""
    val year = student.yearLevel?.toString() ?: ""
    val section = student.section?.takeIf { it.isNotBlank() } ?: ""
    val courseYearSection = when {
        course.isNotEmpty() && year.isNotEmpty() && section.isNotEmpty() -> "$course – $year ($section)"
        course.isNotEmpty() && year.isNotEmpty() -> "$course – $year"
        course.isNotEmpty() && section.isNotEmpty() -> "$course ($section)"
        course.isNotEmpty() -> course
        year.isNotEmpty() && section.isNotEmpty() -> "$year ($section)"
        year.isNotEmpty() -> year
        section.isNotEmpty() -> "($section)"
        else -> ""
    }
    return when {
        id != null && courseYearSection.isNotEmpty() -> "ID $id · $courseYearSection"
        id != null -> "ID $id"
        courseYearSection.isNotEmpty() -> courseYearSection
        else -> "—"
    }
}

@Composable
private fun ProfileStat(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    iconContainerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SignOutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val red = MaterialTheme.colorScheme.error
    val white = MaterialTheme.colorScheme.onError
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = red),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = white
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = "Sign out",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = white,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = white
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
