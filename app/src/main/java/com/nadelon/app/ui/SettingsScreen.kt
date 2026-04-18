package com.nadelon.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    val openSubs by vm.openSubsState.collectAsState()

    var apiKey by rememberSaveable(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var username by rememberSaveable(settings.username) { mutableStateOf(settings.username) }
    // Password is intentionally transient: never persisted, cleared on successful login.
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("OpenSubtitles", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(
            "Create a free API key at opensubtitles.com → Consumers. " +
                "A user account is required to download subtitles. " +
                "Your password is only used for login and is never stored on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (not saved)") },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "OpenSubtitles password, not saved" },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrect = false
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.saveCredentials(apiKey, username) }
            ) { Text("Save") }
            OutlinedButton(
                onClick = {
                    vm.saveCredentials(apiKey, username)
                    vm.loginOpenSubs(password)
                    password = ""
                },
                enabled = !openSubs.busy
            ) { Text(if (openSubs.busy) "Working…" else "Save & log in") }
        }

        if (settings.token.isNotBlank()) {
            Text(
                "Signed in. Download token cached.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        openSubs.message?.let { msg ->
            Text(msg, style = MaterialTheme.typography.bodySmall)
        }
    }
}
