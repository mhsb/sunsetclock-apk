package ir.sheibah.sunsetclock.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.core.Cities
import ir.sheibah.sunsetclock.data.ThemeMode

private fun notificationsGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun fullScreenIntentAllowed(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 34) return true
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return nm.canUseFullScreenIntent()
}

private fun toast(context: Context, @StringRes id: Int) {
    Toast.makeText(context, id, Toast.LENGTH_SHORT).show()
}

private fun openSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e2: Exception) {
            // کاری از دست ما برنمی‌آید
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val fa = isFarsi()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var dialog by remember { mutableStateOf<String?>(null) }
    var notifGranted by remember { mutableStateOf(notificationsGranted(context)) }
    var fullScreenOk by remember { mutableStateOf(fullScreenIntentAllowed(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notifGranted = notificationsGranted(context)
        fullScreenOk = fullScreenIntentAllowed(context)
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }

    fun fetchLocation() {
        vm.useApproximateLocation { ok ->
            toast(context, if (ok) R.string.gps_ok else R.string.gps_failed)
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation() else toast(context, R.string.gps_denied)
    }

    val cityName = Cities.byId(settings.cityId).let { if (fa) it.fa else it.en }
    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        // ---- مکان و روش محاسبه ----
        SectionTitle(stringResource(R.string.section_location))
        SettingsCard {
            ValueRow(
                title = stringResource(R.string.city),
                value = if (settings.useGps && settings.hasGpsFix) stringResource(R.string.approx_location) else cityName,
                onClick = { dialog = "city" }
            )
            RowDivider()
            SwitchRow(
                title = stringResource(R.string.use_gps),
                subtitle = stringResource(R.string.use_gps_hint),
                checked = settings.useGps,
                onChange = { on ->
                    if (!on) {
                        vm.disableGps()
                    } else if (ir.sheibah.sunsetclock.data.LocationHelper.hasPermission(context)) {
                        fetchLocation()
                    } else {
                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
            )
            RowDivider()
            ValueRow(
                title = stringResource(R.string.calc_method),
                value = stringResource(settings.method.labelRes()),
                onClick = { dialog = "method" }
            )
            RowDivider()
            StepperRow(
                title = stringResource(R.string.hijri_adjust),
                subtitle = stringResource(R.string.hijri_adjust_hint),
                valueText = Fmt.digits(
                    when {
                        settings.hijriAdjust > 0 -> "+${settings.hijriAdjust}"
                        settings.hijriAdjust < 0 -> "−${-settings.hijriAdjust}"
                        else -> "0"
                    }, fa
                ),
                onMinus = { vm.setHijriAdjust(settings.hijriAdjust - 1) },
                onPlus = { vm.setHijriAdjust(settings.hijriAdjust + 1) }
            )
        }

        // ---- ظاهر ----
        SectionTitle(stringResource(R.string.section_appearance))
        SettingsCard {
            ValueRow(
                title = stringResource(R.string.theme),
                value = stringResource(
                    when (settings.theme) {
                        ThemeMode.SYSTEM -> R.string.theme_system
                        ThemeMode.LIGHT -> R.string.theme_light
                        ThemeMode.DARK -> R.string.theme_dark
                    }
                ),
                onClick = { dialog = "theme" }
            )
            RowDivider()
            ValueRow(
                title = stringResource(R.string.language),
                value = when {
                    currentLang.startsWith("fa") -> "فارسی"
                    currentLang.startsWith("en") -> "English"
                    else -> stringResource(R.string.theme_system)
                },
                onClick = { dialog = "lang" }
            )
        }

        // ---- هشدارها ----
        SectionTitle(stringResource(R.string.section_alarm_reliability))
        SettingsCard {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActionRow(
                    title = stringResource(R.string.perm_notifications),
                    status = stringResource(if (notifGranted) R.string.perm_ok else R.string.perm_missing),
                    ok = notifGranted,
                    buttonText = stringResource(R.string.perm_grant),
                    onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
                RowDivider()
            }
            if (Build.VERSION.SDK_INT >= 34) {
                ActionRow(
                    title = stringResource(R.string.perm_fullscreen),
                    status = stringResource(if (fullScreenOk) R.string.perm_ok else R.string.perm_missing),
                    ok = fullScreenOk,
                    buttonText = stringResource(R.string.perm_open_settings),
                    onClick = {
                        openSafely(
                            context,
                            Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:" + context.packageName)
                            )
                        )
                    }
                )
                RowDivider()
            }
            ActionRow(
                title = stringResource(R.string.perm_battery),
                status = stringResource(R.string.perm_battery_hint),
                ok = true,
                buttonText = stringResource(R.string.perm_open_settings),
                onClick = { openSafely(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            )
        }

        // ---- درباره ----
        SectionTitle(stringResource(R.string.section_about))
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (versionName.isNotBlank()) {
                    Text(
                        stringResource(R.string.version_fmt, Fmt.digits(versionName, fa)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(stringResource(R.string.about_theory), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.about_credit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { uriHandler.openUri("https://sheibah.ir") }) {
                    Text("sheibah.ir")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    // ---- دیالوگ‌ها ----
    when (dialog) {
        "city" -> CityDialog(
            selectedId = settings.cityId,
            fa = fa,
            onSelect = {
                vm.setCity(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        "method" -> ChoiceDialog(
            title = stringResource(R.string.calc_method),
            options = CalcMethod.values().map { it to stringResource(it.labelRes()) },
            selected = settings.method,
            onSelect = {
                vm.setMethod(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        "theme" -> ChoiceDialog(
            title = stringResource(R.string.theme),
            options = listOf(
                ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                ThemeMode.DARK to stringResource(R.string.theme_dark)
            ),
            selected = settings.theme,
            onSelect = {
                vm.setTheme(it)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        "lang" -> {
            val selectedTag = when {
                currentLang.startsWith("fa") -> "fa"
                currentLang.startsWith("en") -> "en"
                else -> ""
            }
            ChoiceDialog(
                title = stringResource(R.string.language),
                options = listOf(
                    "" to stringResource(R.string.theme_system),
                    "fa" to "فارسی",
                    "en" to "English"
                ),
                selected = selectedTag,
                onSelect = { tag ->
                    dialog = null
                    AppCompatDelegate.setApplicationLocales(
                        if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                        else LocaleListCompat.forLanguageTags(tag)
                    )
                },
                onDismiss = { dialog = null }
            )
        }
    }
}

// ---------- اجزای کمکی ----------

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(
    title: String,
    subtitle: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = null) }
        Text(valueText, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = null) }
    }
}

@Composable
private fun ActionRow(
    title: String,
    status: String,
    ok: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = if (ok) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.width(12.dp))
        OutlinedButton(onClick = onClick) { Text(buttonText) }
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun CityDialog(selectedId: String, fa: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().replace('ي', 'ی').replace('ك', 'ک')
    val filtered = Cities.all.filter {
        q.isEmpty() || it.fa.contains(q) || it.en.contains(q, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.city)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.id }) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(city.id) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = city.id == selectedId, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (fa) city.fa else city.en, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
