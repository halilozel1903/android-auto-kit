package io.github.halilozel1903.autokit.sample.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.halilozel1903.autokit.core.CarAction
import io.github.halilozel1903.autokit.core.CarGrid
import io.github.halilozel1903.autokit.core.CarGridItem
import io.github.halilozel1903.autokit.core.CarImage
import io.github.halilozel1903.autokit.core.CarList
import io.github.halilozel1903.autokit.core.CarPane
import io.github.halilozel1903.autokit.core.CarRow
import io.github.halilozel1903.autokit.sample.R
import io.github.halilozel1903.autokit.sample.RoadtripImages

// Car-like drawings of the templates. They follow the Android Auto layout loosely (rail on the left,
// header with a start action and icon actions, cards on black) and only exist for the phone preview.

enum class HeaderStart { Back, AppIcon }

/** The head unit: a black display with the Android Auto rail. */
@Composable
fun CarDisplay(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .clip(shape)
            .background(CarColors.Screen)
            .border(1.dp, CarColors.Divider, shape),
    ) {
        Rail()
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) { content() }
    }
}

@Composable
private fun Rail() {
    Column(
        Modifier
            .width(76.dp)
            .fillMaxHeight()
            .background(CarColors.Rail)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("9:41", color = CarColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RailIcon(R.drawable.ic_navigation)
            RailIcon(R.drawable.ic_headset)
            Image(painterResource(R.drawable.ic_launcher), contentDescription = "Roadtrip", modifier = Modifier.size(40.dp))
            RailIcon(R.drawable.ic_phone)
        }
        RailIcon(R.drawable.ic_apps)
    }
}

@Composable
private fun RailIcon(drawable: Int) {
    Icon(painterResource(drawable), contentDescription = null, tint = CarColors.TextSecondary, modifier = Modifier.size(26.dp))
}

/** A model image, tinted with its own tint or the text color. */
@Composable
fun ModelImage(image: CarImage, size: Dp, modifier: Modifier = Modifier) {
    val drawable = RoadtripImages.drawable(image.name) ?: return
    Icon(
        painterResource(drawable),
        contentDescription = null,
        tint = image.tint?.let { Color(it) } ?: CarColors.Text,
        modifier = modifier.size(size),
    )
}

@Composable
private fun TemplateHeader(title: String, start: HeaderStart, actions: List<CarAction>) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        when (start) {
            HeaderStart.Back -> CircleIcon(R.drawable.ic_back, "Back")
            HeaderStart.AppIcon -> Image(painterResource(R.drawable.ic_launcher), contentDescription = null, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(
            title,
            color = CarColors.Text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            actions.forEach { action ->
                action.icon?.let { icon -> RoadtripImages.drawable(icon.name)?.let { CircleIcon(it, action.title) } }
            }
        }
    }
}

@Composable
private fun CircleIcon(drawable: Int, description: String) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(CarColors.CardHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(drawable), contentDescription = description, tint = CarColors.Text, modifier = Modifier.size(24.dp))
    }
}

/** `ListTemplate`: rows on a card, with images, two lines of text and chevrons for browsable rows. */
@Composable
fun ListTemplatePreview(list: CarList, start: HeaderStart) {
    Column(Modifier.fillMaxSize()) {
        TemplateHeader(list.title, start, list.actions)
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(CarColors.Card)
                .verticalScroll(rememberScrollState()),
        ) {
            list.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = CarColors.Divider, thickness = 1.dp)
                ListRow(row)
            }
        }
    }
}

@Composable
private fun ListRow(row: CarRow) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.image != null) {
            ModelImage(row.image!!, 30.dp)
            Spacer(Modifier.width(20.dp))
        } else if (isMoreRow(row.id)) {
            Spacer(Modifier.width(50.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(row.title, color = CarColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            row.texts.take(2).forEach { line ->
                Text(line, color = CarColors.TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (row.browsable) Text("›", color = CarColors.TextSecondary, fontSize = 30.sp)
    }
}

/** `GridTemplate`: tiles with a large tinted image, title and text. */
@Composable
fun GridTemplatePreview(grid: CarGrid, start: HeaderStart) {
    Column(Modifier.fillMaxSize()) {
        TemplateHeader(grid.title, start, grid.actions)
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columns = if (maxWidth > 840.dp) 6 else 3
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                grid.items.chunked(columns).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { GridTile(it, Modifier.weight(1f)) }
                        repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridTile(item: CarGridItem, modifier: Modifier) {
    val tint = item.image.tint?.let { Color(it) } ?: CarColors.Accent
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(CarColors.Card)
            .padding(horizontal = 8.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(92.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            ModelImage(item.image, 50.dp)
        }
        Spacer(Modifier.height(14.dp))
        Text(item.title, color = CarColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        item.text?.let { Text(it, color = CarColors.TextSecondary, fontSize = 14.sp, maxLines = 1) }
    }
}

/** `PaneTemplate`: detail rows and buttons on the left, the large image on the right. */
@Composable
fun PaneTemplatePreview(pane: CarPane, start: HeaderStart) {
    Column(Modifier.fillMaxSize()) {
        TemplateHeader(pane.title, start, pane.headerActions)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                Modifier
                    .weight(1.6f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CarColors.Card)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            ) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    pane.rows.forEachIndexed { index, row ->
                        if (index > 0) HorizontalDivider(color = CarColors.Divider, thickness = 1.dp)
                        Column(Modifier.padding(vertical = 7.dp)) {
                            Text(row.title, color = CarColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            row.texts.take(2).forEach { Text(it, color = CarColors.TextSecondary, fontSize = 14.sp, maxLines = 1) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pane.actions.forEach { ActionButton(it) }
                }
            }
            pane.image?.let { image ->
                val tint = image.tint?.let { Color(it) } ?: CarColors.Accent
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.38f), CarColors.Card))),
                    contentAlignment = Alignment.Center,
                ) {
                    ModelImage(image, 120.dp)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(action: CarAction) {
    val background = if (action.primary) CarColors.Accent else CarColors.CardHigh
    val content = if (action.primary) CarColors.OnAccent else CarColors.Text
    Row(
        Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(background)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        action.icon?.let { icon ->
            RoadtripImages.drawable(icon.name)?.let {
                Icon(painterResource(it), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
        }
        Text(action.title, color = content, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
