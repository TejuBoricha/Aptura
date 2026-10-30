package org.aptura.player.data.repository

import android.content.Context
import androidx.core.net.toUri
import org.aptura.player.domain.model.DomainExternalSubtitle
import org.aptura.player.domain.repository.SidecarSubtitleResolver

/** Android implementation: looks next to the video (and in `subs/`) using [SidecarSubtitleDetector]. */
class AndroidSidecarSubtitleResolver(private val context: Context) : SidecarSubtitleResolver {

    override suspend fun resolve(mediaUri: String): List<DomainExternalSubtitle> =
        SidecarSubtitleDetector.detectSidecars(context, mediaUri.toUri()).map { sidecar ->
            // A sidecar without a language tag must not read "External (Sidecar) (File.srt)" - the plain file
            // name is the clearest label for it (and the toast reads "Auto-paired: File.srt").
            DomainExternalSubtitle(
                uri = sidecar.uri.toString(),
                mimeType = sidecar.mimeType,
                label = if (sidecar.languageCode != null) "${sidecar.languageLabel} (${sidecar.displayName})"
                        else sidecar.displayName,
                language = sidecar.languageCode,
                isDefault = sidecar.isDefault
            )
        }
}
