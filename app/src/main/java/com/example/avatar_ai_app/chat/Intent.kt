package com.example.avatar_ai_app.chat

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
            "what is this",
            "what's this",
            "what am I looking at",
            "identify",
            "recognize",
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
    )
}