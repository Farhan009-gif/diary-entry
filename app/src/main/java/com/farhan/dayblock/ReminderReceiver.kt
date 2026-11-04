package com.farhan.dayblock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.farhan.dayblock.data.DayblockDatabase
import com.farhan.dayblock.data.DiaryRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Fires the daily writing reminder. Skips quietly on days where today's
 * block is already placed - no nagging about a finished job.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val repository = DiaryRepository(
            DayblockDatabase.create(context.applicationContext).diaryDao()
        )
        val alreadyWritten = runBlocking {
            repository.getByDate(LocalDate.now().toString()) != null
        }
        if (alreadyWritten) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Writing reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, 7, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = try {
            context.applicationInfo.icon.takeIf { it != 0 }
                ?: android.R.drawable.ic_dialog_info
        } catch (e: Exception) {
            android.R.drawable.ic_dialog_info
        }

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle("Time to place today's block")
            .setContentText("A few honest lines in Dayblock, then the day is sealed.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "dayblock_reminders"
        private const val NOTIFICATION_ID = 1001
    }
}
