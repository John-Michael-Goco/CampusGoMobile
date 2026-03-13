package com.campusgomobile.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.UserStudent
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.auth.AuthViewModel

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState(initial = null)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Header: avatar, name, email, stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardDefaults.shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = user?.name ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user?.email ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                user?.student?.let { student ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildStudentInfo(student),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat("Points", (user?.pointsBalance ?: 0).toString())
                    ProfileStat("Level", (user?.level ?: 1).toString())
                    ProfileStat("XP", (user?.totalXpEarned ?: 0).toString())
                    ProfileStat("Quests", (user?.totalCompletedQuests ?: 0).toString())
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
        ProfileMenuItem(
            icon = Icons.Default.Leaderboard,
            title = "Leaderboard",
            onClick = { navController.navigate(NavRoutes.PROFILE_LEADERBOARD) }
        )
        ProfileMenuItem(
            icon = Icons.Default.EmojiEvents,
            title = "Achievements",
            onClick = { navController.navigate(NavRoutes.PROFILE_ACHIEVEMENTS) }
        )
        ProfileMenuItem(
            icon = Icons.Default.Inventory,
            title = "Inventory",
            onClick = { navController.navigate(NavRoutes.PROFILE_INVENTORY) }
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
            onClick = { navController.navigate(NavRoutes.PROFILE_EDIT) }
        )
        ProfileMenuItem(
            icon = Icons.Default.History,
            title = "Transaction history",
            onClick = { navController.navigate(NavRoutes.PROFILE_TRANSACTIONS) }
        )
        ProfileMenuItem(
            icon = Icons.Default.History,
            title = "Activity log",
            onClick = { navController.navigate(NavRoutes.PROFILE_ACTIVITY) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Sign out", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(32.dp))
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
            color = MaterialTheme.colorScheme.primary
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(16.dp))
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
