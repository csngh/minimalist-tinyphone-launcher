package com.minimalist.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.minimalist.launcher.AppEntry
import com.minimalist.launcher.LauncherViewModel
import com.minimalist.launcher.Slot
import com.minimalist.launcher.openAppInfo

/**
 * The complete app list. With [pickSlot] set it becomes a chooser for that shortcut,
 * and hidden apps are included so nothing can be locked out of selection.
 */
@Composable
fun AppListScreen(
    vm: LauncherViewModel,
    pickSlot: Slot?,
    onBack: () -> Unit,
    onOpen: (AppEntry) -> Unit,
    onChoose: (AppEntry?) -> Unit,
) {
    val accent = LocalAccent.current
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var options by remember { mutableStateOf<AppEntry?>(null) }
    val hidden = vm.prefs.hidden
    val all = vm.apps
    val visible = all.filter {
        it.label.contains(query.trim(), ignoreCase = true) &&
            (pickSlot != null || showHidden || it.key !in hidden)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenHeader(
            title = if (pickSlot != null) "Choose: ${pickSlot.label}" else "Apps",
            onBack = onBack,
            trailing = if (pickSlot == null) {
                { PjButton("SETUP", onClick = { vm.openSettings() }) }
            } else null,
        )

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = Pj.text, fontSize = PjType.body, fontFamily = PjFont),
            cursorBrush = SolidColor(accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .border(1.dp, Pj.edge, PjShape)
                .padding(horizontal = 12.dp),
            decorationBox = { inner ->
                Box(Modifier.heightIn(min = 56.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) PjText("Search", color = Pj.muted, size = PjType.body)
                    inner()
                }
            },
        )

        if (pickSlot == null) {
            PjText("Hold an app for Hide or App info.", size = PjType.caption, color = Pj.muted)
        }

        if (pickSlot == null && hidden.isNotEmpty()) {
            PjButton(
                if (showHidden) "HIDING: SHOWN (${hidden.size})" else "SHOW HIDDEN (${hidden.size})",
                Modifier.fillMaxWidth(), selected = showHidden,
            ) { showHidden = !showHidden }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (pickSlot != null && (pickSlot.hasDefault || vm.prefs.assigned[pickSlot] != null)) {
                item("default", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .clickableRow { onChoose(null) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) { PjText(if (pickSlot.hasBuiltIn) "Use built-in" else if (pickSlot.hasDefault) "Use system default" else "Clear this slot", color = accent, size = PjType.body) }
                }
            }
            if (all.isEmpty()) {
                item("loading", span = { GridItemSpan(maxLineSpan) }) { PjText("Loading apps…", color = Pj.muted, modifier = Modifier.padding(vertical = 12.dp)) }
            } else if (visible.isEmpty()) {
                item("none", span = { GridItemSpan(maxLineSpan) }) { PjText("No matching apps", color = Pj.muted, modifier = Modifier.padding(vertical = 12.dp)) }
            }
            items(visible, key = { it.key }) { app ->
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .alpha(if (app.key in hidden) 0.55f else 1f)
                        .machinedTile(pressed, accent)
                        .semantics { contentDescription = if (app.key in hidden) "${app.label}, hidden" else app.label }
                        .pjClick(
                            interaction,
                            onLongClick = if (pickSlot == null) ({ options = app }) else null,
                        ) { if (pickSlot != null) onChoose(app) else onOpen(app) }
                        .padding(horizontal = 2.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                ) {
                    ThemedIcon(app.key, 36.dp, if (app.key in hidden) Pj.muted else accent)
                    PjText(
                        app.label, size = PjType.caption, maxLines = 1, align = TextAlign.Center,
                        color = if (app.key in hidden) Pj.muted else Pj.text,
                    )
                }
            }
        }
    }

    options?.let { app ->
        val isHidden = app.key in hidden
        Dialog(onDismissRequest = { options = null }) {
            Column(
                Modifier.padding(horizontal = 8.dp).fillMaxWidth().background(Pj.surface, PjPanel)
                    .border(1.dp, Pj.edge, PjPanel).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PjText(app.label, size = PjType.body, weight = FontWeight.SemiBold, maxLines = 2)
                PjText(
                    if (isHidden) "Hidden from the default list. The app is still installed and enabled."
                    else "Hiding only removes it from this list. It is not disabled or uninstalled.",
                    size = PjType.caption, color = Pj.muted,
                )
                PjButton(if (isHidden) "SHOW IN LIST" else "HIDE FROM LIST", Modifier.fillMaxWidth()) {
                    vm.prefs.setHidden(app.key, !isHidden); options = null
                }
                PjButton("APP INFO", Modifier.fillMaxWidth()) { openAppInfo(ctx, app.packageName); options = null }
                PjButton("CLOSE", Modifier.fillMaxWidth()) { options = null }
            }
        }
    }
}

@Composable
private fun Modifier.clickableRow(onLong: (() -> Unit)? = null, onClick: () -> Unit): Modifier =
    pjClick(remember { MutableInteractionSource() }, onLongClick = onLong, onClick = onClick)
