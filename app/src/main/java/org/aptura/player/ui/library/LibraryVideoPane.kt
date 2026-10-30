package org.aptura.player.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.aptura.player.domain.model.LibraryDensity
import org.aptura.player.domain.model.LibraryDetail
import org.aptura.player.domain.model.LibraryLayout
import org.aptura.player.theme.ApturaColors
import org.aptura.player.theme.ApturaTypography
import org.aptura.player.ui.components.SectionTitle
import org.aptura.player.ui.components.ChoiceCard
import org.aptura.player.ui.components.TonalSegmentedPicker
import org.aptura.player.ui.settings.SectionLabel

/*
 * The library's content area: the search header, the videos (as a grid or as rows), and the swipe between folders.
 */

internal const val LIBRARY_VIDEO_GRID_TAG = "library_video_grid"
internal const val LAYOUT_TOGGLE_TAG = "layout_toggle"
internal const val VIEW_OPTIONS_TAG = "view_options_button"

private const val ALL_VIDEOS_KEY = "<all videos>"
/** A drag this far moves to the next folder at once, while the finger is still down. */
private const val FOLDER_SWIPE_DISTANCE_DP = 48

/** A flick this quick (and past the touch slop, so it was a drag at all) counts on letting go, whatever the distance. */
private const val FOLDER_FLICK_SPEED_DP_PER_S = 500

/** The slide between two folders: how long the new folder takes to arrive. Same movement as before, but quicker. */
internal const val FOLDER_SLIDE_MS = 170

// ---- header -----------------------------------------------------------------------------------------------------

/** The search field and the buttons beside it. "View" holds the layout, size, details and sort choices. */
@Composable
internal fun LibraryHeader(
    query: String,
    scopeName: String?,
    isRefreshing: Boolean,
    showRefreshAndSettings: Boolean,
    pagePadding: Dp,
    onQueryChange: (String) -> Unit,
    onSearchDone: () -> Unit,
    onViewOptions: () -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = pagePadding, end = pagePadding - 4.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { SearchHint(if (scopeName != null) "Search $scopeName" else "Search videos") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ApturaColors.TextMediumEmphasis
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = ApturaColors.TextMediumEmphasis
                        )
                    }
                }
            },
            textStyle = ApturaTypography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchDone() }),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ApturaColors.AccentPrimary,
                unfocusedBorderColor = ApturaColors.BorderSubtle,
                focusedContainerColor = ApturaColors.SurfaceElevated1,
                unfocusedContainerColor = ApturaColors.SurfaceElevated1,
                focusedTextColor = ApturaColors.TextHighEmphasis,
                unfocusedTextColor = ApturaColors.TextHighEmphasis,
                cursorColor = ApturaColors.AccentPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 720.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Plain icon buttons: filled tiles beside the field made the header heavier than the content.
        IconButton(onClick = onViewOptions, modifier = Modifier.size(48.dp).testTag(VIEW_OPTIONS_TAG)) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = "View options",
                tint = ApturaColors.TextMediumEmphasis
            )
        }

        if (showRefreshAndSettings) {
            IconButton(onClick = onRefresh, modifier = Modifier.size(48.dp)) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = ApturaColors.AccentPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh library",
                        tint = ApturaColors.TextMediumEmphasis
                    )
                }
            }
            IconButton(onClick = onSettingsClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = ApturaColors.TextMediumEmphasis
                )
            }
        }
    }
}

internal const val SEARCH_HINT_SHORT = "Search"

/**
 * The search field's hint. On a narrow phone the field shares its row with three buttons, and "Search videos" or
 * "Search Documentaries" used to be cut to "Search vi…". A cut word reads as a fault, so the hint steps down to the
 * longest wording that fits whole: the full text, then just "Search", then nothing (the magnifier beside it already says
 * it). The measurement uses the real font size, so a larger system font is handled too.
 */
@Composable
private fun SearchHint(full: String) {
    val measurer = rememberTextMeasurer()
    val style = ApturaTypography.bodyLarge
    BoxWithConstraints {
        fun fits(text: String) =
            !constraints.hasBoundedWidth || measurer.measure(text, style, maxLines = 1, softWrap = false).size.width <= constraints.maxWidth
        val shown = listOf(full, SEARCH_HINT_SHORT).firstOrNull(::fits)
        if (shown != null) {
            Text(
                text = shown,
                style = style,
                color = ApturaColors.TextLowEmphasis,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---- the folder swipe -------------------------------------------------------------------------------------------

/**
 * Moves to the next or previous folder, calling [onSwipe] with +1 or -1, in either of two ways: a horizontal drag of
 * [FOLDER_SWIPE_DISTANCE_DP] (the folder changes the moment the finger has gone that far, not when it lifts), or a
 * flick of [FOLDER_FLICK_SPEED_DP_PER_S] or more, on letting go. Once per touch.
 * "Next" follows the reading direction, so a right-to-left language swipes the other way.
 *
 * It used to wait for the finger to lift after a 64 dp drag, so a short, quick flick did nothing and a long one only
 * began to move after the whole gesture was over.
 */
@Composable
internal fun Modifier.folderSwipe(enabled: Boolean, onSwipe: (delta: Int) -> Unit): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    // The gesture block is not restarted when the callback changes (that would cancel a drag in progress), so it must
    // read the latest callback, which knows the folder the library is on now. A captured first callback would swipe
    // "from" the folder the screen opened on, every time.
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    return pointerInput(enabled, layoutDirection) {
        if (!enabled) return@pointerInput
        val swipeDistance = FOLDER_SWIPE_DISTANCE_DP.dp.toPx()
        val flickSpeed = FOLDER_FLICK_SPEED_DP_PER_S.dp.toPx()
        val velocity = VelocityTracker()
        var horizontalDrag = 0f
        var moved = false

        fun moveAlong() {
            moved = true
            val towardsNext = if (layoutDirection == LayoutDirection.Ltr) horizontalDrag < 0f else horizontalDrag > 0f
            currentOnSwipe(if (towardsNext) 1 else -1)
        }

        detectHorizontalDragGestures(
            onDragStart = {
                horizontalDrag = 0f
                moved = false
                velocity.resetTracking()
            },
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                velocity.addPosition(change.uptimeMillis, change.position)
                horizontalDrag += dragAmount
                if (!moved && kotlin.math.abs(horizontalDrag) >= swipeDistance) moveAlong()
            },
            onDragEnd = {
                if (!moved) {
                    val speed = velocity.calculateVelocity().x
                    // Quick enough, and still going the way the finger went (not a flick that was reversed).
                    if (kotlin.math.abs(speed) >= flickSpeed && speed * horizontalDrag > 0f) moveAlong()
                }
            }
        )
    }
}

/** A fixed order for the folders: All videos first, then the folders as listed. */
internal fun folderOrder(folders: List<String>): List<String?> = listOf<String?>(null) + folders

/**
 * The folder one swipe away, or null at either end. Swiping past the end does not wrap round to the other end: the
 * chips are a row with a beginning and an end, and a swipe that teleports across the whole row loses the viewer.
 */
internal fun neighbouringFolder(folders: List<String>, current: String?, delta: Int): Pair<Int, String?>? {
    val order = folderOrder(folders)
    val index = order.indexOf(current).takeIf { it >= 0 } ?: 0
    val next = index + delta
    return if (next in order.indices) next to order[next] else null
}

// ---- the videos -------------------------------------------------------------------------------------------------

/** What sliding between two folders looks like: the new folder arrives from the side the swipe came from. */
private fun AnimatedContentTransitionScope<MediaLibraryUiState.Success>.folderTransition(): ContentTransform {
    val order = folderOrder(targetState.folders)
    val from = order.indexOf(initialState.selectedFolder)
    val to = order.indexOf(targetState.selectedFolder)
    val forward = to >= from
    val direction = if (forward) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    // The new folder starts moving at once and settles; the old one leaves quickly. The same slide and fades as before,
    // in about two thirds of the time, with an easing that starts fast (the default one starts slowly, which is most of
    // why the slide felt like it was waiting).
    return (slideIntoContainer(direction, tween(FOLDER_SLIDE_MS, easing = LinearOutSlowInEasing)) { it / 4 } +
        fadeIn(tween(130, easing = LinearOutSlowInEasing))) togetherWith
        (slideOutOfContainer(direction, tween(FOLDER_SLIDE_MS, easing = FastOutLinearInEasing)) { it / 4 } +
            fadeOut(tween(100)))
}

/**
 * The videos of the current folder, with the folder swipe. Each folder is its own pane with its own scroll position,
 * kept while the folder is away, so leaving a folder and coming back finds the list where it was.
 */
@Composable
internal fun LibraryVideoPaneHost(
    state: MediaLibraryUiState.Success,
    swipeEnabled: Boolean,
    onSwipe: (delta: Int) -> Unit,
    modifier: Modifier = Modifier,
    pane: @Composable (MediaLibraryUiState.Success, Modifier) -> Unit
) {
    val holder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = state,
        // Typing a search or re-sorting is the same folder: no slide, just new content.
        contentKey = { it.selectedFolder },
        transitionSpec = { folderTransition() },
        modifier = modifier.folderSwipe(swipeEnabled, onSwipe),
        label = "folderPane"
    ) { paneState ->
        holder.SaveableStateProvider(paneState.selectedFolder ?: ALL_VIDEOS_KEY) {
            pane(paneState, Modifier.fillMaxSize())
        }
    }
}

/**
 * One folder's content: the "Continue watching" shelf (home view only), the folder's title with the layout toggle, and
 * the videos as a grid or as rows.
 */
@Composable
internal fun LibraryVideoPane(
    state: MediaLibraryUiState.Success,
    prefs: LibraryViewPrefs,
    scopeTitle: String,
    isWideScreen: Boolean,
    containerWidth: Dp,
    pagePadding: Dp,
    bottomPadding: Dp,
    onResumeContinueWatching: (VideoCardUiModel) -> Unit,
    onToggleLayout: () -> Unit,
    isSelected: (VideoCardUiModel) -> Boolean,
    callbacksFor: (VideoCardUiModel) -> VideoCardCallbacks,
    modifier: Modifier = Modifier,
    shortWindow: Boolean = false
) {
    val fontScale = LocalDensity.current.fontScale
    val isList = prefs.layout == LibraryLayout.LIST
    // Fixed cells at 200% font turn titles into "Aptura Audit Cl…" and make sibling episodes indistinguishable.
    // Growing the cell with the font scale buys the extra line it needs (and at the extreme falls back to a single
    // column) without changing the normal-density layout.
    val minCell = if (isList) {
        LibraryMetrics.listMinCell(prefs.density) * fontScale.coerceIn(1f, 1.3f)
    } else {
        LibraryMetrics.gridMinCell(prefs.density, containerWidth, isWideScreen) * fontScale.coerceIn(1f, 1.6f)
    }
    val columnGap = if (isList) 20.dp else LibraryMetrics.gridColumnGap(prefs.density, isWideScreen)
    val rowGap = if (isList) LibraryMetrics.listRowGap(prefs.density) else LibraryMetrics.gridRowGap(prefs.density)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCell),
        state = rememberLazyGridState(),
        modifier = modifier.testTag(LIBRARY_VIDEO_GRID_TAG),
        contentPadding = PaddingValues(
            start = pagePadding,
            end = pagePadding,
            top = 8.dp,
            bottom = bottomPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(rowGap),
        horizontalArrangement = Arrangement.spacedBy(columnGap)
    ) {
        if (state.continueWatching.isNotEmpty()) {
            item(
                key = "continue_watching",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "continue_watching"
            ) {
                ContinueWatchingRow(
                    cards = state.continueWatching,
                    onResume = onResumeContinueWatching,
                    modifier = Modifier.padding(top = 4.dp, bottom = if (shortWindow) 4.dp else 12.dp),
                    compact = shortWindow
                )
            }
        }
        // The grid gets a title of its own, so it reads as a section rather than as leftovers under the shelf. The
        // layout toggle lives here, where the thing it changes is named.
        item(key = "grid_title", span = { GridItemSpan(maxLineSpan) }, contentType = "section_title") {
            SectionTitle(
                title = scopeTitle,
                count = state.videos.size,
                modifier = Modifier.padding(bottom = 4.dp),
                trailing = {
                    IconButton(onClick = onToggleLayout, modifier = Modifier.size(48.dp).testTag(LAYOUT_TOGGLE_TAG)) {
                        Icon(
                            imageVector = if (isList) Icons.Outlined.GridView else Icons.Outlined.ViewList,
                            contentDescription = if (isList) "Show videos as a grid" else "Show videos as a list",
                            tint = ApturaColors.TextMediumEmphasis
                        )
                    }
                }
            )
        }
        items(
            items = state.videos,
            key = { item -> item.uri },
            contentType = { if (isList) "video_row" else "video_card" }
        ) { card ->
            if (isList) {
                VideoListRow(
                    card = card,
                    callbacks = callbacksFor(card),
                    isSelected = isSelected(card),
                    detail = prefs.detail,
                    density = prefs.density
                )
            } else {
                VideoGridCard(
                    card = card,
                    callbacks = callbacksFor(card),
                    isSelected = isSelected(card),
                    detail = prefs.detail,
                    density = prefs.density
                )
            }
        }
    }
}

// ---- view options -----------------------------------------------------------------------------------------------

/** Layout, size, details and sort, in one sheet, so every way of changing what the library looks like is in one place. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryViewSheet(
    prefs: LibraryViewPrefs,
    sortOrder: LibrarySortOrder,
    onPrefsChange: (LibraryViewPrefs) -> Unit,
    onSortChange: (LibrarySortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ApturaColors.SurfaceElevated1
    ) {
        LibraryViewSheetContent(prefs, sortOrder, onPrefsChange, onSortChange)
    }
}

/** The sheet's content, apart from its window, so it can be shown and tested on its own. */
@Composable
internal fun LibraryViewSheetContent(
    prefs: LibraryViewPrefs,
    sortOrder: LibrarySortOrder,
    onPrefsChange: (LibraryViewPrefs) -> Unit,
    onSortChange: (LibrarySortOrder) -> Unit
) {
    // Scrolls: on a phone held sideways the four choices are taller than the window, and the sort order below the fold
    // would otherwise be out of reach.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Text(
            text = "View",
            style = ApturaTypography.headlineSmall,
            color = ApturaColors.TextHighEmphasis
        )

        ViewSection("Layout") {
            Row(
                modifier = Modifier.fillMaxWidth().selectableGroup().semantics { contentDescription = "Layout" },
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LibraryLayout.entries.forEach { layout ->
                    ChoiceCard(
                        label = if (layout == LibraryLayout.GRID) "Grid" else "List",
                        selected = prefs.layout == layout,
                        onClick = { onPrefsChange(prefs.copy(layout = layout)) },
                        modifier = Modifier.weight(1f)
                    ) { tint -> LayoutGlyph(layout, tint) }
                }
            }
        }
        ViewSection("Size") {
            TonalSegmentedPicker(
                options = listOf("Compact", "Comfortable", "Large"),
                selectedIndex = prefs.density.ordinal,
                onSelect = { onPrefsChange(prefs.copy(density = LibraryDensity.entries[it])) },
                label = "Size"
            )
        }
        ViewSection("Details") {
            TonalSegmentedPicker(
                options = listOf("Title only", "Standard", "Detailed"),
                selectedIndex = prefs.detail.ordinal,
                onSelect = { onPrefsChange(prefs.copy(detail = LibraryDetail.entries[it])) },
                label = "Details"
            )
            Text(
                text = when (prefs.detail) {
                    LibraryDetail.TITLE_ONLY -> "Just the title."
                    LibraryDetail.STANDARD -> "Title, size and folder."
                    LibraryDetail.DETAILED -> "Adds the picture quality and when it was added."
                },
                style = ApturaTypography.bodySmall,
                color = ApturaColors.TextMediumEmphasis,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
        ViewSection("Sort by") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LibrarySortOrder.entries.forEach { order ->
                    SortOptionRow(order.displayName, selected = order == sortOrder) { onSortChange(order) }
                }
            }
        }
    }
}

@Composable
private fun ViewSection(title: String, content: @Composable () -> Unit) {
    Column {
        SectionLabel(title)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    // The chosen row takes the same tint as the chosen chip and card, so the sheet reads as one set of choices.
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(if (selected) ApturaColors.AccentPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick, role = Role.RadioButton)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ApturaTypography.bodyLarge,
            color = if (selected) ApturaColors.AccentPrimary else ApturaColors.TextHighEmphasis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ApturaColors.AccentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** The two layouts, drawn small: four tiles in a grid, or three rows of a picture and its name. */
@Composable
private fun LayoutGlyph(layout: LibraryLayout, tint: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val gap = 5.dp.toPx()
        val corner = CornerRadius(4.dp.toPx())
        if (layout == LibraryLayout.GRID) {
            val tileWidth = (size.width - gap) / 2
            val tileHeight = (size.height - gap) / 2
            for (row in 0..1) for (column in 0..1) {
                drawRoundRect(
                    color = tint.copy(alpha = 0.85f),
                    topLeft = Offset(column * (tileWidth + gap), row * (tileHeight + gap)),
                    size = Size(tileWidth, tileHeight),
                    cornerRadius = corner
                )
            }
        } else {
            val rowHeight = (size.height - gap * 2) / 3
            val pictureWidth = rowHeight * 1.5f
            for (row in 0..2) {
                val top = row * (rowHeight + gap)
                drawRoundRect(tint.copy(alpha = 0.85f), Offset(0f, top), Size(pictureWidth, rowHeight), corner)
                drawRoundRect(
                    color = tint.copy(alpha = 0.5f),
                    topLeft = Offset(pictureWidth + gap, top + rowHeight * 0.3f),
                    size = Size(size.width - pictureWidth - gap, rowHeight * 0.4f),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
    }
}
