package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusgomobile.data.model.TransferStudent
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.theme.CampusGoBlue
import com.campusgomobile.ui.theme.Emerald500
import com.campusgomobile.ui.theme.Indigo500
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.util.nameToInitials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferPointsScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState(initial = null)
    val state by viewModel.transferPointsState.collectAsState()
    var studentIdInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.clearTransferState()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileScreenTopBar(
                title = "Transfer points",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            SectionHeader(title = "Transfer Pts", icon = Icons.AutoMirrored.Filled.Send)
            Spacer(Modifier.height(12.dp))

            user?.pointsBalance?.let { balance ->
                val gradient = Brush.linearGradient(listOf(CampusGoBlue, Indigo500))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(gradient)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your balance",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                text = "$balance Pts",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            Text(
                text = "Search by student number (school ID)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = studentIdInput,
                onValueChange = { studentIdInput = it },
                label = { Text("Student number") },
                placeholder = { Text("e.g. 2024-001") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.searchLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            if (state.searchError != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.searchError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { viewModel.searchStudent(studentIdInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.searchLoading && studentIdInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.searchLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(if (state.searchLoading) "Searching…" else "Search")
            }

            state.student?.let { student ->
                Spacer(Modifier.height(20.dp))
                RecipientCard(
                    student = student,
                    transferSuccessMessage = state.transferSuccessMessage,
                    transferError = state.transferError,
                    transferLoading = state.transferLoading
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Amount: ${state.amount} Pts (min 10, max 100)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = state.amount.toFloat(),
                    onValueChange = { viewModel.setTransferAmount(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Button(
                    onClick = { viewModel.transferPoints() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.transferLoading
                        && state.transferSuccessMessage == null
                        && (user?.pointsBalance ?: 0) >= state.amount,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.transferLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        if (state.transferLoading) "Transferring…"
                        else "Transfer ${state.amount} Pts"
                    )
                }
            }
        }
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
private fun RecipientCard(
    student: TransferStudent,
    transferSuccessMessage: String? = null,
    transferError: String? = null,
    transferLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val detailLine = buildRecipientDetailLine(student)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (student.profileImage != null) {
                AsyncImage(
                    model = student.profileImage,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nameToInitials(student.name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Recipient",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = student.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (detailLine.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            student.pointsBalance?.let { balance ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$balance Pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (transferLoading || transferSuccessMessage != null || transferError != null) {
                Spacer(Modifier.height(12.dp))
                when {
                    transferLoading -> Text(
                        text = "Transferring…",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    transferSuccessMessage != null -> Text(
                        text = transferSuccessMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Emerald600
                    )
                    transferError != null -> Text(
                        text = transferError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun buildRecipientDetailLine(student: TransferStudent): String {
    val id = "ID ${student.schoolId}"
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
        courseYearSection.isNotEmpty() -> "$id · $courseYearSection"
        else -> id
    }
}
