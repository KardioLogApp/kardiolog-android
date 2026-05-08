package com.example.addnevnik.util

import android.content.Context
import androidx.work.*
import com.example.addnevnik.worker.BpReminderWorker
import java.util.*
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleReminder(context: Context, tag: String, hour: Int, minute: Int) {
        val workManager = WorkManager.getInstance(context)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = calendar.timeInMillis - System.currentTimeMillis()
        val notificationId = if (tag == "morning") 1 else 2

        val workRequest = PeriodicWorkRequestBuilder<BpReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(tag)
            .setInputData(workDataOf("notification_id" to notificationId))
            .build()

        workManager.enqueueUniquePeriodicWork(
            tag,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelReminder(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelUniqueWork(tag)
    }
}
