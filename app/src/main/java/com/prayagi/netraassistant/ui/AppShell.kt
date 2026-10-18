package com.prayagi.netraassistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayagi.netraassistant.BuildConfigInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Family UI standard: fixed small header (name, version, date/time), scrollable body, fixed footer. */
@Composable
fun AppShell() {
    var tab by remember { mutableStateOf(0) }
    val titles = listOf("Chat", "Permissions", "Privacy")
    Scaffold(
        topBar = {
            Column {
                Header()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    titles.forEachIndexed { i, t ->
                        TextButton(onClick = { tab = i }) {
                            Text(if (i == tab) "[ $t ]" else t, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        bottomBar = { Footer() }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                0 -> ChatScreen()
                1 -> PermissionsScreen()
                else -> PrivacyScreen()
            }
        }
    }
}

@Composable
private fun Header() {
    val now = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Netravan", fontSize = 13.sp)
            Text("v${BuildConfigInfo.VERSION} | Netra Ecosystem", fontSize = 11.sp)
            Text(now, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Footer() {
    Surface(tonalElevation = 3.dp) {
        Text(
            "On-device by default. Online voice only if you allow it.",
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        )
    }
}
