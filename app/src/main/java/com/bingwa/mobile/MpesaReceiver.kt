package com.bingwa.mobile

import android.content.*
import android.telephony.SmsMessage
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class MpesaReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MpesaReceiver"
        private val TOKEN_NAMES = listOf("victor ngetich", "victor kiplangat ngetich")
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (!appPrefs.getBoolean("automation_enabled", true)) return

        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
        for (pdu in pdus) {
            val sms = SmsMessage.createFromPdu(pdu as ByteArray)
            val body = sms.messageBody ?: continue
            val sender = sms.originatingAddress ?: ""
            if (!sender.equals("MPESA", true)) continue

            val lower = body.lowercase()
            val isTokenPurchase = TOKEN_NAMES.any { lower.contains(it) }

            if (appPrefs.getBoolean("auto_save_contacts", false)) {
                val partyNumber = extractPartyNumber(body)
                if (partyNumber.isNotEmpty()) saveContact(context, partyNumber)
            }

            if (isTokenPurchase) handleTokenPurchase(context, body)
            else if (lower.contains("received")) handleDataSelling(context, body)
        }
    }

    private fun handleTokenPurchase(context: Context, body: String) {
        val tokens = context.getSharedPreferences("TokenStore", Context.MODE_PRIVATE)
        val cur = tokens.getInt("balance", 0)
        val amount = extractAmount(body)
        val add = when {
            amount >= 100 -> 1000
            amount >= 50 -> 500
            amount >= 10 -> 90
            else -> 0
        }
        if (add > 0) {
            tokens.edit().putInt("balance", cur + add).apply()
            notify(context, "Tokens Added", "+$add tokens (KSh $amount)")
        }
    }

    private fun handleDataSelling(context: Context, body: String) {
        val tokens = context.getSharedPreferences("TokenStore", Context.MODE_PRIVATE)
        val cur = tokens.getInt("balance", 0)
        val amount = extractAmount(body)
        val phone = extractPhoneNumber(body)

        val offers = try {
            val json = context.getSharedPreferences("DataOffers", Context.MODE_PRIVATE).getString("offers", "[]")!!
            com.google.gson.Gson().fromJson(json, Array<DataOffer>::class.java).toList()
        } catch (e: Exception) { emptyList<DataOffer>() }

        val offer = offers.find { it.price == amount }

        // Token cost is always 1
        val tokenCost = 1

        if (offer != null && cur >= tokenCost) {
            tokens.edit().putInt("balance", cur - tokenCost).apply()
            context.startService(Intent(context, AutomationService::class.java).apply {
                putExtra("mode", offer.executionMode)   // SIMPLE or ADVANCED from offer
                putExtra("code", offer.ussdCode)
                putExtra("phoneNumber", phone)
            })
            notify(context, "USSD Sent", "${offer.name} for $phone")
        } else if (offer != null) {
            notify(context, "Insufficient Tokens", "Need $tokenCost token, have $cur")
        }
    }

    private fun extractAmount(body: String) = Regex("Ksh\\s*(\\d+)", RegexOption.IGNORE_CASE).find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    private fun extractPhoneNumber(body: String) = Regex("07\\d{8}").find(body)?.value ?: ""
    private fun extractPartyNumber(body: String): String {
        val regexFrom = Regex("from\\s*(07\\d{8})", RegexOption.IGNORE_CASE)
        val regexTo = Regex("to\\s*(07\\d{8})", RegexOption.IGNORE_CASE)
        return regexFrom.find(body)?.groupValues?.get(1) ?: regexTo.find(body)?.groupValues?.get(1) ?: ""
    }

    private fun saveContact(context: Context, number: String) {
        val prefs = context.getSharedPreferences("saved_contacts", Context.MODE_PRIVATE)
        val list = try { JSONArray(prefs.getString("list", "[]")) } catch (e: Exception) { JSONArray() }
        for (i in 0 until list.length()) { if (list.getJSONObject(i).getString("phone") == number) return }
        list.put(JSONObject().apply { put("name", ""); put("phone", number) })
        prefs.edit().putString("list", list.toString()).apply()
    }

    private fun notify(context: Context, title: String, msg: String) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                nm.createNotificationChannel(android.app.NotificationChannel("bingwa", "Bingwa", android.app.NotificationManager.IMPORTANCE_HIGH))
            }
            nm.notify(System.currentTimeMillis().toInt(), androidx.core.app.NotificationCompat.Builder(context, "bingwa")
                .setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(msg).setAutoCancel(true).build())
        } catch (_: Exception) {}
    }
}