package io.github.halilozel1903.autokit.sample.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.autokit.core.CarPaging
import io.github.halilozel1903.autokit.core.paged
import io.github.halilozel1903.autokit.sample.R
import io.github.halilozel1903.autokit.sample.RoadtripData

/** The screens the preview can show. */
enum class PreviewScene(val key: String, val label: String) {
    List("list", "Audio guides"),
    Grid("grid", "Home"),
    Pane("pane", "Place"),
    Navigation("navigation", "Route"),
    ;

    companion object {
        fun from(key: String?): PreviewScene? = entries.firstOrNull { it.key == key }
    }
}

/** A title bar with scene chips above a car display in the 1920 x 720 (8:3) aspect. */
@Composable
fun PreviewApp(initial: PreviewScene) {
    var scene by rememberSaveable { mutableStateOf(initial) }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0F14), Color(0xFF16202C))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_launcher), contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Roadtrip", color = CarColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Android Auto preview · rendered from the same autokit-core model as the car screens",
                    color = CarColors.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewScene.entries.forEach { option ->
                    FilterChip(
                        selected = option == scene,
                        onClick = { scene = option },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CarColors.Accent,
                            selectedLabelColor = CarColors.OnAccent,
                            labelColor = CarColors.TextSecondary,
                        ),
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val frameWidth = min(maxWidth, maxHeight * 8f / 3f)
            CarDisplay(Modifier.size(width = frameWidth, height = frameWidth * 3f / 8f)) {
                SceneContent(scene)
            }
        }
    }
}

@Composable
private fun SceneContent(scene: PreviewScene) {
    when (scene) {
        // Lists are shown the way the car pages them on a host with the default limit of 6 rows.
        PreviewScene.List -> ListTemplatePreview(RoadtripData.audioGuidesList().paged(maxItems = 6).first(), HeaderStart.Back)
        PreviewScene.Grid -> GridTemplatePreview(RoadtripData.home, HeaderStart.AppIcon)
        PreviewScene.Pane -> PaneTemplatePreview(RoadtripData.placePane("lookout"), HeaderStart.Back)
        PreviewScene.Navigation -> ListTemplatePreview(RoadtripData.route.toCarList(), HeaderStart.Back)
    }
}

/** Whether a row is the "More" row added by paging. */
internal fun isMoreRow(id: String): Boolean = CarPaging.nextPageFor(id) != null
