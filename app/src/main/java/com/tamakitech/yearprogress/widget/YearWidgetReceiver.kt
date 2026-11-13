package com.tamakitech.yearprogress.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class YearWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = YearWidget()
}
