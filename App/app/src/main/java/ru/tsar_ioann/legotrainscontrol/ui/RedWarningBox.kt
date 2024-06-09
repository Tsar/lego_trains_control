package ru.tsar_ioann.legotrainscontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RedWarningBox(text: String, onClick: () -> Unit) {
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .background(Color.Red, shape = RoundedCornerShape(7.dp))
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onClick() }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning sign",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRedWarningBox() {
    RedWarningBox(text = "Bluetooth is not enabled") {}
}
