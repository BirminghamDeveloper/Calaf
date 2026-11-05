package com.calaf.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.calaf.core.designsystem.CalafSpacing

/** The common shape of a tab or settings page: action banner, a title and a scrolling list. */
@Composable
fun CalafListScreen(
    title: String,
    action: ActionState,
    listTag: String,
    working: Boolean = false,
    onRetry: (() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ActionBanner(action, working, onRetry)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = CalafSpacing.medium).testTag(listTag),
            verticalArrangement = Arrangement.spacedBy(CalafSpacing.medium),
        ) {
            item {
                Text(
                    title, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = CalafSpacing.medium),
                )
            }
            content()
            item { Spacer(Modifier.height(CalafSpacing.large)) }
        }
    }
}
