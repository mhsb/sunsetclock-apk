package ir.sheibah.sunsetclock.ui

import android.Manifest
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.core.Cities
import ir.sheibah.sunsetclock.data.LocationHelper

private const val LAST_STEP = 3

/** صفحه‌ی خوش‌آمدگویی اولین اجرا: زبان، مکان، روش محاسبه و تاریخ قمری، مجوزها. */
@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val fa = isFarsi()
    val context = LocalContext.current
    // با تغییر زبان، اکتیویتی دوباره ساخته می‌شود؛ پس مرحله ذخیره می‌شود
    var step by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }

    fun fetchLocation() {
        vm.useApproximateLocation { ok ->
            toast(context, if (ok) R.string.gps_ok else R.string.gps_failed)
        }
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation() else toast(context, R.string.gps_denied)
    }

    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val langTag = when {
        currentLang.startsWith("fa") -> "fa"
        currentLang.startsWith("en") -> "en"
        else -> ""
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                stringResource(R.string.onb_step_fmt, Fmt.num(step + 1, fa), Fmt.num(LAST_STEP + 1, fa)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (step + 1f) / (LAST_STEP + 1) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onb"
            ) { s ->
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (s) {
                        0 -> {
                            Text(stringResource(R.string.onb_welcome_title), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(R.string.onb_lang_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsCard {
                                ValueRow(
                                    title = stringResource(R.string.language),
                                    value = when (langTag) {
                                        "fa" -> "فارسی"
                                        "en" -> "English"
                                        else -> stringResource(R.string.theme_system)
                                    },
                                    onClick = { dialog = "lang" }
                                )
                            }
                        }
                        1 -> {
                            Text(stringResource(R.string.onb_location_title), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(R.string.onb_location_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsCard {
                                ValueRow(
                                    title = stringResource(R.string.city),
                                    value = if (settings.useGps && settings.hasGpsFix) stringResource(R.string.approx_location)
                                    else Cities.byId(settings.cityId).let { if (fa) it.fa else it.en },
                                    onClick = { dialog = "city" }
                                )
                                RowDivider()
                                SwitchRow(
                                    title = stringResource(R.string.use_gps),
                                    subtitle = stringResource(R.string.use_gps_hint),
                                    checked = settings.useGps,
                                    onChange = { on ->
                                        if (!on) vm.disableGps()
                                        else if (LocationHelper.hasPermission(context)) fetchLocation()
                                        else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                )
                            }
                        }
                        2 -> {
                            Text(stringResource(R.string.onb_method_title), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(R.string.onb_method_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsCard {
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
                        }
                        else -> {
                            Text(stringResource(R.string.onb_perm_title), style = MaterialTheme.typography.headlineMedium)
                            Text(
                                stringResource(R.string.onb_perm_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsCard { PermissionRows() }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                if (step > 0) {
                    TextButton(onClick = { step -= 1 }) { Text(stringResource(R.string.onb_back)) }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    if (step < LAST_STEP) step += 1 else vm.completeOnboarding()
                }) {
                    Text(stringResource(if (step < LAST_STEP) R.string.onb_next else R.string.onb_finish))
                }
            }
        }
    }

    when (dialog) {
        "lang" -> ChoiceDialog(
            title = stringResource(R.string.language),
            options = listOf(
                "" to stringResource(R.string.theme_system),
                "fa" to "فارسی",
                "en" to "English"
            ),
            selected = langTag,
            onSelect = { tag ->
                dialog = null
                AppCompatDelegate.setApplicationLocales(
                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                    else LocaleListCompat.forLanguageTags(tag)
                )
            },
            onDismiss = { dialog = null }
        )
        "city" -> CityDialog(
            selectedId = settings.cityId,
            fa = fa,
            onSelect = { vm.setCity(it); dialog = null },
            onDismiss = { dialog = null }
        )
        "method" -> ChoiceDialog(
            title = stringResource(R.string.calc_method),
            options = CalcMethod.values().map { it to stringResource(it.labelRes()) },
            selected = settings.method,
            onSelect = { vm.setMethod(it); dialog = null },
            onDismiss = { dialog = null }
        )
    }
}
