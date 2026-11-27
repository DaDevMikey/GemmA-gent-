package com.kairo.agent.ui.components

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import androidx.appcompat.widget.SeslProgressBar

/** Adapted from Twidget's LoadingSpinner: prevent an AVD restart after hiding or detaching. */
internal class OneUiLoadingSpinner(context: Context) : SeslProgressBar(context) {
    override fun onVisibilityAggregated(isVisible: Boolean) {
        if (!isVisible) clearLoopCallback()
        super.onVisibilityAggregated(isVisible)
    }

    override fun onDetachedFromWindow() {
        clearLoopCallback()
        super.onDetachedFromWindow()
    }

    private fun clearLoopCallback() {
        (indeterminateDrawable as? AnimatedVectorDrawable)?.clearAnimationCallbacks()
    }
}
