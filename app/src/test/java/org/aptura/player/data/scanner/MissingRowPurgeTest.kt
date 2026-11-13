package org.aptura.player.data.scanner

import org.aptura.player.data.database.MediaItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * Audit DATA-04: a scan that did not see a file deleted the row even when the person had hidden the video, picked a
 * speed or decoder, set a delay or watched it. These tests pin the rule, one user-owned column at a time.
 */
class MissingRowPurgeTest {

    private fun missingInternalRow() = MediaItemEntity(
        mediaStoreId = 7L,
        uri = "content://media/external/video/media/7",
        title = "clip",
        displayName = "clip.mp4",
        durationMs = 30_000L,
        sizeBytes = 1_000_000L,
        mimeType = "video/mp4",
        isMissing = true
    )

    @Test
    fun aMissingInternalRowWithNothingOfThePersonsIsPurgeable() {
        assertTrue(isPurgeableMissingRow(missingInternalRow(), hasStoragePermission = true))
    }

    @Test
    fun everyUserOwnedColumnProtectsTheRow() {
        val base = missingInternalRow()
        val cases = mapOf(
            "playbackPositionMs" to base.copy(playbackPositionMs = 5_000L),
            "isCompleted" to base.copy(isCompleted = true),
            "lastPlayedTimestamp" to base.copy(lastPlayedTimestamp = 1_700_000_000L),
            "resizeMode" to base.copy(resizeMode = "FILL"),
            "playbackSpeed" to base.copy(playbackSpeed = 1.5f),
            "decoderOverridden" to base.copy(decoderOverridden = true),
            "decoderStrategy" to base.copy(decoderStrategy = 2),
            "audioDelayOverridden" to base.copy(audioDelayOverridden = true),
            "audioDelayMs" to base.copy(audioDelayMs = -200L),
            "subtitleDelayMs" to base.copy(subtitleDelayMs = 300L),
            "customTitle" to base.copy(customTitle = "My title"),
            "isHidden" to base.copy(isHidden = true)
        )
        for ((column, row) in cases) {
            assertTrue("$column must count as the person's data", row.hasUserData())
            assertFalse("a row with $column set must not be purged", isPurgeableMissingRow(row, hasStoragePermission = true))
        }
    }

    @Test
    fun aBlankCustomTitleIsNotUserData() {
        assertFalse(missingInternalRow().copy(customTitle = "  ").hasUserData())
        assertFalse(missingInternalRow().copy(customTitle = null).hasUserData())
    }

    @Test
    fun onlyMissingRowsWithStoragePermissionOnInternalUrisArePurgeable() {
        val row = missingInternalRow()
        assertFalse("a file that is present", isPurgeableMissingRow(row.copy(isMissing = false), true))
        assertFalse("no storage permission: a scan that sees nothing proves nothing", isPurgeableMissingRow(row, false))
        assertFalse("a SAF document is never purged here", isPurgeableMissingRow(row.copy(uri = "content://com.android.externalstorage.documents/tree/x"), true))
    }

    /**
     * Forces the next person who adds a column to [MediaItemEntity] to decide which kind it is. A new user-owned
     * column that is not in `hasUserData` would be erased by a scan again.
     */
    @Test
    fun everyEntityColumnIsClassifiedAsScannerOwnedOrUserOwned() {
        val scannerOwned = setOf(
            "id", "mediaStoreId", "uri", "title", "displayName", "durationMs", "sizeBytes", "mimeType", "width", "height",
            "frameRate", "dateAddedSec", "dateModifiedSec", "folderPath", "isMissing"
        )
        val userOwned = setOf(
            "playbackPositionMs", "isCompleted", "lastPlayedTimestamp", "resizeMode", "playbackSpeed", "decoderStrategy",
            "decoderOverridden", "audioDelayMs", "audioDelayOverridden", "subtitleDelayMs", "customTitle", "isHidden"
        )
        val declared = MediaItemEntity::class.java.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
            .map { it.name }
            .toSet()

        assertEquals("a column is not classified: add it to hasUserData or to the scanner-owned list", declared, scannerOwned + userOwned)
        assertTrue(scannerOwned.intersect(userOwned).isEmpty())
    }
}
