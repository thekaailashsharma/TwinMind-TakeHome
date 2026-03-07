package com.takehome.twinmind.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.takehome.twinmind.core.designsystem.theme.TwinMindDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TmModalBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null,
        content = content,
    )
}

@Composable
fun TmBottomSheetHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<String> = emptyList(),
    selectedTabIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = TmIcons.Close,
                contentDescription = "Close",
                tint = TwinMindDarkNavy,
            )
        }

        if (tabs.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            TmPillTabRow(
                tabs = tabs,
                selectedIndex = selectedTabIndex,
                onTabSelected = onTabSelected,
            )
        }
    }
}
