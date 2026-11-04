package com.farhan.dayblock

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.farhan.dayblock.data.DayblockDatabase
import com.farhan.dayblock.data.DiaryRepository
import com.farhan.dayblock.domain.DiaryStatsCalculator
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Home-screen widget: today's prompt, the current streak, and whether
 * today's block is placed yet. Tapping anywhere opens Dayblock.
 */
class DayblockWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val repository = DiaryRepository(
            DayblockDatabase.create(context.applicationContext).diaryDao()
        )
        val today = LocalDate.now()
        val (streak, writtenToday) = runBlocking {
            val entries = repository.entries.first()
            val stats = DiaryStatsCalculator.calculate(entries, today)
            stats.currentStreak to (repository.getByDate(today.toString()) != null)
        }

        val views = RemoteViews(context.packageName, R.layout.widget_dayblock)
        views.setTextViewText(
            R.id.widgetStreak,
            if (streak > 0) "$streak day streak" else "Start your streak"
        )
        views.setTextViewText(
            R.id.widgetStatus,
            if (writtenToday) "Today's block is placed. Nicely done."
            else "Today's block is still waiting."
        )
        views.setTextViewText(R.id.widgetPrompt, PROMPTS[today.dayOfYear % PROMPTS.size])

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, widgetId, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pending)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    companion object {
        private val PROMPTS = listOf(
            "What was the best small moment of today?",
            "What did you learn today, in or out of school?",
            "Describe today in exactly three sentences.",
            "What is one thing you want tomorrow-you to remember?",
            "Who made your day better, and how?",
            "What challenged you today, and what did you do about it?",
            "Write about a sound, smell or scene from today.",
            "What are you looking forward to this week?"
        )
    }
}
