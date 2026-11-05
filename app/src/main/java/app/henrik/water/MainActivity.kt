package app.henrik.water

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = when {
                Build.VERSION.SDK_INT >= 31 ->
                    if (dark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = colors) {
                Surface(Modifier.fillMaxSize()) { App() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Notifier.updatePersistent(this)
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        Notifier.updatePersistent(ctx)
    }
    LaunchedEffect(Unit) {
        if (!Notifier.canPost(ctx)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Text("💧") }, label = { Text("Intake") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Text("⏰") }, label = { Text("Settings") }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            if (tab == 0) DayScreen() else SettingsScreen()
        }
    }
}

private fun fmtTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

@Composable
fun DayScreen() {
    val ctx = LocalContext.current
    Store.version.intValue // subscribe to data changes
    var daysAgo by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val date = today.minusDays(daysAgo.toLong())
    val glass = Store.glassMl(ctx)
    val ml = Store.intake(ctx, date)
    val bags = Store.movicol(ctx, date)

    val title = when (daysAgo) {
        0 -> "Today"
        1 -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy"))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { daysAgo++ }) { Text("‹ Prev") }
                Text(title, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { daysAgo-- }, enabled = daysAgo > 0) { Text("Next ›") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$ml ml", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                    Text("%.1f glasses (1 glass = %d ml)".format(ml / glass.toFloat(), glass))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        Store.addIntake(ctx, date, -glass)
                        Notifier.updatePersistent(ctx)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("− Glass") }
                Button(
                    onClick = {
                        Store.addIntake(ctx, date, glass)
                        Notifier.updatePersistent(ctx)
                    },
                    modifier = Modifier.weight(2f)
                ) { Text("+ Add glass ($glass ml)") }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Movicol", style = MaterialTheme.typography.titleMedium)
                        Text("bags taken")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { Store.addMovicol(ctx, date, -1) }, enabled = bags > 0) { Text("−") }
                        Text(
                            "$bags",
                            Modifier.padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Button(onClick = { Store.addMovicol(ctx, date, 1) }) { Text("+") }
                    }
                }
            }
        }
        item {
            Text("Last 14 days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
        items(14) { i ->
            val d = today.minusDays(i.toLong())
            val dayMl = Store.intake(ctx, d)
            val dayBags = Store.movicol(ctx, d)
            Row(
                Modifier.fillMaxWidth()
                    .clickable { daysAgo = i }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    d.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                    fontWeight = if (i == daysAgo) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    "$dayMl ml" + if (dayBags > 0) "  ·  $dayBags movicol" else "",
                    fontWeight = if (i == daysAgo) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private fun pickTime(ctx: Context, initial: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, initial / 60, initial % 60, true).show()
}

private fun saveReminders(ctx: Context, list: List<Int>) {
    Store.setReminders(ctx, list)
    Scheduler.scheduleAll(ctx)
}

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    Store.version.intValue // subscribe to data changes
    val reminders = Store.reminders(ctx)
    var glassText by rememberSaveable { mutableStateOf(Store.glassMl(ctx).toString()) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineMedium) }
        item {
            OutlinedTextField(
                value = glassText,
                onValueChange = { t ->
                    val digits = t.filter { it.isDigit() }.take(4)
                    glassText = digits
                    digits.toIntOrNull()?.takeIf { it in 50..2000 }?.let {
                        Store.setGlassMl(ctx, it)
                        Notifier.updatePersistent(ctx)
                    }
                },
                label = { Text("Glass size (ml, 50–2000)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Text("Reminders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        }
        if (reminders.isEmpty()) {
            item { Text("No reminders") }
        }
        items(reminders.size) { i ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    pickTime(ctx, reminders[i]) { m ->
                        saveReminders(ctx, reminders.toMutableList().also { it[i] = m })
                    }
                }) { Text(fmtTime(reminders[i]), style = MaterialTheme.typography.titleLarge) }
                TextButton(onClick = {
                    saveReminders(ctx, reminders.toMutableList().also { it.removeAt(i) })
                }) { Text("Remove") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    pickTime(ctx, 12 * 60) { m -> saveReminders(ctx, reminders + m) }
                }) { Text("Add reminder") }
                OutlinedButton(onClick = { saveReminders(ctx, Store.DEFAULT_REMINDERS) }) {
                    Text("Reset to default")
                }
            }
        }
    }
}
