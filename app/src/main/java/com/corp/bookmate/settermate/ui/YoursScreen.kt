package com.corp.bookmate.settermate.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.corp.bookmate.settermate.R

@Composable
fun YoursScreen(
    modifier: Modifier = Modifier,
    viewModel: YoursViewModel = hiltViewModel(),
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Coming soon",
            color = colorResource(R.color.WhiteSmoke),
        )
    }
}
