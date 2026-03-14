package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusgomobile.data.model.User
import com.campusgomobile.data.model.UserStudent
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.theme.Amber400
import com.campusgomobile.ui.theme.CampusGoBlue
import com.campusgomobile.ui.theme.Emerald500
import com.campusgomobile.ui.theme.Indigo500
import com.campusgomobile.ui.theme.Teal400
import com.campusgomobile.ui.theme.Violet600
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
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            ProfileHeroCard(user = user)

            Spacer(Modifier.height(24.dp))

            SectionHeader(title = "Game", icon = Icons.Default.Leaderboard)
            Spacer(Modifier.height(12.dp))
            ProfileMenuItem(
                icon = Icons.Default.Leaderboard,
                title = "Leaderboard",
                onClick = { navController.navigate(NavRoutes.PROFILE_LEADERBOARD) },
                accentColor = Violet600
            )
            Spacer(Modifier.height(10.dp))
            ProfileMenuItem(
                icon = Icons.Default.EmojiEvents,
                title = "Achievements",
                onClick = { navController.navigate(NavRoutes.PROFILE_ACHIEVEMENTS) },
                accentColor = Amber400
            )
            Spacer(Modifier.height(10.dp))
            ProfileMenuItem(
                icon = Icons.Default.Inventory,
                title = "Inventory",
                onClick = { navController.navigate(NavRoutes.PROFILE_INVENTORY) },
                accentColor = Teal400
            )

            Spacer(Modifier.height(20.dp))

            SectionHeader(title = "Account", icon = Icons.Default.Settings)
            Spacer(Modifier.height(12.dp))
            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Edit profile",
                onClick = { navController.navigate(NavRoutes.PROFILE_EDIT) },
                accentColor = CampusGoBlue
            )
            Spacer(Modifier.height(10.dp))
            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "Transfer points",
                onClick = { navController.navigate(NavRoutes.PROFILE_TRANSFER_POINTS) },
                accentColor = Emerald500
            )
            Spacer(Modifier.height(10.dp))
            ProfileMenuItem(
                icon = Icons.Default.History,
                title = "Transaction history",
                onClick = { navController.navigate(NavRoutes.PROFILE_TRANSACTIONS) },
                accentColor = Indigo500
            )
            Spacer(Modifier.height(10.dp))
            ProfileMenuItem(
                icon = Icons.Default.History,
                title = "Activity log",
                onClick = { navController.navigate(NavRoutes.PROFILE_ACTIVITY) },
                accentColor = Indigo500
            )

            Spacer(Modifier.height(24.dp))
            SignOutButton(onClick = { viewModel.signOut() })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileHeroCard(user: User?) {
    val gradient = Brush.linearGradient(listOf(CampusGoBlue, Indigo500))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(gradient)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(12.dp))
                if (user?.profileImage != null) {
                    AsyncImage(
                        model = user.profileImage,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitials(user),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = user?.name ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = user?.email ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                user?.student?.let { student ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = buildStudentInfo(student),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(label = "Pts", value = "${user?.pointsBalance ?: 0}", color = Amber400)
                    StatPill(label = "Level", value = "${user?.level ?: 1}", color = Emerald500)
                    StatPill(label = "XP", value = "${user?.totalXpEarned ?: 0}", color = Teal400)
                    StatPill(label = "Quests", value = "${user?.totalCompletedQuests ?: 0}", color = Violet600)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
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
                    .size(44.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
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
}

@Composable
private fun SignOutButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
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
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.onError.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "Sign out",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError
            )
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
