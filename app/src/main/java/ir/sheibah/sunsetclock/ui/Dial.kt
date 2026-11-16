package ir.sheibah.sunsetclock.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.sheibah.sunsetclock.core.ClockSnapshot
import kotlin.math.cos
import kotlin.math.sin

/**
 * صفحه‌ی ساعت غروب‌کوک: یک دور کامل = از غروب تا غروب بعدی؛ بالای صفحه = ساعت ۰ (غروب).
 * کمان تیره = شب (غروب تا طلوع)، کمان کهربایی = روز (طلوع تا غروب).
 */
@Composable
fun SunsetDial(
    snap: ClockSnapshot,
    fa: Boolean,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit
) {
    val nightColor = NightColor
    val dayColor = DayColor
    val tickColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sunColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurface
    val labelArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    val cycle = (snap.nextSunset - snap.lastSunset).toFloat()
    val nowFrac = ((snap.now - snap.lastSunset).toFloat() / cycle).coerceIn(0f, 1f)
    val sunriseFrac = ((snap.nextSunrise - snap.lastSunset).toFloat() / cycle).coerceIn(0f, 1f)

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.075f
            val radius = size.minDimension / 2f - stroke / 2f - 2.dp.toPx()
            val c = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(c.x - radius, c.y - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // شب و روز
            drawArc(
                color = nightColor,
                startAngle = -90f,
                sweepAngle = sunriseFrac * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            drawArc(
                color = dayColor,
                startAngle = -90f + sunriseFrac * 360f,
                sweepAngle = (1f - sunriseFrac) * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke)
            )
            // نوار نازک سپری‌شده
            drawArc(
                color = Color.White.copy(alpha = 0.45f),
                startAngle = -90f,
                sweepAngle = nowFrac * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke * 0.22f, cap = StrokeCap.Round)
            )

            // خط‌های ساعت (۲۴ تا)
            val tickOuter = radius - stroke / 2f - 4.dp.toPx()
            for (h in 0 until 24) {
                val major = h % 6 == 0
                val a = Math.toRadians(h / 24.0 * 360.0 - 90.0)
                val len = if (major) 11.dp.toPx() else 5.dp.toPx()
                val x1 = c.x + (tickOuter * cos(a)).toFloat()
                val y1 = c.y + (tickOuter * sin(a)).toFloat()
                val x2 = c.x + ((tickOuter - len) * cos(a)).toFloat()
                val y2 = c.y + ((tickOuter - len) * sin(a)).toFloat()
                drawLine(
                    color = tickColor.copy(alpha = if (major) 0.9f else 0.45f),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = if (major) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // اعداد ۰ و ۶ و ۱۲ و ۱۸
            paint.textSize = 14.sp.toPx()
            paint.color = labelArgb
            val numR = tickOuter - 11.dp.toPx() - 14.dp.toPx()
            for (h in intArrayOf(0, 6, 12, 18)) {
                val a = Math.toRadians(h / 24.0 * 360.0 - 90.0)
                val x = c.x + (numR * cos(a)).toFloat()
                val y = c.y + (numR * sin(a)).toFloat()
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(Fmt.digits(h.toString(), fa), x, y + paint.textSize / 3f, paint)
                }
            }

            // نقطه‌ی رویدادها (اذان‌ها و ...)
            for (ev in snap.cycleEvents) {
                val f = ((ev.time - snap.lastSunset).toFloat() / cycle).coerceIn(0f, 1f)
                val a = Math.toRadians(f * 360.0 - 90.0)
                val p = Offset(c.x + (radius * cos(a)).toFloat(), c.y + (radius * sin(a)).toFloat())
                drawCircle(color = dotColor.copy(alpha = 0.9f), radius = stroke * 0.2f, center = p)
            }

            // خورشید (لحظه‌ی اکنون)
            val a = Math.toRadians(nowFrac * 360.0 - 90.0)
            val sunPos = Offset(c.x + (radius * cos(a)).toFloat(), c.y + (radius * sin(a)).toFloat())
            drawCircle(color = sunColor.copy(alpha = 0.30f), radius = stroke * 1.0f, center = sunPos)
            drawCircle(color = Color.White, radius = stroke * 0.62f, center = sunPos)
            drawCircle(color = sunColor, radius = stroke * 0.5f, center = sunPos)
        }
        center()
    }
}
