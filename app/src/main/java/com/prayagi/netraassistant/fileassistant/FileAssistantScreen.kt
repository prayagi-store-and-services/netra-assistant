package com.prayagi.netraassistant.fileassistant

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.genai.common.FeatureStatus
import kotlinx.coroutines.launch

@Composable
fun FileAssistantScreen() {
    val context = LocalContext.current
    val store = remember { DocumentStore(context.applicationContext) }
    val nano = remember { NanoAnswerer() }
    val engine = remember { AnswerEngine(store, nano) }
    val scope = rememberCoroutineScope()
    var docs by remember { mutableStateOf(store.documents()) }
    var question by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<AnswerEngine.Answer?>(null) }
    var aiStatus by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { aiStatus = nano.status() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris?.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (t: Throwable) {
                // Some providers do not offer persistable permission; the copy is still indexed now.
            }
            val pages = DocumentImporter.import(context, store, uri)
            if (pages == 0) notice = "Kuch files mein text nahin mila (scanned PDF baad mein)."
        }
        docs = store.documents()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("File Assistant", fontSize = 16.sp)
        Text("Apni TXT aur PDF files chuno. Sab kuch phone par hi hota hai - kuch bhi upload nahin hota.", fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { picker.launch(arrayOf("text/*", "application/pdf")) }) { Text("Add files") }
            if (docs.isNotEmpty()) Text("Indexed files: ${docs.size}", fontSize = 13.sp)
        }
        if (notice.isNotEmpty()) Text(notice, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        docs.forEach { d ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(d.name, Modifier.weight(1f), maxLines = 1, fontSize = 14.sp)
                TextButton(onClick = { store.removeDocument(d.uri); docs = store.documents() }) { Text("Remove") }
            }
            HorizontalDivider()
        }
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Sawaal likho") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                scope.launch {
                    busy = true
                    answer = engine.answer(question, false)
                    busy = false
                }
            }) { Text("Search") }
            TextButton(
                onClick = {
                    scope.launch {
                        busy = true
                        answer = engine.answer(question, true)
                        busy = false
                    }
                },
                enabled = aiStatus == FeatureStatus.AVAILABLE
            ) { Text("Ask AI") }
            if (busy) Text("...", fontSize = 13.sp)
        }
        when (aiStatus) {
            FeatureStatus.AVAILABLE -> Text("On-device AI available", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            FeatureStatus.DOWNLOADABLE -> {
                TextButton(onClick = {
                    scope.launch {
                        busy = true
                        nano.downloadModel()
                        aiStatus = nano.status()
                        busy = false
                    }
                }) { Text("Download on-device AI (sirf aapke tap par)") }
            }
            FeatureStatus.DOWNLOADING -> Text("On-device AI download ho raha hai", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            null -> {}
            else -> Text("AI Unavailable on this phone", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        answer?.let { a ->
            HorizontalDivider()
            Text(if (a.aiUsed) "AI jawab:" else "Mile hue hisse:", fontSize = 13.sp)
            Text(a.text, fontSize = 14.sp)
            if (a.citations.isNotEmpty()) {
                Text("Sources:", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                a.citations.forEach { c ->
                    Text("- " + c.docName + ", page " + c.page, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
