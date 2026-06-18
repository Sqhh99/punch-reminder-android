package com.sqhh99.punchreminder.ui.apppicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.sqhh99.punchreminder.domain.model.InstalledApp
import com.sqhh99.punchreminder.viewmodel.AppPickerUiState

const val AppPickerScreenTag = "app_picker_screen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    state: AppPickerUiState,
    onQueryChange: (String) -> Unit,
    onPick: (InstalledApp) -> Unit,
    iconProvider: (String) -> android.graphics.drawable.Drawable?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(AppPickerScreenTag),
        topBar = {
            TopAppBar(
                title = { Text("选择目标应用") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("搜索应用") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.apps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            icon = remember(app.packageName) { iconProvider(app.packageName) },
                            onClick = { onPick(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    icon: android.graphics.drawable.Drawable?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) {
            val bitmap = remember(app.packageName) { icon.toBitmap(96, 96).asImageBitmap() }
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        } else {
            Box(modifier = Modifier.size(40.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
