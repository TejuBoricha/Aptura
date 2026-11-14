package org.aptura.player.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.aptura.player.core.legal.LicenseDocument
import org.aptura.player.core.legal.LicenseDocuments
import org.aptura.player.core.legal.LicenseText
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.StableNavigationBars
import org.aptura.player.ui.components.StableStatusBars

internal const val LICENSES_BACK_TAG = "licenses_back"
internal const val LICENSES_SOURCE_TAG = "licenses_ffmpeg_source"
internal fun licenseDocumentTag(id: String) = "license_document_$id"

/** The list starts with the notice panel and the "Licence texts" label, then one row per document. */
private const val LEADING_ITEMS = 2

private const val UNREADABLE = "This text could not be read from the app. The same texts are in the project repository."

/**
 * Settings > Open-source licences. The notice that FFmpeg is used (LGPL-2.1 section 6) sits above the list, where nobody has to
 * look for it; each licence text opens in place. The texts come from assets packaged by the build (see [LicenseDocuments]).
 */
@Composable
fun LicensesScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // Only a test replaces this, to make a read slow: how quickly a text arrives must never decide where the list ends up.
    readText: (android.content.Context, LicenseDocument) -> String = LicenseDocuments::read
) {
    val context = LocalContext.current
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    val listState = rememberLazyListState()

    // A text is read from the assets the first time it is opened, off the main thread, and split into paragraphs so a long
    // licence is laid out piece by piece instead of as one enormous block.
    LaunchedEffect(expandedId) {
        val document = LicenseDocuments.all.firstOrNull { it.id == expandedId } ?: return@LaunchedEffect
        if (document.id in loaded) return@LaunchedEffect
        val paragraphs = withContext(Dispatchers.IO) {
            runCatching { LicenseText.paragraphs(readText(context, document)) }.getOrElse { listOf(UNREADABLE) }
        }
        loaded = loaded + (document.id to paragraphs)
    }

    // An opened text starts right under its row, so a row tapped near the bottom would open its text below the fold: bring the
    // row to the top. This waits until the text is in the list. Scrolling while it was still being read left only a "Reading…"
    // line below the row, so the list could not scroll far enough, and nothing moved it again once the text arrived. Only one
    // text is open at a time, so a row's place in the list is its place in the order.
    val expandedTextIsShown = expandedId != null && expandedId in loaded
    LaunchedEffect(expandedId, expandedTextIsShown) {
        if (!expandedTextIsShown) return@LaunchedEffect
        val position = LicenseDocuments.all.indexOfFirst { it.id == expandedId }
        if (position >= 0) listState.animateScrollToItem(LEADING_ITEMS + position)
    }

    val statusTop = StableStatusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = StableNavigationBars.asPaddingValues().calculateBottomPadding()
    val topBar = statusTop + SETTINGS_TOP_BAR_HEIGHT_DP.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ApturaColors.SurfaceBase),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topBar + 8.dp, bottom = navigationBottom + 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "intro", contentType = "panel") {
                SettingsPanel {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = LicenseDocuments.FFMPEG_NOTICE,
                            style = ApturaTypography.bodyMedium,
                            color = ApturaColors.TextHighEmphasis
                        )
                        Row {
                            PillButton(
                                text = "View FFmpeg source",
                                onClick = { openUrl(context, LicenseDocuments.FFMPEG_SOURCE_URL) },
                                modifier = Modifier.testTag(LICENSES_SOURCE_TAG),
                                leading = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = ApturaColors.AccentPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item(key = "label", contentType = "label") {
                SectionLabel("Licence texts", modifier = Modifier.padding(top = 8.dp))
            }

            LicenseDocuments.all.forEach { document ->
                val expanded = expandedId == document.id
                item(key = "document-${document.id}", contentType = "document") {
                    DocumentHeader(
                        document = document,
                        expanded = expanded,
                        onClick = { expandedId = if (expanded) null else document.id }
                    )
                }
                if (expanded) {
                    val paragraphs = loaded[document.id]
                    if (paragraphs == null) {
                        item(key = "document-${document.id}-loading", contentType = "paragraph") {
                            Text(
                                text = "Reading…",
                                style = ApturaTypography.bodySmall,
                                color = ApturaColors.TextLowEmphasis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    } else {
                        itemsIndexed(paragraphs, key = { index, _ -> "document-${document.id}-paragraph-$index" }, contentType = { _, _ -> "paragraph" }) { _, paragraph ->
                            Text(
                                text = paragraph,
                                style = ApturaTypography.bodySmall,
                                color = ApturaColors.TextMediumEmphasis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // An opaque strip exactly as tall as the back button and title row, so text scrolling beneath never shows through them.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topBar)
                .background(SettingsTopBarBackground)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ApturaColors.BorderSubtle)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusTop)
                .height(SETTINGS_TOP_BAR_HEIGHT_DP.dp)
                .padding(horizontal = 8.dp)
        ) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = "Back",
                onClick = onBackClick,
                modifier = Modifier.align(Alignment.CenterStart).testTag(LICENSES_BACK_TAG)
            )
            Text(
                text = "Open-source licences",
                style = ApturaTypography.headlineMedium,
                color = ApturaColors.TextHighEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 56.dp)
                    .semantics { heading() }
            )
        }
    }
}

@Composable
private fun DocumentHeader(document: LicenseDocument, expanded: Boolean, onClick: () -> Unit) {
    SettingsPanel(modifier = Modifier.testTag(licenseDocumentTag(document.id))) {
        Box(modifier = Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }) {
            ActionRow(
                icon = Icons.Default.Description,
                title = document.title,
                subtitle = document.summary,
                onClick = onClick,
                trailing = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ApturaColors.TextLowEmphasis
                    )
                }
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
    }
}
