package com.example.fyp_hotspot_mobility.pruner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.fyp_hotspot_mobility.data.local.AppDatabase
import java.util.concurrent.TimeUnit

class LogPruningWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val bluetoothDao = database.bluetoothDao()
        
        // Prune logs older than 24 hours
        val threshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        bluetoothDao.deleteOldLogs(threshold)
        
        return Result.success()
    }
}
