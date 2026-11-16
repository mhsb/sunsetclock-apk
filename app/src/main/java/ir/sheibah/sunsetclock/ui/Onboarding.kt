package ir.sheibah.sunsetclock.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.sheibah.sunsetclock.MainViewModel
import ir.sheibah.sunsetclock.R
import ir.sheibah.sunsetclock.astro.CalcMethod
import ir.sheibah.sunsetclock.astro.Calendars
import ir.sheibah.sunsetclock.core.Cities
import ir.sheibah.sunsetclock.data.LocationHelper
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.sin

private const val LAST_STEP = 3

private val SkyDusk = listOf(Color(0xFF2B2F6B), Color(0xFF7B5EA7), Color(0xFFFF8A5C), Color(0xFFFFC775))

/** تصویر بالای هر مرحله: آسمان غروب با خورشید و ابرهای متحرک و یک آیکن بزرگ */
@Composable
private fun SkyHeader(icon: ImageVector, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "sky")
    val drift by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart), label = "drift"
    )
    val bob by t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob"
    )
    val halo by t.animateFloat(
        0.9f, 1.35f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "halo"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
            .background(Brush.verticalGradient(SkyDusk)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // ستاره‌ها
            for (i in 0 until 22) {
                val x = ((i * 97) % 100) / 100f * w
                val y = ((i * 53) % 55) / 100f * h
                val tw = 0.35f + 0.35f * sin((bob * 2 * PI + i).toFloat())
                drawCircle(Color.White.copy(alpha = tw), radius = (1.2f + (i % 3)).dp.toPx(), center = Offset(x, y))
            }
            // خورشید و هاله
            val sc = Offset(w * 0.5f, h * (0.72f - 0.04f * bob))
            drawCircle(Color(0xFFFFE2A8).copy(alpha = 0.22f), radius = 120.dp.toPx() * halo, center = sc)
            drawCircle(Color(0xFFFFD27A).copy(alpha = 0.35f), radius = 84.dp.toPx() * halo, center = sc)
            drawCircle(Color(0xFFFFF1CF), radius = 56.dp.toPx(), center = sc)
            // ابرها
            for (k in 0 until 3) {
                val x = ((drift + k / 3f) % 1f) * (w + 240.dp.toPx()) - 120.dp.toPx()
                val y = h * (0.22f + 0.17f * k)
                val a = 0.16f + 0.05f * k
                drawRoundRect(
                    Color.White.copy(alpha = a), topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(110.dp.toPx(), 26.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx())
                )
            }
            // تپه‌ها
            val hill = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.84f)
                cubicTo(w * 0.25f, h * 0.70f, w * 0.45f, h * 0.92f, w * 0.7f, h * 0.80f)
                cubicTo(w * 0.85f, h * 0.73f, w * 0.95f, h * 0.80f, w, h * 0.78f)
                lineTo(w, h)
                close()
            }
            drawPath(hill, Color(0xFF1E2252).copy(alpha = 0.55f))
        }
        Box(
            modifier = Modifier
                .padding(top = 28.dp)
                .size(92.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFE67E00), modifier = Modifier.size(48.dp))
        }
    }
}

/** دکمه‌ی بزرگ انتخاب زبان */
@Composable
private fun LanguageButton(title: String, sample: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    SoftCard(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        brush = if (selected) SunsetBrushSoft else null,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(sample, fontSize = 40.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BigChoice(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(SunsetBrushSoft),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** صفحه‌ی خوش‌آمدگویی اولین اجرا: زبان، مکان، روش محاسبه و تاریخ قمری، مجوزها. */
@OptIn(ExperimentalAnimationApi::class)
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

    fun setLang(tag: String) {
        step = 1 // اول مرحله ذخیره شود؛ تغییر زبان صفحه را دوباره می‌سازد
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    val currentLang = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val langTag = when {
        currentLang.startsWith("fa") -> "fa"
        currentLang.startsWith("en") -> "en"
        else -> ""
    }

    Box(
        modifier = Modifier.fillMaxSize().background(appBackgroundBrush())
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(350)) { it * dir / 3 } + fadeIn(tween(350))) togetherWith
                        (slideOutHorizontally(tween(250)) { -it * dir / 3 } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f),
                label = "onb"
            ) { s ->
                Column(modifier = Modifier.fillMaxSize()) {
                    SkyHeader(
                        icon = when (s) {
                            0 -> Icons.Filled.Translate
                            1 -> Icons.Filled.LocationOn
                            2 -> Icons.Filled.CalendarMonth
                            else -> Icons.Filled.NotificationsActive
                        }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (s) {
                            0 -> {
                                // عمداً دوزبانه؛ هر کس با هر زبانی راحت است سریع انتخاب کند
                                Text(
                                    "ساعت غروب‌کوک  ·  Sunset Clock",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    "زبان را انتخاب کنید  ·  Choose your language",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    LanguageButton("فارسی", "فا", langTag == "fa" || (langTag == "" && fa), Modifier.weight(1f)) { setLang("fa") }
                                    LanguageButton("English", "En", langTag == "en" || (langTag == "" && !fa), Modifier.weight(1f)) { setLang("en") }
                                }
                            }
                            1 -> {
                                Text(stringResource(R.string.onb_location_title), style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    stringResource(R.string.onb_location_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                BigChoice(
                                    Icons.Filled.LocationOn,
                                    stringResource(R.string.city),
                                    if (settings.useGps && settings.hasGpsFix) stringResource(R.string.approx_location)
                                    else Cities.byId(settings.cityId).let { if (fa) it.fa else it.en }
                                ) { dialog = "city" }
                                BigChoice(
                                    Icons.Filled.MyLocation,
                                    stringResource(R.string.use_gps),
                                    stringResource(R.string.onb_gps_btn)
                                ) {
                                    if (LocationHelper.hasPermission(context)) fetchLocation()
                                    else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            }
                            2 -> {
                                Text(stringResource(R.string.onb_method_title), style = MaterialTheme.typography.headlineMedium)
                                HijriAdjuster(settings.hijriAdjust, settings.place().zone, fa, vm)
                                BigChoice(
                                    Icons.Filled.CalendarMonth,
                                    stringResource(R.string.calc_method),
                                    stringResource(settings.method.labelRes())
                                ) { dialog = "method" }
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
            }

            if (step > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { step -= 1 }) { Text(stringResource(R.string.onb_back)) }
                    Spacer(Modifier.weight(1f))
                    // نقطه‌های پیشرفت
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (i in 0..LAST_STEP) {
                            Box(
                                Modifier
                                    .size(if (i == step) 22.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    GradientPill(
                        text = stringResource(if (step < LAST_STEP) R.string.onb_next else R.string.onb_finish),
                        onClick = { if (step < LAST_STEP) step += 1 else vm.completeOnboarding() }
                    )
                }
            }
        }
    }

    when (dialog) {
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

@Composable
private fun GradientPill(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .softShadow(RoundedCornerShape(50), 12.dp)
            .clip(RoundedCornerShape(50))
            .background(SunsetBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** نمایش تاریخ قمری امروز با دکمه‌های کم/زیاد تا با تقویم کاربر هماهنگ شود */
@Composable
private fun HijriAdjuster(adjust: Int, zone: java.time.ZoneId, fa: Boolean, vm: MainViewModel) {
    val today = remember(zone) { LocalDate.now(zone) }
    val hijriMonths = stringArrayResource(R.array.hijri_months)
    val jalaliMonths = stringArrayResource(R.array.jalali_months)
    val h = Calendars.toHijri(today, adjust)
    val j = Calendars.toJalali(today)
    SoftCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), brush = SunsetBrushSoft) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.onb_hijri_today),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                RoundActionButton(
                    Icons.Filled.Remove, stringResource(R.string.hijri_adjust),
                    onClick = { vm.setHijriAdjust(adjust - 1) }, size = 52.dp, filled = false
                )
                AnimatedContent(
                    targetState = h,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                    modifier = Modifier.weight(1f),
                    label = "hijri"
                ) { hh ->
                    Text(
                        if (hh != null) Fmt.digits("${hh.day} ${hijriMonths[hh.month - 1]} ${hh.year}", fa) else "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                RoundActionButton(
                    Icons.Filled.Add, stringResource(R.string.hijri_adjust),
                    onClick = { vm.setHijriAdjust(adjust + 1) }, size = 52.dp, filled = false
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                Fmt.digits("${j.day} ${jalaliMonths[j.month - 1]} ${j.year}", fa),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onb_hijri_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
