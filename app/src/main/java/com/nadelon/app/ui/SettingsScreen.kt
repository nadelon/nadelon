package com.nadelon.app.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nadelon.app.ui.theme.Nadelon

// Study — the setup room. Quiet, warm. Credentials sit on a plate, not in a form.
// Password is a transient scrap: typed once, burned after use, never filed.
@Composable
fun SettingsScreen(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsState()
    val openSubs by vm.openSubsState.collectAsState()
    val palette = Nadelon.palette

    var apiKey by rememberSaveable(settings.apiKey) { mutableStateOf(settings.apiKey) }
    var username by rememberSaveable(settings.username) { mutableStateOf(settings.username) }
    // Intentionally transient: never persisted, cleared on successful login.
    var password by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = palette.oakStrong,
        unfocusedBorderColor = palette.oak,
        focusedTextColor = palette.ink,
        unfocusedTextColor = palette.ink,
        focusedLabelColor = palette.margin,
        unfocusedLabelColor = palette.margin,
        cursorColor = palette.lamplight,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = Nadelon.Space.column,
                vertical = Nadelon.Space.reading,
            ),
        verticalArrangement = Arrangement.spacedBy(Nadelon.Space.reading)
    ) {
        Column {
            Text(
                "Study",
                style = MaterialTheme.typography.displayLarge,
                color = palette.ink,
            )
            Text(
                "OpenSubtitles · a standing library card",
                style = MaterialTheme.typography.labelMedium,
                color = palette.margin,
            )
        }

        Text(
            text = "Create a free API key at opensubtitles.com → Consumers. " +
                "A user account is needed to download. Your password is used once to sign in; " +
                "it is never filed on this device.",
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = palette.inkFaint,
            style = MaterialTheme.typography.bodyMedium,
        )

        Surface(
            shape = RoundedCornerShape(Nadelon.Radius.card),
            color = palette.page,
            border = BorderStroke(0.5.dp, palette.oak),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = Nadelon.Space.column,
                    vertical = Nadelon.Space.reading,
                ),
                verticalArrangement = Arrangement.spacedBy(Nadelon.Space.reading)
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("api key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(Nadelon.Radius.input),
                    colors = textFieldColors,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(Nadelon.Radius.input),
                    colors = textFieldColors,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("password · not filed") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "OpenSubtitles password, not saved" },
                    singleLine = true,
                    shape = RoundedCornerShape(Nadelon.Radius.input),
                    colors = textFieldColors,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        autoCorrect = false
                    )
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Nadelon.Space.gutter),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "save",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.inkFaint,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            vm.saveCredentials(apiKey, username)
                        }
                    )
                    Text(
                        text = if (openSubs.busy) "signing in…" else "save & sign in",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (openSubs.busy) palette.muted else palette.lamplight,
                        textDecoration = TextDecoration.Underline,
                        modifier = if (!openSubs.busy) Modifier.clickable {
                            vm.saveCredentials(apiKey, username)
                            vm.loginOpenSubs(password)
                            password = ""
                        } else Modifier,
                    )
                }
            }
        }

        if (settings.token.isNotBlank()) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(6.dp)
                        .background(palette.marked, RoundedCornerShape(1.dp))
                )
                Spacer(Modifier.width(Nadelon.Space.snug))
                Text(
                    "Signed in — download token on file.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = palette.lamplight,
                )
            }
        }
        openSubs.message?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = palette.margin,
            )
        }

        Spacer(Modifier.height(Nadelon.Space.gutter))
    }
}
