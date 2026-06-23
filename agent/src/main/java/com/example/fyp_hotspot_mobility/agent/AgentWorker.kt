package com.example.fyp_hotspot_mobility.agent

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AgentWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val intent = Intent(context, AgentService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        return Result.success()
    }
}
