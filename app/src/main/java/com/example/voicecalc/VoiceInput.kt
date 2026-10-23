package com.example.voicecalc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceInput(
    ctx: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onEnd: () -> Unit
) {
    private val sr = SpeechRecognizer.createSpeechRecognizer(ctx)

    init {
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(b: Bundle?) { first(b)?.let(onPartial) }
            override fun onResults(b: Bundle?) { first(b)?.let(onFinal); onEnd() }
            override fun onError(error: Int) = onEnd()
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
    }

    private fun first(b: Bundle?) = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    fun start(langTag: String) = sr.startListening(
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    )
    fun stop() = sr.stopListening()
    fun destroy() = sr.destroy()
}
