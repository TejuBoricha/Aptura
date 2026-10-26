package org.aptura.player.ui.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaStorageActionsTest {

    @Test
    fun systemTrashIsOfferedOnlyForMediaStoreItemsOnAndroid11AndLater() {
        assertTrue(canMoveToSystemTrash(listOf("content://media/external/video/media/1")))
        assertFalse(canMoveToSystemTrash(listOf("content://com.example.documents/document/1")))
        assertFalse(canMoveToSystemTrash(emptyList()))
    }

    @Test
    @Config(sdk = [29])
    fun systemTrashIsNotOfferedOnAndroid10() {
        assertFalse(canMoveToSystemTrash(listOf("content://media/external/video/media/1")))
    }
}
