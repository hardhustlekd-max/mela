package com.mela.ussdrunner.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mela.ussdrunner.domain.model.Preset

@Composable
fun PresetCard(
    preset: Preset,
    showCode: Boolean,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onEdit,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (showCode) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = preset.ussdCode,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val meta = buildList {
                        preset.category?.let { add(it) }
                        if (preset.isSample) add("Example — verify with your carrier")
                    }.joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.semantics { contentDescription = "Toggle favorite" },
                ) {
                    Icon(
                        imageVector = if (preset.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (preset.isFavorite) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onRun,
                    modifier = Modifier
                        .height(44.dp)
                        .semantics { contentDescription = "Run ${preset.name}" },
                ) {
                    Text("RUN")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
