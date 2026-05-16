package com.bingwa.mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class BalanceChecker : Service() {
    companion object {
        private const val TAG = "BalanceChecker"
        var currentBalance = "Checking..."
        var balanceCallback: ((String) -> Unit)? = null
    }
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private val runnable = object : Runnable {
        override fun run() { if (isRunning) { checkBalance(); handler.postDelayed(this, 4000) } }
    }
    override fun onCreate() { super.onCreate(); isRunning = true; handler.post(runnable) }
    private fun checkBalance() {
        if (!getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("automation_enabled", true)) { stopSelf(); return }
        try {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:*144%23")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            UssdNavigationService.balanceCallback = { balance -> currentBalance = balance; balanceCallback?.invoke(balance) }
        } catch (e: SecurityException) { Log.e(TAG, "Permission denied") }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { isRunning = false; handler.removeCallbacks(runnable); super.onDestroy() }
}
