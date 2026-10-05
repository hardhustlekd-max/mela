package com.mela.ussdrunner.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mela.ussdrunner.R
import com.mela.ussdrunner.domain.model.Preset
import com.mela.ussdrunner.ui.theme.DarkColors
import com.mela.ussdrunner.ui.theme.LightColors

/** The app's own Material 3 light/dark schemes, so widgets match the app. */
internal val MelaWidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

/** Icon + Material 3 colour roles for one kind of shortcut. */
internal enum class TileKind(val icon: Int) {
    BALANCE(R.drawable.ic_w_balance),
    DATA(R.drawable.ic_w_data),
    STATEMENT(R.drawable.ic_w_statement),
    TOPUP(R.drawable.ic_w_topup),
    HUB(R.drawable.ic_w_hub),
    CALL(R.drawable.ic_w_call),
}

internal class TileColors(
    val container: ColorProvider,
    val onContainer: ColorProvider,
    val accent: ColorProvider,
    val onAccent: ColorProvider,
)

@Composable
internal fun TileKind.colors(): TileColors {
    val c = GlanceTheme.colors
    return when (this) {
        TileKind.BALANCE -> TileColors(c.primaryContainer, c.onPrimaryContainer, c.primary, c.onPrimary)
        TileKind.DATA -> TileColors(c.secondaryContainer, c.onSecondaryContainer, c.secondary, c.onSecondary)
        TileKind.STATEMENT -> TileColors(c.tertiaryContainer, c.onTertiaryContainer, c.tertiary, c.onTertiary)
        TileKind.TOPUP -> TileColors(c.surfaceVariant, c.onSurfaceVariant, c.primary, c.onPrimary)
        TileKind.HUB -> TileColors(c.surfaceVariant, c.onSurfaceVariant, c.secondary, c.onSecondary)
        TileKind.CALL -> TileColors(c.errorContainer, c.onErrorContainer, c.error, c.onError)
    }
}

/** Picks icon and colours from the preset's name/category, falling back to a stable hash of its id. */
internal fun tileKindFor(preset: Preset): TileKind {
    val text = "${preset.name} ${preset.category.orEmpty()}".lowercase()
    return when {
        "statement" in text || "history" in text -> TileKind.STATEMENT
        "top" in text || "airtime" in text || "recharge" in text || "voucher" in text -> TileKind.TOPUP
        "call" in text || "voice" in text || "minute" in text -> TileKind.CALL
        "data" in text && "balance" in text -> TileKind.HUB
        "data" in text || "bundle" in text || "internet" in text -> TileKind.DATA
        "balance" in text || "money" in text || "cash" in text -> TileKind.BALANCE
        else -> TileKind.entries[Math.floorMod(preset.id.hashCode(), TileKind.entries.size)]
    }
}

internal fun runAction(presetId: String?) =
    actionStartActivity<WidgetRunActivity>(
        actionParametersOf(WidgetKeys.presetIdParam to (presetId ?: "")),
    )

/** Opens the app straight on a screen such as "settings" or "edit". */
internal fun routeAction(route: String): Action =
    actionStartActivity<WidgetRunActivity>(
        actionParametersOf(WidgetKeys.routeParam to route),
    )

@Composable
internal fun IconCircle(
    icon: Int,
    size: Dp,
    background: ColorProvider,
    tint: ColorProvider,
) {
    Box(
        modifier = GlanceModifier.size(size).background(background).cornerRadius(size / 2),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(size * 0.58f),
        )
    }
}

@Composable
private fun HeaderButton(icon: Int, description: String, action: Action) {
    Box(
        modifier = GlanceModifier.size(40.dp).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface),
            modifier = GlanceModifier.size(22.dp),
        )
    }
}

/** Material 3 Glance title bar: app glyph, title, and settings / add actions. */
@Composable
internal fun WidgetTitleBar() {
    TitleBar(
        startIcon = ImageProvider(R.drawable.ic_w_logo),
        title = "USSD Shortcuts",
        iconColor = GlanceTheme.colors.primary,
        textColor = GlanceTheme.colors.onSurface,
        actions = {
            HeaderButton(R.drawable.ic_w_settings, "Settings", routeAction("settings"))
            HeaderButton(R.drawable.ic_w_add, "Add shortcut", routeAction("edit"))
        },
    )
}

/**
 * One tonal tile: accent icon circle, then name and USSD code.
 * [compact] drops the code line and shrinks the icon for short widgets.
 */
@Composable
internal fun ShortcutTile(preset: Preset?, modifier: GlanceModifier, compact: Boolean) {
    val c = GlanceTheme.colors
    val colors = if (preset == null) {
        TileColors(c.surfaceVariant, c.onSurfaceVariant, c.outline, c.surface)
    } else {
        tileKindFor(preset).colors()
    }
    val icon = if (preset == null) R.drawable.ic_w_add else tileKindFor(preset).icon
    val action = if (preset == null) routeAction("edit") else runAction(preset.id)
    val iconSize = if (compact) 28.dp else 36.dp

    Row(
        modifier = modifier
            .background(colors.container)
            .cornerRadius(18.dp)
            .padding(horizontal = 8.dp)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(icon, iconSize, colors.accent, colors.onAccent)
        Spacer(GlanceModifier.width(8.dp))
        Column(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = preset?.name ?: "Add shortcut",
                style = TextStyle(
                    color = colors.onContainer,
                    fontSize = if (compact) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            if (!compact) {
                Text(
                    text = preset?.ussdCode ?: "Tap to create",
                    style = TextStyle(color = colors.onContainer, fontSize = 11.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
