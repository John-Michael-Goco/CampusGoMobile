package com.campusgomobile.ui.auth

import android.util.Patterns
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.campusgomobile.R

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var studentNumber by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var course by remember { mutableStateOf("") }
    var yearLevel by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var logoVisible by remember { mutableStateOf(false) }
    var fieldErrors by remember { mutableStateOf<Map<String, String?>>(emptyMap()) }
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        val sn = studentNumber.trim()
        val fn = firstName.trim()
        val ln = lastName.trim()
        val c = course.trim()
        val yl = yearLevel.trim()
        val e = email.trim()
        val p = password
        val year = yl.toIntOrNull()
        val err = mutableMapOf<String, String?>()
        if (sn.isEmpty()) err["student_number"] = "Please enter your student number"
        else err["student_number"] = null
        if (fn.isEmpty()) err["first_name"] = "Please enter your first name"
        else err["first_name"] = null
        if (ln.isEmpty()) err["last_name"] = "Please enter your last name"
        else err["last_name"] = null
        if (c.isEmpty()) err["course"] = "Please enter your course"
        else err["course"] = null
        if (yl.isEmpty()) err["year_level"] = "Please enter year level (1–10)"
        else if (year == null || year !in 1..10) err["year_level"] = "Year level must be 1–10"
        else err["year_level"] = null
        if (e.isEmpty()) err["email"] = "Please enter your email"
        else if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) err["email"] = "Please enter a valid email address"
        else err["email"] = null
        if (p.isEmpty()) err["password"] = "Please enter your password"
        else if (p.length < 8) err["password"] = "Password must be at least 8 characters"
        else err["password"] = null
        fieldErrors = err
        return err.values.all { it == null }
    }

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.6f,
        animationSpec = tween(500), label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(500), label = "logoAlpha"
    )

    LaunchedEffect(Unit) { logoVisible = true }
    LaunchedEffect(Unit) { viewModel.clearError() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.sign_logo),
            contentDescription = "Campus Go",
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(300.dp)
                .scale(logoScale)
                .alpha(logoAlpha),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            text = "Join the quest — start your adventure",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

        fun fieldError(key: String): String? =
            fieldErrors[key] ?: uiState.fieldErrors?.get(key)?.firstOrNull()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = studentNumber,
                    onValueChange = { studentNumber = it; fieldErrors = fieldErrors - "student_number"; viewModel.clearError() },
                    label = { Text("Student number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = fieldError("student_number") != null
                )
                fieldError("student_number")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it; fieldErrors = fieldErrors - "first_name"; viewModel.clearError() },
                    label = { Text("First name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = fieldError("first_name") != null
                )
                fieldError("first_name")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it; fieldErrors = fieldErrors - "last_name"; viewModel.clearError() },
                    label = { Text("Last name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = fieldError("last_name") != null
                )
                fieldError("last_name")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it; fieldErrors = fieldErrors - "course"; viewModel.clearError() },
                    label = { Text("Course") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = fieldError("course") != null
                )
                fieldError("course")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                OutlinedTextField(
                    value = yearLevel,
                    onValueChange = { yearLevel = it.filter(Char::isDigit); fieldErrors = fieldErrors - "year_level"; viewModel.clearError() },
                    label = { Text("Year level") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = fieldError("year_level") != null
                )
                fieldError("year_level")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; fieldErrors = fieldErrors - "email"; viewModel.clearError() },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = fieldError("email") != null
                )
                fieldError("email")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; fieldErrors = fieldErrors - "password"; viewModel.clearError() },
                    label = { Text("Password (min 8 characters)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = fieldError("password") != null
                )
                fieldError("password")?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Button(
                        onClick = {
                            if (validate()) {
                                val year = yearLevel.trim().toIntOrNull() ?: 0
                                viewModel.signUp(
                                    studentNumber = studentNumber.trim(),
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    course = course.trim(),
                                    yearLevel = year,
                                    email = email.trim(),
                                    password = password
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Create account", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onSignInClick) {
            Text("Already have an account? Sign in", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
