package com.varun.malariatracker

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// Shared option lists pulled from the questionnaire. These are placeholders for the
// dropdowns whose real values sir will share later (addresses, names, sites, etc.).
val HEALTH_CENTRES = listOf("UHC Panaji", "PHC Candolim", "UHC Margao", "PHC Cortalim")
// Professional roles — the display label is also the value stored in Professionals.csv "Role".
val PROFESSIONAL_ROLES = listOf("Clinician", "Pathology", "Health Worker", "Research Lab", "Contractor")
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
            // The whole row is tappable, not just the small radio circle.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected == opt, onClick = { onSelect(opt) }, role = Role.RadioButton)
            ) {
                RadioButton(selected = selected == opt, onClick = null)
                Text(opt, modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
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
        // The whole row is tappable, not just the small checkbox.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = opt in selected, onValueChange = { onToggle(opt, it) }, role = Role.Checkbox)
        ) {
            Checkbox(checked = opt in selected, onCheckedChange = null)
            Text(opt, modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp))
        }
    }
}

/** Returns a new set with [item] added or removed — convenient for checkbox toggles. */
fun Set<String>.toggle(item: String, on: Boolean): Set<String> =
    if (on) this + item else this - item

/** CSV-safe "; "-joined string of selected options. */
fun Set<String>.joined(): String = joinToString("; ")

/** Joins the ticked options in the order they appear on screen. */
fun joinInOrder(options: List<String>, selected: Set<String>): String =
    options.filter { it in selected }.joinToString("; ")

/**
 * Splits a long form into pages so less scrolling is needed and fewer fields sit
 * behind the keyboard. Handles Next / Previous, the phone's Back button between
 * pages, per-page validation, and scrolling to the top of each new page.
 *
 * [validatePage] returns an error message to block moving on, or null to allow it.
 */
@Composable
fun FormPager(
    pageCount: Int,
    page: Int,
    onPageChange: (Int) -> Unit,
    finishLabel: String,
    onFinish: () -> Unit,
    validatePage: (Int) -> String? = { null },
    header: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.(Int) -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    LaunchedEffect(page) { scroll.scrollTo(0) }
    // Back steps to the previous page before it leaves the screen.
    BackHandler(enabled = page > 0) { onPageChange(page - 1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        header()
        if (pageCount > 1) {
            Text(
                text = "Page ${page + 1} of $pageCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        content(page)

        Spacer(modifier = Modifier.height(16.dp))

        val isLast = page >= pageCount - 1
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val error = validatePage(page)
                when {
                    error != null -> Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                    isLast -> onFinish()
                    else -> onPageChange(page + 1)
                }
            }
        ) {
            Text(if (isLast) finishLabel else "Next")
        }
        if (page > 0) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { onPageChange(page - 1) }) {
                Text("Previous")
            }
        }
        footer()
    }
}

/**
 * Combines a "main" value with an optional free-text "Other" specification, e.g.
 * "Mixed (Pf + Pv)" or "Other (some text)". Keeps the empty case clean.
 */
fun withOther(main: String, otherText: String): String =
    if (otherText.isNotBlank()) {
        if (main.isBlank()) otherText else "$main ($otherText)"
    } else main

// ---- Identifier-field validation (phone is the login key, so it matters most) ----

/** Strips whitespace so accidental spaces don't break equality/lookup. */
fun cleanPhone(raw: String): String = raw.filter { !it.isWhitespace() }

/** Exactly 10 digits after removing whitespace. */
fun isValidPhone(raw: String): Boolean =
    cleanPhone(raw).let { it.length == 10 && it.all(Char::isDigit) }

/** Exactly 12 digits after removing whitespace (Aadhaar). */
fun isValidAadhaar(raw: String): Boolean =
    raw.filter { !it.isWhitespace() }.let { it.length == 12 && it.all(Char::isDigit) }
