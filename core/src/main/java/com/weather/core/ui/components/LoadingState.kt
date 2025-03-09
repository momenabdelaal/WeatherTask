package com.weather.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * A reusable loading state component that displays a centered CircularProgressIndicator.
 * 
 * @param modifier Modifier to be applied to the loading state container
 * @param fillMaxSize Whether the loading state should fill the maximum available size
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = true
) {
    // Check if we're in preview mode to avoid crashes
    val isPreview = LocalInspectionMode.current
    
    Box(
        modifier = if (fillMaxSize) modifier.fillMaxSize() else modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
        )
    }
}
