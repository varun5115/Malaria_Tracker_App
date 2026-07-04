package com.varun.malariatracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Shared option lists pulled from the questionnaire. These are placeholders for the
// dropdowns whose real values sir will share later (addresses, names, sites, etc.).
val HEALTH_CENTRES = listOf("UHC Panaji", "PHC Candolim", "UHC Margao", "PHC Cortalim")
val RESEARCH_LABS = listOf("NIMR", "Goa University")
val PARASITE_SPECIES = listOf("P. falciparum", "P. vivax", "Mixed (Pf + Pv)", "Other")

/**
 * Read-only dropdown built on the same manual DropdownMenu + transparent IconButton
 * overlay pattern already used in DemographicsScreen, so it compiles cleanly on this
 * Compose BOM and stays visually consistent with the existing screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabeledDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
        IconButton(onClick = { expanded = true }, modifier = Modifier.matchParentSize()) {}
    }
}

/** Single-choice question rendered as a vertical group of radio buttons. */
@Composable
fun RadioGroup(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
    Column(modifier = Modifier.selectableGroup()) {
        options.forEach { opt ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == opt, onClick = { onSelect(opt) })
                Text(opt)
            }
        }
    }
}

/**
 * Multi-choice question rendered as checkboxes. Selection is held as an immutable
 * Set<String> so the caller can back it with rememberSaveable (a Set survives
 * rotation / process death, unlike a SnapshotStateMap).
 */
@Composable
fun CheckboxGroup(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String, Boolean) -> Unit
) {
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
    options.forEach { opt ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = opt in selected,
                onCheckedChange = { onToggle(opt, it) }
            )
            Text(opt)
        }
    }
}

/** Returns a new set with [item] added or removed — convenient for checkbox toggles. */
fun Set<String>.toggle(item: String, on: Boolean): Set<String> =
    if (on) this + item else this - item

/** CSV-safe "; "-joined string of selected options. */
fun Set<String>.joined(): String = joinToString("; ")

/**
 * Combines a "main" value with an optional free-text "Other" specification, e.g.
 * "Mixed (Pf + Pv)" or "Other (some text)". Keeps the empty case clean.
 */
fun withOther(main: String, otherText: String): String =
    if (otherText.isNotBlank()) {
        if (main.isBlank()) otherText else "$main ($otherText)"
    } else main
