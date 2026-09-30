package com.composebasics.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.imePadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composebasics.data.repository.AuthRepository
import com.composebasics.ui.appViewModel
import com.composebasics.ui.components.EmailField
import com.composebasics.ui.components.PasswordField

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    viewModel: LoginViewModel = appViewModel { LoginViewModel(it.authRepository) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.loggedIn) { if (state.loggedIn) onLoggedIn() }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Welcome back", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Sign in to continue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            EmailField(state.email, viewModel::onEmailChange, state.emailError, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            PasswordField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                error = state.passwordError,
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Done,
                onImeAction = viewModel::login,
            )

            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?")
            }

            state.formError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = viewModel::login,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Log in")
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSignUp) { Text("Don't have an account? Sign up") }

            Spacer(Modifier.height(16.dp))
            Text(
                "Demo account: ${AuthRepository.DEMO_EMAIL} / ${AuthRepository.DEMO_PASSWORD}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
