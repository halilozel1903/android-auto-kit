package io.github.halilozel1903.autokit.sample.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/**
 * A phone preview of the car screens. Android Auto renders templates on the head unit, so this
 * activity draws the same `autokit-core` model as car-like cards in a wide 1920 x 720 frame. It is
 * a preview for development and screenshots; test the real templates with the Desktop Head Unit.
 *
 * `scripts/screenshots.sh` starts it with `--es scene <scene>`: `list` (audio guides), `grid` (home),
 * `pane` (a place) or `navigation` (the route).
 */
class PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val initial = PreviewScene.from(intent.getStringExtra(EXTRA_SCENE)) ?: PreviewScene.Grid
        setContent {
            PreviewTheme {
                // The Surface gives text a readable default color on the dark background.
                Surface(color = MaterialTheme.colorScheme.background) {
                    PreviewApp(initial)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
