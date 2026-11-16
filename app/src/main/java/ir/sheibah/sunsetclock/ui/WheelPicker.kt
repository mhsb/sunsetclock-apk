package ir.sheibah.sunsetclock.ui

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

private const val LOOP_REPEAT = 400

/**
 * چرخنده‌ی عمودی به سبک ساعت شیائومی: عدد وسط درشت و پررنگ، بقیه کم‌رنگ و کوچک‌تر.
 * loop=true یعنی چرخش بی‌پایان (۵۹ بعد از ۰۰). آیتم‌ها تنبل ساخته می‌شوند؛ پس حتی با هزار مورد هم سریع است.
 */
@Composable
fun WheelPicker(
    count: Int,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 64.dp,
    visibleItems: Int = 5,
    loop: Boolean = true,
    item: @Composable (index: Int, selected: Boolean) -> Unit
) {
    val half = visibleItems / 2
    val safeCount = count.coerceAtLeast(1)
    val total = if (loop) safeCount * LOOP_REPEAT else safeCount
    val sel = selectedIndex.coerceIn(0, safeCount - 1)
    val startItem = if (loop) (LOOP_REPEAT / 2) * safeCount + sel else sel
    val firstIndexFor: (Int) -> Int = { target -> if (loop) target - half else target }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = firstIndexFor(startItem).coerceAtLeast(0))
    val fling = rememberSnapFlingBehavior(state, SnapPosition.Center)
    val haptic = LocalHapticFeedback.current
    val currentOnSelected by rememberUpdatedState(onSelected)
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }

    // نزدیک‌ترین آیتم به مرکز
    fun centerItem(): Int {
        val info = state.layoutInfo
        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        var best = -1
        var bestDist = Float.MAX_VALUE
        for (v in info.visibleItemsInfo) {
            val d = abs(v.offset + v.size / 2f - center)
            if (d < bestDist) {
                bestDist = d
                best = v.index
            }
        }
        return best
    }

    // لرزش خفیف هنگام گذر از هر عدد
    LaunchedEffect(state) {
        snapshotFlow { centerItem() }.distinctUntilChanged().collect {
            if (state.isScrollInProgress) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    // گزارش مقدار بعد از ایستادن چرخنده
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().collect { scrolling ->
            if (!scrolling) {
                val c = centerItem()
                if (c >= 0) currentOnSelected(c % safeCount)
            }
        }
    }
    // همگام‌سازی وقتی مقدار از بیرون عوض شد (مثلاً انتخاب پیش‌فرض‌ها)
    LaunchedEffect(sel, safeCount) {
        if (state.isScrollInProgress) return@LaunchedEffect
        val c = centerItem()
        if (c < 0 || c % safeCount == sel) return@LaunchedEffect
        val diff = ((sel - c % safeCount) + safeCount) % safeCount
        val delta = if (loop && diff > safeCount / 2) diff - safeCount else diff
        val target = if (loop) c + delta else sel
        state.animateScrollToItem(firstIndexFor(target).coerceIn(0, (total - 1).coerceAtLeast(0)))
    }

    Box(modifier = modifier.height(itemHeight * visibleItems)) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = if (loop) PaddingValues(0.dp) else PaddingValues(vertical = itemHeight * half),
            modifier = Modifier.fillMaxWidth().height(itemHeight * visibleItems)
        ) {
            items(total) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            val info = state.layoutInfo
                            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val v = info.visibleItemsInfo.firstOrNull { it.index == i }
                            val dist = if (v == null) 3f else abs(v.offset + v.size / 2f - center) / itemPx
                            val f = (dist / 2f).coerceIn(0f, 1f)
                            alpha = 1f - 0.82f * f
                            val s = 1f - 0.22f * f
                            scaleX = s
                            scaleY = s
                        },
                    contentAlignment = Alignment.Center
                ) {
                    item(i % safeCount, i % safeCount == sel)
                }
            }
        }
    }
}

/** متن استاندارد چرخنده: درشت و سبک؛ عدد انتخاب‌شده پررنگ‌تر */
@Composable
fun WheelLabel(text: String, selected: Boolean, fontSize: TextUnit = 40.sp) {
    Text(
        text,
        fontSize = fontSize,
        fontWeight = if (selected) FontWeight.Normal else FontWeight.Light,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false
    )
}
