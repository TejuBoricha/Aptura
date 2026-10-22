package org.aptura.player

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import org.aptura.player.data.playback.PlayerManager
import org.aptura.player.theme.ApturaTheme

class MainActivity : ComponentActivity() {

    private val isPipMode = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchUri = intent?.data?.toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        enableEdgeToEdge()
        setContent {
            ApturaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation(
                        initialLaunchUri = launchUri,
                        isPipMode = isPipMode.value,
                        onEnterPip = ::enterPictureInPicture
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val controller = PlayerManager.getOrCreate(this)
        if (controller.player.isPlaying && !isPipMode.value) {
            enterPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPipMode.value = isInPictureInPictureMode
    }

    fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val controller = PlayerManager.getOrCreate(this)
            val videoSize = controller.player.videoSize
            val width = videoSize.width
            val height = videoSize.height
            val ratio = if (width > 0 && height > 0) width.toFloat() / height.toFloat() else 1.777f

            val rational = if (ratio in 0.4184f..2.39f) {
                Rational((ratio * 1000).toInt(), 1000)
            } else if (ratio > 2.39f) {
                Rational(239, 100)
            } else {
                Rational(100, 239)
            }

            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(rational)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                builder.setAutoEnterEnabled(true)
            }

            enterPictureInPictureMode(builder.build())
        }
    }
}
