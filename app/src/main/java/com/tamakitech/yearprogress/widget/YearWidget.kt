package com.tamakitech.yearprogress.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider
import com.tamakitech.yearprogress.MainActivity
import com.tamakitech.yearprogress.R
import com.tamakitech.yearprogress.calcYearProgress

class YearWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { GlanceTheme { WidgetContent() } }
    }
}

@Composable
private fun WidgetContent() {
    val context = LocalContext.current
    val size = LocalSize.current
    val progress = calcYearProgress()
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val bg = if (dynamic) GlanceTheme.colors.widgetBackground else ColorProvider(day = Color(0xFFF3F5FE), night = Color(0xFF232532))
    val primary = if (dynamic) GlanceTheme.colors.onSurface else ColorProvider(day = Color(0xFF292B31), night = Color(0xFFE9E9ED))
    val secondary = if (dynamic) GlanceTheme.colors.onSurfaceVariant else ColorProvider(day = Color(0xFF595D6C), night = Color(0xFFB2B6CA))
    val yearColor = if (dynamic) secondary else ColorProvider(day = Color(0xFF75798C), night = Color(0xFF9397AB))
    val tick = if (dynamic) secondary else ColorProvider(day = Color(0xFF75798C), night = Color(0xFF75798C))
    val track = if (dynamic) GlanceTheme.colors.secondaryContainer else ColorProvider(day = Color(0xFFCFD3E5), night = Color(0xFF3F424D))
    val bar = if (progress.isLow) ColorProvider(day = Color(0xFFC13C3B), night = Color(0xFFEA6A64)) else ColorProvider(day = Color(0xFF14874E), night = Color(0xFF57BC80))
    var root = GlanceModifier.fillMaxSize().appWidgetBackground().background(bg).clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    if (dynamic) root = root.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    if (size.width >= 250.dp && size.height >= 250.dp) {
        Column(root.padding(24.dp)) {
            Text(progress.year.toString(), style = TextStyle(color = yearColor, fontSize = 16.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.defaultWeight())
            Text(context.getString(R.string.label_remaining_prefix), style = TextStyle(color = secondary, fontSize = 18.sp))
            Spacer(GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(progress.percentText, style = TextStyle(color = primary, fontSize = 88.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Spacer(GlanceModifier.width(2.dp))
                Text(context.getString(R.string.percent_suffix), style = TextStyle(color = secondary, fontSize = 36.sp, fontWeight = FontWeight.Medium))
            }
            Spacer(GlanceModifier.height(20.dp))
            LinearProgressIndicator(progress.remainingRatio.toFloat(), GlanceModifier.fillMaxWidth().height(8.dp), color = bar, backgroundColor = track)
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.Horizontal.Start) {
                Text(context.getString(R.string.month_start), style = TextStyle(color = tick, fontSize = 12.sp))
                Spacer(GlanceModifier.defaultWeight())
                Text(context.getString(R.string.month_end), style = TextStyle(color = tick, fontSize = 12.sp))
            }
            Spacer(GlanceModifier.height(16.dp))
            Text(context.getString(R.string.remaining_days, progress.remainingDays), style = TextStyle(color = primary, fontSize = 18.sp))
        }
    } else {
        val trackHeight = (size.height.value - 24f - 14f - 18f - 16f).coerceAtLeast(0f)
        Column(root.padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(progress.year.toString(), style = TextStyle(color = yearColor, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.height(8.dp))
            Box(GlanceModifier.width(10.dp).defaultWeight().background(track).cornerRadius(5.dp), contentAlignment = Alignment.BottomCenter) {
                Box(GlanceModifier.width(10.dp).height((trackHeight * progress.remainingRatio.toFloat()).dp).background(bar).cornerRadius(5.dp)) {}
            }
            Spacer(GlanceModifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(progress.percentText, style = TextStyle(color = primary, fontSize = 18.sp, fontWeight = FontWeight.Medium), maxLines = 1)
                Text(context.getString(R.string.percent_suffix), style = TextStyle(color = secondary, fontSize = 11.sp))
            }
        }
    }
}
