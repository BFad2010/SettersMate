package com.corp.bookmate.settermate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.painterResource
import settermate.shared.generated.resources.Res
import settermate.shared.generated.resources.down_arrow
import settermate.shared.generated.resources.up_arrow

@Composable
fun DropDownList(
    dropDownTitle: String,
    listOptions: List<String>,
    onSortItemClick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    AnimatedVisibility(visible = expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .heightIn(0.dp, 300.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(8.dp),
                    ),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(listOptions) { option ->
                    DropDownItem(sortTitle = option) {
                        expanded = false
                        onSortItemClick(option)
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .border(1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(50.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = dropDownTitle,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
        )
        Column(modifier = Modifier.size(20.dp)) {
            AnimatedVisibility(visible = expanded, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.up_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            AnimatedVisibility(visible = !expanded, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(Res.drawable.down_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun DropDownItem(sortTitle: String, onSortItemClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onSortItemClick(sortTitle) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = sortTitle,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.background,
            fontSize = 18.sp,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
        )
    }
}
