package com.example.voicecontrol

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button

class VoiceService : AccessibilityService() {
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var micButton: Button? = null

    override fun onServiceConnected() {
        tts = TextToSpeech(this) {}
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(listener)
        }
        val button = Button(this).apply {
            text = "🎤"
            contentDescription = "Voice command"
            setOnClickListener { listen() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        getSystemService(WindowManager::class.java).addView(button, params)
        micButton = button
    }

    private fun listen() {
        tts?.stop()
        recognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            ).putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        )
    }

    private fun run(command: Command) {
        val root = rootInActiveWindow ?: return say("I cannot see the screen")
        val nodes = visibleNodes(root)
        when (command) {
            Command.ReadScreen -> {
                val words = nodes.filterNot { it.isPassword }
                    .mapNotNull { label(it) }.distinct().joinToString(". ")
                say(words.take(3500).ifEmpty { "Nothing to read" })
            }
            is Command.Tap -> {
                val match = nodes.firstOrNull { label(it).equals(command.label, true) }
                    ?: nodes.firstOrNull { label(it)?.contains(command.label, true) == true }
                var target = match
                while (target != null && !target.isClickable) target = target.parent
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) != true) {
                    say("Could not tap ${command.label}")
                }
            }
            is Command.Type -> {
                val field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (field == null || field.isPassword) return say("Select a text box first")
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, command.text
                    )
                }
                if (!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                    say("Could not type")
                }
            }
            is Command.Scroll -> {
                val action = if (command.forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                if (nodes.none { it.isScrollable && it.performAction(action) }) say("Cannot scroll")
            }
        }
    }

    private fun visibleNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val found = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (node.isVisibleToUser) found.add(node)
            for (i in 0 until node.childCount) node.getChild(i)?.let { visit(it) }
        }
        visit(root)
        return found
    }

    private fun label(node: AccessibilityNodeInfo): String? =
        (node.text ?: node.contentDescription)?.toString()?.takeIf { it.isNotBlank() }

    private fun say(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle) {
            val spoken = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull() ?: return
            val command = Command.parse(spoken)
            if (command == null) say("Unknown command") else run(command)
        }
        override fun onError(error: Int) = say("Voice error $error")
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { tts?.stop() }

    override fun onDestroy() {
        micButton?.let { getSystemService(WindowManager::class.java).removeView(it) }
        recognizer?.destroy()
        tts?.shutdown()
        super.onDestroy()
    }
}
