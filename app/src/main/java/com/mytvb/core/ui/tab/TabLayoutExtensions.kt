package com.mytvb.core.ui.tab

import com.mytvb.core.ui.focus.SpatialFocusNavigator
import com.mytvb.core.common.log.AppLog

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

private const val TAB_SWITCH_BASE_DURATION_MS = 60
private const val TAB_SWITCH_STEP_DURATION_MS = 20
private const val TAB_SWITCH_MAX_DURATION_MS = 60
private const val TAB_DIRECT_SWITCH_OUT_DURATION_MS = 120L
private const val TAB_DIRECT_SWITCH_IN_DURATION_MS = 150L

fun TabLayout.enableTouchNavigation(
    viewPager: ViewPager2,
    tabFocusable: Boolean = true,
    smoothScrollOnSelection: Boolean = true,
    onNavigateDown: (() -> Boolean)? = null,
    onNavigateLeft: (() -> Boolean)? = null,
    onNavigateRight: (() -> Boolean)? = null,
    onTabReselected: ((index: Int) -> Unit)? = null
) {
    viewPager.isUserInputEnabled = true
    post {
        val tabStrip = getChildAt(0) as? ViewGroup ?: return@post
        for (index in 0 until tabStrip.childCount) {
            val tabView = tabStrip.getChildAt(index)
            tabView.isClickable = true
            tabView.isFocusable = tabFocusable
            bindTabTouchTextColor(tabView)
            if (tabFocusable) {
                bindTabFocusTextColor(tabView)
                tabView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                if (viewPager.currentItem != index) {
                                    viewPager.setCurrentItemFromTab(
                                        item = index,
                                        smoothScroll = smoothScrollOnSelection
                                    )
                                } else {
                                    onTabReselected?.invoke(index)
                                }
                                keepSelectedTabFocused(tabStrip, index, viewPager)
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                val result = if (onNavigateDown != null) {
                                    onNavigateDown.invoke()
                                } else {
                                    focusViewPagerContent(viewPager)
                                }
                                AppLog.d("TabNav", "DPAD_DOWN tab[$index] result=$result")
                                result
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (index == 0) {
                                    consumeEdgeNavigation { onNavigateLeft?.invoke() }
                                } else {
                                    false
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (index == tabStrip.childCount - 1) {
                                    consumeEdgeNavigation { onNavigateRight?.invoke() }
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
            }
            tabView.setOnClickListener {
                if (viewPager.currentItem != index) {
                    viewPager.setCurrentItemFromTab(
                        item = index,
                        smoothScroll = smoothScrollOnSelection
                    )
                } else {
                    onTabReselected?.invoke(index)
                }
                keepSelectedTabFocused(tabStrip, index, viewPager)
            }
        }
        reapplyTextColorOnSelectionChange()
    }
}

fun TabLayout.enableTouchNavigation(
    viewPager: ViewPager,
    tabFocusable: Boolean = true,
    smoothScrollOnSelection: Boolean = true,
    onNavigateDown: (() -> Boolean)? = null,
    onNavigateLeft: (() -> Boolean)? = null,
    onNavigateRight: (() -> Boolean)? = null
) {
    post {
        val tabStrip = getChildAt(0) as? ViewGroup ?: return@post
        for (index in 0 until tabStrip.childCount) {
            val tabView = tabStrip.getChildAt(index)
            tabView.isClickable = true
            tabView.isFocusable = tabFocusable
            bindTabTouchTextColor(tabView)
            if (tabFocusable) {
                bindTabFocusTextColor(tabView)
                tabView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                if (viewPager.currentItem != index) {
                                    viewPager.setCurrentItem(index, smoothScrollOnSelection)
                                } else {
                                    getTabAt(index)?.select()
                                }
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                val result = if (onNavigateDown != null) {
                                    onNavigateDown.invoke()
                                } else {
                                    focusViewPagerContent(viewPager)
                                }
                                AppLog.d("TabNav", "DPAD_DOWN tab[$index] result=$result")
                                result
                            }
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                if (index == 0) {
                                    consumeEdgeNavigation { onNavigateLeft?.invoke() }
                                } else {
                                    false
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                if (index == tabStrip.childCount - 1) {
                                    consumeEdgeNavigation { onNavigateRight?.invoke() }
                                } else {
                                    false
                                }
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
            }
            tabView.setOnClickListener {
                if (viewPager.currentItem != index) {
                    viewPager.setCurrentItem(index, smoothScrollOnSelection)
                } else {
                    getTabAt(index)?.select()
                }
            }
        }
        reapplyTextColorOnSelectionChange()
    }
}

fun TabLayout.focusSelectedTab(): Boolean {
    val selectedIndex = selectedTabPosition
    if (selectedIndex < 0) {
        return false
    }
    val tabStrip = getChildAt(0) as? ViewGroup ?: return false
    return tabStrip.getChildAt(selectedIndex)?.requestFocus() == true
}

fun TabLayout.focusNearestTabTo(anchorView: View?): Boolean {
    val tabStrip = getChildAt(0) as? ViewGroup ?: return false
    val candidates = (0 until tabStrip.childCount)
        .mapNotNull(tabStrip::getChildAt)
        .filter { it.visibility == View.VISIBLE && it.isFocusable }
    if (candidates.isEmpty()) {
        return false
    }
    return SpatialFocusNavigator.requestBestCandidate(
        anchorView = anchorView,
        candidates = candidates,
        direction = View.FOCUS_UP,
        fallback = {
            focusSelectedTab() || candidates.firstOrNull()?.requestFocus() == true
        }
    )
}

/**
 * 首/末 tab 的越界方向键一律消费：回调自行决定去向（如聚焦右侧排序按钮/左侧功能栏），
 * 未接管则焦点停在原 tab。此前未消费会放行 FocusFinder 几何搜索，把焦点送到
 * 下方列表卡片等不可预期的位置。
 */
private fun consumeEdgeNavigation(action: () -> Unit): Boolean {
    action()
    return true
}

/**
 * 焦点移入 tab 时文字提亮为选中色（白），避免默认灰字与焦点高亮色块融在一起难以辨认；
 * 失焦后按 tab 自身选中状态恢复（选中→选中色，未选中→默认灰）。
 * 颜色全部取自 tabTextColors（含 tabSelectedTextColor 合入后的 selected 态），不硬编码。
 */
private fun TabLayout.bindTabFocusTextColor(tabView: View) {
    tabView.setOnFocusChangeListener { _, hasFocus ->
        applyTabTextColor(tabView, hasFocus)
    }
    if (tabView.hasFocus()) {
        applyTabTextColor(tabView, true)
    }
}

/**
 * 触摸按压 tab 时同样提亮文字，避免灰字与按压高亮色块融在一起；
 * 抬起/取消后 post 恢复——此时 click 选中切换已完成，按最新焦点/选中状态取色。
 */
private fun TabLayout.bindTabTouchTextColor(tabView: View) {
    tabView.setOnTouchListener { _, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> applyTabTextColor(tabView, true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                post { applyTabTextColor(tabView, tabView.hasFocus()) }
        }
        false
    }
}

/**
 * tab 选中被程序切换（非焦点触发，如 ViewPager 滑动/setCurrentItem）时，
 * TabLayout 会按选中态重刷文字颜色，焦点 tab 的提亮可能被冲掉；选中变化后再校正一次。
 */
private fun TabLayout.reapplyTextColorOnSelectionChange() {
    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            post { refreshFocusedTabTextColor() }
        }

        override fun onTabUnselected(tab: TabLayout.Tab) = Unit
        override fun onTabReselected(tab: TabLayout.Tab) = Unit
    })
}

private fun TabLayout.refreshFocusedTabTextColor() {
    val tabStrip = getChildAt(0) as? ViewGroup ?: return
    for (index in 0 until tabStrip.childCount) {
        val tabView = tabStrip.getChildAt(index) ?: continue
        if (tabView.hasFocus()) {
            applyTabTextColor(tabView, true)
        }
    }
}

private fun TabLayout.applyTabTextColor(tabView: View, hasFocus: Boolean) {
    val textView = findFirstTextView(tabView)
    val colors = tabTextColors
    if (textView == null || colors == null) {
        return
    }
    val selectedColor = colors.getColorForState(
        intArrayOf(android.R.attr.state_selected),
        colors.defaultColor
    )
    textView.setTextColor(if (hasFocus || tabView.isSelected) selectedColor else colors.defaultColor)
}

private fun findFirstTextView(view: View): TextView? {
    if (view is TextView) {
        return view
    }
    if (view is ViewGroup) {
        for (index in 0 until view.childCount) {
            findFirstTextView(view.getChildAt(index))?.let { return it }
        }
    }
    return null
}

private fun TabLayout.keepSelectedTabFocused(
    tabStrip: ViewGroup,
    index: Int,
    viewPager: ViewPager2
) {
    val target = tabStrip.getChildAt(index) ?: return
    if (!target.isShown || !target.isFocusable) {
        return
    }
    target.requestFocus()
    post {
        requestSelectedTabFocusIfNeeded(tabStrip, index, viewPager)
    }
    postDelayed({
        requestSelectedTabFocusIfNeeded(tabStrip, index, viewPager)
    }, TAB_SWITCH_MAX_DURATION_MS + 32L)
}

private fun TabLayout.requestSelectedTabFocusIfNeeded(
    tabStrip: ViewGroup,
    index: Int,
    viewPager: ViewPager2
) {
    if (selectedTabPosition != index) {
        return
    }
    val target = tabStrip.getChildAt(index) ?: return
    if (!target.isShown || !target.isFocusable || target.hasFocus()) {
        return
    }
    val focused = rootView?.findFocus()
    if (
        focused == null ||
        !focused.isAttachedToWindow ||
        !focused.isShown ||
        isDescendantOf(focused, tabStrip) ||
        isDescendantOf(focused, viewPager)
    ) {
        target.requestFocus()
    }
}

private fun focusViewPagerContent(viewPager: ViewPager2): Boolean {
    val pagerRecycler = viewPager.getChildAt(0) as? RecyclerView ?: return false
    val currentPage = pagerRecycler.layoutManager?.findViewByPosition(viewPager.currentItem)
        ?: return tryFocusFirstFocusableDescendant(pagerRecycler) || pagerRecycler.requestFocus()
    return tryFocusFirstFocusableDescendant(currentPage) || currentPage.requestFocus()
}

private fun focusViewPagerContent(viewPager: ViewPager): Boolean {
    val scrollAnchor = viewPager.scrollX + viewPager.paddingLeft
    val currentView = (0 until viewPager.childCount)
        .map(viewPager::getChildAt)
        .firstOrNull { child ->
            child.visibility == View.VISIBLE &&
                child.width > 0 &&
                child.left <= scrollAnchor &&
                child.right >= scrollAnchor
        }
        ?: (0 until viewPager.childCount)
            .map(viewPager::getChildAt)
            .filter { it.visibility == View.VISIBLE && it.width > 0 }
            .minByOrNull { kotlin.math.abs(it.left - scrollAnchor) }
        ?: return tryFocusFirstFocusableDescendant(viewPager) || viewPager.requestFocus()
    return tryFocusFirstFocusableDescendant(currentView) || currentView.requestFocus()
}

private fun tryFocusFirstFocusableDescendant(view: View): Boolean {
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            if (child.isFocusable && child.visibility == View.VISIBLE) {
                if (child.requestFocus()) {
                    return true
                }
            }
            if (child is ViewGroup && child.visibility == View.VISIBLE) {
                if (tryFocusFirstFocusableDescendant(child)) {
                    return true
                }
            }
        }
    }
    return false
}

private fun isDescendantOf(view: View?, ancestor: View?): Boolean {
    var current = view
    while (current != null) {
        if (current === ancestor) {
            return true
        }
        current = current.parent as? View
    }
    return false
}

private fun ViewPager2.setCurrentItemFromTab(
    item: Int,
    smoothScroll: Boolean
) {
    val recyclerView = getChildAt(0) as? RecyclerView
    val adapter = adapter
    if (!smoothScroll || recyclerView == null || adapter == null) {
        setCurrentItem(item, smoothScroll)
        return
    }
    if (adapter.itemCount <= 0) {
        return
    }

    val targetItem = item.coerceIn(0, adapter.itemCount - 1)
    val pageDelta = targetItem - currentItem
    if (pageDelta == 0) {
        return
    }
    if (kotlin.math.abs(pageDelta) > 1) {
        animateDirectTabSwitch(targetItem, pageDelta)
        return
    }

    val pageSize = when (orientation) {
        ViewPager2.ORIENTATION_VERTICAL -> height - paddingTop - paddingBottom
        else -> width - paddingLeft - paddingRight
    }
    if (pageSize <= 0 || scrollState != ViewPager2.SCROLL_STATE_IDLE) {
        setCurrentItem(targetItem, true)
        return
    }

    val distance = pageSize * pageDelta * horizontalDirectionMultiplier()
    recyclerView.stopScroll()
    recyclerView.smoothScrollBy(
        if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) distance else 0,
        if (orientation == ViewPager2.ORIENTATION_VERTICAL) distance else 0,
        DecelerateInterpolator(1.8f),
        calculateTabSwitchDuration(kotlin.math.abs(pageDelta))
    )
}

private fun ViewPager2.horizontalDirectionMultiplier(): Int {
    if (orientation != ViewPager2.ORIENTATION_HORIZONTAL) {
        return 1
    }
    return if (layoutDirection == View.LAYOUT_DIRECTION_RTL) -1 else 1
}

private fun calculateTabSwitchDuration(pageDistance: Int): Int {
    val duration = TAB_SWITCH_BASE_DURATION_MS +
        (pageDistance - 1).coerceAtLeast(0) * TAB_SWITCH_STEP_DURATION_MS
    return duration.coerceAtMost(TAB_SWITCH_MAX_DURATION_MS)
}

private fun ViewPager2.animateDirectTabSwitch(targetItem: Int, pageDelta: Int) {
    val pageSize = when (orientation) {
        ViewPager2.ORIENTATION_VERTICAL -> height - paddingTop - paddingBottom
        else -> width - paddingLeft - paddingRight
    }
    if (pageSize <= 0) {
        setCurrentItem(targetItem, false)
        return
    }

    animate().cancel()
    val direction = pageDelta.signForPager(this)
    val slideDistance = pageSize.toFloat()

    setCurrentItem(targetItem, false)

    val startTranslationX = if (orientation == ViewPager2.ORIENTATION_HORIZONTAL) slideDistance * direction else 0f
    val startTranslationY = if (orientation == ViewPager2.ORIENTATION_VERTICAL) slideDistance * direction else 0f
    translationX = startTranslationX
    translationY = startTranslationY

    animate()
        .setDuration(TAB_DIRECT_SWITCH_OUT_DURATION_MS + TAB_DIRECT_SWITCH_IN_DURATION_MS)
        .translationX(0f)
        .translationY(0f)
        .start()
}

private fun Int.signForPager(viewPager: ViewPager2): Float {
    val sign = if (this >= 0) 1f else -1f
    return if (
        viewPager.orientation == ViewPager2.ORIENTATION_HORIZONTAL &&
        viewPager.layoutDirection == View.LAYOUT_DIRECTION_RTL
    ) {
        -sign
    } else {
        sign
    }
}
