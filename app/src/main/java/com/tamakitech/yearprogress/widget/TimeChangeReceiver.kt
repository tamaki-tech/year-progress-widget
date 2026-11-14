package com.tamakitech.yearprogress.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 時刻・タイムゾーンが変わったら、ウィジェットをすぐ描き直す。 */
class TimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED ->
                YearWidgetProvider.updateAll(context)
        }
    }
}
