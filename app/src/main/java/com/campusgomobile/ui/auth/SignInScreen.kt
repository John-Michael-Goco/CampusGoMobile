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
fun SignInScreen(
    viewModel: AuthViewModel,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var logoVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    fun validate(): Boolean {
        val e = email.trim()
        val p = password
        var valid = true
        emailError = when {
            e.isEmpty() -> { valid = false; "Please enter your email" }
            !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> { valid = false; "Please enter a valid email address" }
            else -> null
        }
        passwordError = when {
            p.isEmpty() -> { valid = false; "Please enter your password" }
            else -> null
        }
        return valid
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
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) { /* navigation handled by parent */ }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
            text = "Sign In",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Continue your adventure",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))

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
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                        viewModel.clearError()
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = emailError != null || uiState.fieldErrors?.get("email")?.isNotEmpty() == true
                )
                (emailError ?: uiState.fieldErrors?.get("email")?.firstOrNull())?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                        viewModel.clearError()
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    isError = passwordError != null || uiState.fieldErrors?.get("password")?.isNotEmpty() == true
                )
                (passwordError ?: uiState.fieldErrors?.get("password")?.firstOrNull())?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Button(
                        onClick = {
                            if (validate()) {
                                viewModel.signIn(email.trim(), password)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Sign in", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onSignUpClick) {
            Text("New user? Join now", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
