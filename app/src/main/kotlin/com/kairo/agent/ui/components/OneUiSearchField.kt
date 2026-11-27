package com.kairo.agent.ui.components

import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.SearchView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.kairo.agent.R

/** Used only inside the selected One UI theme's conversation drawer. */
@Composable
fun OneUiSearchField(value: String, onValueChange: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    val currentCallback by rememberUpdatedState(onValueChange)
    AndroidView(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        factory = { context ->
            SearchView(ContextThemeWrapper(context, R.style.Theme_Kairo_Sesl9)).apply {
                setIconifiedByDefault(false)
                isIconified = false
                maxWidth = Int.MAX_VALUE
                findViewById<TextView>(androidx.appcompat.R.id.search_src_text)?.typeface =
                    ResourcesCompat.getFont(context, R.font.one_ui_sans)
            }
        },
        update = { view ->
            view.setOnQueryTextListener(null)
            view.queryHint = hint
            if (view.query.toString() != value) view.setQuery(value, false)
            view.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(text: String): Boolean { currentCallback(text); return true }
                override fun onQueryTextSubmit(query: String): Boolean { view.clearFocus(); return true }
            })
        },
        onRelease = { it.setOnQueryTextListener(null) }
    )
}
