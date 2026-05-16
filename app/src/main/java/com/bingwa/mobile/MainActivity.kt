@file:OptIn(ExperimentalMaterial3Api::class)

package com.bingwa.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("automation_enabled", true)) startService(Intent(this, BalanceChecker::class.java))
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF1A73E8),
                secondary = Color(0xFF34A853),
                background = Color(0xFFF8F9FA),
                surface = Color.White
            )) {
                BingwaApp()
            }
        }
    }
}

@Composable
fun BingwaApp() {
    var selected by remember { mutableStateOf(0) }
    val ctx = LocalContext.current
    val tm = remember { TokenManager(ctx) }
    var balance by remember { mutableStateOf(tm.getBalance()) }
    var airtime by remember { mutableStateOf("...") }

    DisposableEffect(Unit) {
        BalanceChecker.balanceCallback = { raw ->
            // Parse the USSD response to extract a numeric balance
            val cleaned = raw.replace(Regex("[^\\d.]"), " ").trim()
            val numbers = cleaned.split("\\s+".toRegex()).filter { it.toDoubleOrNull() != null }
            airtime = if (numbers.isNotEmpty()) "KSh ${numbers.first()}" else "KSh --"
        }
        onDispose { BalanceChecker.balanceCallback = null }
    }
    LaunchedEffect(selected) { balance = tm.getBalance() }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                listOf(
                    "Home" to Icons.Default.Home,
                    "Offers" to Icons.Default.ShoppingCart,
                    "Contacts" to Icons.Default.Contacts,
                    "Transactions" to Icons.Default.History,
                    "Settings" to Icons.Default.Settings
                ).forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(icon, label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1A73E8),
                            selectedTextColor = Color(0xFF1A73E8),
                            unselectedIconColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> DashboardScreen(balance, airtime)
                1 -> OffersScreen()
                2 -> ContactsScreen()
                3 -> TransactionsScreen()
                4 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen(bal: Int, air: String) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // Header card
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF1557B0))))
                .padding(24.dp)
        ) {
            Column {
                Text("Bingwa Mobile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Automated M‑PESA Agent", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Token balance
                    Card(
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Token, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("$bal", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("Tokens", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    // Airtime balance
                    Card(
                        Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(air, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("Airtime", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Info cards
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("How It Works", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121)) }
            item {
                InfoCard(Icons.Default.Add, "Buy Tokens",
                    "Send money to VICTOR NGETICH via M‑PESA.\nKSh 10 = 90 tokens | KSh 50 = 500 tokens | KSh 100 = 1000 tokens",
                    Color(0xFF1A73E8))
            }
            item {
                InfoCard(Icons.Default.ShoppingCart, "Sell Data",
                    "Client sends money to YOUR M‑PESA.\nApp deducts 1 token and dials USSD automatically.",
                    Color(0xFF34A853))
            }
            item {
                InfoCard(Icons.Default.Refresh, "Airtime Balance",
                    "Checks *144# every 4 seconds via Accessibility.\nPopup appears briefly; balance shown above.",
                    Color(0xFFFF9800))
            }
        }
    }
}

@Composable
fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, color: Color) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, title, tint = color, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Spacer(Modifier.height(4.dp))
                Text(desc, fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

// ─── Offers, Contacts, Transactions, Settings (unchanged from last full file) ───
// … (keep the existing implementations from the previous complete MainActivity.kt)