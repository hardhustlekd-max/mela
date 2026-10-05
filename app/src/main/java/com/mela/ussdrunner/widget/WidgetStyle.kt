package com.mela.ussdrunner.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.mela.ussdrunner.R
import com.mela.ussdrunner.domain.model.Preset

/** The six tile looks used by the widgets: pastel tile, strong circle, white glyph. */
internal enum class TileKind(val tile: Int, val circle: Int, val icon: Int) {
    BALANCE(R.drawable.widget_tile_balance_bg, R.drawable.widget_circle_balance, R.drawable.ic_w_balance),
    DATA(R.drawable.widget_tile_data_bg, R.drawable.widget_circle_data, R.drawable.ic_w_data),
    STATEMENT(R.drawable.widget_tile_statement_bg, R.drawable.widget_circle_statement, R.drawable.ic_w_statement),
    TOPUP(R.drawable.widget_tile_topup_bg, R.drawable.widget_circle_topup, R.drawable.ic_w_topup),
    HUB(R.drawable.widget_tile_hub_bg, R.drawable.widget_circle_hub, R.drawable.ic_w_hub),
    CALL(R.drawable.widget_tile_call_bg, R.drawable.widget_circle_call, R.drawable.ic_w_call),
}

/** Picks icon and colour from the preset's name/category, falling back to a stable hash of its id. */
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
    circle: Int,
    icon: Int,
    size: Dp,
    glyph: Dp,
) {
    Box(
        modifier = GlanceModifier.size(size).background(ImageProvider(circle)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            modifier = GlanceModifier.size(glyph),
        )
    }
}

@Composable
internal fun HeaderButton(icon: Int, description: String, action: Action) {
    Box(
        modifier = GlanceModifier.size(36.dp).clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(ColorProvider(R.color.widget_icon_button)),
            modifier = GlanceModifier.size(24.dp),
        )
    }
}

/** App logo + "USSD SHORTCUTS" + gear and plus buttons, as in the reference design. */
@Composable
internal fun WidgetHeader() {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.size(28.dp).background(ImageProvider(R.drawable.widget_logo_bg)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_w_logo),
                contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = "USSD SHORTCUTS",
            style = TextStyle(
                color = ColorProvider(R.color.widget_text_primary),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        HeaderButton(R.drawable.ic_w_settings, "Settings", routeAction("settings"))
        HeaderButton(R.drawable.ic_w_add, "Add shortcut", routeAction("edit"))
    }
}

/** One pastel tile: coloured icon circle on the left, name and USSD code on the right. */
@Composable
internal fun ShortcutTile(preset: Preset?, modifier: GlanceModifier) {
    if (preset == null) {
        Row(
            modifier = modifier
                .background(ImageProvider(R.drawable.widget_tile_empty_bg))
                .padding(horizontal = 10.dp)
                .clickable(routeAction("edit")),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconCircle(R.drawable.widget_circle_empty, R.drawable.ic_w_add, 34.dp, 20.dp)
            Spacer(GlanceModifier.width(10.dp))
            Text(
                text = "Add shortcut",
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_secondary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
        }
        return
    }
    val kind = tileKindFor(preset)
    Row(
        modifier = modifier
            .background(ImageProvider(kind.tile))
            .padding(horizontal = 10.dp)
            .clickable(runAction(preset.id)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(kind.circle, kind.icon, 34.dp, 20.dp)
        Spacer(GlanceModifier.width(10.dp))
        Column {
            Text(
                text = preset.name,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_primary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = preset.ussdCode,
                style = TextStyle(
                    color = ColorProvider(R.color.widget_text_secondary),
                    fontSize = 12.sp,
                ),
                maxLines = 1,
            )
        }
    }
}
