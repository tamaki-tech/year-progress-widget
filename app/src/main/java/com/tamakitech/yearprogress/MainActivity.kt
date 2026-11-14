package com.tamakitech.yearprogress

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tamakitech.yearprogress.widget.YearWidgetProvider

private object AppColors {
    val Background = Color(0xFF161826)
    val TextPrimary = Color(0xFFE9E9ED)
    val TextYear = Color(0xFF9397AB)
    val TextSecondary = Color(0xFFB2B6CA)
    val TextTick = Color(0xFF75798C)
    val Track = Color(0xFF3F424D)
    val BarGreen = Color(0xFF57BC80)
    val BarRed = Color(0xFFEA6A64)
    val Accent = Color(0xFF9184D9)
    val AccentText = Color(0xFFD2CEFD)
    val AccentPressed = Color(0xFFB5ABFC)
}

/** 数字を等幅にする（tnum）。 */
private const val TABULAR_NUMS = "tnum"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val progress = calcYearProgress()
        val manager = getSystemService(AppWidgetManager::class.java)
        val canPin = manager?.isRequestPinAppWidgetSupported == true
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = AppColors.Background)) {
                MainScreen(
                    progress = progress,
                    canPin = canPin,
                    onAddWidget = {
                        manager?.requestPinAppWidget(
                            ComponentName(this, YearWidgetProvider::class.java),
                            null,
                            null,
                        )
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // アプリを開いたらウィジェットも最新の値に揃える
        YearWidgetProvider.updateAll(this)
    }
}

@Composable
private fun MainScreen(progress: YearProgress, canPin: Boolean, onAddWidget: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .safeDrawingPadding()
            .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 28.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.app_name),
                color = AppColors.TextYear,
                fontSize = 14.sp,
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                ProgressBlock(progress)
            }
            Text(
                stringResource(R.string.main_description),
                color = AppColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = (14 * 1.7).sp,
            )
            if (canPin) {
                Spacer(Modifier.height(20.dp))
                AddButton(onAddWidget)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.add_hint),
                color = AppColors.TextTick,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ProgressBlock(progress: YearProgress) {
    val barColor = if (progress.isLow) AppColors.BarRed else AppColors.BarGreen
    Text(
        progress.year.toString(),
        color = AppColors.TextYear,
        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR_NUMS),
    )
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.label_remaining_prefix), color = AppColors.TextSecondary, fontSize = 22.sp)
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            progress.percentText,
            color = AppColors.TextPrimary,
            style = TextStyle(
                fontSize = 104.sp,
                lineHeight = 104.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.035).em,
                fontFeatureSettings = TABULAR_NUMS,
            ),
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            stringResource(R.string.percent_suffix),
            color = AppColors.TextSecondary,
            style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.alignByBaseline(),
        )
    }
    Spacer(Modifier.height(24.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.Track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.remainingRatio.toFloat().coerceIn(0f, 1f))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(barColor),
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.month_start), color = AppColors.TextTick, fontSize = 13.sp)
        Text(stringResource(R.string.month_end), color = AppColors.TextTick, fontSize = 13.sp)
    }
    Spacer(Modifier.height(20.dp))
    Text(
        stringResource(R.string.remaining_days, progress.remainingDays.toInt()),
        color = AppColors.TextPrimary,
        style = TextStyle(fontSize = 20.sp, fontFeatureSettings = TABULAR_NUMS),
    )
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fg = if (pressed) AppColors.AccentPressed else AppColors.AccentText
    val border = if (pressed) AppColors.AccentPressed else AppColors.Accent
    OutlinedButton(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, border),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (pressed) AppColors.Accent.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = fg,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(stringResource(R.string.add_to_home), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun MainScreenPreview() {
    MainScreen(progress = calcYearProgress(), canPin = true, onAddWidget = {})
}
