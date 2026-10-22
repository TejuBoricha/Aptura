package org.aptura.player.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography

/**
 * High-precision Subtitle Web Search Assistant Dialog.
 *
 * Provides title sanitation review, provider selection (Subdl, OpenSubtitles, Google),
 * and dispatches a clean browser intent while safeguarding playback position.
 */
@Composable
fun WebSearchDialog(
    initialQuery: String,
    onDismissRequest: () -> Unit,
    onLaunchSearch: (query: String, providerIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember(initialQuery) { mutableStateOf(initialQuery) }
    var selectedProvider by remember { mutableIntStateOf(0) } // 0: Subdl, 1: OpenSubtitles, 2: Google
    val haptic = LocalHapticFeedback.current

    val providers = remember {
        listOf(
            Triple(0, "Subdl (Recommended)", "Direct multi-language .srt releases & sync hashes"),
            Triple(1, "OpenSubtitles Web", "Massive community-contributed subtitle index"),
            Triple(2, "Google Search", "Universal fallback search with exact release title")
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ApturaColors.SurfaceElevated2,
            border = BorderStroke(1.dp, ApturaColors.BorderSubtle),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ApturaColors.AccentSubtle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = ApturaColors.AccentPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Search Subtitles on Web",
                                style = ApturaTypography.headlineMedium.copy(fontSize = 17.sp),
                                color = ApturaColors.TextHighEmphasis
                            )
                            Text(
                                text = "Sanitized query for maximum search accuracy",
                                style = ApturaTypography.labelSmall,
                                color = ApturaColors.TextMediumEmphasis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ApturaColors.TextMediumEmphasis,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Query Input Field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SEARCH QUERY",
                        style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = ApturaColors.TextMediumEmphasis
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = ApturaTypography.bodyMedium.copy(
                            color = ApturaColors.TextHighEmphasis,
                            fontWeight = FontWeight.Medium
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = ApturaColors.AccentPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = ApturaColors.TextLowEmphasis,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ApturaColors.SurfaceElevated1,
                            unfocusedContainerColor = ApturaColors.SurfaceElevated1,
                            focusedBorderColor = ApturaColors.AccentPrimary,
                            unfocusedBorderColor = ApturaColors.BorderSubtle,
                            cursorColor = ApturaColors.AccentPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Provider Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SEARCH PROVIDER",
                        style = ApturaTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = ApturaColors.TextMediumEmphasis
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        providers.forEach { (index, title, description) ->
                            val isSelected = selectedProvider == index
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ApturaColors.SurfaceElevated3 else ApturaColors.SurfaceElevated1,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) ApturaColors.AccentPrimary else ApturaColors.BorderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedProvider = index
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) ApturaColors.AccentSubtle else ApturaColors.SurfaceElevated2,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = null,
                                                    tint = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextLowEmphasis,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = title,
                                                style = ApturaTypography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = if (isSelected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis
                                            )
                                            Text(
                                                text = description,
                                                style = ApturaTypography.labelSmall,
                                                color = ApturaColors.TextMediumEmphasis,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = ApturaColors.AccentPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Workflow Notice Banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ApturaColors.SurfaceElevated1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Video will pause automatically. Download .srt or .zip in your browser, then return to Aptura to attach in 1-tap.",
                        style = ApturaTypography.labelSmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                        color = ApturaColors.TextMediumEmphasis,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(contentColor = ApturaColors.TextMediumEmphasis)
                    ) {
                        Text("Cancel", style = ApturaTypography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val targetQuery = searchQuery.trim().ifBlank { initialQuery }
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onLaunchSearch(targetQuery, selectedProvider)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ApturaColors.AccentPrimary,
                            contentColor = ApturaColors.TextOnAccent
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Search in Browser",
                                style = ApturaTypography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = ApturaColors.TextOnAccent
                                )
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = ApturaColors.TextOnAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
