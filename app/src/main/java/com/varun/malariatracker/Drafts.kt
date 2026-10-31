package com.varun.malariatracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Unsaved form answers, kept in SharedPreferences so they survive Back, rotation
 * and the app being closed. Each form uses a draft key, e.g. "worker:<phone>",
 * "visit:<phone>" or "pro:<phone>:<role>"; every answer is stored under
 * "<key>|<field>". A draft is cleared once its form is saved or cancelled.
 */
object Drafts {
    private fun prefs(context: Context) = context.getSharedPreferences("drafts", Context.MODE_PRIVATE)

    fun get(context: Context, key: String, field: String): String? =
        prefs(context).getString("$key|$field", null)

    fun put(context: Context, key: String, field: String, value: String) =
        prefs(context).edit().putString("$key|$field", value).apply()

    fun getSet(context: Context, key: String, field: String): Set<String>? =
        prefs(context).getStringSet("$key|$field", null)?.toSet()

    fun putSet(context: Context, key: String, field: String, value: Set<String>) =
        prefs(context).edit().putStringSet("$key|$field", value).apply()

    fun clear(context: Context, key: String) {
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys.filter { it.startsWith("$key|") }.forEach { editor.remove(it) }
        editor.apply()
    }
}

/** A text answer that is read from, and written back to, the draft. */
@Composable
fun rememberDraft(key: String, field: String, default: String = ""): MutableState<String> {
    val context = LocalContext.current
    val state = remember(key, field) { mutableStateOf(Drafts.get(context, key, field) ?: default) }
    LaunchedEffect(key, field, state.value) { Drafts.put(context, key, field, state.value) }
    return state
}

/** A multi-select answer backed by the draft. */
@Composable
fun rememberDraftSet(key: String, field: String): MutableState<Set<String>> {
    val context = LocalContext.current
    val state = remember(key, field) { mutableStateOf(Drafts.getSet(context, key, field) ?: emptySet()) }
    LaunchedEffect(key, field, state.value) { Drafts.putSet(context, key, field, state.value) }
    return state
}

/** The current page of a form, backed by the draft so a resumed form opens where it was left. */
@Composable
fun rememberDraftPage(key: String, field: String = "page"): MutableState<Int> {
    val context = LocalContext.current
    val state = remember(key, field) { mutableStateOf(Drafts.get(context, key, field)?.toIntOrNull() ?: 0) }
    LaunchedEffect(key, field, state.value) { Drafts.put(context, key, field, state.value.toString()) }
    return state
}
