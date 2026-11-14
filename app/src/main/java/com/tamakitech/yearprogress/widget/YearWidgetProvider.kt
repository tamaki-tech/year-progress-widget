package com.tamakitech.yearprogress.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.tamakitech.yearprogress.MainActivity
import com.tamakitech.yearprogress.R
import com.tamakitech.yearprogress.YearProgress
import com.tamakitech.yearprogress.calcYearProgress
import kotlin.math.roundToInt

/**
 * ホーム画面ウィジェット。
 *
 * 更新の合図（updatePeriodMillis・時刻変更・リサイズ）を受けたその場で RemoteViews を組み立てて反映する。
 * WorkManager などの遅延実行を経由しないため、アプリをほとんど開かなくても描画が後回しにされない。
 */
class YearWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        render(context, manager, appWidgetId)
    }

    companion object {
        /** 4×4 レイアウトに切り替える最小サイズ（dp）。 */
        private const val LARGE_MIN_DP = 250

        /** 配置済みのすべてのウィジェットを即時に描き直す。 */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, YearWidgetProvider::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val progress = calcYearProgress()
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+：ランチャーが実寸に合わせて収まる最大のレイアウトを選ぶ
                RemoteViews(
                    mapOf(
                        SizeF(57f, 110f) to buildViews(context, progress, large = false),
                        SizeF(LARGE_MIN_DP.toFloat(), LARGE_MIN_DP.toFloat()) to
                            buildViews(context, progress, large = true),
                    ),
                )
            } else {
                val options = manager.getAppWidgetOptions(appWidgetId)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                buildViews(context, progress, large = width >= LARGE_MIN_DP && height >= LARGE_MIN_DP)
            }
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun buildViews(context: Context, progress: YearProgress, large: Boolean): RemoteViews {
            val layout = if (large) R.layout.widget_large else R.layout.widget_small
            val fill = when {
                large && progress.isLow -> R.drawable.widget_bar_fill_red_h
                large -> R.drawable.widget_bar_fill_green_h
                progress.isLow -> R.drawable.widget_bar_fill_red_v
                else -> R.drawable.widget_bar_fill_green_v
            }
            // ClipDrawable のレベル 0〜10000 = 残り割合
            val level = (progress.remainingRatio * 10_000).roundToInt().coerceIn(0, 10_000)

            return RemoteViews(context.packageName, layout).apply {
                setTextViewText(R.id.text_year, progress.year.toString())
                setTextViewText(R.id.text_percent, progress.percentText)
                setImageViewResource(R.id.bar_fill, fill)
                setInt(R.id.bar_fill, "setImageLevel", level)
                if (large) {
                    setTextViewText(
                        R.id.text_days,
                        context.getString(R.string.remaining_days, progress.remainingDays.toInt()),
                    )
                }
                setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
            }
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
