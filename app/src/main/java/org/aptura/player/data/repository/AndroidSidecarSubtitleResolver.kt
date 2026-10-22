package org.aptura.player.data.repository

import android.content.Context
import android.net.Uri
import org.aptura.player.domain.model.DomainExternalSubtitle
import org.aptura.player.domain.repository.SidecarSubtitleResolver

/** Android implementation: looks next to the video (and in `subs/`) using [SidecarSubtitleDetector]. */
class AndroidSidecarSubtitleResolver(private val context: Context) : SidecarSubtitleResolver {

    override suspend fun resolve(mediaUri: String): List<DomainExternalSubtitle> =
        SidecarSubtitleDetector.detectSidecars(context, Uri.parse(mediaUri)).map { sidecar ->
            DomainExternalSubtitle(
                uri = sidecar.uri.toString(),
                mimeType = sidecar.mimeType,
                label = "${sidecar.languageLabel} (${sidecar.displayName})",
                language = sidecar.languageCode,
                isDefault = sidecar.isDefault
            )
        }
}
