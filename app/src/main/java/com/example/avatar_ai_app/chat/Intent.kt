package com.example.avatar_ai_app.chat

/**
 * Enum class representing different user intents that the [ChatService] can recognize.
 *
 * Each intent is associated with a list of trigger phrases that users might use to express that intent.
 *
 * @property triggerPhrases A list of trigger phrases associated with the intent.
 * @constructor Creates an instance of the [Intent] enum with its associated trigger phrases.
 */
enum class Intent(val triggerPhrases: List<String>) {
    GREETING(
        listOf(
            "hello",
            "hi",
            "hey",
            "howdy",
            "greetings",
            "good morning",
            "good afternoon",
            "good day",
            "good evening",
            "yo",
            "hiya",
            "sup",
            "hiya",
            "salutations"
        )
    ),
    HELP(
        listOf(
            "help",
            "use you",
            "use this",
            "what can",
            "who are you",
            "how to use",
            "how does this work",
            "can you assist",
            "guide me",
            "assistance",
            "tips",
            "instructions",
            "what can you do",
            "I need help",
            "can you help me",
            "assist me",
            "support",
            "show me how to"
        )
    ),
    NAVIGATION(
        listOf(
            "where",
            "get to",
            "go to",
            "to reach",
            "to find",
            "directions",
            "navigate",
            "way to",
            "take me",
            "route to",
            "path to",
            "where",
            "locate",
            "guide me",
            "lead me",
            "the way"
        )
    ),
    RECOGNITION(
        listOf(
            "identify",
            "recognize",
            "what is this",
            "what's this",
            "what is that",
            "what's that",
            "what am I looking at",
            "what do you see",
            "what's in front",
            "what is in front",
            "what's in the camera",
            "what is in the camera",
            "tell me about this",
            "what's in view",
            "what is in view",
            "what's in sight",
            "what is in sight"
        )
    ),
    INFORMATION(
        listOf(
            "tell me about",
            "what is",
            "what are",
            "how does this work",
            "how do these work",
            "how does this function",
            "how do these function",
            "explain",
            "details",
            "tell me",
            "information on",
            "describe",
            "facts about",
            "details about",
            "features of",
            "characteristics of",
            "functionality of"
        )
    ),
    TOUR(
        listOf(
            "tour",
            "show me around",
            "take me around",
            "guide me around",
            "lead me through",
            "walk me around",
            "show me what's here"
        )
    )
}