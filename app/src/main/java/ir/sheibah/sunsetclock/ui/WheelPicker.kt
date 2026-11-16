package ir.sheibah.sunsetclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * چرخنده‌ی عمودی (شبیه ساعت آیفون/شیائومی).
 * آیتم وسط انتخاب‌شده است؛ بعد از رها کردن، روی نزدیک‌ترین آیتم می‌نشیند و onSelected صدا زده می‌شود.
 */
@Composable
fun WheelPicker(
    count: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
    visibleItems: Int = 3,
    item: @Composable (index: Int) -> Unit
) {
    val currentOnSelected by rememberUpdatedState(onSelected)
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }
    val safeSelected = selectedIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    val scrollState = rememberScrollState(initial = (safeSelected * itemPx).roundToInt())

    // اندیس آیتمِ وسط؛ فقط وقتی عوض شود بازترسیم می‌شود
    val currentIndex by remember(count, itemPx) {
        derivedStateOf { (scrollState.value / itemPx).roundToInt().coerceIn(0, (count - 1).coerceAtLeast(0)) }
    }

    LaunchedEffect(scrollState, count, itemPx) {
        var lastReported = -1
        snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) {
                val idx = (scrollState.value / itemPx).roundToInt().coerceIn(0, (count - 1).coerceAtLeast(0))
                val target = (idx * itemPx).roundToInt()
                if (scrollState.value != target) scrollState.animateScrollTo(target)
                if (idx != lastReported) {
                    lastReported = idx
                    currentOnSelected(idx)
                }
            }
        }
    }

    val padding = itemHeight * (visibleItems / 2)

    Box(
        modifier = modifier.height(itemHeight * visibleItems),
        contentAlignment = Alignment.Center
    ) {
        // نوار پس‌زمینه‌ی آیتم انتخاب‌شده
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(modifier = Modifier.verticalScroll(scrollState)) {
            Spacer(Modifier.height(padding))
            for (i in 0 until count) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(if (i == currentIndex) 1f else 0.45f),
                    contentAlignment = Alignment.Center
                ) {
                    item(i)
                }
            }
            Spacer(Modifier.height(padding))
        }
    }
}
