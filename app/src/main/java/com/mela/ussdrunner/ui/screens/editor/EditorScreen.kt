package com.mela.ussdrunner.ui.screens.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mela.ussdrunner.LocalAppContainer
import com.mela.ussdrunner.domain.model.SimPreference

private val suggestedCategories = listOf("Data", "Voice", "Balance", "Mobile Money", "Service", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    presetId: String?,
    onDone: () -> Unit,
    onRun: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: EditorViewModel = viewModel(
        factory = EditorViewModel.factory(presetId, container.presetRepository, container.widgetUpdater),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val dualSim = remember { container.simManager.isDualSim() }

    LaunchedEffect(state.savedId) {
        val id = state.savedId
        if (id != null) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (presetId == null) "Add USSD preset" else "Edit preset") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::setName,
                label = { Text("Name") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.ussdCode,
                onValueChange = vm::setCode,
                label = { Text("USSD code") },
                placeholder = { Text("*999*1*2#") },
                isError = state.codeError != null,
                supportingText = { Text(state.codeError ?: "Digits, * and # only") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::setDescription,
                label = { Text("Description (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.category,
                onValueChange = vm::setCategory,
                label = { Text("Category (optional)") },
                placeholder = { Text("Data, Balance, Voice…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestedCategories.forEach { cat ->
                    FilterChip(
                        selected = state.category.equals(cat, ignoreCase = true),
                        onClick = { vm.setCategory(cat) },
                        label = { Text(cat) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (dualSim) {
                DropdownField(
                    label = "SIM",
                    value = simLabel(state.sim),
                    options = listOf("Default SIM", "SIM 1", "SIM 2"),
                    onSelect = { selected ->
                        vm.setSim(
                            when (selected) {
                                "SIM 1" -> SimPreference.SLOT_0
                                "SIM 2" -> SimPreference.SLOT_1
                                else -> SimPreference.DEFAULT
                            },
                        )
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Favorite", modifier = Modifier.weight(1f))
                Switch(checked = state.favorite, onCheckedChange = vm::setFavorite)
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = vm::save, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Save")
            }
            if (presetId != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onRun(presetId) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) { Text("Test / run") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun simLabel(sim: SimPreference) = when (sim) {
    SimPreference.DEFAULT -> "Default SIM"
    SimPreference.SLOT_0 -> "SIM 1"
    SimPreference.SLOT_1 -> "SIM 2"
}
