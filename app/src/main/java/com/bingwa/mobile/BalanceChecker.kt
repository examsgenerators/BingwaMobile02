package com.bingwa.mobile

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class BalanceChecker : Service() {

    companion object {
        private const val TAG = "BalanceChecker"
        var currentBalance = "Checking..."
        var balanceCallback: ((String) -> Unit)? = null
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val runnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                checkBalance()
                handler.postDelayed(this, 4000)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        handler.post(runnable)
        Log.d(TAG, "✅ Background Balance Checker Started (every 4s)")
    }

    private fun checkBalance() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("automation_enabled", true)) {
            stopSelf()
            return
        }

        try {
            // Use TelephonyManager to send USSD in background (NO POPUP)
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val ussdResponseCallback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    telephonyManager: TelephonyManager,
                    request: String,
                    response: CharSequence
                ) {
                    val balanceText = response.toString()
                    Log.d(TAG, "USSD Response: $balanceText")
                    currentBalance = balanceText
                    balanceCallback?.invoke(balanceText)
                }

                override fun onReceiveUssdResponseFailed(
                    telephonyManager: TelephonyManager,
                    request: String,
                    failureCode: Int
                ) {
                    Log.e(TAG, "USSD Failed with code: $failureCode")
                }
            }

            // Send USSD request silently
            telephonyManager.sendUssdRequest("*144#", ussdResponseCallback, Handler(Looper.getMainLooper()))
            Log.d(TAG, "📡 Background USSD sent: *144#")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "USSD error: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }
}