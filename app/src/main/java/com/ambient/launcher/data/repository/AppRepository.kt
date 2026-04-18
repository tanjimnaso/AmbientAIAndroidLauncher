package com.ambient.launcher.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import com.ambient.launcher.home.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AppRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val profiles = launcherApps.profiles
        
        val apps = mutableListOf<AppInfo>()
        for (profile in profiles) {
            launcherApps.getActivityList(null, profile).forEach { info ->
                if (info.componentName.packageName != context.packageName) {
                    val pkg = info.componentName.packageName
                    val installTime = try {
                        context.packageManager.getPackageInfo(pkg, 0).firstInstallTime
                    } catch (_: Exception) { 0L }
                    apps.add(AppInfo(label = info.label.toString(), packageName = pkg, firstInstallTime = installTime))
                }
            }
        }
        
        val sortedApps = sortAppsByUsage(apps)
        sortedApps.distinctBy { it.packageName }
    }

    private fun sortAppsByUsage(apps: List<AppInfo>): List<AppInfo> {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }

        if (mode != AppOpsManager.MODE_ALLOWED) {
            return apps.sortedBy { it.label.lowercase() }
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - java.util.concurrent.TimeUnit.DAYS.toMillis(3)

        val stats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        
        return apps.sortedByDescending { app ->
            stats[app.packageName]?.totalTimeInForeground ?: 0L
        }
    }
}
