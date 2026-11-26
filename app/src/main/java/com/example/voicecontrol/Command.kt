package com.example.voicecontrol

sealed class Command {
    object ReadScreen : Command()
    data class Tap(val label: String) : Command()
    data class Type(val text: String) : Command()
    data class Scroll(val forward: Boolean) : Command()

    companion object {
        fun parse(spoken: String): Command? {
            val s = spoken.trim()
            val lower = s.lowercase()
            return when {
                lower == "read screen" || lower == "read the screen" -> ReadScreen
                lower.startsWith("tap ") -> Tap(s.substring(4).trim())
                lower.startsWith("type ") -> Type(s.substring(5).trim())
                lower == "scroll" || lower == "scroll down" -> Scroll(true)
                lower == "scroll up" -> Scroll(false)
                else -> null
            }
        }
    }
}
