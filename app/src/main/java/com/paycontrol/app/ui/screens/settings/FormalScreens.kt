package com.paycontrol.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paycontrol.app.R
import com.paycontrol.app.domain.model.AppInfo
import com.paycontrol.app.domain.model.FormalContent
import com.paycontrol.app.ui.components.SoftPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormalInfoScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    FormalInfoScaffold(stringResource(R.string.about), onBack) {
        SoftPanel {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text("Versión ${AppInfo.VERSION_NAME} (${AppInfo.VERSION_CODE})")
            Text(AppInfo.COMPANY, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Aplicación nativa Android offline-first para control financiero personal y comercial.",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    FormalInfoScaffold(stringResource(R.string.changelog), onBack) {
        FormalContent.changelog.forEach { entry ->
            SoftPanel {
                Text("${entry.version} · ${entry.date}", fontWeight = FontWeight.SemiBold)
                entry.highlights.forEach { item ->
                    Text("• $item", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun TeamScreen(onBack: () -> Unit) {
    val resources = LocalContext.current.resources
    FormalInfoScaffold(stringResource(R.string.team), onBack) {
        FormalContent.team(resources).forEach { member ->
            SoftPanel {
                Text(member.name, fontWeight = FontWeight.SemiBold)
                Text(member.role, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val resources = LocalContext.current.resources
    FormalInfoScaffold(stringResource(R.string.licenses), onBack) {
        SoftPanel {
            FormalContent.licenses(resources).forEach { license ->
                Text("• $license", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val resources = LocalContext.current.resources
    FormalInfoScaffold(stringResource(R.string.privacy), onBack) {
        SoftPanel {
            Text(
                FormalContent.privacySummary(resources),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
