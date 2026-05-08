package com.example.addnevnik.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.addnevnik.data.repository.SettingsRepository
import com.example.addnevnik.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val repository = SettingsRepository.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                val settings = repository.settings.first()
                if (settings.morningReminderEnabled) {
                    ReminderScheduler.scheduleReminder(
                        context,
                        "morning",
                        settings.morningReminderHour,
                        settings.morningReminderMinute
                    )
                }
                if (settings.eveningReminderEnabled) {
                    ReminderScheduler.scheduleReminder(
                        context,
                        "evening",
                        settings.eveningReminderHour,
                        settings.eveningReminderMinute
                    )
                }
            }
        }
    }
}
