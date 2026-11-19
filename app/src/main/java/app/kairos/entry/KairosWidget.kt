package app.kairos.entry

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.kairos.MainActivity
import app.kairos.panic.PanicActivity
import app.kairos.ui.theme.Paper

/** Two big buttons on the home screen, each one tap from the right place. */
class KairosWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val urge = MainActivity.urgeIntent(context)
        val panic = PanicActivity.intent(context)
        provideContent {
            val page = ColorProvider(day = Paper.Page, night = Paper.NightPage)
            val card = ColorProvider(day = Paper.Card, night = Paper.NightCard)
            val ink = ColorProvider(day = Paper.Ink, night = Paper.NightInk)
            val muted = ColorProvider(day = Paper.Muted, night = Paper.NightMuted)
            Row(
                GlanceModifier.fillMaxSize().background(page).cornerRadius(20.dp).padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cell("An urge", "Ride it out", card, ink, muted,
                    GlanceModifier.defaultWeight().clickable(actionStartActivity(urge))
                        .semantics { contentDescription = "Ride out an urge" })
                Spacer(GlanceModifier.width(6.dp))
                Cell("Panic", "Breathe with me", card, ink, muted,
                    GlanceModifier.defaultWeight().clickable(actionStartActivity(panic))
                        .semantics { contentDescription = "Panic help" })
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Cell(
        title: String,
        sub: String,
        bg: androidx.glance.unit.ColorProvider,
        ink: androidx.glance.unit.ColorProvider,
        muted: androidx.glance.unit.ColorProvider,
        modifier: GlanceModifier,
    ) {
        Box(modifier.fillMaxHeight().background(bg).cornerRadius(16.dp).padding(12.dp), contentAlignment = Alignment.CenterStart) {
            Column {
                Text(title, style = TextStyle(color = ink, fontSize = 22.sp, fontFamily = FontFamily.Serif))
                Text(sub, style = TextStyle(color = muted, fontSize = 13.sp))
            }
        }
    }

}

class KairosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KairosWidget()
}
