package ru.tsar_ioann.legotrainscontrol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun LegoTrainsControlTheme(
    content: @Composable () -> Unit
) {
    dynamicDarkColorScheme(LocalContext.current)

    MaterialTheme(
        colorScheme = darkColorScheme(),
        typography = Typography,
        content = content
    )
}
