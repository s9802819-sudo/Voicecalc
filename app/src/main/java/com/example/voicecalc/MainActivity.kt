package com.example.voicecalc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import java.util.Locale

class CalcViewModel : ViewModel() {
    var expr by mutableStateOf("")
    var result by mutableStateOf("")
    var heard by mutableStateOf("")
    var failed by mutableStateOf(false)
    var lang by mutableStateOf("ar")
    var speakResult by mutableStateOf(true)
    var listening by mutableStateOf(false)

    fun press(s: String) { expr += s; result = ""; failed = false }
    fun back() { expr = expr.dropLast(1); result = ""; failed = false }
    fun clear() { expr = ""; result = ""; heard = ""; failed = false }
    fun compute() {
        runCatching { Evaluator.eval(expr) }
            .onSuccess { result = it; failed = false }
            .onFailure { result = ""; failed = expr.isNotBlank() }
    }
    fun applySpeech(text: String) { heard = text; expr = SpeechParser.toExpression(text); compute() }
}

class MainActivity : ComponentActivity() {
    private val vm: CalcViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private lateinit var voice: VoiceInput

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) {}
        voice = VoiceInput(this,
            onPartial = { vm.heard = it },
            onFinal = { vm.applySpeech(it); if (vm.speakResult && !vm.failed && vm.result.isNotEmpty()) speak(vm.result) },
            onEnd = { vm.listening = false })
        setContent {
            val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) listen() }
            App(vm) {
                if (vm.listening) voice.stop()
                else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
                else ask.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun listen() { vm.heard = ""; vm.listening = true; voice.start(if (vm.lang == "ar") "ar-SA" else "en-US") }
    private fun speak(s: String) { tts?.language = Locale(vm.lang); tts?.speak(s, TextToSpeech.QUEUE_FLUSH, null, "r") }
    override fun onDestroy() { voice.destroy(); tts?.shutdown(); super.onDestroy() }
}

@Composable
fun App(vm: CalcViewModel, onMic: () -> Unit) {
    val ctx = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val ar = vm.lang == "ar"
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalLayoutDirection provides if (ar) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.systemBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { vm.lang = if (ar) "en" else "ar" }) { Text(if (ar) "English" else "العربية") }
                        Spacer(Modifier.weight(1f))
                        Text(if (ar) "نطق النتيجة" else "Speak result", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(8.dp))
                        Switch(vm.speakResult, { vm.speakResult = it })
                    }
                    Text(
                        if (vm.listening) (if (ar) "أستمع…  " else "Listening…  ") + vm.heard else vm.heard,
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(Modifier.fillMaxWidth().weight(0.8f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
                            Text(vm.expr, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                            Text(
                                if (vm.failed) (if (ar) "خطأ" else "Error") else vm.result,
                                style = MaterialTheme.typography.displayMedium,
                                color = if (vm.failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                        }
                        Keypad(vm, onMic, ar, Modifier.weight(2.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun Keypad(vm: CalcViewModel, onMic: () -> Unit, ar: Boolean, modifier: Modifier) {
    val rows = listOf(
        listOf("C", "⌫", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"), listOf("(", "0", ")", ".")
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    val m = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp)
                    val click = { when (k) { "C" -> vm.clear(); "⌫" -> vm.back(); else -> vm.press(k) } }
                    val label = @Composable { Text(k, style = MaterialTheme.typography.titleLarge) }
                    if (k in "÷×-+%") Button(click, m, contentPadding = PaddingValues(0.dp)) { label() }
                    else FilledTonalButton(click, m, contentPadding = PaddingValues(0.dp)) { label() }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button({ vm.compute() }, Modifier.weight(1f).height(72.dp)) { Text("=", style = MaterialTheme.typography.headlineMedium) }
            LargeFloatingActionButton(
                onClick = onMic,
                containerColor = if (vm.listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(Icons.Filled.Mic, contentDescription = if (vm.listening) (if (ar) "إيقاف الاستماع" else "Stop listening") else (if (ar) "ابدأ الحساب بالصوت" else "Start voice input"))
            }
        }
    }
}
