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
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1A73E8), secondary = Color(0xFF34A853), background = Color(0xFFF8F9FA), surface = Color.White)) {
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
                listOf("Home" to Icons.Default.Home, "Offers" to Icons.Default.ShoppingCart, "Contacts" to Icons.Default.Contacts, "Transactions" to Icons.Default.History, "Settings" to Icons.Default.Settings)
                    .forEachIndexed { i, (label, icon) ->
                        NavigationBarItem(selected = selected == i, onClick = { selected = i },
                            icon = { Icon(icon, label) }, label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF1A73E8), selectedTextColor = Color(0xFF1A73E8), unselectedIconColor = Color.Gray))
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
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF1557B0)))).padding(24.dp)) {
            Column {
                Text("Bingwa Mobile", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Automated M-PESA Agent", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Token, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp)); Text("$bal", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Text("Tokens", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f))) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp)); Text(air, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("Airtime", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("How It Works", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121)) }
            item { InfoCard(Icons.Default.Add, "Buy Tokens", "Send money to VICTOR NGETICH via M-PESA.\nKSh 10 = 90 tokens | KSh 50 = 500 tokens | KSh 100 = 1000 tokens", Color(0xFF1A73E8)) }
            item { InfoCard(Icons.Default.ShoppingCart, "Sell Data", "Client sends money to YOUR M-PESA.\nApp deducts 1 token and dials USSD automatically.", Color(0xFF34A853)) }
            item { InfoCard(Icons.Default.Refresh, "Airtime Balance", "Checks *144# every 4 seconds via Accessibility.\nPopup appears briefly; balance shown above.", Color(0xFFFF9800)) }
        }
    }
}

@Composable
fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String, color: Color) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) { Icon(icon, title, tint = color, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(12.dp))
            Column { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121)); Spacer(Modifier.height(4.dp)); Text(desc, fontSize = 13.sp, color = Color.Gray) }
        }
    }
}

// ─── Offers ───
@Composable
fun OffersScreen() {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("DataOffers", Context.MODE_PRIVATE)
    val gson = remember { Gson() }
    var offers by remember {
        mutableStateOf(try {
            gson.fromJson<List<DataOffer>>(prefs.getString("offers", "[]")!!, object : TypeToken<List<DataOffer>>() {}.type)
        } catch (_: Exception) {
            listOf(DataOffer("250MB Daily", 20, "*180*5*2*1#", "ADVANCED", "daily"), DataOffer("1GB Weekly", 50, "*180*5*2*2#", "ADVANCED", "weekly"))
        })
    }
    var showDialog by remember { mutableStateOf(false) }
    var editOffer by remember { mutableStateOf<DataOffer?>(null) }

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Data Offers", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Configure USSD products", fontSize = 14.sp, color = Color.Gray) }
                Button(onClick = { editOffer = null; showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(4.dp)); Text("Add Offer")
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(offers) { idx, offer ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(offer.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("USSD: ${offer.ussdCode}  |  KSh ${offer.price}  |  ${offer.executionMode}  |  ${offer.mode}", fontSize = 13.sp, color = Color.Gray)
                            }
                            Row {
                                IconButton(onClick = { editOffer = offer; showDialog = true }) { Icon(Icons.Default.Edit, "Edit", tint = Color(0xFF1A73E8)) }
                                IconButton(onClick = {
                                    offers = offers.toMutableList().also { it.removeAt(idx) }
                                    prefs.edit().putString("offers", gson.toJson(offers)).apply()
                                }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFF44336)) }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDialog) {
        AddOfferDialog(existing = editOffer, onSave = { offer ->
            offers = offers.toMutableList().also { list ->
                val idx = list.indexOfFirst { it.name == offer.name }
                if (idx >= 0) list[idx] = offer else list.add(offer)
            }
            prefs.edit().putString("offers", gson.toJson(offers)).apply()
            showDialog = false
        }, onDismiss = { showDialog = false })
    }
}

@Composable
fun AddOfferDialog(existing: DataOffer?, onSave: (DataOffer) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var price by remember { mutableStateOf(existing?.price?.toString() ?: "") }
    var code by remember { mutableStateOf(existing?.ussdCode ?: "") }
    var execMode by remember { mutableStateOf(existing?.executionMode ?: "SIMPLE") }
    var mode by remember { mutableStateOf(existing?.mode ?: "daily") }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (existing != null) "Edit Offer" else "Add Offer") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (KSh)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("USSD code") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Execution: ")
                FilterChip(selected = execMode == "SIMPLE", onClick = { execMode = "SIMPLE" }, label = { Text("SIMPLE") })
                Spacer(Modifier.width(4.dp))
                FilterChip(selected = execMode == "ADVANCED", onClick = { execMode = "ADVANCED" }, label = { Text("ADVANCED") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode: ")
                listOf("daily", "weekly", "monthly").forEach {
                    FilterChip(selected = mode == it, onClick = { mode = it }, label = { Text(it) })
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }, confirmButton = {
        Button(onClick = {
            val p = price.toIntOrNull() ?: 0
            if (p > 0 && code.isNotBlank()) onSave(DataOffer(name.ifBlank { "New" }, p, code, execMode, mode))
        }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─── Contacts ───
@Composable
fun ContactsScreen() {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("saved_contacts", Context.MODE_PRIVATE)
    var contacts by remember { mutableStateOf(loadContacts(prefs)) }
    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Saved Contacts", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("From M-PESA messages", color = Color.Gray) }
                TextButton(onClick = { contacts.forEach { exportToSystem(ctx, it) }; Toast.makeText(ctx, "Exporting...", Toast.LENGTH_SHORT).show() }) { Text("Export all") }
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts, key = { it.phone }) { contact ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text(contact.phone, fontWeight = FontWeight.Medium); if (contact.name.isNotBlank()) Text(contact.name, color = Color.Gray) }
                        IconButton(onClick = { contacts = contacts.toMutableList().also { it.remove(contact) }; saveContacts(prefs, contacts) }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFF44336)) }
                    }
                }
            }
        }
    }
}

data class SavedContact(val name: String, val phone: String)
private fun loadContacts(prefs: SharedPreferences): List<SavedContact> {
    return try {
        val arr = JSONArray(prefs.getString("list", "[]")!!)
        (0 until arr.length()).map { val o = arr.getJSONObject(it); SavedContact(o.optString("name"), o.getString("phone")) }
    } catch (_: Exception) { emptyList() }
}
private fun saveContacts(prefs: SharedPreferences, list: List<SavedContact>) {
    val arr = JSONArray(); list.forEach { arr.put(JSONObject().apply { put("name", it.name); put("phone", it.phone) }) }
    prefs.edit().putString("list", arr.toString()).apply()
}
private fun exportToSystem(context: Context, contact: SavedContact) {
    try {
        val ops = arrayListOf<android.content.ContentProviderOperation>()
        ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build())
        ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phone)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build())
        if (contact.name.isNotBlank()) {
            ops.add(android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.name).build())
        }
        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    } catch (_: Exception) { Toast.makeText(context, "Permission needed", Toast.LENGTH_SHORT).show() }
}

// ─── Transactions ───
@Composable
fun TransactionsScreen() {
    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) { Column(Modifier.padding(24.dp)) { Text("Transactions", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Automated operations", color = Color.Gray) } }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(64.dp)); Spacer(Modifier.height(16.dp)); Text("No transactions yet", color = Color.Gray) } }
    }
}

// ─── Settings ───
@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
    var auto by remember { mutableStateOf(prefs.getBoolean("automation_enabled", true)) }
    var saveContacts by remember { mutableStateOf(prefs.getBoolean("auto_save_contacts", false)) }
    var simId by remember { mutableStateOf(prefs.getInt("selected_sim_id", -1)) }
    var showSimDlg by remember { mutableStateOf(false) }
    val simList = remember { getAvailableSims(ctx) }
    val simLabel = if (simId == -1) "Default" else simList.find { it.subscriptionId == simId }?.displayName?.toString() ?: "Unknown"

    Column(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        Surface(Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 4.dp) { Column(Modifier.padding(24.dp)) { Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Bingwa Mobile v1.0", color = Color.Gray) } }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column {
                        SwitchRow("Enable Automation", "Turn ON/OFF SMS & USSD processing", Icons.Default.PowerSettingsNew, auto) {
                            auto = it; prefs.edit().putBoolean("automation_enabled", it).apply()
                            if (it) ctx.startService(Intent(ctx, BalanceChecker::class.java)) else ctx.stopService(Intent(ctx, BalanceChecker::class.java))
                        }
                        Divider(Modifier.padding(horizontal = 16.dp))
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SimCard, null, tint = Color(0xFF1A73E8), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text("SIM for USSD", fontWeight = FontWeight.Medium); Text(simLabel, color = Color.Gray) }
                            TextButton(onClick = { showSimDlg = true }) { Text("Change") }
                        }
                        Divider(Modifier.padding(horizontal = 16.dp))
                        SwitchRow("Auto-save contacts", "Save numbers from M-PESA messages", Icons.Default.Contacts, saveContacts) {
                            saveContacts = it; prefs.edit().putBoolean("auto_save_contacts", it).apply()
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Bingwa Mobile v1.0", fontWeight = FontWeight.Bold)
                        Text("Powered by Victor Ngetich", color = Color(0xFF1A73E8), fontSize = 12.sp)
                    }
                }
            }
        }
    }
    if (showSimDlg) {
        SimDialog(simList, simId, { simId = it; prefs.edit().putInt("selected_sim_id", it).apply() }, { showSimDlg = false })
    }
}

@SuppressLint("MissingPermission")
fun getAvailableSims(ctx: Context): List<SubscriptionInfo> {
    if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return emptyList()
    return if (Build.VERSION.SDK_INT >= 22) (ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager).activeSubscriptionInfoList ?: emptyList() else emptyList()
}

@Composable
fun SimDialog(list: List<SubscriptionInfo>, cur: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Select SIM") }, text = {
        if (list.isEmpty()) Text("No SIMs found or permission missing.")
        else Column { list.forEach { sim ->
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = sim.subscriptionId == cur, onClick = { onSelect(sim.subscriptionId) })
                Spacer(Modifier.width(8.dp)); Text("${sim.displayName} (Slot ${sim.simSlotIndex+1})")
            }
        } }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
fun SwitchRow(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) { Icon(icon, title, tint = Color(0xFF1A73E8), modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Medium); Text(desc, fontSize = 13.sp, color = Color.Gray) }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF1A73E8)))
    }
}